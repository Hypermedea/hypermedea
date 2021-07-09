package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import org.hypermedea.tools.Identifiers;

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
        if (!expTerm.isStructure()) this.expTerm = null;
        else this.expTerm = (Structure) expTerm;

        parseTerm();
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

            for (Term st : expTerm.getTerms()) {
                if (!(st.isString() || st.isAtom())) throw new TermWrapperException(st, "predicate is not well-formed");

                Symbol.Kind kind = null;

                if (st.isString()) kind = Symbol.Kind.VARIABLE;
                else if (st.isAtom()) kind = Symbol.Kind.CONSTANT;

                Symbol arg = new Symbol(kind, Identifiers.getLexicalForm(st));
                if (arg.getKind().equals(Symbol.Kind.CONSTANT)) constants.add(arg);

                predicate.add(arg);
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
     * List open variables in the PDDL logical expression.
     *
     * @return a list of (open) variables
     */
    public Set<Symbol> getOpenVariables() {
        Set<Symbol> vars = new HashSet<>();

        if (exp.getConnective().equals(Connective.ATOM)) {
            for (Symbol s : exp.getAtom()) {
                if (s.getKind().equals(Symbol.Kind.VARIABLE)) vars.add(s);
            }
        } else {
            for (TermExpWrapper w : children) {
                vars.addAll(w.getOpenVariables());
            }

            // TODO take quantified variables into account
        }

        return vars;
    }

    /**
     * List predicates used in the PDDL logical expression.
     *
     * @return a list of predicates with their arity (e.g. [p -> 3, q -> 2])
     */
    public Map<Symbol, Integer> getPredicates() {
        Map<Symbol, Integer> preds = new HashMap<>();

        // TODO deal with predicates with same name but different arities

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
