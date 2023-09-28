package org.hypermedea.op;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of basic operation features, including:
 * <ul>
 *   <li>validation of JSON payload</li>
 *   <li>management of asynchronous calls and blocking calls</li>
 * </ul>
 */
public abstract class BaseOperation implements Operation {

  /**
   * Default value for {@link BaseOperation#timeout}
   */
  public static final long DEFAULT_TIMEOUT = 60l;

  /**
   * Target resource of the operation.
   */
  protected final String target;

  /**
   * Form from which the operation was instantiated.
   */
  protected final Map<String, Object> form;

  /**
   * Semaphore to block getter if no response sent before call
   * (if an error occurs, an empty value is passed to the semaphore)
   */
  private BlockingDeque<Optional<Response>> lastResponse = new LinkedBlockingDeque<>(1);

  /**
   * Callbacks registered for the pending request
   */
  private Collection<ResponseCallback> callbacks = new LinkedList<>();

  /**
   * Response timeout (in seconds): after request was sent,
   * the Thing has {@code timeout} seconds to send a response
   */
  private long timeout = DEFAULT_TIMEOUT;

  public BaseOperation(String targetURI, Map<String, Object> formFields) {
    this.target = targetURI;
    this.form = formFields;

    // TODO check method field is a string and one of the allowed methods
  }

  @Override
  public String getTargetURI() {
    return this.target;
  }

  @Override
  public Map<String, Object> getForm() {
    return this.form;
  }

  /**
   * Set timeout between request and (first) response.
   *
   * @param timeout timeout (in seconds). A timeout of 0s is equivalent to no timeout.
   */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  /**
   * Call an internal method to set the payload ({@link BaseOperation#setJSONPayload(Object)}).
   *
   * @param payload any payload that can map to a JSON value
   */
  @Override
  public void setPayload(Object payload) {
    setJSONPayload(payload);
  }

  /**
   * Implementations are protocol binding-dependent.
   * See {@link Operation#sendRequest()} for expected behavior.
   */
  @Override
  public abstract void sendRequest() throws IOException;

  /**
   * Use a semaphore ({@link BlockingDeque} of size 1) to implement
   * the expected behavior of {@link Operation#getResponse()}.
   */
  @Override
  public Response getResponse() throws NoResponseException {
    try {
      Optional<Response> r = timeout > 0 ? lastResponse.poll(timeout, TimeUnit.SECONDS) : lastResponse.take();

      if (r != null && r.isPresent()) return r.get();
      else throw new NoResponseException();
    } catch (InterruptedException e) {
      throw new NoResponseException(e);
    }
  }

  @Override
  public void registerResponseCallback(ResponseCallback callback) {
    callbacks.add(callback);
  }

  @Override
  public void unregisterResponseCallback(ResponseCallback callback) {
    callbacks.remove(callback);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    String method = (String) this.getForm().get(Operation.METHOD_NAME_FIELD);

    builder.append(String.format("[%s] Method: %s", getClass().getSimpleName(), method));
    builder.append(String.format(", Target: ", getTargetURI()));
    builder.append(String.format(", Payload: %s", getPayload()));

    return builder.toString();
  }

  protected String getMethod() {
    return (String) this.form.get(Operation.METHOD_NAME_FIELD);
  }

  /**
   * Return a binding-dependent encapsulation of the payload for the request initiating the operation.
   * <i>This method is used in <code>toString()</code></i>.
   *
   * @return the payload, encapsulated in some arbitrary object
   */
  protected abstract Object getPayload();

  /**
   * Check the type of the input payload and defer setting the payload to methods with typed signatures
   * corresponding each to a particular JSON value type:
   * <ul>
   *   <li>{@link BaseOperation#setArrayPayload(List)}</li>
   *   <li>{@link BaseOperation#setObjectPayload(Map)}</li>
   *   <li>{@link BaseOperation#setStringPayload(String)}</li>
   *   <li>...</li>
   * </ul>
   * These methods are to be implemented per protocol binding. If a protocol binding allows arbitrary payloads,
   * {@code setPayload(Object)} may be overridden as well.
   *
   * @param payload a payload expected to be equivalent to a JSON value (object, array, string, ...)
   */
  protected void setJSONPayload(Object payload) {
    if (payload instanceof Map) setObjectPayload((Map<String, Object>) payload);
    else if (payload instanceof List) setArrayPayload((List<Object>) payload);
    else if (payload instanceof String) setStringPayload((String) payload);
    else if (payload instanceof Boolean) setBooleanPayload((Boolean) payload);
    else if (payload instanceof BigInteger
          || payload instanceof Long
          || payload instanceof Integer
          || payload instanceof Short
          || payload instanceof Byte)
      setIntegerPayload(((Number) payload).longValue());
    else if (payload instanceof Number)
      setNumberPayload(((Number) payload).doubleValue());
    else throw new IllegalArgumentException(String.format("Given payload type isn't supported: %s", payload.getClass()));
  }

  protected abstract void setObjectPayload(Map<String, Object> payload);

  protected abstract void setArrayPayload(List<Object> payload);

  protected abstract void setStringPayload(String payload);

  protected abstract void setBooleanPayload(Boolean payload);

  protected abstract void setIntegerPayload(Long payload);

  protected abstract void setNumberPayload(Double payload);

  /**
   * Pass the input response to the semaphore and notify registered callbacks.
   *
   * @param r a response received by the Thing during the operation
   */
  protected void onResponse(Response r) {
    lastResponse.clear();

    lastResponse.push(Optional.of(r));
    callbacks.forEach(cb -> cb.onResponse(r));
  }

  /**
   * Pass an empty value to the semaphore and notify registered callbacks of an error.
   */
  protected void onError() {
    lastResponse.clear();

    lastResponse.push(Optional.empty());
    callbacks.forEach(cb -> cb.onError());
  }

}
