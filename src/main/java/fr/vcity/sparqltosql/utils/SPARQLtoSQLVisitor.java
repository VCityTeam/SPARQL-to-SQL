package fr.vcity.sparqltosql.utils;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.op.*;

@Getter
@Slf4j
public class SPARQLtoSQLVisitor implements OpVisitor {

    private static final String GRAPH_NAME_PATTERN = "/GraphName#";
    private static final String VERSION_PATTERN = "/Version#";

    private String generatedSQL = "";

    /**
     * gets the list of triple from the Basic Graph Pattern
     *
     * @param opBGP the Basic Graph Pattern operator
     */
    @Override
    public void visit(OpBGP opBGP) {
        log.info("Visiting OpBGP size: {}", opBGP.getPattern().size());
        opBGP
                .getPattern()
                .getList()
                .forEach(triple -> log.info("    Triple: {}", triple.toString()));
    }

    /**
     * @param quadPattern
     */
    @Override
    public void visit(OpQuadPattern quadPattern) {
        log.info("Visiting OpQuadPattern");
    }

    /**
     * @param quadBlock
     */
    @Override
    public void visit(OpQuadBlock quadBlock) {
        log.info("Visiting OpQuadBlock");
    }

    /**
     * @param opTriple
     */
    @Override
    public void visit(OpTriple opTriple) {
        log.info("Visiting OpTriple");
    }

    /**
     * @param opQuad
     */
    @Override
    public void visit(OpQuad opQuad) {
        log.info("Visiting OpQuad");
    }

    /**
     * @param opPath
     */
    @Override
    public void visit(OpPath opPath) {
        log.info("Visiting OpPath");
    }

    /**
     * @param opTable
     */
    @Override
    public void visit(OpTable opTable) {
        log.info("Visiting OpTable");
    }

    /**
     * @param opNull
     */
    @Override
    public void visit(OpNull opNull) {
        log.info("Visiting OpNull");
    }

    /**
     * @param opProc
     */
    @Override
    public void visit(OpProcedure opProc) {
        log.info("Visiting OpProcedure");
    }

    /**
     * @param opPropFunc
     */
    @Override
    public void visit(OpPropFunc opPropFunc) {
        log.info("Visiting OpPropFunc");
    }

    /**
     * @param opFilter
     */
    @Override
    public void visit(OpFilter opFilter) {
        log.info("Visiting OpFilter Expr size: {}", opFilter.getExprs().size());
        opFilter
                .getExprs()
                .forEach(expr -> log.info("Expr: {}", expr.toString()));
    }

    /**
     * gets the validity String from graph name
     *
     * @param opGraph the Graph Operator
     */
    @Override
    public void visit(OpGraph opGraph) {
        String nodeString = opGraph.getNode().toString();
        log.info("Visiting OpGraph: {}", nodeString);

        if (nodeString.contains(VERSION_PATTERN)) {
            log.info("V: {}", getAnchorValueFromURI(nodeString));
        } else if (nodeString.contains(GRAPH_NAME_PATTERN)) {
            log.info("NG: {}", getAnchorValueFromURI(nodeString));
        }
    }

    /**
     * @param opService
     */
    @Override
    public void visit(OpService opService) {
        log.info("Visiting OpService");
    }

    /**
     * @param dsNames
     */
    @Override
    public void visit(OpDatasetNames dsNames) {
        log.info("Visiting OpDatasetNames");
    }

    /**
     * @param opLabel
     */
    @Override
    public void visit(OpLabel opLabel) {
        log.info("Visiting OpLabel");
    }

    /**
     * @param opAssign
     */
    @Override
    public void visit(OpAssign opAssign) {
        log.info("Visiting OpAssign");
    }

    /**
     * @param opExtend
     */
    @Override
    public void visit(OpExtend opExtend) {

        log.info("Visiting OpExtend");
    }

    /**
     * @param opJoin
     */
    @Override
    public void visit(OpJoin opJoin) {
        log.info("Visiting OpJoin");
        log.info("    opJoin Left: {}", opJoin.getLeft().getName());
        log.info("    opJoin Right: {}", opJoin.getRight().getName());
    }

    /**
     * @param opLeftJoin
     */
    @Override
    public void visit(OpLeftJoin opLeftJoin) {
        log.info("Visiting OpLeftJoin");
    }

    /**
     * @param opUnion
     */
    @Override
    public void visit(OpUnion opUnion) {
        log.info("Visiting OpUnion");
    }

    /**
     * @param opDiff
     */
    @Override
    public void visit(OpDiff opDiff) {
        log.info("Visiting OpDiff");
    }

    /**
     * @param opMinus
     */
    @Override
    public void visit(OpMinus opMinus) {
        log.info("Visiting OpMinus");
    }

    /**
     * @param opLateral
     */
    @Override
    public void visit(OpLateral opLateral) {
        log.info("Visiting OpLateral");
    }

    /**
     * @param opCondition
     */
    @Override
    public void visit(OpConditional opCondition) {
        log.info("Visiting OpConditional");
    }

    /**
     * @param opSequence
     */
    @Override
    public void visit(OpSequence opSequence) {
        log.info("Visiting OpSequence");
    }

    /**
     * @param opDisjunction
     */
    @Override
    public void visit(OpDisjunction opDisjunction) {
        log.info("Visiting OpDisjunction");
    }

    /**
     * @param opList
     */
    @Override
    public void visit(OpList opList) {
        log.info("Visiting OpList");
    }

    /**
     * @param opOrder
     */
    @Override
    public void visit(OpOrder opOrder) {
        log.info("Visiting OpOrder");
    }

    /**
     * gets the variable list from the SELECT statement
     *
     * @param opProject The project operator
     */
    @Override
    public void visit(OpProject opProject) {
        log.info("Visiting OpProject size: {}", opProject.getVars().size());
        opProject
                .getVars()
                .forEach(variable -> log.info("    Var: {}", variable.getVarName()));
    }

    /**
     * @param opReduced
     */
    @Override
    public void visit(OpReduced opReduced) {
        log.info("Visiting OpReduced");
    }

    /**
     * @param opDistinct
     */
    @Override
    public void visit(OpDistinct opDistinct) {
        log.info("Visiting OpDistinct");
    }

    /**
     * @param opSlice
     */
    @Override
    public void visit(OpSlice opSlice) {
        log.info("Visiting OpSlice");
    }

    /**
     * gets the aggregator and var list
     *
     * @param opGroup the group operator
     */
    @Override
    public void visit(OpGroup opGroup) {
        log.info("Visiting OpGroup var size: {}", opGroup.getGroupVars().size());
        opGroup
                .getGroupVars()
                .getVars()
                .forEach(variable -> log.info("    Var: {}", variable.getVarName()));
        log.info("Visiting OpGroup aggregator size: {}", opGroup.getAggregators().size());
        opGroup
                .getAggregators()
                .forEach(exprAggregator -> {
                    log.info("    Aggregator: {} ", exprAggregator.getAggregator().getName());
                    log.info("    Associated aggregator expression size: {}", exprAggregator.getAggregator().getExprList().size());
                    exprAggregator
                            .getAggregator()
                            .getExprList()
                            .forEach(expr -> log.info("        Agg {} Expression: {}", exprAggregator.getAggregator().getName(), expr.getVarName()));
                });
    }

    /**
     * @param opTopN
     */
    @Override
    public void visit(OpTopN opTopN) {
        log.info("Visiting OpTopN");
    }

    private static String getAnchorValueFromURI(String uri) {
        return uri.substring(uri.indexOf('#') + 1);
    }
}
