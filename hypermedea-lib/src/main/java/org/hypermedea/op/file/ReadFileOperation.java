package org.hypermedea.op.file;

import jason.asSyntax.Literal;
import org.hypermedea.op.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class ReadFileOperation extends FileOperation {

    public ReadFileOperation(String targetURI, Map<String, Object> formFields) {
        super(targetURI, formFields);

        if (!formFields.containsKey(METHOD_NAME_FIELD)) formFields.put(METHOD_NAME_FIELD, GET);
    }

    @Override
    public void setPayload(Collection<Literal> payload) {
        // TODO log that call is ignored
    }

    @Override
    protected void sendSingleRequest() throws IOException {
        if (file.exists() && file.canRead()) {
            FileInputStream in = new FileInputStream(file);
            onResponse(new FileResponse(this, in));
        } else {
            onResponse(new FileResponse(this, Response.ResponseStatus.CLIENT_ERROR));
        }
    }

}
