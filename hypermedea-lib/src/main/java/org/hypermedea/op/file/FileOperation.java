package org.hypermedea.op.file;

import org.hypermedea.op.BaseOperation;

import java.io.File;
import java.net.URI;
import java.util.Map;

public abstract class FileOperation extends BaseOperation {

    protected final File file;

    public FileOperation(String targetURI, Map<String, Object> formFields) {
        super(targetURI, formFields);
        file = new File(URI.create(targetURI));
    }

}
