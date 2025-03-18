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

    public static final String HTML_DOC = "<html lang=\"en\" class=\"e\">\n" +
            "  <head>\n" +
            "    <title>Test page</title>\n" +
            "  </head>\n" +
            "  <body>\n" +
            "    Hi!\n" +
            "  </body>\n" +
            "</html>\n";

    @Test
    public void testRoundTrip() throws IOException {
        DOMHandler h = new DOMHandler();
        InputStream in = new ByteArrayInputStream(HTML_DOC.getBytes());

        Collection<Literal> l = h.deserialize(in, "http://example.org/", "text/html");

        assertEquals(1, l.size());

        Literal dom = l.stream().findAny().get();
        assertEquals(h.getFunctor(), dom.getFunctor());
        assertTrue(dom.getTerm(0) instanceof MapTerm);

        MapTerm domMap = (MapTerm) dom.getTerm(0);

        MapTerm root = (MapTerm) domMap.get(ASSyntax.createAtom("document_element"));

        Term tag = root.get(ASSyntax.createAtom("tag"));
        assertEquals("html", Identifiers.getLexicalForm(tag));

        ListTerm list = (ListTerm) root.get(ASSyntax.createAtom("children"));
        MapTerm body = (MapTerm) list.get(1);

        tag = body.get(ASSyntax.createAtom("tag"));
        assertEquals("body", Identifiers.getLexicalForm(tag));

        Term text = body.get(ASSyntax.createAtom("text"));
        assertEquals("Hi!", Identifiers.getLexicalForm(text));
    }

}
