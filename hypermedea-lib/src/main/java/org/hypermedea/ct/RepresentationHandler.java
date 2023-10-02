package org.hypermedea.ct;

import jason.asSyntax.Structure;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

/**
 * Object handling resource representation, capable of serializing Jason terms
 * in some well-known format, and round-tripping to Jason terms.
 */
public interface RepresentationHandler {

    /**
     * Serialize the input representation into an alternative representation,
     * in a well-known format (a Content-Type), assumed to be the first Content-Type
     * returned by {@link #getSupportedContentTypes()}.
     *
     * @param terms some resource representation as Jason terms
     * @return a stream in which the alternative representation will be serialized
     */
    OutputStream serialize(Collection<Structure> terms) throws UnsupportedRepresentationException;

    /**
     * Deserialize the input representation into a collection of Jason terms.
     *
     * If the input Content-Type is not in the list returned by {@link #getSupportedContentTypes()},
     * an error is thrown.
     *
     * @param representation some serialized representation
     * @param contentType the Content-Type of the serialization
     *
     * @return one or more Jason terms representing the input representation
     */
    Collection<Structure> deserialize(InputStream representation, String contentType) throws UnsupportedRepresentationException;

    /**
     * Return the functor appearing in the Jason structure(s) returned by {@link #deserialize(InputStream, String)}
     * and accepted as input of {@link #serialize(Collection)}.
     *
     * @return the functor of the Jason term(s) manipulated by the wrapper
     */
    String getFunctor();

    /**
     * Return the Content-Types in which the wrapped representation can alternatively be represented.
     * The list is returned in order of preference, starting from the preferred Content-Type (index 0).
     *
     * A Content-Type is equivalent to a
     * <a href="https://www.iana.org/assignments/media-types/media-types.xhtml">Media-Type</a>
     * with optional parameters.
     *
     * @return a list of supported Content-Types
     */
    List<String> getSupportedContentTypes();

}
