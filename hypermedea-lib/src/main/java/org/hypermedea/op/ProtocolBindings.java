package org.hypermedea.op;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Factory class to generate generic operations from TD forms.
 */
public class ProtocolBindings {

  public static final String DEFAULT_SCHEME = "file";

  private static final ServiceLoader<ProtocolBinding> loader = ServiceLoader.load(ProtocolBinding.class);

  public static Operation bind(String targetURI, Map<String, Object> formFields) throws BindingNotFoundException {
    targetURI = resolve(targetURI);
    ProtocolBinding b = getBinding(targetURI);
    return b.bind(targetURI, formFields);
  }

  public static Operation bind(String targetURITemplate, Map<String, Object> formFields, Map<String, Object> uriVariableMappings) {
    targetURITemplate = resolve(targetURITemplate);
    ProtocolBinding b = getBinding(targetURITemplate);
    return b.bind(targetURITemplate, formFields, uriVariableMappings);
  }

  private static String getScheme(String uriOrTemplate) {
    int i = uriOrTemplate.indexOf(":");

    if (i < 0) return DEFAULT_SCHEME; // assuming uriOrTemplate is a relative path
    else return uriOrTemplate.substring(0, i);
  }

  private static ProtocolBinding getBinding(String targetURI) {
    String scheme = getScheme(targetURI);
    Optional<ProtocolBinding> opt = loadFromScheme(scheme);

    if (opt.isEmpty()) throw new BindingNotFoundException();
    else return opt.get();
  }

  private static String resolve(String uriOrFilename) {
    if (uriOrFilename.indexOf(":") > 0) return uriOrFilename;
    else return new File(uriOrFilename).toURI().toString();
  }

  private static Optional<ProtocolBinding> loadFromScheme(String scheme) {
    for (ProtocolBinding b : loader) {
      if (b.getSupportedSchemes().contains(scheme)) return Optional.of(b);
    }

    return Optional.empty();
  }

  private ProtocolBindings() {};

}
