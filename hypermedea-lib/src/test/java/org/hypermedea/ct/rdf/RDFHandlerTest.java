package org.hypermedea.ct.rdf;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RDFHandlerTest {

    public static final String TEST_RDF_TERM = "[ rdf(\"http://example.org/alice\", \"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\", \"http://example.org/Person\") [ rdf_type_map(uri, uri, uri) ] ,\n" +
            " rdf(\"http://example.org/alice\", \"http://example.org/name\", \"Alice\") [ rdf_type_map(uri, uri, literal) ] ,\n" +
            " rdf(\"http://example.org/alice\", \"http://example.org/age\", 42) [ rdf_type_map(uri, uri, literal) ] ,\n" +
            " rdf(\"http://example.org/alice\", \"http://example.org/knows\", someone) [ rdf_type_map(uri, uri, bnode) ] ]";

    public static final String TEST_RDF_GRAPH = "@prefix ex: <http://example.org/> .\n" +
            "ex:alice a ex:Person .\n" +
            " ex:alice ex:name \"Alice\" .\n" +
            " ex:alice ex:age 42 .\n" +
            " ex:alice ex:knows _:someone .";

    private final RDFHandler h = new RDFHandler();

    @Test
    public void testSerialize() throws ParseException {
        ListTerm l = ASSyntax.parseList(TEST_RDF_TERM);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Collection<Literal> terms = new HashSet<>();
        for (Term t : l.getAsList()) terms.add((Literal) t);

        h.serialize(terms, out, "http://example.org/");
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        Model m = ModelFactory.createDefaultModel();
        m.read(in, "http://example.org/", "text/turtle");

        assertEquals(4, m.size());

        Model expected = ModelFactory.createDefaultModel();
        expected.read(new StringReader(TEST_RDF_GRAPH), null, "TTL");

        assertTrue(m.getGraph().isIsomorphicWith(expected.getGraph()));
    }

    @Test
    public void testDeserialize() throws UnsupportedEncodingException, ParseException {
        ByteArrayInputStream in = new ByteArrayInputStream(TEST_RDF_GRAPH.getBytes("UTF-8"));

        Collection<Literal> terms = h.deserialize(in, "http://example.org/", "text/turtle");

        assertEquals(4, terms.size());

        ListTerm expected = ASSyntax.parseList(TEST_RDF_TERM);

        Collection<Term> termsWithoutAnnot = new HashSet<>();
        termsWithoutAnnot.addAll(terms);
        ListTerm actual = ASSyntax.createList(termsWithoutAnnot);

        // note: only the bnode differs between expected/actual
        assertEquals(1, expected.difference(actual).size());
        assertEquals(1, actual.difference(expected).size());
    }

}
