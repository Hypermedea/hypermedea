package org.hypermedea.op;

import java.io.IOException;

/**
 * Exception thrown whenever the connection with the server is closed or broken before it responded
 * to a client request during some Web operation. Note that the server may respond with errors,
 * in which case this exception is not thrown. Instead, an instance of {@link Response} is returned
 * with an error response status.
 */
public class NoResponseException extends IOException {

  public NoResponseException() {
    super();
  }

  public NoResponseException(Throwable cause) {
    super(cause);
  }

}
