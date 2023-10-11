package org.hypermedea.op.file;

import jason.asSyntax.Literal;
import org.hypermedea.ct.RepresentationHandlers;
import org.hypermedea.op.Response;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class WriteFileOperation extends FileOperation {

    private final Boolean append;

    private Collection<Literal> payload = new HashSet<>();

    public WriteFileOperation(String targetURI, Map<String, Object> formFields, Boolean append) {
        super(targetURI, formFields);
        this.append = append;
    }

    @Override
    public void setPayload(Collection<Literal> payload) {
        this.payload = payload;
    }

    @Override
    protected void sendSingleRequest() throws IOException {
        FileOutputStream out = new FileOutputStream(file, append);
        RepresentationHandlers.serialize(payload, out, target);

        onResponse(new FileResponse(this, Response.ResponseStatus.OK));
    }

    @Override
    protected Object getPayload() {
        return null;
    }

}
