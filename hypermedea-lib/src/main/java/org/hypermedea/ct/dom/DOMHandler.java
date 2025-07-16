package org.hypermedea.ct.dom;

import jason.asSyntax.*;
import org.hypermedea.ct.BaseRepresentationHandler;
import org.hypermedea.ct.UnsupportedRepresentationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

public class DOMHandler extends BaseRepresentationHandler {

    public DOMHandler() {
        super("dom", "text/html", "text/xml", "application/xml");
    }

    @Override
    public void serialize(Collection<Literal> terms, OutputStream out, String resourceURI) throws UnsupportedRepresentationException, IOException {
        // Document doc = Document.createShell(resourceURI);
        // out.write(doc.outerHtml().getBytes(StandardCharsets.UTF_8));

        throw new UnsupportedRepresentationException("HTML/XML serialization not supported");
    }

    @Override
    public Collection<Literal> deserialize(InputStream representation, String resourceURI, String contentType) throws UnsupportedRepresentationException, IOException {
        Document doc = Jsoup.parse(representation, "utf-8", resourceURI);

        MapTerm m = getAsMap(doc);

        return List.of(ASSyntax.createLiteral(functor, m));
    }

    private MapTerm getAsMap(Document doc) {
        MapTerm m = new MapTermImpl();

        m.put(ASSyntax.createAtom("title"), ASSyntax.createString(doc.title()));
        m.put(ASSyntax.createAtom("document_element"), getAsMap(doc.firstElementChild()));

        m.put(ASSyntax.createAtom("links"), getAsList(doc.getElementsByTag("a")));
        m.put(ASSyntax.createAtom("forms"), getAsList(doc.forms()));

        return m;
    }

    private MapTerm getAsMap(Element e) {
        MapTerm m = new MapTermImpl();

        m.put(ASSyntax.createAtom("tag"), ASSyntax.createAtom(e.tagName()));

        m.put(ASSyntax.createAtom("attributes"), getAsMap(e.attributes()));
        m.put(ASSyntax.createAtom("child_nodes"), getAsList(e.childNodes()));

        // TODO add prefix, localName if XML?

        return m;
    }

    private MapTerm getAsMap(Attributes attrs) {
        MapTerm m = new MapTermImpl();

        attrs.forEach(attr -> {
            Term name = ASSyntax.createAtom(attr.getKey());
            Term val = ASSyntax.createString(attr.getValue());
            m.put(name, val);
        });

        return m;
    }

    private ListTerm getAsList(List<? extends Node> forms) {
        ListTerm list = ASSyntax.createList();

        forms.forEach(n -> {
            if (n instanceof Element) {
                list.append(getAsMap((Element) n));
            } else if (n instanceof TextNode) {
                TextNode tn = (TextNode) n;
                if (!tn.isBlank()) list.append(ASSyntax.createString(tn.getWholeText()));
            }
        });

        return list;
    }

}
