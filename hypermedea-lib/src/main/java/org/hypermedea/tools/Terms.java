package org.hypermedea.tools;

import jason.asSyntax.Literal;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Terms {

    public static String getOneLineString(Collection<Literal> terms) {
        Optional<Literal> tOpt = terms.stream().findAny();

        if (tOpt.isEmpty()) return "<none>";

        Literal t = tOpt.get();

        Pattern p = Pattern.compile("([^\\r\\n]*)\\r?\\n");
        Matcher m = p.matcher(t.toString());

        return m.find() ? m.group(1) + "..." : t.toString();
    }

}
