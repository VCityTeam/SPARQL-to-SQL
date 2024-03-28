package fr.cnrs.liris.jpugetgil.sparqltosql;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.dao.ResourceOrLiteral;
import fr.cnrs.liris.jpugetgil.sparqltosql.dao.VersionedNamedGraph;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLClause;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator.EqualToOperator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator.NotEqualToOperator;
import org.apache.jena.graph.*;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used to translate a SPARQL query into a SQL query
 */
public class SPARQLtoSQLTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLtoSQLTranslator.class);

    private final Session session;

    private final Map<String, Integer> uriToIdMap = new HashMap<>();

    public SPARQLtoSQLTranslator(SessionFactory sessionFactory) {
        this.session = sessionFactory.openSession();
    }

    /**
     * Translate the SPARQL query into a SQL query
     *
     * @return the SQL query
     */
    public String translate(Query query) {
        Op op = Algebra.compile(query);

        SQLQuery qu = buildSPARQLContext(op);
        log.info(qu.getSql());

        return qu.getSql();
    }

    private SQLQuery buildSPARQLContext(Op op) {
        return buildSPARQLContext(op, new SQLContext(null, new HashMap<>()));
    }

    private SQLQuery buildSPARQLContext(Op op, SQLContext context) {

        return switch (op) {
            case OpJoin opJoin -> {
                SQLQuery leftSQLQuery = buildSPARQLContext(opJoin.getLeft(), context);
                SQLQuery rightSQLQuery = buildSPARQLContext(opJoin.getRight(), context);
                yield buildSQLQueryJoin(leftSQLQuery, rightSQLQuery);
            }
            case OpLeftJoin opLeftJoin -> {
                buildSPARQLContext(opLeftJoin.getLeft(), context);
                buildSPARQLContext(opLeftJoin.getRight(), context);
                yield null;
            }
            case OpUnion opUnion -> {
//                buildSelectSQL(opUnion.getLeft(), context);
//                buildSelectSQL(opUnion.getRight(), context);
                yield null;
            }
            case OpProject opProject -> {
//                context.setProjections(opProject.getVars());
                yield buildSPARQLContext(opProject.getSubOp(), context);
            }
            case OpTable opTable -> {
                opTable.getTable().toString();
                yield null;
            }
            case OpQuadPattern opQuadPattern -> {
                opQuadPattern.getGraphNode().getURI();
                yield null;
            }
            case OpExtend opExtend -> {
                yield buildSPARQLContext(opExtend.getSubOp(), null);
            }
            case OpDistinct opDistinct -> {
                yield buildSPARQLContext(opDistinct.getSubOp(), null);
            }
            case OpFilter opFilter -> {
                yield buildSPARQLContext(opFilter.getSubOp(), null);
            }
            case OpOrder opOrder -> {
                yield buildSPARQLContext(opOrder.getSubOp(), null);
            }
            case OpGroup opGroup -> {
                yield buildSPARQLContext(opGroup.getSubOp(), null);
            }
            case OpSlice opSlice -> {
                yield buildSPARQLContext(opSlice.getSubOp(), null);
            }
            case OpTopN opTopN -> {
                yield buildSPARQLContext(opTopN.getSubOp(), null);
            }
            case OpPath opPath -> {
                yield null;
            }
            case OpLabel opLabel -> {
                yield buildSPARQLContext(opLabel.getSubOp(), null);
            }
            case OpNull ignored -> {
                yield buildSPARQLContext(null, context);
            }
            case OpList opList -> {
                yield buildSPARQLContext(opList.getSubOp(), null);
            }
            case OpBGP opBGP -> {
                SQLContext cont = addURIsToContext(opBGP, context);
                addURIsToMap(opBGP);
                getURIsIds();

                if (cont.graph() != null && cont.graph() instanceof Node_Variable) {
                    yield buildContextBGPWithGraph(opBGP, cont);
                } else if (cont.graph() != null && cont.graph() instanceof Node_URI) {
                    yield buildContextBGPWithGraphURI(opBGP, cont);
                } else {
                    yield buildContextBGPWorkspace(opBGP, cont);
                }
            }
            case OpGraph opGraph -> {
                Map<Node, List<Occurrence>> newVarOccurrences = new HashMap<>(context.varOccurrences());

                Integer count = newVarOccurrences
                        .keySet()
                        .stream()
                        .filter(
                                node -> newVarOccurrences.get(node).stream().anyMatch(occurrence -> occurrence.getType().equals("graph"))
                        )
                        .map(node -> 1)
                        .reduce(0, Integer::sum, Integer::sum);

                newVarOccurrences
                        .computeIfAbsent(opGraph.getNode(), k -> new ArrayList<>())
                        .add(new Occurrence("graph", count));

                SQLContext cont = context.setGraph(opGraph.getNode());
                cont = cont.setVarOccurrences(newVarOccurrences);
                SQLQuery sqlQuery = buildSPARQLContext(opGraph.getSubOp(), cont);

                sqlQuery = buildSQLQueryWithGraph(sqlQuery, count);

                yield sqlQuery;
            }
            case OpTriple opTriple -> {
//                context.getTriples().add(opTriple.getTriple());
//                return context.buildSQL();
                yield null;
            }
            default -> throw new IllegalArgumentException("TODO: Unknown operator " + op.getClass().getName());
        };
    }

    /**
     * Build the SQL query in an URI graph context
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SQL query
     * @return the built SQL query
     */
    private SQLQuery buildContextBGPWithGraphURI(OpBGP opBGP, SQLContext context) {
        String select = generateSelect(opBGP, context);
        String tables = generateFromTables(opBGP, false);
        return getSqlQuery(opBGP, context, select, tables, false);
    }

    /**
     * Collect the occurrences of the variables in the BGP
     *
     * @param opBGP   the current BGP
     * @param context the current SQL context
     * @return the modified SQL context
     */
    private SQLContext addURIsToContext(OpBGP opBGP, SQLContext context) {
        Map<Node, List<Occurrence>> newVarOccurrences = new HashMap<>(context.varOccurrences());

        for (int i = 0; i < opBGP.getPattern().getList().size(); i++) {
            Triple triple = opBGP.getPattern().getList().get(i);
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();

            newVarOccurrences.computeIfAbsent(subject, k -> new ArrayList<>()).add(new Occurrence("subject", i));
            newVarOccurrences.computeIfAbsent(predicate, k -> new ArrayList<>()).add(new Occurrence("predicate", i));
            newVarOccurrences.computeIfAbsent(object, k -> new ArrayList<>()).add(new Occurrence("object", i));

            context = context.setVarOccurrences(newVarOccurrences);
        }

        return context;
    }

    /**
     * Add URI to the idMap and retrieve ids later
     *
     * @param opBGP the current BGP
     */
    private void addURIsToMap(OpBGP opBGP) {
        for (int i = 0; i < opBGP.getPattern().getList().size(); i++) {
            Triple triple = opBGP.getPattern().getList().get(i);
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();

            if (subject instanceof Node_URI) {
                uriToIdMap.put(subject.getURI(), null);
            }
            if (predicate instanceof Node_URI) {
                uriToIdMap.put(predicate.getURI(), null);
            }
            if (object instanceof Node_URI) {
                uriToIdMap.put(object.getURI(), null);
            }
            if (object instanceof Node_Literal) {
                uriToIdMap.put(object.getLiteralLexicalForm(), null);
            }
        }
    }

    /**
     * Build the SQL query in a workspace context (no graph)
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SQL query
     * @return the SQL query
     */
    private SQLQuery buildContextBGPWorkspace(OpBGP opBGP, SQLContext context) {
        String select = generateSelectWorkspace(context);
        String tables = generateFromTables(opBGP, true);
        return getSqlQuery(opBGP, context, select, tables, true);
    }

    /**
     * Build the SQL query in a graph context
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SQL query
     * @return the SQL query
     */
    private SQLQuery buildContextBGPWithGraph(OpBGP opBGP, SQLContext context) {
        String select = generateSelect(opBGP, context);
        String tables = generateFromTables(opBGP, false);
        return getSqlQuery(opBGP, context, select, tables, false);
    }

    /**
     * Build the SQL query with the graph
     *
     * @param sqlQuery the SQL query
     * @param position the position of the graph
     * @return the SQL query with the graph
     */
    private SQLQuery buildSQLQueryWithGraph(SQLQuery sqlQuery, Integer position) {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        String from = " FROM (" + sqlQuery.getSql() + ") sq" + position;

        if (sqlQuery.getContext().graph() instanceof Node_Variable) {
            String select = "SELECT " + sqlQuery.getContext().varOccurrences().keySet() // FIXME: Hardcoded
                    .stream()
                    .filter(node -> node instanceof Node_Variable)
                    .filter(node -> !node.equals(sqlQuery.getContext().graph()))
                    .map(node -> "sq" + position + ".v$" + node.getName())
                    .collect(Collectors.joining(", ")) +
                    ", sq" + position + ".ng$" + sqlQuery.getContext().graph().getName() + ", sq" + position + ".bs$" +
                    sqlQuery.getContext().graph().getName();

            return new SQLQuery(
                    select + from,
                    sqlQuery.getContext()
            );
        } else {
            String select = "SELECT sq" + position + ".ng$graph, sq" + position + ".bs$graph," + // FIXME: Hardcoded
                    sqlQuery.getContext().varOccurrences().keySet()
                            .stream()
                            .filter(node -> node instanceof Node_Variable)
                            .map(node -> "sq" + position + ".v$" + node.getName())
                            .collect(Collectors.joining(", "));

            VersionedNamedGraph versionedNamedGraph = getAssociatedVNG(sqlQuery.getContext().graph().getURI());

            return new SQLQuery(
                    select + from + "\n JOIN versioned_named_graph vng" + position + " ON " +
                            sqlClauseBuilder.and(
                                    new EqualToOperator()
                                            .buildComparisonOperatorSQL(
                                                    "sq" + position + ".ng$graph",
                                                    "vng" + position + ".id_named_graph"
                                            )
                            ).and(
                                    new EqualToOperator()
                                            .buildComparisonOperatorSQL(
                                                    "get_bit(sq" + position + ".bs$graph, vng" + position + ".index_version)",
                                                    "1"
                                            )
                            ).and(
                                    new EqualToOperator()
                                            .buildComparisonOperatorSQL(
                                                    "vng" + position + ".index_version",
                                                    String.valueOf(versionedNamedGraph.getIndex())
                                            )
                            ).and(
                                    new EqualToOperator()
                                            .buildComparisonOperatorSQL(
                                                    "vng" + position + ".id_named_graph",
                                                    String.valueOf(versionedNamedGraph.getIdNamedGraph())
                                            )
                            ).build().clause,
                    sqlQuery.getContext()
            );
        }
    }



    /**
     * Build the SQL query with the join
     *
     * @param leftSQLQuery  the left SQL query
     * @param rightSQLQuery the right SQL query
     * @return the SQL query with the joined terms
     */
    private SQLQuery buildSQLQueryJoin(SQLQuery leftSQLQuery, SQLQuery rightSQLQuery) {
        List<Node> commonNodes = new ArrayList<>();
        leftSQLQuery.getContext().varOccurrences().keySet().stream().filter(node -> node instanceof Node_Variable).forEach(node -> {
            if (rightSQLQuery.getContext().varOccurrences().containsKey(node)) {
                commonNodes.add(node);
            }
        });

        // TODO : Handle JOINs
        String sql = "SELECT * FROM (" + leftSQLQuery.getSql() + ") left1, (" + rightSQLQuery.getSql() + ") right1";
        return new SQLQuery(sql, null);
    }


    /**
     * Get the SQL query
     *
     * @param opBGP       the BGP operator of the SPARQL query
     * @param context     the context of the SQL query
     * @param select      the SELECT clause of the SQL query
     * @param tables      the FROM clause of the SQL query
     * @param isWorkspace true if the query is in a workspace context, false otherwise
     * @return the SQL query
     */
    private SQLQuery getSqlQuery(OpBGP opBGP, SQLContext context, String select, String tables, boolean isWorkspace) {
        StringBuilder query = new StringBuilder("SELECT " + select + " FROM " + tables);

        String where;
        if (isWorkspace) {
            where = generateWhereWorkspace(opBGP);
        } else {
            where = generateWhere(opBGP, context);
        }
        if (!where.isEmpty()) {
            query.append(" WHERE ").append(where);
        }

        return new SQLQuery(query.toString(), context);
    }

    /**
     * Generate the SELECT clause of the SQL query
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SPARQL query
     * @return the SELECT clause of the SQL query
     */
    private String generateSelect(OpBGP opBGP, SQLContext context) {
        if (context.graph() instanceof Node_Variable) {
            return intersectionValidity(opBGP) + " as bs$" + context.graph().getName() + ", " + getSelectVariables(context);
        } else {
            return intersectionValidity(opBGP) + " as bs$graph, t0.id_named_graph as ng$graph, " + // FIXME: Hardcoded
                    getSelectVariables(context);
        }
    }

    /**
     * Get the SELECT clause of the SQL query
     *
     * @param context the context of the SPARQL query
     * @return the SELECT clause of the SQL query
     */
    private String getSelectVariables(SQLContext context) {
        return Streams.mapWithIndex(context.varOccurrences().keySet().stream()
                .filter(node -> node instanceof Node_Variable), (node, index) -> {
            if (context.varOccurrences().get(node).getFirst().getType().equals("graph")) {
                return (
                        "t" + context.varOccurrences().get(node).getFirst().getPosition() +
                                ".id_named_graph as ng$" + node.getName()
                );
            }
            return (
                    "t" + context.varOccurrences().get(node).getFirst().getPosition() + "." +
                            getColumnByOccurrence(context.varOccurrences().get(node).getFirst()) +
                            " as v$" + node.getName()
            );
        }).collect(Collectors.joining(", \n"));
    }

    private static String intersectionValidity(OpBGP opBGP) {
        return "(" + Streams.mapWithIndex(opBGP.getPattern().getList().stream(), (triple, index) ->
                "t" + index + ".validity"
        ).collect(Collectors.joining(" & ")) + ")";
    }

    /**
     * Return the column name of the SQL query according to the occurrence type
     *
     * @param occurrence the occurrence of the Node
     * @return the column name of the versioned quad table
     */
    private String getColumnByOccurrence(Occurrence occurrence) {
        return switch (occurrence.getType()) {
            case "subject" -> "id_subject";
            case "predicate" -> "id_property";
            case "object" -> "id_object";
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Generate the SELECT clause of the SQL query in a workspace context
     *
     * @param context the context of the SPARQL query
     * @return the SELECT clause of the SQL query
     */
    private String generateSelectWorkspace(SQLContext context) {
        return Streams.mapWithIndex(context.varOccurrences().keySet().stream()
                .filter(node -> node instanceof Node_Variable), (node, index) ->
                "t" + context.varOccurrences().get(node).getFirst().getPosition() +
                        "." + getColumnByOccurrence(context.varOccurrences().get(node).getFirst()) +
                        " as t$" + node.getName()
        ).collect(Collectors.joining(", \n"));
    }

    /**
     * Generate the FROM clause of the SQL query
     *
     * @param opBGP       the BGP operator of the SPARQL query
     * @param isWorkspace true if the query is in a workspace context, false otherwise
     * @return the FROM clause of the SQL query
     */
    private String generateFromTables(OpBGP opBGP, boolean isWorkspace) {
        return Streams.mapWithIndex(opBGP.getPattern().getList().stream(), (triple, index) -> {
            if (isWorkspace) {
                return ("workspace t" + index);
            } else {
                return ("versioned_quad t" + index);
            }
        }).collect(Collectors.joining(", "));
    }

    /**
     * Generate the WHERE clause of the SQL query with a graph variable
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SPARQL query
     * @return the WHERE clause of the SQL query
     */
    private String generateWhere(OpBGP opBGP, SQLContext context) {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        String idSelect = "";
        List<Triple> triples = opBGP.getPattern().getList();

        if (context.graph() instanceof Node_Variable) {
            sqlClauseBuilder = sqlClauseBuilder.and(
                    new NotEqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "bit_count" + intersectionValidity(opBGP),
                                    "0"
                            )
            );
        }

        for (int i = 0; i < triples.size(); i++) {
            switch (context.graph()) {
                case Node_Variable ignored -> {
                    // where
                    if (i < triples.size() - 1) {
                        sqlClauseBuilder = sqlClauseBuilder.and(
                                new EqualToOperator()
                                        .buildComparisonOperatorSQL(
                                                "t" + i + ".id_named_graph",
                                                "t" + (i + 1) + ".id_named_graph")
                        );
                    }
                }
                case Node_URI nodeUri -> {
                    // where
                    VersionedNamedGraph versionedNamedGraph = getAssociatedVNG(nodeUri.getURI());
                    sqlClauseBuilder = sqlClauseBuilder.and(
                            new EqualToOperator().buildComparisonOperatorSQL(
                                    "t" + i + ".id_named_graph",
                                    String.valueOf(versionedNamedGraph.getIdNamedGraph())
                            )
                    );

                    sqlClauseBuilder = sqlClauseBuilder.and(
                            new EqualToOperator()
                                    .buildComparisonOperatorSQL(
                                            "get_bit(t" + i + ".validity," + versionedNamedGraph.getIndex() + ")",
                                            "1"
                                    )
                    );
                }
                default -> throw new IllegalStateException("Unexpected value: " + context.graph());
            }

            sqlClauseBuilder.and(buildFiltersOnIds(triples, i));
        }

        return sqlClauseBuilder.and(idSelect).build().clause;
    }

    /**
     * Build the filters on the IDs of the triple
     *
     * @param triples the list of triples
     * @param i       the index of the current triple
     * @return the filters on the IDs of the triple
     */
    private String buildFiltersOnIds(List<Triple> triples, int i) {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        Node subject = triples.get(i).getSubject();
        Node predicate = triples.get(i).getPredicate();
        Node object = triples.get(i).getObject();

        if (subject instanceof Node_URI) {
            sqlClauseBuilder.and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "t" + i + ".id_subject",
                                    String.valueOf(uriToIdMap.get(subject.getURI()))
                            )
            );
        }
        if (predicate instanceof Node_URI) {
            sqlClauseBuilder.and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "t" + i + ".id_property",
                                    String.valueOf(uriToIdMap.get(predicate.getURI()))
                            )
            );
        }
        if (object instanceof Node_URI) {
            sqlClauseBuilder.and(
                    new EqualToOperator().buildComparisonOperatorSQL(
                            "t" + i + ".id_object",
                            String.valueOf(uriToIdMap.get(object.getURI()))
                    )
            );
        } else if (object instanceof Node_Literal) {
            sqlClauseBuilder.and(
                    new EqualToOperator()
                            .buildComparisonOperatorSQL(
                                    "t" + i + ".id_object",
                                    String.valueOf(uriToIdMap.get(object.getLiteralLexicalForm()))
                            )
            );
        }

        return sqlClauseBuilder.build().clause;
    }

    private VersionedNamedGraph getAssociatedVNG(String uri) {
        Transaction tx = session.beginTransaction();
        VersionedNamedGraph versionedNamedGraph = session.createQuery(
                        "from VersionedNamedGraph vng join ResourceOrLiteral rl " +
                                "on vng.idVersionedNamedGraph = rl.idResourceOrLiteral where rl.name = :uri",
                        VersionedNamedGraph.class
                )
                .setParameter("uri", uri)
                .getSingleResult();
        tx.commit();

        return versionedNamedGraph;
    }

    /**
     * Generate the WHERE clause of the SQL query in a workspace context
     *
     * @param opBGP the BGP operator of the SPARQL query
     * @return the WHERE clause of the SQL query
     */
    private String generateWhereWorkspace(OpBGP opBGP) {
        SQLClause.SQLClauseBuilder sqlClauseBuilder = new SQLClause.SQLClauseBuilder();
        StringBuilder idSelect = new StringBuilder();
        List<Triple> triples = opBGP.getPattern().getList();

        for (int i = 0; i < triples.size(); i++) {
            idSelect.append(buildFiltersOnIds(triples, i));
        }

        return sqlClauseBuilder.and(idSelect.toString()).build().clause;
    }

    /**
     * Fills the map of URIs with their IDs
     */
    private void getURIsIds() {
        if (!uriToIdMap.isEmpty()) {
            Transaction tx = session.beginTransaction();
            List<ResourceOrLiteral> resourceOrLiterals =
                    session.createSelectionQuery("from ResourceOrLiteral where name in :names", ResourceOrLiteral.class)
                            .setParameter("names", uriToIdMap.keySet())
                            .getResultList();
            tx.commit();

            resourceOrLiterals.forEach(resourceOrLiteral -> uriToIdMap.put(resourceOrLiteral.getName(), resourceOrLiteral.getIdResourceOrLiteral()));
        }
    }
}
