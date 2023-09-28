package org.hypermedea.op;

import java.util.Collection;
import java.util.Map;

public interface ProtocolBinding {

  String getProtocol();

  Collection<String> getSupportedSchemes();

  Operation bind(String targetURI, Map<String, Object> formFields) throws InvalidFormException;

  Operation bind(String targetURITemplate, Map<String, Object> formFields, Map<String, Object> uriVariableMappings) throws InvalidFormException;

}
