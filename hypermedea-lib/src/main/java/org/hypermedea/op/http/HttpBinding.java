package org.hypermedea.op.http;

import org.hypermedea.op.BaseProtocolBinding;
import org.hypermedea.op.Operation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Default HTTP binding.
 */
public class HttpBinding extends BaseProtocolBinding {

  private final static String HTTP_PROTOCOL = "HTTP";

  private final static Collection<String> SUPPORTED_SCHEMES = new HashSet<>();

  static {
    SUPPORTED_SCHEMES.add("http");
    SUPPORTED_SCHEMES.add("https");
  }

  @Override
  public String getProtocol() {
    return HTTP_PROTOCOL;
  }

  @Override
  public Collection<String> getSupportedSchemes() {
    return SUPPORTED_SCHEMES;
  }

  @Override
  public Operation bind(String targetURI, Map<String, Object> formFields) {
    return new HttpOperation(targetURI, formFields);
  }

}
