package org.hypermedea.op.file;

import jason.asSyntax.Literal;
import org.hypermedea.ct.RepresentationHandlers;
import org.hypermedea.op.BaseResponse;
import org.hypermedea.op.Operation;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class FileResponse extends BaseResponse {

    public static final String DEFAULT_FILE_CT = "text/plain";

    private static final Map<String, String> knownContentTypes = new HashMap<>();

    static {
        // TODO write in file resource instead
        knownContentTypes.put("ttl", "text/turtle");
        knownContentTypes.put("nt", "application/n-triples");
        knownContentTypes.put("jsonld", "application/ld+json");
        knownContentTypes.put("trig", "application/trig");
        knownContentTypes.put("nq", "application/n-quads");
        knownContentTypes.put("rdf", "application/rdf+xml");

        knownContentTypes.put("json", "application/json");

        knownContentTypes.put("txt", "text/plain");
    }

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
        // TODO deserialize before setting response status (in ReadFileOperation)?
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
        try {
            String uri = operation.getTargetURI();
            return RepresentationHandlers.deserialize(in, uri, getContentType(uri));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getContentType(String targetURI) {
        int i = targetURI.lastIndexOf(".");

        if (i > 0) {
            String ext = targetURI.substring(i + 1);

            if (knownContentTypes.containsKey(ext))
                return knownContentTypes.get(ext);
        }

        // note: if i == 0, assuming file is hidden and has no extension
        return DEFAULT_FILE_CT;
    }

}
