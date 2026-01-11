package org.hypermedea.ct.json;

import jason.asSyntax.*;
import org.hypermedea.ct.BaseRepresentationHandler;
import org.hypermedea.ct.UnsupportedRepresentationException;
import org.hypermedea.tools.Identifiers;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handler for representations with Content-Type <code>application/json</code>.
 */
public class JsonHandler extends BaseRepresentationHandler {

    public static final String JSON_FUNCTOR = "json";

    public static final String[] APPLICATION_JSON_CT = { "application/json", "application/[^+]+\\+json" };

    public JsonHandler() {
        super(JSON_FUNCTOR, APPLICATION_JSON_CT);
    }

    @Override
    public void serialize(Collection<Literal> terms, OutputStream out, String resourceURI) throws UnsupportedRepresentationException {
        JsonGenerator g = Json.createGenerator(out);

        Collection<Literal> jsonTerms = terms.stream().filter(this::isJsonTerm).collect(Collectors.toSet());

        if (jsonTerms.isEmpty())
            throw new UnsupportedRepresentationException("No " + JSON_FUNCTOR + " structure found in representation: " + terms);

        boolean wrappedInArray = jsonTerms.size() > 1;

        if (wrappedInArray) g.writeStartArray();
        for (Literal t : jsonTerms) g.write(getJsonValue(t.getTerm(0)));
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
        return Collections.singletonList(ASSyntax.createLiteral(JSON_FUNCTOR, t));
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
            MapTerm map = new MapTermImpl();

            for (Map.Entry<String, JsonValue> kv : ((JsonObject) value).entrySet()) {
                Atom k = ASSyntax.createAtom(kv.getKey());
                Term v = readJsonValue(kv.getValue());

                map.put(k, v);
            }

            return map;
        } else {
            throw new IllegalArgumentException("JSON value not recognized by handler: " + value);
        }
    }

    private JsonValue getJsonValue(Term t) {
        if (t.isAtom()) {
            Atom a = (Atom) t;

            switch (a.getFunctor()) {
                case "true": return JsonValue.TRUE;
                case "false": return JsonValue.FALSE;
                case "null": return JsonValue.NULL;
            }
        } else if (t.isString() || t.isNumeric()) {
            return t.getAsJson();
        } else if (t.isList()) {
            ListTerm l = (ListTerm) t;

            JsonArrayBuilder builder = Json.createArrayBuilder();
            for (Term element : l.getAsList()) builder.add(getJsonValue(element));

            return builder.build();
        } else if (t.isMap()) {
            MapTerm m = (MapTerm) t;

            JsonObjectBuilder builder = Json.createObjectBuilder();
            for (Term k : m.keys()) builder.add(Identifiers.getLexicalForm(k), getJsonValue(m.get(k)));

            return builder.build();
        }

        throw new IllegalArgumentException("Term not supported by handler: " + t);
    }

    private boolean isJsonTerm(Literal t) {
        return t.getFunctor().equals(JSON_FUNCTOR) && t.getArity() == 1 && !t.negated();
    }

}
