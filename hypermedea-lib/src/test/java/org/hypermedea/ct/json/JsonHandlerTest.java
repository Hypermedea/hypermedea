package org.hypermedea.ct.json;

import jason.asSyntax.*;
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;

public class JsonHandlerTest {

    public static final String TEST_JSON_TERM = "json([ kv(name, \"Demo term\"),\n" +
            "       kv(created, [kv(day, null), kv(month, \"December\"), kv(year, 2007)]),\n" +
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

    public static final String TEST_ROS_MSG = "json([\n" +
            "        kv(\"target_pose\", [\n" +
            "            kv(\"header\", [\n" +
            "                kv(\"frame_id\", \"map\")\n" +
            "            ]),\n" +
            "            kv(\"pose\", [\n" +
            "                kv(\"position\", [\n" +
            "                        kv(\"x\", -0.80),\n" +
            "                        kv(\"y\", -1.68),\n" +
            "                        kv(\"z\", 0.0)\n" +
            "                ]),\n" +
            "                kv(\"orientation\", [\n" +
            "                        kv(\"x\", 0.0),\n" +
            "                        kv(\"y\", 0.0),\n" +
            "                        kv(\"z\", -0.51),\n" +
            "                        kv(\"w\", 0.86)\n" +
            "                ])\n" +
            "            ])\n" +
            "        ])\n" +
            "    ])";

    private final JsonHandler h = new JsonHandler();

    @Test
    public void testSerialize() throws ParseException {
        Literal t = ASSyntax.parseLiteral(TEST_JSON_TERM);
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
    public void testDeserialize() throws UnsupportedEncodingException {
        InputStream in = new ByteArrayInputStream(TEST_JSON_OBJECT.getBytes("UTF-8"));

        Collection<Literal> terms = h.deserialize(in, "http://example.org/", "application/json");

        assert terms.size() == 1;

        Literal t = terms.stream().findAny().get();

        assert t.getFunctor().equals(JsonHandler.JSON_FUNCTOR);
        assert t.getArity() == 1;

        Term val = t.getTerm(0);

        assert val.isList();
        assert ((ListTerm) val).size() == 4;

        Term m0 = ((ListTerm) val).get(0);

        assert m0.isStructure() && ((Structure) m0).getFunctor().equals(JsonHandler.JSON_MEMBER_FUNCTOR);
        assert ((Structure) m0).getTerm(0).isAtom();

        Optional<Term> m1 = ((ListTerm) val).stream().filter(m -> hasValue(m, "name")).findAny();
        Optional<Term> m2 = ((ListTerm) val).stream().filter(m -> hasValue(m, "created")).findAny();
        Optional<Term> m3 = ((ListTerm) val).stream().filter(m -> hasValue(m, "confirmed")).findAny();
        Optional<Term> m4 = ((ListTerm) val).stream().filter(m -> hasValue(m, "members")).findAny();

        assert m1.isPresent() && m2.isPresent() && m3.isPresent() && m4.isPresent();

        Term t1 = ((Structure) m1.get()).getTerm(1);
        Term t2 = ((Structure) m2.get()).getTerm(1);
        Term t3 = ((Structure) m3.get()).getTerm(1);
        Term t4 = ((Structure) m4.get()).getTerm(1);

        assert t1.isString() && t1.toString().equals("\"Demo term\"");
        assert t2.isList() && ((ListTerm) t2).size() == 3;
        assert t3.equals(Atom.LTrue);
        assert t4.isList() && ((ListTerm) t2).size() == 3;
    }

    @Test
    public void testStringKeys() throws ParseException {
        Literal t = ASSyntax.parseLiteral(TEST_ROS_MSG);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        h.serialize(Arrays.asList(t), out, "http://example.org/");

        String json = out.toString();

        assertFalse(json.equals("{}"));
    }

    @Test
    public void testDeserializeZero() {
        ByteArrayInputStream in = new ByteArrayInputStream("0".getBytes(StandardCharsets.UTF_8));
        Collection<Literal> t = h.deserialize(in, "http://example.org/", "application/json");

        assertEquals(t.size(), 1);
        assertEquals(t.stream().findAny().get().toString(), "json(0)");
    }

    private boolean hasValue(Term t, String key) {
        if (t.isStructure() && ( (Structure) t).getFunctor().equals(JsonHandler.JSON_MEMBER_FUNCTOR)) {
            Structure st = (Structure) t;

            if (st.getArity() == 2) {
                Term k = st.getTerm(0);
                return k.isAtom() && k.toString().equals(key);
            }
        }

        return false;
    }

}
