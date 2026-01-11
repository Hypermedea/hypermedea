package org.hypermedea.ct;

import java.util.ArrayList;
import java.util.List;

abstract public class BaseRepresentationHandler implements RepresentationHandler {

    protected final String functor;

    protected final List<String> supportedContentTypes = new ArrayList<>();

    /**
     * Descendant classes must provide a default constructor without arguments.
     *
     * @param functor the functor of Jason structures recognized by this representation handler
     * @param contentTypes Content-Types recognized by this representation handler, in decreasing order of importance
     */
    public BaseRepresentationHandler(String functor, String... contentTypes) {
        this.functor = functor;
        for (String ct : contentTypes) supportedContentTypes.add(ct);
    }

    @Override
    public String getFunctor() {
        return functor;
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    public List<String> getSupportedContentTypes() {
        return supportedContentTypes;
    }

}
