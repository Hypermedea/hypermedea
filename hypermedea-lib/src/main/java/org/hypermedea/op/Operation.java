package org.hypermedea.op;

import jason.asSyntax.Structure;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * A Web operation is a temporal entity (~time interval) that starts with a request sent
 * by the client to the server and that remains active as long as the server returns responses.
 * A server is e.g. expected to return a single response during an HTTP {@code GET} operation
 * and several responses during a CoAP {@code OBSERVE} operation.
 */
public interface Operation {

  /**
   * Field name to indicate the method used in the operation.
   * Every form should have at least one field with this name.
   */
  String METHOD_NAME_FIELD = "urn:hypermedea:methodName";

  String GET = "GET";
  String PUT = "PUT";
  String POST = "POST";
  String PATCH = "PATCH";
  String DELETE = "DELETE";
  String WATCH = "WATCH";

  /**
   * Return the URI of the resource targeted in the operation.
   *
   * @return URI of the operation's target resource
   */
  String getTargetURI();

  /** Return the filled-out form passed as argument to the instantiation of the operation.
   * A form is a collection of key-value pairs (fields) that parameterize the request sent to initiate the operation.
   *
   * @return a form
   */
  Map<String, Object> getForm();

  /**
   * Fill out the form with a payload. Convenience method for
   * {@link #setPayload(Collection)} if the collection is a singleton.
   *
   * @param payload payload to send to the server
   */
  void setPayload(Structure payload);

  /**
   * Fill out the form with a payload. The server may reject it and return
   * {@link Response.ResponseStatus#CLIENT_ERROR CLIENT_ERROR} status.
   *
   * @param payload payload to send to the server
   */
  void setPayload(Collection<Structure> payload);

  /**
   * Start the operation by sending a message to the server with payload.
   * When the method returns, the client may assume the request was received by the server.
   * This doesn't imply that the server already responded, though.
   * To synchronously wait for a response, use {@link Operation#getResponse()}.
   *
   * @throws IOException if connection to the server is lost or if the request is never received by the server
   */
  void sendRequest() throws IOException;

  // TODO should the operation fail if request is sent several times?

  /**
   * Wait synchronously for a response from the server and return it.
   * If the method is called after the server responded, it immediately returns the cached response.
   *
   * @return the unique response sent by the server
   * @throws NoResponseException if no response has been received after some timeout
   * or if connection to the server was lost
   */
  Response getResponse() throws NoResponseException;

  /**
   * Register a callback for asynchronous responses sent by the server.
   *
   * @param callback response callback exposing a method with a {@link Response} as argument
   */
  void registerResponseCallback(ResponseCallback callback);

  /**
   * Remove a callback from the list of registered callbacks for the operation.
   *
   * @param callback response callback already registered via {@link Operation#registerResponseCallback(ResponseCallback)}
   */
  void unregisterResponseCallback(ResponseCallback callback);

}
