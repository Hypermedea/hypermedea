package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.*;
import org.hypermedea.tools.Identifiers;

import java.util.*;

/**
 * Wrapper for a PDDL domain.
 *
 * @author Victor Charpenay
 */
public class TermDomainWrapper extends TermWrapper {

    /**
     * recursively browse a PDDL expression and collect predicates (name and arity)
     */
    private class PredicateCollector {

        private Map<Symbol, Integer> predicates = new HashMap<>();

        public void visit(Exp exp) {
            if (exp.getChildren() != null) {
                for (Exp c : exp.getChildren()) visit(c);
            }

            List<Symbol> p = exp.getAtom();

            if (p != null) predicates.put(p.get(0), p.size() - 1);
        }

        public Map<Symbol, Integer> getPredicates() {
            return predicates;
        }
    }

    private Domain domain;

    /**
     * Construct a wrapper for a PDDL domain specified as Jason term.
     *
     * @param domainTerm
     */
    public TermDomainWrapper(Term domainTerm) throws TermWrapperException {
        super(domainTerm);

        if (!domainTerm.isStructure()) throw new TermWrapperException(domainTerm, "expected structure");

        parseTerm((Structure) term);
    }

    /**
     * Turn a Jason structure (seen as an abstract syntax tree) into a PDDL domain definition.
     *
     * @return a PDDL domain definition
     */
    public Domain getDomain() {
        return domain;
    }

    private void parseTerm(Structure domainTerm) throws TermWrapperException {
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

        nameTerm = normalize(nameTerm);

        Symbol domainName = new Symbol(Symbol.Kind.DOMAIN, Identifiers.getLexicalForm(nameTerm));
        domain = new Domain(domainName);

        addSymbol(domainName, nameTerm);

        if (!domainTerm.getTerm(1).isList()) {
            throw new TermWrapperException(domainTerm.getTerm(1), "action declarations are not given as a list");
        }

        PredicateCollector collector = new PredicateCollector();

        List<Term> declarations = ((ListTerm) domainTerm.getTerm(1)).getAsList();

        for (Term d : declarations) {
            TermActionWrapper w = new TermActionWrapper(d);

            children.add(w);

            for (TermWrapper ew : w.children) {
                if (ew instanceof TermExpWrapper) collector.visit(((TermExpWrapper) ew).getExp());
            }

            domain.addOperator(w.getAction());
        }

        // TODO other declaration types (derived predicates, constraints)

        for (Map.Entry<Symbol, Integer> entry : collector.getPredicates().entrySet()) {
            NamedTypedList p = new NamedTypedList(entry.getKey());

            for (int i = 0; i < entry.getValue(); i++) {
                Symbol v = new Symbol(Symbol.Kind.VARIABLE, String.format("?var%d", i));
                p.add(new TypedSymbol(v));
            }

            domain.addPredicate(p);
        }
    }

}
