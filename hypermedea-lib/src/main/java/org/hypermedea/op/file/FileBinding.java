package org.hypermedea.op.file;

import org.hypermedea.op.BaseProtocolBinding;
import org.hypermedea.op.Operation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Default HTTP binding.
 */
public class FileBinding extends BaseProtocolBinding {

  private final static String FILE_PSEUDO_PROTOCOL = "File";

  private final static Collection<String> SUPPORTED_SCHEMES = new HashSet<>();

  static {
    SUPPORTED_SCHEMES.add("file");
  }

  @Override
  public String getProtocol() {
    return FILE_PSEUDO_PROTOCOL;
  }

  @Override
  public Collection<String> getSupportedSchemes() {
    return SUPPORTED_SCHEMES;
  }

  @Override
  public Operation bind(String targetURI, Map<String, Object> formFields) {
    return new FileOperation(targetURI, formFields);
  }

}
