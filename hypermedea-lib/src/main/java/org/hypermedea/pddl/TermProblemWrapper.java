package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import org.hypermedea.tools.Identifiers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wrapper for a Jason term encoding a PDDL problem.
 *
 * @author Victor Charpenay
 */
public class TermProblemWrapper extends TermWrapper {

    private Problem problem;

    /**
     * Construct a wrapper for a PDDL problem specified as Jason term.
     *
     * @param problemTerm
     */
    public TermProblemWrapper(Term problemTerm) throws TermWrapperException {
        super(problemTerm);

        if (!problemTerm.isStructure()) throw new TermWrapperException(problemTerm, "expected structure");

        parseTerm((Structure) problemTerm);
    }

    /**
     * Turn its internal Jason structure (seen as an abstract syntax tree) into a PDDL problem definition.
     *
     * @return a PDDL problem definition
     */
    public Problem getProblem() {
        return problem;
    }

    private void parseTerm(Structure problemTerm) throws TermWrapperException {
        if (!problemTerm.getFunctor().equals("problem")) {
            throw new TermWrapperException(problemTerm, "the structure is not defining a problem");
        }

        if (problemTerm.getArity() < 4) {
            throw new TermWrapperException(problemTerm, "the problem has missing arguments (name, domainName, initialState, goalState)");
        }

        Term pbNameTerm = problemTerm.getTerm(0);

        if (!pbNameTerm.isString() && !pbNameTerm.isAtom()) {
            throw new TermWrapperException(pbNameTerm, "problem name is expected to be a string or atom");
        }

        pbNameTerm = normalize(pbNameTerm);

        Symbol pbName = new Symbol(Symbol.Kind.PROBLEM, Identifiers.getLexicalForm(pbNameTerm));
        problem = new Problem(pbName);

        addSymbol(pbName, pbNameTerm);

        Term domainTerm = problemTerm.getTerm(1);

        if (!domainTerm.isString() && !domainTerm.isAtom()) {
            throw new TermWrapperException(problemTerm, "domain reference is expected to be a string or atom");
        }

        Symbol domainName = new Symbol(Symbol.Kind.DOMAIN, Identifiers.getLexicalForm(domainTerm));
        problem.setDomain(domainName);

        if (!problemTerm.getTerm(2).isList()) {
            throw new TermWrapperException(problemTerm, "the problem has no initial state");
        }

        List<Term> initialFacts = ((ListTerm) problemTerm.getTerm(2)).getAsList();

        for (Term f : initialFacts) {
            TermExpWrapper w = new TermExpWrapper(f);

            problem.addInitialFact(w.getExp());

            children.add(w);
        }

        if (!problemTerm.getTerm(3).isStructure()) {
            throw new TermWrapperException(problemTerm, "goal state is not well-defined");
        }

        TermExpWrapper w = new TermExpWrapper(problemTerm.getTerm(3));

        problem.setGoal(w.getExp());

        children.add(w);

        for (Symbol o : getDictionary().keySet()) {
            if (o.getKind().equals(Symbol.Kind.CONSTANT)) problem.addObject(new TypedSymbol(o));
        }
    }

}
