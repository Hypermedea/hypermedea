package org.hypermedea.ct;

import jason.asSyntax.Term;

import java.util.HashMap;
import java.util.Map;

public class RepresentationWrappers {

    private static final Map<String, String> registeredWrappers = new HashMap<>();

    public static RepresentationWrapper wrap(Object representation, String contentType) {
        // TODO
        return null;
    }

    public static RepresentationWrapper wrap(Term jasonRepresentation) {
        return wrap(jasonRepresentation, RepresentationWrapper.JASON_CONTENT_TYPE);
    }

    private RepresentationWrappers() {}

}
