package org.hypermedea.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities to deal with URI templates. See <a href="https://tools.ietf.org/html/rfc6570">RFC 6570</a>.
 */
public class URITemplates {

    private final static Pattern VAR_PATTERN = Pattern.compile("\\{([^\\}]+)\\}");

    /**
     * Try to match URI instance with URI template and return bindings for template variables if instance matches.
     * For instance, if the URI <code>http://example.org/1234</code> is matched against
     * <code>http://example.org/{someId}</code>, the map <code>{ someId: 1234 }</code> is returned.
     * If matched strings are the lexical value of a number, they are cast to a Java number (Long or Double).
     *
     * TODO support patterns with question mark (? -> value is percent-encoded)
     *
     * @param template a URI template
     * @param instance a plain URI
     * @return bindings, if instance matches template
     */
    public static Map<String, Object> bind(String template, String instance) {
        List<String> vars = new ArrayList<>();
        Map<String, Object> bindings = new HashMap<>();

        Matcher varMatcher = VAR_PATTERN.matcher(template);

        while (varMatcher.find()) {
            String var = template.substring(varMatcher.start(1), varMatcher.end(1));
            vars.add(var);
        }

        if (!vars.isEmpty()) {
            String regex = varMatcher.replaceAll("(.+)");
            Matcher templateMatcher = Pattern.compile(regex).matcher(instance);

            if (templateMatcher.matches()) {
                for (int i = 1; i <= templateMatcher.groupCount(); i++) {
                    String var = vars.get(i - 1);
                    String val = templateMatcher.group(i);

                    try {
                        Long longVal = Long.parseLong(val);
                        bindings.put(var, longVal);
                    } catch (NumberFormatException e1) {
                        try {
                            Double doubleVal = Double.parseDouble(val);
                            bindings.put(var, doubleVal);
                        } catch (NumberFormatException e2) {
                            bindings.put(var, val);
                        }
                    }
                }
            }
        }

        return bindings;
    }

}
