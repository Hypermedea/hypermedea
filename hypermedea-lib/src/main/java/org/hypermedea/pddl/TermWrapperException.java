package org.hypermedea.pddl;

import jason.asSyntax.Term;

public class TermWrapperException extends Exception {

    private static final String MSG_TEMPLATE = "Could not parse structure: %s. Cause: %s.";

    public TermWrapperException(Term term) {
        super(String.format(MSG_TEMPLATE, term, "unknown error"));
    }

    public TermWrapperException(Term term, String msg) {
        super(String.format(MSG_TEMPLATE, term, msg));
    }

}
