package org.hypermedea.op;

import jason.asSyntax.Structure;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
      String str = getOneLineString(getPayload());
      builder.append(str);
    } else {
      builder.append("<none>");
    }

    return builder.toString();
  }

  private String getOneLineString(Collection<Structure> terms) {
    Optional<Structure> tOpt = terms.stream().findAny();

    if (tOpt.isEmpty()) return "<none>";

    Structure t = tOpt.get();

    Pattern p = Pattern.compile("([^\\r\\n]*)\\r?\\n");
    Matcher m = p.matcher(t.toString());

    return m.find() ? m.group(1) + "..." : t.toString();
  }

}
