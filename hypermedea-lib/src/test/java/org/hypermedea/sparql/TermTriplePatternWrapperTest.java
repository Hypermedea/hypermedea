package org.hypermedea.sparql;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Pred;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.hypermedea.common.WrapperException;
import org.hypermedea.ld.RDFTripleWrapper;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TermTriplePatternWrapperTest {

    public static final String RESOURCE_URI = "http://example.org#alice";

    @Test
    public void testWrappedTriple() {
        Resource alice = ResourceFactory.createResource(RESOURCE_URI);
        Literal name = ResourceFactory.createPlainLiteral("Alice");
        Statement t1 = ResourceFactory.createStatement(alice, FOAF.firstName, name);

        RDFTripleWrapper w1 = new RDFTripleWrapper(t1);

        Object[] args = w1.getPropertyArguments();
        Pred t2 = new Pred(ASSyntax.createLiteral(w1.getPropertyName(), (Term) args[0], (Term) args[1], (Term) args[2]));
        for (Term annot : w1.getPropertyAnnotations()) t2.addAnnot(annot);

        TermTriplePatternWrapper w2 = new TermTriplePatternWrapper(t2);
        Triple t3 = w2.getTriplePattern();

        assertTrue(t1.equals(getStatementFromTriple(t3)));
    }

    @Test
    public void testUnambiguousTriple() {
        StringTerm s = ASSyntax.createString(RESOURCE_URI);
        StringTerm p = ASSyntax.createString(FOAF.age.getURI());
        NumberTerm o = ASSyntax.createNumber(42);

        Pred t = new Pred(ASSyntax.createLiteral("rdf", s, p, o));

        TermTriplePatternWrapper w = new TermTriplePatternWrapper(t);

        assertNotNull(w.getTriplePattern());
    }

    @Test(expected = WrapperException.class)
    public void testAmbiguousTriple() {
        StringTerm s = ASSyntax.createString(RESOURCE_URI);
        StringTerm p = ASSyntax.createString(FOAF.firstName.getURI());
        StringTerm o = ASSyntax.createString("Alice");

        Pred t = new Pred(ASSyntax.createLiteral("rdf", s, p, o));

        TermTriplePatternWrapper w = new TermTriplePatternWrapper(t);
    }

    @Test
    public void testTriplePattern() {
        VarTerm s = ASSyntax.createVar("Alice");
        StringTerm p = ASSyntax.createString(FOAF.firstName.getURI());
        VarTerm o = ASSyntax.createVar("Name");

        Pred t = new Pred(ASSyntax.createLiteral("rdf", s, p, o));

        TermTriplePatternWrapper w = new TermTriplePatternWrapper(t);

        assertNotNull(w.getTriplePattern());
    }

    // TODO test arbitrary predicates

    private Statement getStatementFromTriple(Triple t) {
        Graph g = GraphFactory.createPlainGraph();
        g.add(t);

        Model m = ModelFactory.createModelForGraph(g);
        return m.listStatements().nextStatement();
    }

}
