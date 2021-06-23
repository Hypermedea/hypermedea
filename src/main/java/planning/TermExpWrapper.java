package planning;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

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

    /**
     * Construct a wrapper for a PDDL logical expression specified as a Jason term.
     *
     * @param expTerm
     */
    public TermExpWrapper(Term expTerm) {
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

    private void parseTerm() {
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
            predicate.add(predicateName);

            for (Term st : expTerm.getTerms()) {
                // predicate is not well-formed
                if (!(st.isString() || st.isAtom())) exp = null; // FIXME throw new exception instead

                Symbol.Kind kind = null;

                if (st.isString()) kind = Symbol.Kind.VARIABLE;
                else if (st.isAtom()) kind = Symbol.Kind.CONSTANT;

                predicate.add(new Symbol(kind, st.toString()));
            }

            exp.setAtom(predicate);
        } else {
            exp = new Exp(con);

            for (Term st : expTerm.getTerms()) {
                // expression is not well-formed
                if (!st.isStructure()) exp = null; // FIXME throw new exception instead

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

}
