package planning;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper for a Jason term encoding a PDDL problem.
 *
 * @author Victor Charpenay
 */
public class TermProblemWrapper {

    private final Structure problemTerm;

    /**
     * Construct a wrapper for a PDDL problem specified as Jason term.
     *
     * @param problemTerm
     */
    public TermProblemWrapper(Term problemTerm) {
        if (!problemTerm.isStructure()) this.problemTerm = null;
        else this.problemTerm = (Structure) problemTerm;
    }

    /**
     * Turn its internal Jason structure (seen as an abstract syntax tree) into a PDDL problem definition.
     *
     * @return a PDDL problem definition
     */
    public Problem getProblem() {
        if (!problemTerm.getFunctor().equals("problem")) return null;

        // the problem has no name
        if (problemTerm.getArity() < 1 || !problemTerm.getTerm(0).isString()) return null;

        Symbol pbName = new Symbol(Symbol.Kind.PROBLEM, problemTerm.getTerm(0).toString());
        Problem pb = new Problem(pbName);

        // the problem has no domain reference
        if (problemTerm.getArity() < 2 || !problemTerm.getTerm(1).isString()) return null;

        Symbol domainName = new Symbol(Symbol.Kind.DOMAIN, problemTerm.getTerm(1).toString());
        pb.setDomain(domainName);

        // the domain has no initial state
        if (problemTerm.getArity() < 3 || !problemTerm.getTerm(2).isList()) return null;

        List<Term> initialFacts = ((ListTerm) problemTerm.getTerm(2)).getAsList();

        Set<Symbol> objects = new HashSet<>();

        for (Term f : initialFacts) {
            // initial fact is not well-defined
            if (!f.isStructure()) return null;

            TermExpWrapper w = new TermExpWrapper(f);

            pb.addInitialFact(w.getExp());
            objects.addAll(w.getConstants());
        }

        for (Symbol o : objects) {
            pb.addObject(new TypedSymbol(o));
        }

        // the domain has no goal state
        if (problemTerm.getArity() < 4 || !problemTerm.getTerm(3).isStructure()) return null;

        // goal state is not well-defined
        if (!problemTerm.getTerm(3).isStructure()) return null;

        pb.setGoal(new TermExpWrapper(problemTerm.getTerm(3)).getExp());

        // TODO list objects from initialState

        return pb;
    }

}
