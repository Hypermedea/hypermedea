package org.hypermedea.sparql;

import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.common.WrapperException;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TermSPARQLQueryWrapperTest {

    @Test
    public void testFormula() throws ParseException {
        LogicalFormula phi = ASSyntax.parseFormula("rdf(S, P, O) & rdf(S, Q, O)");

        TermSPARQLQueryWrapper w = new TermSPARQLQueryWrapper(phi);
        assertNotNull(w.getSPARQLQuery());
    }

    @Test(expected = WrapperException.class)
    public void testUnknownTerm() throws ParseException {
        LogicalFormula phi = ASSyntax.parseFormula("other_pred(S, P, O) & rdf(S, Q, O)");

        TermSPARQLQueryWrapper w = new TermSPARQLQueryWrapper(phi);
    }
}
