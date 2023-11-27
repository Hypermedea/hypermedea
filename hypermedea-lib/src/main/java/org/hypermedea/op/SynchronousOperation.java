package org.hypermedea.op;

import java.io.IOException;
import java.util.Map;

public abstract class SynchronousOperation extends BaseOperation {

    public SynchronousOperation(String targetURI, Map<String, Object> formFields) {
        super(targetURI, formFields);

        if (isAsync()) {
            String msg = String.format("operation cannot be synchronous: %s %s", getMethod(), targetURI);
            throw new InvalidFormException(msg);
        }
    }

    /**
     * Calls {@link BaseOperation#end()} before returning the response.
     * Synchronous operations can only have one response.
     *
     * @return the server's response
     * @throws NoResponseException
     */
    @Override
    public Response getResponse() throws NoResponseException {
        Response r = super.getResponse();

        try {
            end();
        } catch (IOException e) {
            // TODO log or, depending on the error return an error message?
        }

        return r;
    }

}
