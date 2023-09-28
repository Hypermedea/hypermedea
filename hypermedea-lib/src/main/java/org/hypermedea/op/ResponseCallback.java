package org.hypermedea.op;

public interface ResponseCallback {

  /**
   * Called whenever a server asynchronously notifies the client (during e.g. a CoAP {@code OBSERVE} or
   * MQTT {@code SUBSCRIBE} operation).
   */
  void onResponse(Response response);

  /**
   * Called if connection to the server is lost, after initial request was sent.
   * Note that if the server is in erroneous state but does send messages,
   * these messages will be passed to client via {@link ResponseCallback#onResponse(Response)}
   * (with error status).
   */
  void onError();

}
