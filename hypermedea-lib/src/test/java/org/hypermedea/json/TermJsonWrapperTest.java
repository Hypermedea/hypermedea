package org.hypermedea.json;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Test object taken from https://www.swi-prolog.org/pldoc/man?section=jsonsupport.
 *
 * @author Victor Charpenay
 */
public class TermJsonWrapperTest {

    public static final String TEST_JSON_OBJECT = "json([ kv(name, \"Demo term\"),\n" +
            "       kv(created, json([kv(day, null), kv(month, \"December\"), kv(year, 2007)])),\n" +
            "       kv(confirmed, true),\n" +
            "       kv(members, [1, 2, 3])\n" +
            "     ])";

    @Test
    public void testGetJsonObject() throws ParseException {
        Term t = ASSyntax.parseTerm(TEST_JSON_OBJECT);

        Map<String, Object> obj = new TermJsonWrapper(t).getJsonObject();

        assert obj.get("name").equals("Demo term");

        assert obj.get("confirmed").equals(true);

        assert obj.get("members") instanceof List;

        List<Object> l = (List<Object>) obj.get("members");

        assert l.size() == 3;

        assert l.get(1).equals(Long.valueOf(2));

        assert obj.get("created") instanceof Map;

        Map<String, Object> m = (Map<String, Object>) obj.get("created");

        assert m.get("day") == null;

        assert m.get("month").equals("December");

        assert m.get("year").equals(Long.valueOf(2007));
    }

}
