package org.hypermedea.ct.json;

import jason.NoValueException;
import jason.asSyntax.*;
import org.hypermedea.ct.BaseRepresentationHandler;
import org.hypermedea.ct.UnsupportedRepresentationException;
import org.hypermedea.tools.Identifiers;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *     Handler for JSON values.
 * </p>
 * <p>
 *     The mapping between JSON objects and Jason terms is inspired by
 *     <a href="https://www.swi-prolog.org/pldoc/man?section=jsonsupport">the SWI-Prolog JSON library</a>.
 *     Simple JSON values and arrays have a straightforward equivalent in Jason. JSON objects are represented
 *     in Jason as unary structures holding a list of key-value pairs, themselves represented as binary structures.
 *     See examples:
 * </p>
 * <table>
 *     <tr>
 *         <th>JSON</th>
 *         <th>Jason</th>
 *     </tr>
 *     <tr>
 *         <td><code>true</code>, <code>false</code></td>
 *         <td><code>true</code>, <code>false</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>null</code></td>
 *         <td><code>null</code> (encoded as an atom)</td>
 *     </tr>
 *     <tr>
 *         <td><code>42.5</code></td>
 *         <td><code>42.5</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>"abc"</code></td>
 *         <td><code>"abc"</code> or <code>abc</code> (if valid Jason atom)</td>
 *     </tr>
 *     <tr>
 *         <td><code>[ 1, 2, 3, 4 ]</code></td>
 *         <td><code>[ 1, 2, 3, 4 ]</code></td>
 *     </tr>
 *     <tr>
 *         <td><code>{ "a": 12.5, "b": [...] }</code></td>
 *         <td><code>json([kv("a", 12.5), kv("b", [...])])</code> (object keys may also be Jason atoms)</td>
 *     </tr>
 * </table>
 *
 * <p>
 *     Any root JSON value must be enclosed in a <code>json</code> structure.
 *     For instance, the primitive value <code>"abc"</code> or the object <code>[kv("a", 12.5), kv("b", [...])]</code>
 *     are represented respectively as <code>json("abc")</code> and <code>json([kv("a", 12.5), kv("b", [...])])</code>.
 * </p>
 */
public class JsonHandler extends BaseRepresentationHandler {

    public static final String JSON_FUNCTOR = "json";

    /**
     * TODO replace with native Jason map
     */
    public static final String JSON_MEMBER_FUNCTOR = "kv";

    public static final String APPLICATION_JSON_CT = "application/json";

    public JsonHandler() {
        super(JSON_FUNCTOR, APPLICATION_JSON_CT);
    }

    @Override
    public void serialize(Collection<Literal> terms, OutputStream out, String resourceURI) throws UnsupportedRepresentationException {
        JsonGenerator g = Json.createGenerator(out);

        Collection<Literal> jsonTerms = terms.stream().filter(t -> isJsonTerm(t)).collect(Collectors.toSet());

        if (jsonTerms.isEmpty())
            throw new UnsupportedRepresentationException("No " + JSON_FUNCTOR + " structure found in representation: " + terms);

        boolean wrappedInArray = jsonTerms.size() > 1;

        if (wrappedInArray) g.writeStartArray();
        for (Literal t : jsonTerms) generateJsonValue(t.getTerm(0), g);
        if (wrappedInArray) g.writeEnd();

        g.close();
    }

    @Override
    public Collection<Literal> deserialize(InputStream representation, String resourceURI, String contentType) throws UnsupportedRepresentationException {
        if (!contentType.equals(APPLICATION_JSON_CT))
            throw new UnsupportedRepresentationException("JSON handler does not support Content-Type: " + contentType);

        JsonReader reader = Json.createReader(representation);
        JsonValue value = reader.readValue();

        Term t = readJsonValue(value);

        return Arrays.asList(ASSyntax.createStructure(JSON_FUNCTOR, t));
    }

    private void generateJsonValue(Term t, JsonGenerator g) {
        acceptVisitor(t, new JsonTermVisitor() {
            @Override
            public void visit(Boolean val) {
                g.write(val);
            }

            @Override
            public void visit(Double val) {
                g.write(val);
            }

            @Override
            public void visit(Long val) {
                g.write(val);
            }

            @Override
            public void visit() {
                g.writeNull();
            }

            @Override
            public void visit(String val) {
                g.write(val);
            }

            @Override
            public void visit(List<Term> val) {
                g.writeStartArray();
                for (Term m : val) generateJsonValue(m, g);
                g.writeEnd();
            }

            @Override
            public void visit(Map<String, Term> val) {
                g.writeStartObject();
                for (Map.Entry<String, Term> m : val.entrySet()) generateJsonObjectMember(m.getKey(), m.getValue(), g);
                g.writeEnd();
            }
        });
    }

    private void generateJsonObjectMember(String key, Term t, JsonGenerator g) {
        acceptVisitor(t, new JsonTermVisitor() {
            @Override
            public void visit(Boolean val) {
                g.write(key, val);
            }

            @Override
            public void visit(Double val) {
                g.write(key, val);
            }

            @Override
            public void visit(Long val) {
                g.write(key, val);
            }

            @Override
            public void visit() {
                g.writeNull(key);
            }

            @Override
            public void visit(String val) {
                g.write(key, val);
            }

            @Override
            public void visit(List<Term> val) {
                g.writeStartArray(key);
                for (Term m : val) generateJsonValue(m, g);
                g.writeEnd();
            }

            @Override
            public void visit(Map<String, Term> val) {
                g.writeStartObject(key);
                for (Map.Entry<String, Term> m : val.entrySet()) generateJsonObjectMember(m.getKey(), m.getValue(), g);
                g.writeEnd();
            }
        });
    }

    private void acceptVisitor(Term t, JsonTermVisitor v) {
        if (t.isAtom() && t.equals(Literal.LTrue)) {
            v.visit(true);
        } else if (t.isAtom() && t.equals(Literal.LFalse)) {
            v.visit(false);
        } else if (t.isNumeric()) {
            try {
                double d = ((NumberTerm) t).solve();
                if (isIntegral(d)) v.visit((long) d);
                else v.visit(d);
            } catch (NoValueException e) {
                // TODO log
                e.printStackTrace();
            }
        } else if (t.isString() || t.isAtom()) {
            if (t.isAtom() && ((Atom) t).getFunctor().equals("null")) {
                v.visit();
            } else {
                String s = Identifiers.getLexicalForm(t);
                v.visit(s);
            }
        } else if (t.isList()) {
            List<Term> l = ((ListTerm) t).getAsList();
            List<Term> objectMembers = l.stream().filter(m -> isObjectMember(m)).collect(Collectors.toList());

            if (objectMembers.isEmpty()) {
                v.visit(l);
            } else {
                // TODO warn if some non-kv members were found

                Map<String, Term> obj = new HashMap<>();

                for (Term m : objectMembers) {
                    Structure kv = (Structure) m;

                    // TODO warn if invalid kv was found
                    if (kv.getArity() == 2 && kv.getTerm(0).isAtom()) {
                        String key = Identifiers.getLexicalForm(kv.getTerm(0));
                        Term val = kv.getTerm(1);

                        obj.put(key, val);
                    }
                }

                v.visit(obj);
            }
        }
    }

    private Term readJsonValue(JsonValue value) {
        JsonValue.ValueType type = value.getValueType();
        if (type.equals(JsonValue.ValueType.TRUE)) {
            return Literal.LTrue;
        } else if (type.equals(JsonValue.ValueType.FALSE)) {
            return Literal.LFalse;
        } else if (type.equals(JsonValue.ValueType.NUMBER)) {
            return ASSyntax.createNumber(((JsonNumber) value).doubleValue());
        } else if (type.equals(JsonValue.ValueType.NULL)) {
            return ASSyntax.createAtom("null");
        } else if (type.equals(JsonValue.ValueType.STRING)) {
            return ASSyntax.createString(((JsonString) value).getString());
        } else if (type.equals(JsonValue.ValueType.ARRAY)) {
            Collection<Term> members = new HashSet<>();

            for (JsonValue m : ((JsonArray) value)) members.add(readJsonValue(m));

            return ASSyntax.createList(members);
        } else if (type.equals(JsonValue.ValueType.OBJECT)) {
            Collection<Term> members = new HashSet<>();

            for (Map.Entry<String, JsonValue> kv : ((JsonObject) value).entrySet()) {
                Atom k = ASSyntax.createAtom(kv.getKey());
                Term v = readJsonValue(kv.getValue());

                members.add(ASSyntax.createStructure(JSON_MEMBER_FUNCTOR, k, v));
            }

            return ASSyntax.createList(members);
        } else {
            throw new IllegalArgumentException("JSON value not recognized by handler: " + value);
        }
    }

    private boolean isJsonTerm(Literal t) {
        return t.getFunctor().equals(JSON_FUNCTOR) && t.getArity() == 1 && !t.negated();
    }

    private boolean isObjectMember(Term t) {
        return t.isStructure() && ( (Structure) t).getFunctor().equals(JSON_MEMBER_FUNCTOR);
    }

    private boolean isIntegral(double d) {
        return d == Math.round(d);
    }

}
