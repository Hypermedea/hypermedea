package planning;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TermDomainWrapper {

    private final Structure domainTerm;

    /**
     * Construct a wrapper for a PDDL domain specified as Jason term.
     *
     * @param domainTerm
     */
    public TermDomainWrapper(Term domainTerm) {
        if (!domainTerm.isStructure()) this.domainTerm = null;
        else this.domainTerm = (Structure) domainTerm;
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

                    // action precondition is not well-defined
                    if (!s.getTerm(1).isStructure()) return null;

                    TermExpWrapper w = new TermExpWrapper(s.getTerm(1));
                    Exp precond = w.getExp();

                    List<TypedSymbol> params = new ArrayList<>();

                    for (Symbol v : w.getOpenVariables()) params.add(new TypedSymbol(v));

                    // action effect is not well-defined
                    if (!s.getTerm(2).isStructure()) return null;

                    Exp effect = new TermExpWrapper(s.getTerm(2)).getExp();

                    Op action = new Op(actionName, params, precond, effect);
                    domain.addOperator(action);
                }
                // TODO other declaration types (derived predicates, constraints)
            } else {
                //log(String.format("warning: ignoring domain declaration %s", d));
            }
        }

        // TODO list predicates from actions
        // TODO or: let agents define "fluents" (predicates with changing variables);
        //  to allow for typed variables with (non-fluents) unary preds

        return domain;
    }

}
