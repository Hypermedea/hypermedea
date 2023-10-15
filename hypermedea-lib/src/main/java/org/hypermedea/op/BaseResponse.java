package org.hypermedea.op;

import org.hypermedea.tools.Terms;

public abstract class BaseResponse implements Response {

  protected final Operation operation;

  public BaseResponse(Operation op) {
    operation = op;
  }

  @Override
  public Operation getOperation() {
    return operation;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();

    builder.append(String.format("[%s] %s", this.getClass().getSimpleName(), getStatus()));

    builder.append(String.format(", Payload: "));

    if (!getPayload().isEmpty()) {
      String str = Terms.getOneLineString(getPayload());
      builder.append(str);
    } else {
      builder.append("<none>");
    }

    return builder.toString();
  }

}
