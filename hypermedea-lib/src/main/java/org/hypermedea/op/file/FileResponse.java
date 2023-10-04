package org.hypermedea.op.file;

import jason.asSyntax.Literal;
import org.hypermedea.ct.RepresentationHandlers;
import org.hypermedea.op.BaseResponse;
import org.hypermedea.op.Operation;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

public class FileResponse extends BaseResponse {

    private final ResponseStatus status;

    private final Optional<FileInputStream> payloadOpt;

    public FileResponse(Operation op) {
        this(op, ResponseStatus.OK);
    }

    public FileResponse(Operation op, ResponseStatus status) {
        super(op);
        this.status = status;
        this.payloadOpt = Optional.empty();
    }

    public FileResponse(Operation op, FileInputStream in) {
        super(op);
        this.status = ResponseStatus.OK;
        this.payloadOpt = Optional.of(in);
    }

    @Override
    public ResponseStatus getStatus() {
        return status;
    }

    @Override
    public Collection<Literal> getPayload() {
        if (payloadOpt.isEmpty()) return new HashSet<>();

        FileInputStream in = payloadOpt.get();
        // TODO check file extension to guess Content-Type
        // TODO deserialize in Op object instead?
        try {
            return RepresentationHandlers.deserialize(in, operation.getTargetURI(), "text/plain");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
