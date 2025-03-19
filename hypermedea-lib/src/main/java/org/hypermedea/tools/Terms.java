package org.hypermedea.tools;

import jason.asSyntax.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Terms {

    public static String getSummary(Collection<Literal> terms) {
        Optional<Literal> tOpt = terms.stream().findAny();

        if (tOpt.isEmpty()) return "<none>";

        Literal t = tOpt.get();

        List<String> argStrings = new ArrayList<>();
        for (Term arg : t.getTerms()) argStrings.add(getShortString(arg));

        String item = t.getFunctor() + '(' + String.join(", ", argStrings) + ')';
        return getStringWithEllipsis(item, terms);
    }

    public static String getShortString(ListTerm t) {
        Optional<Term> itemOpt = t.stream().findAny();

        if (itemOpt.isEmpty()) return "[]";
        return '[' + getStringWithEllipsis(getShortString(itemOpt.get()), t) + ']';
    }

    public static String getShortString(MapTerm t) {
        Optional<Term> itemOpt = t.keys().stream().findAny();

        if (itemOpt.isEmpty()) return "{}";
        return '{' + getStringWithEllipsis(getShortString(itemOpt.get()) + "->...", t.keys()) + '}';
    }

    public static String getShortString(SetTerm t) {
        return getShortString(t.getAsListTerm());
    }

    public static String getShortString(Term t) {
        if (t.isList()) return getShortString((ListTerm) t);
        if (t.isMap()) return getShortString((MapTerm) t);
        if (t.isSet()) return getShortString((SetTerm) t);

        Pattern p = Pattern.compile("([^\\r\\n]*)\\r?\\n");
        Matcher m = p.matcher(t.toString());

        return m.find() ? m.group(1) + "..." : t.toString();
    }

    private static String getStringWithEllipsis(String item, Collection<? extends Term> terms) {
        return item + (terms.size() > 1 ? ", ..." : "");
    }

}
