package org.hypermedea.op;

import java.util.Map;

/**
 * Implementation of basic binding mechanisms, such as URI template instantiation.
 */
abstract public class BaseProtocolBinding implements ProtocolBinding {

  @Override
  public Operation bind(String targetURITemplate, Map<String, Object> formFields, Map<String, Object> uriVariableMappings) {
    String targetURI = new URITemplate(targetURITemplate).createUri(uriVariableMappings);
    return bind(targetURI, formFields);
  }

}
