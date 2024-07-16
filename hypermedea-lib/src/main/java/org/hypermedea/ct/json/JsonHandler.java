package org.hypermedea.ct.json;

import jason.NoValueException;
import jason.asSyntax.*;
import org.hypermedea.ct.BaseRepresentationHandler;
import org.hypermedea.ct.UnsupportedRepresentationException;
import org.hypermedea.tools.Identifiers;
import org.hypermedea.tools.KVPairs;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handler for representations with Content-Type <code>application/json</code>.
 */
public class JsonHandler extends BaseRepresentationHandler {

    public static final String JSON_FUNCTOR = "json";

    /**
     * TODO replace with native Jason map
     */
    public static final String JSON_MEMBER_FUNCTOR = "kv";

    public static final String[] APPLICATION_JSON_CT = { "application/json", "application/[^+]+\\+json" };

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
        JsonValue value;

        try {
            JsonReader reader = Json.createReader(representation);
            value = reader.readValue();
        } catch (JsonException e) {
            // try to parse input as single-digit number
            // see https://github.com/Hypermedea/hypermedea/issues/43
            try {
                representation.reset();
                byte[] buf = new BufferedInputStream(representation).readAllBytes();
                double nb = Double.parseDouble(new String(buf, StandardCharsets.UTF_8));

                value = Json.createValue(nb);
            } catch (IOException other) {
                throw new UnsupportedRepresentationException(e);
            }
        }

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
            ListTerm l = (ListTerm) t;
            Map<String, Term> obj = KVPairs.getAsMap(l);

            if (obj.isEmpty()) v.visit(l.getAsList());
            else v.visit(obj);
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

    private boolean isIntegral(double d) {
        return d == Math.round(d);
    }

}
