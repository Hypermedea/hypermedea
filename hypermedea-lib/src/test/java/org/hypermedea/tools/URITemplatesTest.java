package org.hypermedea.tools;

import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class URITemplatesTest {

    public static final String URI_TEMPLATE = "http://example.org/{someId}";

    public static final String VAR_1 = "someId";

    public static final String VAR_2 = "someOtherId";

    public static final String URI_TEMPLATE_MULTIPLE = "http://example.org/{someId}/{someOtherId}";

    public static final String URI_1 = "http://example.org/1234";

    public static final String URI_2 = "http://example.org/1234/abcd";

    public static final String URI_3 = "http://example.com/1234";

    @Test
    public void testBind() {
        Map<String, Object> b = URITemplates.bind(URI_TEMPLATE, URI_1);

        assertTrue(b.containsKey(VAR_1));
        assertEquals(b.get(VAR_1), 1234l);
    }

    @Test
    public void testBindMultiple() {
        Map<String, Object> b = URITemplates.bind(URI_TEMPLATE_MULTIPLE, URI_2);

        assertTrue(b.containsKey(VAR_1));
        assertEquals(b.get(VAR_1), 1234l);

        assertTrue(b.containsKey(VAR_2));
        assertEquals(b.get(VAR_2), "abcd");
    }

    @Test
    public void testNoBind() {
        Map<String, Object> b = URITemplates.bind(URI_TEMPLATE, URI_3);

        assertTrue(b.isEmpty());
    }

}
