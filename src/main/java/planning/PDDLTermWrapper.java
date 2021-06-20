package planning;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for Jason tersm encoding a PDDL domain and problem.
 *
 * @author Victor Charpenay
 */
public class PDDLTermWrapper {

    private final Structure domainTerm;

    private final Structure problemTerm;

    /**
     * Construct a wrapper for a PDDL domain/problem pair specified as Jason terms.
     *
     * @param domainTerm
     * @param problemTerm
     */
    public PDDLTermWrapper(Term domainTerm, Term problemTerm) {
        if (!domainTerm.isStructure()) this.domainTerm = null;
        else this.domainTerm = (Structure) domainTerm;

        if (!problemTerm.isStructure()) this.problemTerm = null;
        else this.problemTerm = (Structure) problemTerm;
    }

    /**
     * Turn a Jason structure (seen as an abstract syntax tree) into a PDDL domain definition.
     *
     * @return a PDDL domain definition
     */
    public Domain getDomain() {
        // the structure is not defining a domain
        if (!domainTerm.getFunctor().equals("domain")) return null;

        // the domain has no name
        if (domainTerm.getArity() < 1 || !domainTerm.getTerm(0).isString()) return null;

        Symbol domainName = new Symbol(Symbol.Kind.DOMAIN, domainTerm.getTerm(0).toString());
        Domain domain = new Domain(domainName);

        // declarations are absent or not given as a list
        if (domainTerm.getArity() != 2 || !domainTerm.getTerm(1).isList()) return null;

        List<Term> declarations = ((ListTerm) domainTerm.getTerm(1)).getAsList();

        for (Term d : declarations) {
            if (d.isStructure()) {
                Structure s = (Structure) d;

                if (s.getFunctor().equals("action")) {
                    // action declaration has missing arguments (name, precondition, effect)
                    if (s.getArity() < 3) return null;

                    // action name is not a string
                    if (!s.getTerm(0).isString()) return null;

                    Symbol actionName = new Symbol(Symbol.Kind.ACTION, s.getTerm(0).toString());
                    List<TypedSymbol> params = new ArrayList<>();

                    // action precondition is not well-defined
                    if (!s.getTerm(1).isStructure()) return null;

                    Exp precond = asLogicalExpression((Structure) s.getTerm(1));

                    // action effect is not well-defined
                    if (!s.getTerm(2).isStructure()) return null;

                    Exp effect = asLogicalExpression((Structure) s.getTerm(2));

                    Op action = new Op(actionName, params, precond, effect);
                    domain.addOperator(action);
                }
                // TODO other declaration types (derived predicates, constraints)
            } else {
                //log(String.format("warning: ignoring domain declaration %s", d));
            }
        }

        // TODO list predicates from actions

        return domain;
    }

    /**
     * Turn a Jason structure (seen as an abstract syntax tree) into a PDDL problem definition.
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

        for (Term f : initialFacts) {
            // initial fact is not well-defined
            if (!f.isStructure()) return null;

            pb.addInitialFact(asLogicalExpression((Structure) f));
        }

        // the domain has no goal state
        if (problemTerm.getArity() < 4 || !problemTerm.getTerm(3).isStructure()) return null;

        // goal state is not well-defined
        if (!problemTerm.getTerm(3).isStructure()) return null;

        pb.setGoal(asLogicalExpression((Structure) problemTerm.getTerm(3)));

        // TODO list objects from initialState

        return pb;
    }

    /**
     * Turn a Jason structure into a PDDL logical expression.
     *
     * @param s an abstract expression as Jason structure
     * @return a PDDL logical expression
     */
    private Exp asLogicalExpression(Structure s) {
        Exp exp;

        Connective con = null;

        try {
            String str = s.getFunctor().trim().toUpperCase();
            // FIXME "not" is serialized by Jason as "not ", which must be trimmed before parsing
            con = Connective.valueOf(str);
        } catch (IllegalArgumentException e) {
            // do nothing, structure is a predicate
        }

        if (con == null) {
            // atomic predicate
            exp = new Exp(Connective.ATOM);

            List<Symbol> predicate = new ArrayList<>();

            Symbol predicateName = new Symbol(Symbol.Kind.PREDICATE, s.getFunctor());
            predicate.add(predicateName);

            for (Term st : s.getTerms()) {
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

            for (Term st : s.getTerms()) {
                // expression is not well-formed
                if (!st.isStructure()) return null;

                exp.addChild(asLogicalExpression((Structure) st));
            }
        }

        return exp;
    }

}
