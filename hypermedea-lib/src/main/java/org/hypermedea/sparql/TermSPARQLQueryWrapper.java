package org.hypermedea.sparql;

import jason.asSyntax.*;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.syntax.*;
import org.hypermedea.common.WrapperException;

/**
 * Wrapper for a SPARQL query built from a Jason logical formula.
 *
 * @author Victor Charpenay
 */
public class TermSPARQLQueryWrapper {

    private final Query sparqlQuery;

    public TermSPARQLQueryWrapper(LogicalFormula phi) {
        Element pattern = parseFormula(phi);

        sparqlQuery = new Query();

        sparqlQuery.setQuerySelectType();
        sparqlQuery.setQueryResultStar(true);
        sparqlQuery.setLimit(10); // TODO make configurable

        sparqlQuery.setQueryPattern(pattern);
    }

    public Query getSPARQLQuery() {
        return sparqlQuery;
    }

    private Element parseFormula(LogicalFormula phi) {
        if (phi instanceof LogExpr) {
            LogExpr expr = (LogExpr) phi;
            LogExpr.LogicalOp op = expr.getOp();

            // TODO reshape (and-BGP vs. and-block, or-or)
            
            if (op.equals(LogExpr.LogicalOp.and)) {
                Element lhs = parseFormula(expr.getLHS());
                Element rhs = parseFormula(expr.getRHS());

                ElementGroup group = new ElementGroup();
                group.addElement(lhs);
                group.addElement(rhs);

                return group;
            } else if (op.equals(LogExpr.LogicalOp.or)) {
                Element lhs = parseFormula(expr.getLHS());
                Element rhs = parseFormula(expr.getRHS());

                ElementUnion union = new ElementUnion();
                union.addElement(lhs);
                union.addElement(rhs);

                return union;
            } else if (op.equals(LogExpr.LogicalOp.not)) {
                Element child = parseFormula(expr.getLHS());
                return new ElementNotExists(child);
            }

            return new ElementTriplesBlock();
        } else if (phi instanceof Pred) {
            Pred p = (Pred) phi;
            Triple t = new TermTriplePatternWrapper(p).getTriplePattern();

            ElementTriplesBlock bgp = new ElementTriplesBlock();
            bgp.addTriple(t);

            return bgp;
        }

        throw new WrapperException(String.format("Cannot parse formula %s (as SPARQL query)", phi));
    }

}
