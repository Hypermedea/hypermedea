package org.hypermedea.ct;

import jason.asSyntax.Structure;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RepresentationHandlers {

    private static final Map<String, RepresentationHandler> registeredHandlers = new HashMap<>();

    public static OutputStream serialize(Collection<Structure> terms) throws UnsupportedRepresentationException {
        // TODO prioritize (RDF, then anything else?)
        Structure t = terms.stream().findFirst().get();
        String fn = t.getFunctor();

        if (!registeredHandlers.containsKey(fn))
            throw new UnsupportedRepresentationException("No handler found for Jason functor: " + fn + "/" + t.getArity());

        RepresentationHandler h = registeredHandlers.get(fn);
        return h.serialize(terms);
    }

    public static Collection<Structure> deserialize(InputStream representation, String contentType) throws UnsupportedRepresentationException {
        if (!registeredHandlers.containsKey(contentType))
            throw new UnsupportedRepresentationException("No handler found for Content-Type: " + contentType);

        RepresentationHandler h = registeredHandlers.get(contentType);
        return h.deserialize(representation, contentType);
    }

    public static void registerHandler(String wrapperClass) {
        try {
            RepresentationHandler h = (RepresentationHandler) Class.forName(wrapperClass).newInstance();

            for (String ct : h.getSupportedContentTypes()) registeredHandlers.put(ct, h);
            registeredHandlers.put(h.getFunctor(), h);
        } catch (Exception e) {
            // TODO throw specific exception instead
            throw new RuntimeException(e);
        }
    }

    private RepresentationHandlers() {}

}
