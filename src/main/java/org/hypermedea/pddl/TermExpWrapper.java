package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.tools.Identifiers;

import java.util.*;

/**
 * Wrapper for a Jason term encoding a PDDL logical expression.
 *
 * @author Victor Charpenay
 */
public class TermExpWrapper extends TermWrapper {

    private Exp exp;

    private List<Symbol> predicate = null;

    /**
     * Construct a wrapper for a PDDL logical expression specified as a Jason term.
     *
     * @param expTerm
     */
    public TermExpWrapper(Term expTerm) throws TermWrapperException {
        super(expTerm);

        if (!expTerm.isAtom() && !expTerm.isStructure() && !expTerm.equals(Literal.LTrue)) throw new TermWrapperException(expTerm, "expected atom or structure");

        if (expTerm.equals(Literal.LTrue)) this.exp = new Exp(Connective.TRUE);
        else if (expTerm.isAtom()) parseTerm(asAtomicStructure((Atom) expTerm));
        else parseTerm((Structure) expTerm);
    }

    /**
     * Turn a Jason structure into a PDDL logical expression.
     *
     * @return a PDDL logical expression
     */
    public Exp getExp() {
        return this.exp;
    }

    private void parseTerm(Structure expTerm) throws TermWrapperException {
        Connective con = null;

        try {
            String str = expTerm.getFunctor().trim().toUpperCase();
            // FIXME "not" is serialized by Jason as "not ", which must be trimmed before parsing
            con = Connective.valueOf(str);
        } catch (IllegalArgumentException e) {
            // do nothing, structure is a predicate
        }

        if (con == null) {
            // atomic predicate
            exp = new Exp(Connective.ATOM);

            predicate = new ArrayList<>();

            Symbol predicateName = new Symbol(Symbol.Kind.PREDICATE, expTerm.getFunctor());
            addSymbol(predicateName, expTerm);

            predicate.add(predicateName);

            if (expTerm.hasTerm()) {
                for (Term st : expTerm.getTerms()) {
                    Symbol s = new AtomicTermWrapper(st).getSymbol();

                    if (s.getKind().equals(Symbol.Kind.CONSTANT)) addSymbol(s, st);

                    predicate.add(s);
                }
            }

            exp.setAtom(predicate);
        } else {
            exp = new Exp(con);

            for (Term st : expTerm.getTerms()) {
                if (!st.isStructure()) throw new TermWrapperException(st, "expression is not well-formed");

                TermExpWrapper c = new TermExpWrapper(st);

                children.add(c);
                exp.addChild(c.getExp());
            }
        }
    }

    /**
     * List predicates used in the PDDL logical expression.
     *
     * @return a list of predicates with their arity (e.g. [p -> 3, q -> 2])
     */
    public Map<Symbol, Integer> getPredicates() {
        Map<Symbol, Integer> preds = new HashMap<>();

        if (exp.getConnective().equals(Connective.ATOM)) {
            preds.put(predicate.get(0), predicate.size() - 1);
        } else {
            for (TermWrapper w : children) preds.putAll(((TermExpWrapper) w).getPredicates());
        }

        return preds;
    }

    private Structure asAtomicStructure(Atom atom) throws TermWrapperException {
        try {
            return ASSyntax.parseStructure(Identifiers.getLexicalForm(atom));
        } catch (ParseException e) {
            throw new TermWrapperException(atom, "the input atom has no valid name");
        }
    }

}
