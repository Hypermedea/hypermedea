package planning;

import fr.uga.pddl4j.parser.Connective;
import fr.uga.pddl4j.parser.Exp;
import fr.uga.pddl4j.parser.Symbol;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for a Jason term encoding a PDDL logical expression.
 *
 * @author Victor Charpenay
 */
public class TermExpWrapper {

    private final Structure expTerm;

    /**
     * Construct a wrapper for a PDDL logical expression specified as a Jason term.
     *
     * @param expTerm
     */
    public TermExpWrapper(Term expTerm) {
        if (!expTerm.isStructure()) this.expTerm = null;
        else this.expTerm = (Structure) expTerm;
    }

    /**
     * Turn a Jason structure into a PDDL logical expression.
     *
     * @return a PDDL logical expression
     */
    public Exp getExp() {
        Exp exp;

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

            List<Symbol> predicate = new ArrayList<>();

            Symbol predicateName = new Symbol(Symbol.Kind.PREDICATE, expTerm.getFunctor());
            predicate.add(predicateName);

            for (Term st : expTerm.getTerms()) {
                // predicate is not well-formed
                if (!(st.isString() || st.isAtom())) return null;

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
                if (!st.isStructure()) return null;

                exp.addChild(new TermExpWrapper(st).getExp());
            }
        }

        return exp;
    }

}
