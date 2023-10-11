package org.hypermedea.op.http;

import jason.asSyntax.Literal;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.io.CloseMode;
import org.hypermedea.ct.RepresentationHandlers;
import org.hypermedea.op.BaseOperation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Wrapper for constructing and executing an HTTP request based on a given <code>ThingDescription</code>.
 * When constructing the request, clients can set payloads that conform to a <code>DataSchema</code>.
 */
public class HttpOperation extends BaseOperation {

  /**
   * Accept header set by default, favoring RDF representations.
   */
  public final static String ACCEPT_HEADER = "application/ld+json;q=1," +
          "text/turtle;q=1," +
          "application/n-triples;q=1," +
          "application/trig;q=0.9," +
          "application/n-quads;q=0.9," +
          "application/rdf+xml;q=0.75," +
          "*/*;q=0.5";

  private final static Logger LOGGER = Logger.getLogger(HttpOperation.class.getCanonicalName());

  private final class HttpOperationHandler implements FutureCallback<SimpleHttpResponse> {

    @Override
    public void completed(SimpleHttpResponse r) {
      onResponse(new HttpResponse(r, HttpOperation.this));
      client.close(CloseMode.GRACEFUL);
    }

    @Override
    public void failed(Exception ex) {
      onError();
      client.close(CloseMode.GRACEFUL);
    }

    @Override
    public void cancelled() {
      client.close(CloseMode.GRACEFUL);
    }

  }

  private final String target;

  private final HttpOperationHandler handler;
  private final SimpleHttpRequest request;

  private CloseableHttpAsyncClient client;

  public HttpOperation(String targetURI, Map<String, Object> formFields) {
    super(targetURI, formFields);

    this.target = targetURI;
    this.handler = new HttpOperationHandler();
    this.client = HttpAsyncClients.createDefault();

    this.client.start();

    String methodName = getMethod();
    this.request = SimpleHttpRequest.create(methodName, getTargetURI());

    addHeader(HttpHeaders.ACCEPT, ACCEPT_HEADER);
  }

  @Override
  protected void sendSingleRequest() {
    client.execute(request, handler);
  }

  @Override
  protected Object getPayload() {
    return this.request.getBody();
  }

  @Override
  public void setPayload(Collection<Literal> payload) {
    ContentType ct = ContentType.create(RepresentationHandlers.getDefaultContentType(payload));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      RepresentationHandlers.serialize(payload, out, target);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    request.setBody(out.toByteArray(), ct);
  }

  SimpleHttpRequest getRequest() {
    return this.request;
  }

  public HttpOperation addHeader(String key, String value) {
    this.request.addHeader(key, value);
    return this;
  }

}
