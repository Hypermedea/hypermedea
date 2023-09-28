package org.hypermedea.op.http;

import com.google.gson.Gson;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.io.CloseMode;
import org.hypermedea.op.BaseOperation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
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
    this.handler = new TDHttpHandler();
    this.client = HttpAsyncClients.createDefault();

    this.client.start();

    String methodName = getMethod();
    this.request = SimpleHttpRequest.create(methodName, getTargetURI());

    this.request.setHeader(HttpHeaders.CONTENT_TYPE, form.getContentType());
  }

  @Override
  public void sendRequest() throws IOException {
    client.execute(request, handler);
  }

  public HttpOperation addHeader(String key, String value) {
    this.request.addHeader(key, value);
    return this;
  }

  @Override
  protected Object getPayload() {
    return this.request.getBody();
  }

  @Override
  protected void setBooleanPayload(Boolean value) {
    request.setBody(String.valueOf(value), ContentType.create(form.getContentType()));
  }

  @Override
  protected void setStringPayload(String value) {
    request.setBody(value, ContentType.create(form.getContentType()));
  }

  @Override
  protected void setIntegerPayload(Long value) {
    request.setBody(String.valueOf(value), ContentType.create(form.getContentType()));
  }

  @Override
  protected void setNumberPayload(Double value) {
    request.setBody(String.valueOf(value), ContentType.create(form.getContentType()));
  }

  @Override
  protected void setObjectPayload(Map<String, Object> payload) {
    String body = new Gson().toJson(payload);
    request.setBody(body, ContentType.create(form.getContentType()));
  }

  @Override
  protected void setArrayPayload(List<Object> payload) {
    String body = new Gson().toJson(payload);
    request.setBody(body, ContentType.create(form.getContentType()));
  }

  public String getPayloadAsString() {
    return request.getBodyText();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[TDHttpRequest] Method: " + request.getMethod());

    try {
      builder.append(", Target: " + request.getUri().toString());

      for (Header header : request.getHeaders()) {
        builder.append(", " + header.getName() + ": " + header.getValue());
      }

      if (request.getBodyText() != null) {
        builder.append(", Payload: " + request.getBodyText());
      }
    } catch (UnsupportedOperationException | URISyntaxException e) {
      LOGGER.log(Level.WARNING, e.getMessage());
    }

    return builder.toString();
  }

  SimpleHttpRequest getRequest() {
    return this.request;
  }

}
