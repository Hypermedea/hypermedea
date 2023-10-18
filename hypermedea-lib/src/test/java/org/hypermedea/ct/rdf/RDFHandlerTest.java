package org.hypermedea.ct.rdf;

import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;

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

        assert m.size() == 4;

        // TODO further test
    }

    @Test
    public void testDeserialize() throws UnsupportedEncodingException {
        ByteArrayInputStream in = new ByteArrayInputStream(TEST_RDF_GRAPH.getBytes("UTF-8"));

        Collection<Literal> terms = h.deserialize(in, "http://example.org/", "text/turtle");

        assert terms.size() == 4;

        // TODO further test
    }

}
