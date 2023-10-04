package org.hypermedea.ct.json;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;

public class JsonHandlerTest {

    public static final String TEST_JSON_TERM = "json([ kv(name, \"Demo term\"),\n" +
            "       kv(created, json([kv(day, null), kv(month, \"December\"), kv(year, 2007)])),\n" +
            "       kv(confirmed, true),\n" +
            "       kv(members, [1, 1.5, 2])\n" +
            "     ])";

    public static final String TEST_JSON_OBJECT = "{" +
            "   \"name\": \"Demo term\"," +
            "   \"created\": {" +
            "       \"day\": null," +
            "       \"month\": \"December\"," +
            "       \"year\": 2007" +
            "   }," +
            "   \"confirmed\": true," +
            "   \"members\": [1, 1.5, 2]" +
            "}";

    public final JsonHandler h = new JsonHandler();

    @Test
    public void testSerialize() throws ParseException {
        Structure t = ASSyntax.parseStructure(TEST_JSON_TERM);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        h.serialize(Arrays.asList(t), out, "http://example.org/");
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

        JsonObject obj = Json.createReader(in).readObject();

        assert obj.getString("name").equals("Demo term");

        assert obj.getBoolean("confirmed") == true;

        JsonArray l = obj.getJsonArray("members");

        assert l.size() == 3;

        assert l.getJsonNumber(1).doubleValue() == 1.5d;
        assert l.getInt(2) == 2;

        JsonObject m = obj.getJsonObject("created");

        assert m.get("day").getValueType().equals(JsonValue.ValueType.NULL);

        assert m.getString("month").equals("December");

        assert m.getInt("year") == 2007;
    }

    @Test
    public void testGetTerm() throws UnsupportedEncodingException {
        InputStream in = new ByteArrayInputStream(TEST_JSON_OBJECT.getBytes("UTF-8"));

        Collection<Structure> terms = h.deserialize(in, "http://example.org/", "application/json");

        assert terms.size() == 1;

        Structure t = terms.stream().findAny().get();

        assert t.getFunctor().equals(JsonHandler.JSON_FUNCTOR);
        assert t.getArity() == 1;

        // TODO check deep structure of term
    }

}
