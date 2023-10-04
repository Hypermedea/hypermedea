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

/**
 * <p>
 *     Handler for JSON values.
 * </p>
 * <p>
 *     The mapping between JSON objects and Jason terms is inspired by
 *     <a href="https://www.swi-prolog.org/pldoc/man?section=jsonsupport">the SWI-Prolog JSON library</a>.
 *     Simple JSON values and arrays have an straightforward equivalent in Jason. JSON objects are represented
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

        Optional<Literal> termOpt = terms.stream().filter(t -> t.isLiteral() && t.getFunctor().equals(JSON_FUNCTOR)).findAny();

        // TODO minimize duplicates between generateJsonValue and generateJsonObjectMember...
        if (termOpt.isPresent()) generateJsonValue(termOpt.get(), g);
        else throw new UnsupportedRepresentationException("No " + JSON_FUNCTOR + " structure found in representation: " + terms);

        g.close();
    }

    @Override
    public Collection<Literal> deserialize(InputStream representation, String resourceURI, String contentType) throws UnsupportedRepresentationException {
        if (!contentType.equals(APPLICATION_JSON_CT))
            throw new UnsupportedRepresentationException("JSON handler does not support Content-Type: " + contentType);

        JsonReader reader = Json.createReader(representation);
        JsonStructure value = reader.read();

        // FIXME not always a structure. If not a structure?
        Literal t = (Literal) readJsonValue(value);

        return Arrays.asList(t);
    }

    private void generateJsonValue(Term t, JsonGenerator g) {
        if (t.isAtom() && t.equals(Literal.LTrue)) {
            g.write(true);
        } else if (t.isAtom() && t.equals(Literal.LFalse)) {
            g.write(false);
        } else if (t.isNumeric()) {
            try {
                double d = ((NumberTerm) t).solve();
                // TODO if integral, write integral, not double
                g.write(d);
            } catch (NoValueException e) {
                // TODO log
                e.printStackTrace();
            }
        } else if (t.isString() || t.isAtom()) {
            if (t.isAtom() && ((Atom) t).getFunctor().equals("null")) {
                g.writeNull();
            } else {
                String s = Identifiers.getLexicalForm(t);
                g.write(s);
            }
        } else if (t.isList()) {
            g.writeStartArray();
            for (Term member : ((ListTerm) t).getAsList()) generateJsonValue(member, g);
            g.writeEnd();
        } else if (t.isStructure()) {
            Structure json = (Structure) t;

            if (!json.getFunctor().equals(JSON_FUNCTOR) || json.getArity() != 1 || !json.getTerm(0).isList()) return;

            ListTerm l = (ListTerm) json.getTerm(0);

            g.writeStartObject();
            for (Term member : l.getAsList()) generateJsonObjectMember(member, g);
            g.writeEnd();
        }
    }

    private void generateJsonObjectMember(Term t, JsonGenerator g) {
        if (t.isStructure()) {
            Structure kv = (Structure) t;

            if (!kv.getFunctor().equals(JSON_MEMBER_FUNCTOR) && kv.getArity() != 2 && !kv.getTerm(0).isAtom()) return;

            String key = Identifiers.getLexicalForm(kv.getTerm(0));
            Term value = kv.getTerm(1);

            if (value.isAtom() && value.equals(Literal.LTrue)) {
                g.write(key, true);
            } else if (value.isAtom() && value.equals(Literal.LFalse)) {
                g.write(key,false);
            } else if (value.isNumeric()) {
                try {
                    double d = ((NumberTerm) value).solve();
                    // TODO if integral, write integral, not double
                    g.write(key, d);
                } catch (NoValueException e) {
                    // TODO log
                    e.printStackTrace();
                }
            } else if (value.isString() || value.isAtom()) {
                if (value.isAtom() && ((Atom) value).getFunctor().equals("null")) {
                    g.writeNull(key);
                } else {
                    String s = Identifiers.getLexicalForm(value);
                    g.write(key, s);
                }
            } else if (value.isList()) {
                g.writeStartArray(key);
                for (Term member : ((ListTerm) value).getAsList()) generateJsonValue(member, g);
                g.writeEnd();
            } else if (value.isStructure()) {
                Structure json = (Structure) value;

                if (!json.getFunctor().equals(JSON_FUNCTOR) || json.getArity() != 1 || !json.getTerm(0).isList()) return;

                ListTerm l = (ListTerm) json.getTerm(0);

                g.writeStartObject(key);
                for (Term member : l.getAsList()) generateJsonObjectMember(member, g);
                g.writeEnd();
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
            // TODO or int?
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

            ListTerm pairs = ASSyntax.createList(members);
            return ASSyntax.createStructure(JSON_FUNCTOR, pairs);
        } else {
            throw new IllegalArgumentException("JSON value not recognized by handler: " + value);
        }
    }

}
