package org.hypermedea.ct;

import jason.asSyntax.Literal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

/**
 * Object handling resource representation, capable of serializing Jason terms
 * in some well-known format, and round-tripping to Jason terms.
 *
 * Representation handlers should be liberal in the data they get as input.
 * If parts of a stream or a Jason term have unexpected content, they should
 * do their best to (de)serialize the rest of the stream or term.
 */
public interface RepresentationHandler {

    /**
     * Serialize the input representation into an alternative representation,
     * in a well-known format (a Content-Type), assumed to be the first Content-Type
     * returned by {@link #getSupportedContentTypes()}.
     *
     * @param terms some resource representation as Jason terms
     * @param out a stream in which the alternative representation will be serialized
     * @param resourceURI URI of the resource being represented
     */
    void serialize(Collection<Literal> terms, OutputStream out, String resourceURI) throws UnsupportedRepresentationException, IOException;

    /**
     * Deserialize the input representation into a collection of Jason terms.
     *
     * If the input Content-Type is not in the list returned by {@link #getSupportedContentTypes()},
     * an error is thrown.
     *
     * @param representation some serialized representation
     * @param resourceURI URI of the resource being represented
     * @param contentType the Content-Type of the serialization
     *
     * @return one or more Jason terms representing the input representation
     */
    Collection<Literal> deserialize(InputStream representation, String resourceURI, String contentType) throws UnsupportedRepresentationException, IOException;

    /**
     * Return the functor appearing in the Jason structure(s) returned by {@link #deserialize(InputStream, String, String)}
     * and accepted as input of {@link #serialize(Collection, OutputStream, String)}.
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
