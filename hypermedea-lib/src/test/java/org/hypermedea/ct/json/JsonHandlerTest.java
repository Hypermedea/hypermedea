package org.hypermedea.ct.json;

import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JsonHandlerTest {

    public static final Literal TEST_JSON_TERM = getTestJsonTerm();

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

    public static final Literal TEST_ROS_MSG = getTestROSMsg();

    private final JsonHandler h = new JsonHandler();

    @Test
    public void testSerialize() throws ParseException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        h.serialize(List.of(TEST_JSON_TERM), out, "http://example.org/");
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

        assert val.isMap();

        Set<Term> keys = ((MapTerm) val).keys();

        assert keys.size() == 4;

        Term t1 = ((MapTerm) val).get(ASSyntax.createAtom("name"));
        Term t2 = ((MapTerm) val).get(ASSyntax.createAtom("created"));
        Term t3 = ((MapTerm) val).get(ASSyntax.createAtom("confirmed"));
        Term t4 = ((MapTerm) val).get(ASSyntax.createAtom("members"));

        assert t1.isString() && t1.toString().equals("\"Demo term\"");
        assert t2.isMap() && ((MapTerm) t2).size() == 3;
        assert t3.equals(Atom.LTrue);
        assert t4.isList() && ((ListTerm) t4).size() == 3;
    }

    @Test
    public void testStringKeys() throws ParseException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        h.serialize(List.of(TEST_ROS_MSG), out, "http://example.org/");

        String json = out.toString();

        assertNotEquals("{}", json);
    }

    @Test
    public void testDeserializeZero() {
        ByteArrayInputStream in = new ByteArrayInputStream("0".getBytes(StandardCharsets.UTF_8));
        Collection<Literal> t = h.deserialize(in, "http://example.org/", "application/json");

        assertEquals(1, t.size());
        assertEquals("json(0)", t.stream().findAny().get().toString());
    }

    private boolean hasValue(Term t, String key) {
        if (t.isStructure() && ( (Structure) t).getFunctor().equals("kv")) {
            Structure st = (Structure) t;

            if (st.getArity() == 2) {
                Term k = st.getTerm(0);
                return k.isAtom() && k.toString().equals(key);
            }
        }

        return false;
    }
    
    private static Literal getTestJsonTerm() {
        MapTerm t = new MapTermImpl();

        try {
            MapTerm t2 = new MapTermImpl();

            t2.put(ASSyntax.createAtom("day"), ASSyntax.createAtom("null"));
            t2.put(ASSyntax.createAtom("month"), ASSyntax.createString("December"));
            t2.put(ASSyntax.createAtom("year"), ASSyntax.createNumber(2007));

            t.put(ASSyntax.createAtom("name"), ASSyntax.createString("Demo term"));
            t.put(ASSyntax.createAtom("created"), t2);
            t.put(ASSyntax.createAtom("confirmed"), ASSyntax.createAtom("true"));
            t.put(ASSyntax.createAtom("members"), ASSyntax.parseList("[1, 1.5, 2]"));
        } catch (ParseException e) {
            // do nothing
        }

        return ASSyntax.createStructure("json", t);
    }

    private static Literal getTestROSMsg() {
        MapTerm t = new MapTermImpl();
        MapTerm targetPose = new MapTermImpl();
        MapTerm header = new MapTermImpl();
        MapTerm pose = new MapTermImpl();
        MapTerm position = new MapTermImpl();
        MapTerm orientation = new MapTermImpl();

        t.put(ASSyntax.createAtom("target_pose"), targetPose);

        targetPose.put(ASSyntax.createAtom("header"), header);
        targetPose.put(ASSyntax.createAtom("pose"), pose);

        header.put(ASSyntax.createAtom("frame_id"), ASSyntax.createString("map"));

        pose.put(ASSyntax.createAtom("position"), position);
        pose.put(ASSyntax.createAtom("orientation"), orientation);

        position.put(ASSyntax.createAtom("x"), ASSyntax.createNumber(-0.8));
        position.put(ASSyntax.createAtom("y"), ASSyntax.createNumber(-1.68));
        position.put(ASSyntax.createAtom("z"), ASSyntax.createNumber(0.0));

        position.put(ASSyntax.createAtom("x"), ASSyntax.createNumber(0.0));
        position.put(ASSyntax.createAtom("y"), ASSyntax.createNumber(0.0));
        position.put(ASSyntax.createAtom("z"), ASSyntax.createNumber(-0.51));
        position.put(ASSyntax.createAtom("w"), ASSyntax.createNumber(0.86));

        return ASSyntax.createStructure("json", t);
    }

}
