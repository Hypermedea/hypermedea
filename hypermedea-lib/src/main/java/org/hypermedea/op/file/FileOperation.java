package org.hypermedea.op.file;

import jason.asSyntax.Structure;
import org.hypermedea.op.BaseOperation;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class FileOperation extends BaseOperation {

    public FileOperation(String targetURI, Map<String, Object> formFields) {
        super(targetURI, formFields);
    }

    @Override
    public void setPayload(Collection<Structure> payload) {

    }

    @Override
    public void sendRequest() throws IOException {

    }

    @Override
    protected Object getPayload() {
        return null;
    }

}
