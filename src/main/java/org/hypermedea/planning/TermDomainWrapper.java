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

    private Domain domain;

    /**
     * Construct a wrapper for a PDDL domain specified as Jason term.
     *
     * @param domainTerm
     */
    public TermDomainWrapper(Term domainTerm) throws TermWrapperException {
        if (!domainTerm.isStructure()) this.domainTerm = null;
        else this.domainTerm = (Structure) domainTerm;

        parseTerm();
    }

    /**
     * Turn a Jason structure (seen as an abstract syntax tree) into a PDDL domain definition.
     *
     * @return a PDDL domain definition
     */
    public Domain getDomain() {
        return domain;
    }

    private void parseTerm() throws TermWrapperException {
        if (!domainTerm.getFunctor().equals("domain")) {
            throw new TermWrapperException(domainTerm, "the structure is not defining a domain");
        }

        if (domainTerm.getArity() < 2) {
            throw new TermWrapperException(domainTerm, "the domain has missing arguments (name, [actions])");
        }

        Term nameTerm = domainTerm.getTerm(0);

        if (!nameTerm.isString() && !nameTerm.isAtom()) {
            throw new TermWrapperException(nameTerm, "domain name is expected to be a string or atom");
        }

        Symbol domainName = new Symbol(Symbol.Kind.DOMAIN, Identifiers.getLexicalForm(domainTerm.getTerm(0)));
        domain = new Domain(domainName);

        if (!domainTerm.getTerm(1).isList()) {
            throw new TermWrapperException(domainTerm.getTerm(1), "action declarations are not given as a list");
        }

        List<Term> declarations = ((ListTerm) domainTerm.getTerm(1)).getAsList();

        for (Term d : declarations) {
            if (d.isStructure()) {
                Structure s = (Structure) d;

                if (s.getFunctor().equals("action")) {
                    if (s.getArity() < 4) {
                        throw new TermWrapperException(s, "action declaration has missing arguments (name, parameters, precondition, effect)");
                    }

                    Term actionNameTerm = s.getTerm(0);

                    if (!actionNameTerm.isString() && !actionNameTerm.isAtom()) {
                        throw new TermWrapperException(actionNameTerm, "action name is expected to be a string or atom");
                    }

                    Symbol actionName = new Symbol(Symbol.Kind.ACTION, Identifiers.getLexicalForm(actionNameTerm));

                    if (!s.getTerm(1).isList()) {
                        throw new TermWrapperException(s.getTerm(1), "action parameters are not a defined as a list");
                    }

                    List<TypedSymbol> params = new ArrayList<>();

                    for (Term p : ((ListTerm) s.getTerm(1)).getAsList()) {
                        if (!p.isString()) throw new TermWrapperException(p, "action parameter is expected to be a string");

                        Symbol v = new Symbol(Symbol.Kind.VARIABLE, Identifiers.getLexicalForm(p));
                        params.add(new TypedSymbol(v));
                    }

                    if (!s.getTerm(2).isStructure()) {
                        throw new TermWrapperException(s.getTerm(2), "action precondition is not well-defined");
                    }

                    TermExpWrapper precondWrapper = new TermExpWrapper(s.getTerm(2));
                    Exp precond = precondWrapper.getExp();

                    if (!s.getTerm(3).isStructure()) {
                        throw new TermWrapperException(s.getTerm(3), "action effect is not well-defined");
                    }

                    TermExpWrapper effectWrapper = new TermExpWrapper(s.getTerm(3));
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
    }

}
