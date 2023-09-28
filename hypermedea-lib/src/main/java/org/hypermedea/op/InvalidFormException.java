package org.hypermedea.op;

public class InvalidFormException extends IllegalArgumentException {

  public InvalidFormException(String message) {
    super(message);
  }

  public InvalidFormException(Throwable cause) {
    super(cause);
  }

  public InvalidFormException(String message, Throwable cause) {
    super(message, cause);
  }

}
