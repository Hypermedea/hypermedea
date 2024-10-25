package org.hypermedea.op;

import org.hypermedea.tools.URITemplate;

import java.util.Map;

/**
 * Implementation of basic binding mechanisms, such as URI template instantiation.
 */
abstract public class BaseProtocolBinding implements ProtocolBinding {

  @Override
  public Operation bind(String targetURI, Map<String, Object> formFields) throws InvalidFormException {
    String method = getMethod(formFields);

    switch (method) {
      case Operation.GET: return bindGet(targetURI, formFields);
      case Operation.WATCH: return bindWatch(targetURI, formFields);
      case Operation.PUT: return bindPut(targetURI, formFields);
      case Operation.POST: return bindPost(targetURI, formFields);
      case Operation.PATCH: return bindPatch(targetURI, formFields);
      case Operation.DELETE: return bindDelete(targetURI, formFields);
      default: throw new InvalidFormException("Unknown operation method: " + method);
    }
  }

  @Override
  public Operation bind(String targetURITemplate, Map<String, Object> formFields, Map<String, Object> uriVariableMappings) {
    String targetURI = new URITemplate(targetURITemplate).createUri(uriVariableMappings);
    return bind(targetURI, formFields);
  }

  protected Operation bindGet(String targetURI, Map<String, Object> formFields) {
    throw new InvalidFormException("Method not supported by " + getProtocol() + " binding: " + Operation.GET);
  }

  protected Operation bindWatch(String targetURI, Map<String, Object> formFields) {
    throw new InvalidFormException("Method not supported by " + getProtocol() + " binding: " + Operation.WATCH);
  }

  protected Operation bindPut(String targetURI, Map<String, Object> formFields) {
    throw new InvalidFormException("Method not supported by " + getProtocol() + " binding: " + Operation.PUT);
  }

  protected Operation bindPost(String targetURI, Map<String, Object> formFields) {
    throw new InvalidFormException("Method not supported by " + getProtocol() + " binding: " + Operation.POST);
  }

  protected Operation bindPatch(String targetURI, Map<String, Object> formFields) {
    throw new InvalidFormException("Method not supported by " + getProtocol() + " binding: " + Operation.PATCH);
  }

  protected Operation bindDelete(String targetURI, Map<String, Object> formFields) {
    throw new InvalidFormException("Method not supported by " + getProtocol() + " binding: " + Operation.DELETE);
  }

  protected String getMethod(Map<String, Object> form) {
    return (String) form.get(Operation.METHOD_NAME_FIELD);
  }

}
