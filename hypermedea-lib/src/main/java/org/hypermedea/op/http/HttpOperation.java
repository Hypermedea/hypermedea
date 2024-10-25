package org.hypermedea.op.http;

import jason.asSyntax.Literal;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.hypermedea.ct.RepresentationHandlers;
import org.hypermedea.op.SynchronousOperation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class HttpOperation extends SynchronousOperation {

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

  private static CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();

  private final class HttpOperationHandler implements FutureCallback<SimpleHttpResponse> {

    @Override
    public void completed(SimpleHttpResponse r) {
      onResponse(new HttpResponse(r, HttpOperation.this));
    }

    @Override
    public void failed(Exception ex) {
      onError();
    }

    @Override
    public void cancelled() {
      // TODO do something?
    }

  }

  private final String target;

  private final HttpOperationHandler handler;
  private final SimpleHttpRequest request;

  public HttpOperation(String targetURI, Map<String, Object> formFields) {
    super(targetURI, formFields);

    this.target = targetURI;
    this.handler = new HttpOperationHandler();

    // TODO when to close the client?
    if (client.getStatus().equals(IOReactorStatus.INACTIVE)) client.start();

    String methodName = getMethod();
    this.request = SimpleHttpRequest.create(methodName, getTargetURI());

    boolean acceptHeaderSet = false;

    for (String name : formFields.keySet()) {
      String h = HTTP.getHeader(name);

      if (h != null) request.addHeader(h, formFields.get(name));
      if (h == HTTP.Accept) acceptHeaderSet = true;
    }

    if (!acceptHeaderSet) request.addHeader(HttpHeaders.ACCEPT, ACCEPT_HEADER);
  }

  @Override
  protected void sendSingleRequest() {
    client.execute(request, handler);
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

}
