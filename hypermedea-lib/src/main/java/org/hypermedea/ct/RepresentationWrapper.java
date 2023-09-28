package org.hypermedea.ct;

import jason.asSyntax.*;

/**
 * Wrapper for a canonical resource representation that also has an equivalent representation as a Jason term.
 */
public interface RepresentationWrapper {

    String JASON_CONTENT_TYPE = "application/jason";

    /**
     * Return the representation given as argument at instantiation time.
     *
     * @return the wrapped representation as POJO
     */
    Object getRepresentation();

    /**
     * Return the Content-Type of the canonical representation. A Content-Type is equivalent to a
     * <a href="https://www.iana.org/assignments/media-types/media-types.xhtml">Media-Type</a>
     * with optional parameters.
     *
     * @return a Content-Type
     */
    String getContentType();

    /**
     * Return the wrapped representation as a Jason term.
     *
     * @return a Jason term representing the wrapped representation
     */
    Term getTerm();

}
