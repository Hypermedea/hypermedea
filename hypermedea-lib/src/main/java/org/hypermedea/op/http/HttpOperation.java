package org.hypermedea.op.http;

import jason.asSyntax.Structure;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
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
  }

  @Override
  public void sendRequest() throws IOException {
    client.execute(request, handler);
  }

  @Override
  protected Object getPayload() {
    return this.request.getBody();
  }

  @Override
  public void setPayload(Collection<Structure> payload) {
    ContentType ct = ContentType.create(RepresentationHandlers.getDefaultContentType(payload));

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    RepresentationHandlers.serialize(payload, out);

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
