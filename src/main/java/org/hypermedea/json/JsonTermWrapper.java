package org.hypermedea.json;

import jason.asSyntax.*;

import java.util.List;
import java.util.Map;

/**
 * Inverse of <code>TermJsonWrapper</code>: wrapper for a Jason term built from a JSON value.
 *
 * @author Victor Charpenay
 */
public class JsonTermWrapper {

    private Term term;

    public JsonTermWrapper(Object value) {
        parseValue(value);
    }

    public Term getTerm() {
        return term;
    }

    private void parseValue(Object value) {
        if (value instanceof Boolean) {
            if (((Boolean) value).booleanValue()) term = Literal.LTrue;
            else term = Literal.LFalse;
        } else if (value instanceof Double) {
            term = ASSyntax.createNumber((Double) value);
        } else if (value instanceof Long) {
            term = ASSyntax.createNumber(((Long) value).doubleValue());
        } else if (value instanceof String) {
            term = ASSyntax.createString(value);
        } else if (value instanceof List) {
            ListTerm l = ASSyntax.createList();
            term = l;

            for (Object member : (List<Object>) value) l.add(new JsonTermWrapper(member).getTerm());
        } else if (value instanceof Map) {
            ListTerm l = ASSyntax.createList();

            for (Map.Entry<String, Object> member : ((Map<String, Object>) value).entrySet()) {
                Atom k = ASSyntax.createAtom(member.getKey());
                Term v = new JsonTermWrapper(member.getValue()).getTerm();

                Structure kv = ASSyntax.createStructure(TermJsonWrapper.JSON_MEMBER_FUNCTOR, k, v);

                l.add(kv);
            }

            term = ASSyntax.createStructure(TermJsonWrapper.JSON_FUNCTOR, l);
        }
    }

}
