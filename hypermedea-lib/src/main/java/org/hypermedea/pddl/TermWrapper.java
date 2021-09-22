package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.Symbol;
import jason.asSyntax.*;
import org.hypermedea.tools.Identifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for a non-atomic Jason term. The wrapper maintains a dictionary, to map case-insensitive PDDL symbols
 * to case-sensitive Jason atoms/strings.
 *
 * @author Victor Charpenay
 */
public abstract class TermWrapper {

    protected final Term term;

    protected List<TermWrapper> children = new ArrayList<>();

    private final Map<Symbol, Term> dictionary = new HashMap<>();

    public TermWrapper(Term term) {
        this.term = term;
    }

    public Map<Symbol, Term> getDictionary() {
        Map<Symbol, Term> dict = new HashMap<>();

        dict.putAll(dictionary);
        for (TermWrapper w : children) dict.putAll(w.getDictionary());

        return dict;
    }

    /**
     * turn string terms into atoms.
     *
     * @param t a Jason term
     * @return either <code>t</code> or an atom, if <code>t</code> is a string
     */
    protected Term normalize(Term t) {
        if (t.isString()) return ASSyntax.createAtom(Identifiers.getLexicalForm(t));
        else return t;
    }

    protected void addSymbol(Symbol s, Term t) {
        if (t.isAtom() || t.isString()) dictionary.put(s, t);
        else if (t.isStructure()) dictionary.put(s, ASSyntax.createAtom(((Structure) t).getFunctor()));
    }

}
