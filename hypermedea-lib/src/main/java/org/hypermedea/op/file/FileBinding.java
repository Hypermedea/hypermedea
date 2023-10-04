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
  protected Operation bindGet(String targetURI, Map<String, Object> formFields) {
    return new ReadFileOperation(targetURI, formFields);
  }

  @Override
  protected Operation bindPut(String targetURI, Map<String, Object> formFields) {
    return new WriteFileOperation(targetURI, formFields, false);
  }

  @Override
  protected Operation bindPost(String targetURI, Map<String, Object> formFields) {
    return new WriteFileOperation(targetURI, formFields, true);
  }

  @Override
  protected Operation bindDelete(String targetURI, Map<String, Object> formFields) {
    return new DeleteFileOperation(targetURI, formFields);
  }

}
