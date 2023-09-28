package org.hypermedea.op;

public class BindingNotRegisteredException extends RuntimeException {

  public BindingNotRegisteredException(Throwable cause) {
    super(cause);
  }

  public BindingNotRegisteredException(String message, Throwable cause) {
    super(message, cause);
  }

}
