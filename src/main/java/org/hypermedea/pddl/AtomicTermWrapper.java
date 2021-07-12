package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.Connective;
import fr.uga.pddl4j.parser.Exp;
import fr.uga.pddl4j.parser.Symbol;
import jason.asSyntax.*;
import org.hypermedea.tools.Identifiers;

/**
 * Wrapper for a Jason atom or string encoding a PDDL symbol (constant or variable).
 * If the input string starts with '?', the term is interpreted as a variable.
 *
 * @author Victor Charpenay
 */
public class AtomicTermWrapper {

    private static String STRING_SUFFIX = "-str";

    private final Term term;

    private Symbol symbol;

    public AtomicTermWrapper(Term term) throws TermWrapperException {
        if (!(term.isString() || term.isAtom())) throw new TermWrapperException(term, "expected atom or string");

        this.term = term;

        parseTerm();
    }

    public Symbol getSymbol() {
        return symbol;
    }

    private void parseTerm() {
        Symbol.Kind kind = term.isString() && term.toString().startsWith("\"?")
            ? Symbol.Kind.VARIABLE
            : Symbol.Kind.CONSTANT;

        String lex = Identifiers.getLexicalForm(term);

        if (term.isString() && kind.equals(Symbol.Kind.CONSTANT)) lex += STRING_SUFFIX;

        symbol = new Symbol(kind, lex);
    }

}
