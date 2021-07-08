package org.hypermedea.planning;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import org.hypermedea.tools.Identifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        Symbol domainName = new Symbol(Symbol.Kind.DOMAIN, Identifiers.getLexicalForm(domainTerm.getTerm(0)));
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

                    Symbol actionName = new Symbol(Symbol.Kind.ACTION, Identifiers.getLexicalForm(s.getTerm(0)));

                    // action precondition is not well-defined
                    if (!s.getTerm(1).isStructure()) return null;

                    TermExpWrapper precondWrapper = new TermExpWrapper(s.getTerm(1));
                    Exp precond = precondWrapper.getExp();

                    List<TypedSymbol> params = new ArrayList<>();

                    for (Symbol v : precondWrapper.getOpenVariables()) params.add(new TypedSymbol(v));

                    // action effect is not well-defined
                    if (!s.getTerm(2).isStructure()) return null;

                    TermExpWrapper effectWrapper = new TermExpWrapper(s.getTerm(2));
                    Exp effect = effectWrapper.getExp();

                    Map<Symbol, Integer> preds = precondWrapper.getPredicates();
                    preds.putAll(effectWrapper.getPredicates());

                    for (Symbol name : preds.keySet()) {
                        NamedTypedList p = new NamedTypedList(name);

                        for (int i = 0; i < preds.get(name); i++) {
                            Symbol v = new Symbol(Symbol.Kind.VARIABLE, String.format("?var%d", i));
                            p.add(new TypedSymbol(v));
                        }

                        domain.addPredicate(p);
                    }

                    Op action = new Op(actionName, params, precond, effect);
                    domain.addOperator(action);
                }
                // TODO other declaration types (derived predicates, constraints)
            } else {
                //log(String.format("warning: ignoring domain declaration %s", d));
            }
        }

        return domain;
    }

}
