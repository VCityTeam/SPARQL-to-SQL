package fr.cnrs.liris.jpugetgil.sparqltosql;

import fr.cnrs.liris.jpugetgil.sparqltosql.hibernate.HibernateSessionSingleton;
import fr.cnrs.liris.jpugetgil.sparqltosql.hibernate.JdbcConnection;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VersioningQueryExecution implements QueryExecution {

    private static final Logger log = LoggerFactory.getLogger(VersioningQueryExecution.class);

    private final Query query;

    private final SessionFactory sessionFactory;

    private final JdbcConnection jdbcConnection;

    public VersioningQueryExecution(Query query) {
        this.query = query;
        this.sessionFactory = HibernateSessionSingleton.getInstance().getSessionFactory();
        this.jdbcConnection = JdbcConnection.getInstance();
    }

    @Override
    public void setInitialBinding(QuerySolution binding) {

    }

    @Override
    public void setInitialBinding(Binding binding) {

    }

    @Override
    public Dataset getDataset() {
        return null;
    }

    @Override
    public Context getContext() {
        return Context.emptyContext();
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public String getQueryString() {
        return query.toString();
    }

    @Override
    public org.apache.jena.query.ResultSet execSelect() {
        SPARQLtoSQLTranslator translator = new SPARQLtoSQLTranslator(sessionFactory);
        String sqlQuery = translator.translate(query);

        try (ResultSet rs = jdbcConnection.executeSQL(sqlQuery)) {
            List<Var> vars = new ArrayList<>();
            List<Binding> bindings = new ArrayList<>();

            while (Objects.requireNonNull(rs).next()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int nbColumns = rsmd.getColumnCount();
                for (int i = 1; i <= nbColumns; i++) {
                    String columnName = rsmd.getColumnName(i);
                    Var variable = Var.alloc(columnName);
                    Node variableValue;
                    if (rs.getString(columnName) != null) {
                        // FIXME : Handle resource or literal type
                        variableValue = NodeFactory.createLiteral(rs.getString(columnName));
                        if (!vars.contains(variable)) {
                            vars.add(variable);
                        }
                        Binding binding = Binding.builder().add(variable, variableValue).build();
                        bindings.add(binding);
                    }
                }
            }

            return ResultSetStream.create(vars, bindings.iterator());

        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public Model execConstruct() {
        return null;
    }

    @Override
    public Model execConstruct(Model model) {
        return null;
    }

    @Override
    public Iterator<Triple> execConstructTriples() {
        return null;
    }

    @Override
    public Iterator<Quad> execConstructQuads() {
        return null;
    }

    @Override
    public Dataset execConstructDataset() {
        return null;
    }

    @Override
    public Dataset execConstructDataset(Dataset dataset) {
        return null;
    }

    @Override
    public Model execDescribe() {
        return null;
    }

    @Override
    public Model execDescribe(Model model) {
        return null;
    }

    @Override
    public Iterator<Triple> execDescribeTriples() {
        return null;
    }

    @Override
    public boolean execAsk() {
        return false;
    }

    @Override
    public JsonArray execJson() {
        return null;
    }

    @Override
    public Iterator<JsonObject> execJsonItems() {
        return null;
    }

    @Override
    public void abort() {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void setTimeout(long timeout, TimeUnit timeoutUnits) {

    }

    @Override
    public void setTimeout(long timeout) {

    }

    @Override
    public void setTimeout(long timeout1, TimeUnit timeUnit1, long timeout2, TimeUnit timeUnit2) {

    }

    @Override
    public void setTimeout(long timeout1, long timeout2) {

    }

    @Override
    public long getTimeout1() {
        return 0;
    }

    @Override
    public long getTimeout2() {
        return 0;
    }
}
