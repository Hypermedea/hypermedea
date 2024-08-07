package org.hypermedea.ct;

import jason.asSyntax.Literal;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Factory class to (de)serialize resource representations in (from) known Content-Types.
 */
public class RepresentationHandlers {

    private static final ServiceLoader<RepresentationHandler> loader = ServiceLoader.load(RepresentationHandler.class);

    public static void serialize(Collection<Literal> terms, OutputStream out, String resourceURI) throws UnsupportedRepresentationException, IOException {
        if (terms.isEmpty()) {
            // do not write anything
            out.close();
            return;
        }

        String fn = getDefaultFunctor(terms);
        Optional<RepresentationHandler> opt = loadFromFunctor(fn);

        if (opt.isEmpty())
            throw new UnsupportedRepresentationException("No handler found for Jason functor: " + fn);

        opt.get().serialize(terms, out, resourceURI);
    }

    public static Collection<Literal> deserialize(InputStream representation, String resourceURI, String contentType) throws UnsupportedRepresentationException, IOException {
        String mediaType = getMediaType(contentType);
        Optional<RepresentationHandler> opt = loadFromContentType(mediaType);

        if (opt.isEmpty())
            throw new UnsupportedRepresentationException("No handler found for Content-Type: " + contentType);

        // TODO pass contentType instead, e.g. to retrieve charset
        return opt.get().deserialize(representation, resourceURI, mediaType);
    }

    public static String getDefaultContentType(Collection<Literal> terms) throws UnsupportedRepresentationException {
        String fn = getDefaultFunctor(terms);
        Optional<RepresentationHandler> opt = loadFromFunctor(fn);

        if (opt.isEmpty())
            throw new UnsupportedRepresentationException("No handler found for Jason functor: " + fn);

        return opt.get().getSupportedContentTypes().get(0);
    }

    /**
     * TODO improve detection (give priority to RDF? To non-RDF?)
     */
    private static String getDefaultFunctor(Collection<Literal> terms) {
        Literal t = terms.stream().findFirst().get();
        return t.getFunctor();
    }

    private static String getMediaType(String contentType) {
        ContentType ct = ContentType.parse(contentType);
        return ct.getMimeType();
    }

    private static Optional<RepresentationHandler> loadFromFunctor(String fn) {
        for (RepresentationHandler h : loader) {
            // TODO same as for loadFromContentType: manage hierarchy
            if (h.getFunctor().equals(fn)) return Optional.of(h);
        }

        return Optional.empty();
    }

    private static Optional<RepresentationHandler> loadFromContentType(String ct) {
        Map<Integer, RepresentationHandler> exactMatches = new HashMap<>();
        Collection<RepresentationHandler> closeMatches = new ArrayList<>();

        for (RepresentationHandler h : loader) {
            List<String> supportedCTs = h.getSupportedContentTypes();

            for (int i = 0; i < supportedCTs.size(); i++) {
                String supportedCT = supportedCTs.get(i);

                if (supportedCT.equals(ct)) {
                    // record index of CT for handler (0: favorite)
                    exactMatches.put(i, h);
                    break;
                } else if (ct.matches(supportedCT)) {
                    closeMatches.add(h);
                }
            }
        }

        Optional<Integer> rankOpt = exactMatches.keySet().stream().sorted().findFirst();

        if (rankOpt.isPresent()) return Optional.of(exactMatches.get(rankOpt.get()));
        else return closeMatches.stream().findFirst();
    }

    private RepresentationHandlers() {}

}
