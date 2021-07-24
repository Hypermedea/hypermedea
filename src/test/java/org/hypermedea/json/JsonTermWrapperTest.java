package org.hypermedea.json;

import jason.asSyntax.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonTermWrapperTest {

    @Test
    public void testGetTerm() {
        Term t = new JsonTermWrapper(getObj()).getTerm();

        assert t.isStructure() && ((Structure) t).getFunctor().equals(TermJsonWrapper.JSON_FUNCTOR);

        // TODO check deeper structure
    }

    private Object getObj() {
        Map<String, Object> obj = new HashMap<>();

        Map<String, Object> m = new HashMap<>();

        m.put("day", null);
        m.put("month", "December");
        m.put("year", Long.valueOf(2007));

        List<Object> l = new ArrayList<>();

        l.add(Long.valueOf(1));
        l.add(Long.valueOf(2));
        l.add(Long.valueOf(3));

        obj.put("name", "Demo term");
        obj.put("created", m);
        obj.put("confirmed", true);
        obj.put("members", l);

        return obj;
    }

}
