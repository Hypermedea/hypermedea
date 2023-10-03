package org.hypermedea.op.file;

import jason.asSyntax.Structure;
import org.hypermedea.op.BaseResponse;
import org.hypermedea.op.Operation;

import java.util.Collection;

public class FileResponse extends BaseResponse {

    public FileResponse(Operation op) {
        super(op);
    }

    @Override
    public ResponseStatus getStatus() {
        return null;
    }

    @Override
    public Collection<Structure> getPayload() {
        return null;
    }

}
