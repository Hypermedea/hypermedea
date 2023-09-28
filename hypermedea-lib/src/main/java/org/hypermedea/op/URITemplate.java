package org.hypermedea.op;

import java.util.*;

/**
 * Code forked from <a href="https://github.com/Interactions-HSG/wot-td-java"><code>wot-td-java</code></a>,
 * maintained by University St. Gallen (Interaction- and Communication-based Systems research group).
 *
 */
public class URITemplate {

    private final List<String> extracted;

    public URITemplate(String expression) {
        this.extracted = extract(expression);
    }

    static List<String> extract(String path) {
        List<String> extracted = new ArrayList<>();
        int n = path.length();
        String s = "";
        for (int i = 0; i < n; i++) {
            if (path.charAt(i) == '{') {
                extracted.add(s);
                s = "{";
            } else if (path.charAt(i) == '}') {
                s = s + "}";
                extracted.add(s);
                s = "";
            } else if (i == n - 1) {
                s = s + path.charAt(i);
                extracted.add(s);
            } else {
                s = s + path.charAt(i);
            }
        }
        return extracted;
    }

    static List<String> getListVariables(String expression) {
        List<String> variables = new ArrayList<>();
        String s = "";
        int n = expression.length();
        for (int i = 0; i < n; i++) {
            char c = expression.charAt(i);
            if (c == ',') {
                variables.add(s);
                s = "";
            } else if (i == n - 1) {
                variables.add(s);
            } else if ("{}?".indexOf(c) == -1) {
                s = s + c;
            }
        }

        return variables;
    }

    static Set<String> getVariables(String expression) {
        return new HashSet<>(getListVariables(expression));
    }

    static String replace(String expression, Map<String, Object> values) {
        String s = "";
        if (expression.charAt(1) == '?') {
            s = s + '?';
            List<String> variables = getListVariables(expression);
            int n = variables.size();
            for (int i = 0; i < n; i++) {
                String variable = variables.get(i);
                Object object = values.get(variable);
                String value = getValue(object);
                s = s + variable + "=" + value;
                if (i != n - 1) {
                    s = s + "&";
                }
            }
        } else {
            List<String> variables = getListVariables(expression);
            int n = variables.size();
            for (int i = 0; i < n; i++) {
                String variable = variables.get(i);
                Object object = values.get(variable);
                String value = getValue(object);
                s = s + value;
                if (i != n - 1) {
                    s = s + ",";
                }
            }
        }

        return s;
    }

    static String getValue(Object object) {
        if (object instanceof String || object instanceof Number || object instanceof Boolean) {
            return object.toString();
        }

        throw new IllegalArgumentException("Invalid value for URI variable: " + object);
    }

    public String createUri(Map<String, Object> values) {
        String s = "";
        int n = extracted.size();
        for (int i = 0; i < n; i++) {
            String e = extracted.get(i);
            if (e.charAt(0) == '{') {
                e = replace(e, values);
            }
            s = s + e;
        }
        return s;
    }

}
