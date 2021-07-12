package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;

import java.util.*;

/**
 * Wrapper for a Jason term encoding a PDDL logical expression.
 *
 * @author Victor Charpenay
 */
public class TermExpWrapper {

    private final Structure expTerm;

    private Exp exp;

    private List<TermExpWrapper> children = new ArrayList<>();

    private List<Symbol> predicate = null;

    private Set<Symbol> constants = null;

    /**
     * Construct a wrapper for a PDDL logical expression specified as a Jason term.
     *
     * @param expTerm
     */
    public TermExpWrapper(Term expTerm) throws TermWrapperException {
        if (!expTerm.isAtom() && !expTerm.isStructure()) throw new TermWrapperException(expTerm, "expected atom or structure");

        if (expTerm.isAtom()) {
            try {
                this.expTerm = ASSyntax.parseStructure(expTerm.toString());
            } catch (ParseException e) {
                throw new TermWrapperException(expTerm, "the input atom has no valid name");
            }
        } else {
            this.expTerm = (Structure) expTerm;
        }

        if (expTerm.equals(Literal.LTrue)) this.exp = new Exp(Connective.TRUE);
        else parseTerm();
    }

    /**
     * Turn a Jason structure into a PDDL logical expression.
     *
     * @return a PDDL logical expression
     */
    public Exp getExp() {
        return this.exp;
    }

    private void parseTerm() throws TermWrapperException {
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
            constants = new HashSet<>();

            Symbol predicateName = new Symbol(Symbol.Kind.PREDICATE, expTerm.getFunctor());
            predicate.add(predicateName);

            if (expTerm.hasTerm()) {
                for (Term st : expTerm.getTerms()) {
                    Symbol s = new TermSymbolWrapper(st).getSymbol();

                    if (s.getKind().equals(Symbol.Kind.CONSTANT)) constants.add(s);

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
            for (TermExpWrapper w : children) preds.putAll(w.getPredicates());
        }

        return preds;
    }

    /**
     * List constants used in the PDDL logical expression
     *
     * @return a set of constants
     */
    public Set<Symbol> getConstants() {
        if (exp.getConnective().equals(Connective.ATOM)) {
            return constants;
        } else {
            Set<Symbol> constants = new HashSet<>();

            for (TermExpWrapper w : children) constants.addAll(w.getConstants());

            return constants;
        }
    }

}
