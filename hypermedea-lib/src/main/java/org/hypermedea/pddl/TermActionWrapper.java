package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.*;
import jason.asSyntax.ListTerm;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import org.hypermedea.tools.Identifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for a PDDL action definition.
 *
 * @author Victor Charpenay
 */
public class TermActionWrapper extends TermWrapper {

    private Op action;

    public TermActionWrapper(Term actionTerm) throws TermWrapperException {
        super(actionTerm);

        if (!actionTerm.isStructure()) throw new TermWrapperException(actionTerm, "expected structure");

        parseTerm((Structure) actionTerm);
    }

    public Op getAction() {
        return action;
    }

    private void parseTerm(Structure s) throws TermWrapperException {
        if (!s.getFunctor().equals("action")) throw new TermWrapperException(s, "the structure does not define an action");

        if (s.getArity() < 4) {
            throw new TermWrapperException(s, "action declaration has missing arguments (name, parameters, precondition, effect)");
        }

        Term actionNameTerm = s.getTerm(0);

        if (!actionNameTerm.isString() && !actionNameTerm.isAtom()) {
            throw new TermWrapperException(actionNameTerm, "action name is expected to be a string or atom");
        }

        actionNameTerm = normalize(actionNameTerm);

        Symbol actionName = new Symbol(Symbol.Kind.ACTION, Identifiers.getLexicalForm(actionNameTerm));
        addSymbol(actionName, actionNameTerm);

        if (!s.getTerm(1).isList()) {
            throw new TermWrapperException(s.getTerm(1), "action parameters are not a defined as a list");
        }

        List<TypedSymbol> params = new ArrayList<>();

        for (Term p : ((ListTerm) s.getTerm(1)).getAsList()) {
            Symbol v = new AtomicTermWrapper(p).getSymbol();
            params.add(new TypedSymbol(v));
        }

        TermExpWrapper precondWrapper = new TermExpWrapper(s.getTerm(2));
        Exp precond = precondWrapper.getExp();

        children.add(precondWrapper);

        TermExpWrapper effectWrapper = new TermExpWrapper(s.getTerm(3));
        Exp effect = effectWrapper.getExp();

        children.add(effectWrapper);

        action = new Op(actionName, params, precond, effect);
    }

}
