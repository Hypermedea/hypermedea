package org.hypermedea.tools;

import jason.asSyntax.ListTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility to manage lists of key-value pairs represented in Jason as <code>[kv(K1, V1), kv(K2, V2), ...]</code>, e.g.
 * to represent JSON objects.
 *
 * @see org.hypermedea.ct.json.JsonHandler
 */
public class KVPairs {

    public static final String JSON_MEMBER_FUNCTOR = "kv";

    public static Map<String, Term> getAsMap(ListTerm l) {
        List<Term> objectMembers = l.stream().filter(m -> isObjectMember(m)).collect(Collectors.toList());

        // TODO warn if some non-kv members were found

        Map<String, Term> pairs = new HashMap<>();

        for (Term m : objectMembers) {
            Structure kv = (Structure) m;

            // TODO warn if invalid kv was found
            if (kv.getArity() == 2 && isObjectMemberKey(kv.getTerm(0))) {
                String key = Identifiers.getLexicalForm(kv.getTerm(0));
                Term val = kv.getTerm(1);

                pairs.put(key, val);
            }
        }

        return pairs;
    }

    private static boolean isObjectMember(Term t) {
        return t.isStructure() && ( (Structure) t).getFunctor().equals(JSON_MEMBER_FUNCTOR);
    }

    private static boolean isObjectMemberKey(Term t) {
        return t.isAtom() || t.isString();
    }

}
