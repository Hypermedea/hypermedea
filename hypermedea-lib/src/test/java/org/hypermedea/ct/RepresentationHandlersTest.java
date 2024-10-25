package org.hypermedea.ct;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.ct.json.JsonHandler;
import org.hypermedea.ct.json.JsonHandlerTest;
import org.hypermedea.ct.rdf.RDFHandler;
import org.hypermedea.ct.rdf.RDFHandlerTest;
import org.hypermedea.ct.txt.PlainTextHandler;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testJSONTypeMatching() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(JsonHandlerTest.TEST_JSON_OBJECT.getBytes());
        Collection<Literal> t = RepresentationHandlers.deserialize(in, "http://example.org/", "application/td+json");

        String ct = RepresentationHandlers.getDefaultContentType(t);

        assertEquals(ct, JsonHandler.APPLICATION_JSON_CT[0]);
    }

    @Test
    public void testTextTypeMatching() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("abc".getBytes());
        Collection<Literal> t = RepresentationHandlers.deserialize(in, "http://example.org/", "text/unknown");

        String ct = RepresentationHandlers.getDefaultContentType(t);

        assertEquals(ct, PlainTextHandler.TXT_CT[0]);
    }

    @Test
    public void testEmptyRepresentation() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RepresentationHandlers.serialize(new HashSet<>(), out, "http://example.org/");

        assertTrue(out.size() == 0);
    }

}
