package org.hypermedea.ct;

public class UnsupportedRepresentationException extends IllegalArgumentException {

    public UnsupportedRepresentationException() {
        super();
    }

    public UnsupportedRepresentationException(String msg) {
        super(msg);
    }

    public UnsupportedRepresentationException(Throwable e) {
        super(e);
    }

    public UnsupportedRepresentationException(String msg, Throwable e) {
        super(msg, e);
    }

}
