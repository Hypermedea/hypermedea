package org.hypermedea.ct.dom;

import jason.asSyntax.*;
import org.hypermedea.tools.Identifiers;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DOMHandlerTest {

    public static final String HTML_DOC = "<html lang=\"en\" class=\"e\">" +
            "<head><title>Test page</title></head>" +
            "<body>Hi <a href=\"https://jason-lang.github.io/\">Jason</a>!</body>" +
            "</html>\n";

    public static final String XML_DOC = "";

    @Test
    public void testDeserializeHTML() throws IOException {
        DOMHandler h = new DOMHandler();
        InputStream in = new ByteArrayInputStream(HTML_DOC.getBytes());

        Collection<Literal> l = h.deserialize(in, "http://example.org/", "text/html");

        assertEquals(1, l.size());

        Literal dom = l.stream().findAny().get();
        assertEquals(h.getFunctor(), dom.getFunctor());
        assertTrue(dom.getTerm(0) instanceof MapTerm);

        MapTerm domMap = (MapTerm) dom.getTerm(0);

        ListTerm links = (ListTerm) domMap.get(ASSyntax.createAtom("links"));
        assertEquals(1, links.size());

        MapTerm root = (MapTerm) domMap.get(ASSyntax.createAtom("document_element"));

        Term tag = root.get(ASSyntax.createAtom("tag"));
        assertEquals("html", Identifiers.getLexicalForm(tag));

        ListTerm list = (ListTerm) root.get(ASSyntax.createAtom("child_nodes"));
        MapTerm body = (MapTerm) list.get(1);

        tag = body.get(ASSyntax.createAtom("tag"));
        assertEquals("body", Identifiers.getLexicalForm(tag));

        ListTerm bodyNodes = (ListTerm) body.get(ASSyntax.createAtom("child_nodes"));
        Term text = bodyNodes.get(0);
        assertTrue(Identifiers.getLexicalForm(text).contains("Hi"));
    }

}
