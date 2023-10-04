package org.hypermedea.ct;

import jason.asSyntax.Structure;
import org.apache.hc.core5.http.ContentType;
import org.hypermedea.ct.json.JsonHandler;
import org.hypermedea.ct.rdf.RDFHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO manage conflicts (on Content-Types, functors and priority)
 */
public class RepresentationHandlers {

    private static final Map<String, RepresentationHandler> registeredHandlers = new HashMap<>();

    private static final Map<String, String> defaultContentTypes = new HashMap<>();

    static {
        registerHandler(RDFHandler.class.getCanonicalName());
        registerHandler(JsonHandler.class.getCanonicalName());
    }

    public static void serialize(Collection<Structure> terms, OutputStream out, String resourceURI) throws UnsupportedRepresentationException {
        String fn = getDefaultFunctor(terms);

        if (!registeredHandlers.containsKey(fn))
            throw new UnsupportedRepresentationException("No handler found for Jason functor: " + fn);

        RepresentationHandler h = registeredHandlers.get(fn);
        h.serialize(terms, out, resourceURI);
    }

    public static Collection<Structure> deserialize(InputStream representation, String resourceURI, String contentType) throws UnsupportedRepresentationException {
        String mediaType = getMediaType(contentType);

        if (!registeredHandlers.containsKey(mediaType))
            throw new UnsupportedRepresentationException("No handler found for Content-Type: " + contentType);

        RepresentationHandler h = registeredHandlers.get(mediaType);
        return h.deserialize(representation, resourceURI, mediaType);
    }

    public static String getDefaultContentType(Collection<Structure> terms) throws UnsupportedRepresentationException {
        String fn = getDefaultFunctor(terms);

        if (!defaultContentTypes.containsKey(fn))
            throw new UnsupportedRepresentationException("No handler found for Jason functor: " + fn);

        return defaultContentTypes.get(fn);
    }

    public static void registerHandler(String handlerClass) {
        try {
            RepresentationHandler h = (RepresentationHandler) Class.forName(handlerClass).newInstance();

            if (h.getSupportedContentTypes().isEmpty())
                throw new IllegalArgumentException("Representation handler must at least support one Content-Type: " + handlerClass);

            if (h.getFunctor() == null || h.getFunctor().isEmpty())
                throw new IllegalArgumentException("Representation handler declares no Jason functor: " + handlerClass);

            for (String ct : h.getSupportedContentTypes()) registeredHandlers.put(getMediaType(ct), h);
            registeredHandlers.put(h.getFunctor(), h);

            defaultContentTypes.put(h.getFunctor(), h.getSupportedContentTypes().get(0));
        } catch (Exception e) {
            // TODO throw specific exception instead
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO improve detection (give priority to RDF? To non-RDF?)
     */
    private static String getDefaultFunctor(Collection<Structure> terms) {
        Structure t = terms.stream().findFirst().get();
        return t.getFunctor();
    }

    private static String getMediaType(String contentType) {
        ContentType ct = ContentType.parse(contentType);
        return ct.getMimeType();
    }

    private RepresentationHandlers() {}

}
