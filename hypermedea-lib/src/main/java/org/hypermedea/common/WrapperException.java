package org.hypermedea.common;

public class WrapperException extends RuntimeException {

    public WrapperException(Throwable e) {
        super(e);
    }

    public WrapperException(String message) {
        super(message);
    }

    public WrapperException(String message, Throwable e) {
        super(message, e);
    }

}
