package org.hypermedea.op.file;

import jason.asSyntax.Literal;
import org.hypermedea.op.Response;

import java.util.Collection;
import java.util.Map;

public class DeleteFileOperation extends FileOperation {

    public DeleteFileOperation(String targetURI, Map<String, Object> formFields) {
        super(targetURI, formFields);
    }

    @Override
    public void setPayload(Collection<Literal> payload) {
        // TODO log that call is ignored
    }

    @Override
    protected void sendSingleRequest() {
        boolean success = file.delete();

        if (success) onResponse(new FileResponse(this));
        else onResponse(new FileResponse(this, Response.ResponseStatus.CLIENT_ERROR));
    }

    @Override
    protected Object getPayload() {
        return null;
    }

}
