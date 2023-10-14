package org.hypermedea.op.http;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.hypermedea.ct.RepresentationHandlers;
import org.hypermedea.op.BaseResponse;
import org.hypermedea.op.Operation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper for an HTTP response received when performing a
 * {@link HttpOperation}. The payload of the response is
 * deserialized based on a <code>DataSchema</code> from a given <code>ThingDescription</code>.
 *
 */
public class HttpResponse extends BaseResponse {

  private final static Logger LOGGER = Logger.getLogger(HttpResponse.class.getCanonicalName());

  public static final String DEFAULT_HTTP_CT = "text/plain";

  private final static Pattern LINK_HEADER_PATTERN = Pattern.compile("\\w*<(?<target>.*)>;\\w*rel=\"(?<rel>.*)\"");

  private final SimpleHttpResponse response;

  public HttpResponse(SimpleHttpResponse response, Operation op) {
    super(op);

    this.response = response;
  }

  @Override
  public ResponseStatus getStatus() {
    if (response.getCode() >= 200 && response.getCode() < 300) return ResponseStatus.OK;
    else if (response.getCode() >= 400 && response.getCode() < 500) return ResponseStatus.CLIENT_ERROR;
    else if (response.getCode() >= 500 && response.getCode() < 600) return ResponseStatus.SERVER_ERROR;
    else return ResponseStatus.UNKNOWN_ERROR;
  }

  @Override
  public Collection<Literal> getPayload() {
    Collection<Literal> terms = new HashSet<>();
    byte[] bytes = response.getBodyBytes();

    if (bytes == null) return terms;

    InputStream in = new ByteArrayInputStream(bytes);
    String ct = getContentType();

    try {
      terms.addAll(RepresentationHandlers.deserialize(in, operation.getTargetURI(), ct));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    terms.addAll(getLinks());

    return terms;
  }

  public Collection<Literal> getLinks() {
    HashSet<Literal> links = new HashSet<>();

    for (Header h : response.getHeaders()) {
      if (h.getName().equals("Location") && response.getCode() == 201) {
        Term s = ASSyntax.createString(operation.getTargetURI());
        Term p = ASSyntax.createAtom("related"); // FIXME proper predicate
        Term o = ASSyntax.createString(h.getValue());

        Structure t = ASSyntax.createStructure("rdf", s, p, o);
        links.add(t);
      } else if (h.getName().equals("Link")) {
        Matcher m = LINK_HEADER_PATTERN.matcher(h.getValue());

        if (m.matches()) {
          Term s = ASSyntax.createString(operation.getTargetURI());
          Term p = ASSyntax.createAtom(m.group("rel")); // FIXME only if URI
          Term o = ASSyntax.createString(m.group("target"));

          Structure t = ASSyntax.createStructure("rdf", s, p, o);
          links.add(t);
        }
      }
    }

    return links;
  }

  private String getContentType() {
    ContentType ct = response.getContentType();

    if (ct != null) return ct.toString();
    return DEFAULT_HTTP_CT;

    // TODO if Content-Type not provided in response, could be provided in form.
  }

}
