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

    public static final String XML_DOC = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<rss version=\"2.0\">\n" +
            "<channel>\n" +
            " <title>RSS Title</title>\n" +
            " <description>This is an example of an RSS feed</description>\n" +
            " <link>http://www.example.com/main.html</link>\n" +
            " <copyright>2020 Example.com All rights reserved</copyright>\n" +
            " <lastBuildDate>Mon, 6 Sep 2010 00:01:00 +0000</lastBuildDate>\n" +
            " <pubDate>Sun, 6 Sep 2009 16:20:00 +0000</pubDate>\n" +
            " <ttl>1800</ttl>\n" +
            "\n" +
            " <item>\n" +
            "  <title>Example entry</title>\n" +
            "  <description>Here is some text containing an interesting description.</description>\n" +
            "  <link>http://www.example.com/blog/post/1</link>\n" +
            "  <guid isPermaLink=\"false\">7bd204c6-1655-4c27-aeee-53f933c5395f</guid>\n" +
            "  <pubDate>Sun, 6 Sep 2009 16:20:00 +0000</pubDate>\n" +
            " </item>\n" +
            "\n" +
            "</channel>\n" +
            "</rss>\n";

    public static final String XML_WITH_SCHEMA_DOC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
            "<xsl:output encoding=\"iso-8859-1\"/>\n" +
            "    <xsl:template match=\"/\">\n" +
            "      <html>\n" +
            "         <body>\n" +
            "            <h2><xsl:apply-templates/></h2>\n" +
            "         </body>\n" +
            "      </html>\n" +
            "    </xsl:template>\n" +
            "</xsl:stylesheet>";

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

    @Test
    public void testDeserializeXML() throws IOException {
        DOMHandler h = new DOMHandler();
        InputStream in = new ByteArrayInputStream(XML_DOC.getBytes());

        Collection<Literal> l = h.deserialize(in, "http://example.org/", "application/xml");

        MapTerm domMap = (MapTerm) l.stream().findAny().get().getTerm(0);

        MapTerm root = (MapTerm) domMap.get(ASSyntax.createAtom("document_element"));

        Term tag = root.get(ASSyntax.createAtom("tag"));
        assertEquals("rss", Identifiers.getLexicalForm(tag));
    }

    @Test
    public void testDeserializeXMLWithSchema() throws IOException {
        DOMHandler h = new DOMHandler();
        InputStream in = new ByteArrayInputStream(XML_WITH_SCHEMA_DOC.getBytes());

        Collection<Literal> l = h.deserialize(in, "http://example.org/", "application/xml");

        MapTerm domMap = (MapTerm) l.stream().findAny().get().getTerm(0);

        MapTerm root = (MapTerm) domMap.get(ASSyntax.createAtom("document_element"));

        Term tag = root.get(ASSyntax.createAtom("tag"));
        assertEquals("http://www.w3.org/1999/XSL/Transform/stylesheet", Identifiers.getLexicalForm(tag));
    }

}
