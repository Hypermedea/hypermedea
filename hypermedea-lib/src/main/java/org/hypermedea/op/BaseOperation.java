package org.hypermedea.op;

import jason.asSyntax.Literal;
import org.hypermedea.tools.Terms;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of basic operation features, including:
 * <ul>
 *   <li>management of operation states</li>
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
   * Payload to send with the operation's request.
   */
  protected final Collection<Literal> payload = new HashSet<>();

  /**
   * Flag that the operation has already started (no further request can be sent)
   */
  protected boolean operationStarted = false;

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
  }

  @Override
  public String getTargetURI() {
    return this.target;
  }

  @Override
  public Map<String, Object> getForm() {
    return this.form;
  }

  @Override
  public Collection<Literal> getPayload() {
    return payload;
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
   * Wrap the input term into an array and call {@link #setPayload(Collection)}.
   *
   * @param p a single Jason term
   */
  @Override
  public void setPayload(Literal p) {
    setPayload(Arrays.asList(p));
  }

  @Override
  public void setPayload(Collection<Literal> p) {
    payload.clear();
    payload.addAll(p);
  }

  @Override
  public boolean isSafe() {
    return getMethod().equals(GET)
        || getMethod().equals(WATCH);
  }

  @Override
  public boolean isIdempotent() {
    return getMethod().equals(GET)
        || getMethod().equals(WATCH)
        || getMethod().equals(PUT)
        || getMethod().equals(DELETE);
  }

  /**
   * Note: to implement synchronous operations, use {@link SynchronousOperation}.
   */
  @Override
  public boolean isAsync() {
    return getMethod().equals(WATCH);
  }

  /**
   * Ensure that only a single request is sent.
   * protocol binding-dependent behavior is implemented in {@link #sendSingleRequest()}.
   *
   * @throws OperationAlreadyStartedException
   * @throws IOException
   */
  @Override
  public void sendRequest() throws OperationAlreadyStartedException, IOException {
    if (operationStarted) throw new OperationAlreadyStartedException();

    sendSingleRequest();
    operationStarted = true;

    if (callbacks.isEmpty()) end();
    // TODO add operationEnded flag (in case getResponse is called afterwards)
  }

  /**
   * Use a semaphore ({@link BlockingDeque} of size 1) to implement
   * the expected behavior of {@link Operation#getResponse()}.
   */
  @Override
  public Response getResponse() throws NoResponseException {
    try {
      Optional<Response> rOpt = timeout > 0 ? lastResponse.poll(timeout, TimeUnit.SECONDS) : lastResponse.take();

      if (rOpt == null || rOpt.isEmpty()) throw new NoResponseException();
      else return rOpt.get();
    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void registerResponseCallback(ResponseCallback callback) {
    callbacks.add(callback);
  }

  @Override
  public void unregisterResponseCallback(ResponseCallback callback) {
    callbacks.remove(callback);

    try {
      if (callbacks.isEmpty()) end();
    } catch (IOException e) {
      // TODO log op may be hanging
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    String method = (String) this.getForm().get(Operation.METHOD_NAME_FIELD);

    builder.append(String.format("[%s] Method: %s", getClass().getSimpleName(), method));
    builder.append(String.format(", Target: %s", getTargetURI()));
    builder.append(String.format(", Payload: %s", Terms.getOneLineString(getPayload())));

    return builder.toString();
  }

  protected String getMethod() {
    return (String) this.form.get(Operation.METHOD_NAME_FIELD);
  }

  /**
   * Implementations are protocol binding-dependent.
   * See {@link Operation#sendRequest()} for expected behavior.
   */
  protected abstract void sendSingleRequest() throws IOException;

  /**
   * <p>
   *   This method is called as a side effect of {@link Operation#unregisterResponseCallback(ResponseCallback)},
   *   if more callback is registered for the operation.
   * </p>
   * <p>
   *   Some implementations are protocol binding-dependent. The default implementation does nothing.
   * </p>
   */
  protected void end() throws IOException {
    // do nothing
  }

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
