package org.hypermedea.op;

import org.hypermedea.op.file.FileBinding;
import org.hypermedea.op.http.HttpBinding;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class to generate generic operations from TD forms.
 */
public class ProtocolBindings {

  public static final String DEFAULT_SCHEME = "file";

  private static final Map<String, ProtocolBinding> registeredBindings = new HashMap<>();

  static {
    registerBinding(HttpBinding.class.getName());
    registerBinding(FileBinding.class.getName());
  }

  public static Operation bind(String targetURI, Map<String, Object> formFields) throws BindingNotFoundException {
    ProtocolBinding b = getBinding(targetURI);
    return b.bind(targetURI, formFields);
  }

  public static Operation bind(String targetURITemplate, Map<String, Object> formFields, Map<String, Object> uriVariableMappings) {
    ProtocolBinding b = getBinding(targetURITemplate);
    return b.bind(targetURITemplate, formFields, uriVariableMappings);
  }

  public static void registerBinding(String bindingClass) throws BindingNotRegisteredException {
    for (Map.Entry<String, ProtocolBinding> entry : registeredBindings.entrySet()) {
      if (entry.getValue().getClass().getName().equals(bindingClass)) {
        // TODO warn that no change is performed
        return;
      }
    }

    Map<String, ProtocolBinding> newBindings = new HashMap<>();

    try {
      ProtocolBinding binding = (ProtocolBinding) Class.forName(bindingClass).newInstance();

      for (String scheme : binding.getSupportedSchemes()) {
        if (registeredBindings.containsKey(scheme)) {
          // TODO warn that bindings have conflict
        }

        newBindings.put(scheme, binding);
      }
    } catch (Exception e) {
      throw new BindingNotRegisteredException(e);
    }

    registeredBindings.putAll(newBindings);
  }

  private static String getScheme(String uriOrTemplate) {
    int i = uriOrTemplate.indexOf(":");

    if (i < 0) return DEFAULT_SCHEME; // assuming uriOrTemplate is a relative path
    else return uriOrTemplate.substring(0, i);
  }

  private static ProtocolBinding getBinding(String targetURI) {
    String scheme = getScheme(targetURI);

    if (!registeredBindings.containsKey(scheme)) throw new BindingNotFoundException();
    else return registeredBindings.get(scheme);
  }

  private ProtocolBindings() {};

}
