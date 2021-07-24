package org.hypermedea.json;

import jason.NoValueException;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import org.hypermedea.tools.Identifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for a JSON value built from a Jason term. Used by the <code>ThingArtifact</code> to read/write payloads.
 *
 * The mapping between JSON objects and Jason terms is inspired by the SWI-Prolog JSON library.
 * See https://www.swi-prolog.org/pldoc/man?section=jsonsupport
 *
 * TODO use TermWrapperException (and move it to upper package)
 *
 * @author Victor Charpenay
 */
public class TermJsonWrapper {

    public static final String JSON_FUNCTOR = "json";

    public static final String JSON_MEMBER_FUNCTOR = "kv";

    private Object value = null;

    public TermJsonWrapper(Term t) {
        parseTerm(t);
    }

    public boolean isJsonBoolean() {
        return value != null && value instanceof Boolean;
    }

    public boolean isJsonNumber() {
        return value != null && (value instanceof Double || value instanceof Long);
    }

    public boolean isJsonString() {
        return value != null && value instanceof String;
    }

    public boolean isJsonArray() {
        return value != null && value instanceof List;
    }

    public boolean isJsonObject() {
        return value != null && value instanceof Map;
    }

    public Boolean getJsonBoolean() {
        if (isJsonBoolean()) return (Boolean) value;
        else return null;
    }

    public Number getJsonNumber() {
        if (isJsonNumber() && value instanceof Double) return (Double) value;
        else if (isJsonNumber() && value instanceof Long) return (Long) value;
        else return null;
    }

    public String getJsonString() {
        if (isJsonString()) return (String) value;
        else return null;
    }

    public List<Object> getJsonArray() {
        if (isJsonArray()) return (List<Object>) value;
        else return null;
    }

    public Map<String, Object> getJsonObject() {
        if (isJsonObject()) return (Map<String, Object>) value;
        else return null;
    }

    public Object getJsonValue() {
        return value;
    }

    private void parseTerm(Term t) {
        if (t.isAtom() && t.equals(Literal.LTrue)) {
            value = true;
        } else if (t.isAtom() && t.equals(Literal.LFalse)) {
            value = false;
        } else if (t.isNumeric()) {
            try {
                double d = ((NumberTerm) t).solve();

                if (isInteger(d)) value = (long) d;
                else value = d;
            } catch (NoValueException e) {
                // TODO log
                e.printStackTrace();
            }
        } else if (t.isString() || t.isAtom()) {
            if (t.isAtom() && ((Atom) t).getFunctor().equals("null")) return; // JSON value is null

            value = Identifiers.getLexicalForm(t);
        } else if (t.isList()) {
            List<Object> l = new ArrayList<>();
            value = l;

            for (Term member : ((ListTerm) t).getAsList()) l.add(new TermJsonWrapper(member).getJsonValue());
        } else if (t.isStructure()) {
            Structure json = (Structure) t;

            if (!json.getFunctor().equals(JSON_FUNCTOR) || json.getArity() != 1 || !json.getTerm(0).isList()) return;

            Map<String, Object> obj = new HashMap<>();
            value = obj;

            ListTerm l = (ListTerm) json.getTerm(0);

            for (Term member : l.getAsList()) parseMember(obj, member);
        }
    }

    private void parseMember(Map<String, Object> obj, Term member) {
        if (member.isStructure()) {
            Structure kv = (Structure) member;

            if (!kv.getFunctor().equals(JSON_MEMBER_FUNCTOR) && kv.getArity() != 2 && !kv.getTerm(0).isAtom()) return;

            String key = Identifiers.getLexicalForm(kv.getTerm(0));
            Object value = new TermJsonWrapper(kv.getTerm(1)).getJsonValue();

            obj.put(key, value);
        }
    }

    private boolean isInteger(Double nb) {
        return nb - nb.longValue() == 0;
    }

}
