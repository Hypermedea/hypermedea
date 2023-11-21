package org.hypermedea.ct;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.ct.rdf.RDFHandler;
import org.hypermedea.ct.rdf.RDFHandlerTest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class RepresentationHandlersTest {

    @Test
    public void testLoadFromFunctor() throws ParseException, IOException {
        ListTerm l = ASSyntax.parseList(RDFHandlerTest.TEST_RDF_TERM);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RepresentationHandlers.serialize(List.of((Literal) l.get(0)), out, "http://example.org/");

        assertTrue(out.size() > 0);
    }

    @Test
    public void testLoadFromContentType() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(RDFHandlerTest.TEST_RDF_GRAPH.getBytes());
        Collection<Literal> t = RepresentationHandlers.deserialize(in, "http://example.org/", "text/turtle");

        assertTrue(t.size() > 0);
    }

    @Test
    public void testGetDefaultContentType() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(RDFHandlerTest.TEST_RDF_GRAPH.getBytes());
        Collection<Literal> t = RepresentationHandlers.deserialize(in, "http://example.org/", "text/turtle");

        String ct = RepresentationHandlers.getDefaultContentType(t);

        assertEquals(ct, RDFHandler.RDF_CT[0]);
    }

}
