package org.hypermedea.tools;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.MapTermImpl;
import jason.asSyntax.SetTermImpl;
import jason.asSyntax.parser.ParseException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TermsTest {

    @Test
    @Disabled
    public void testParseMap() throws ParseException {
        MapTermImpl m = new MapTermImpl();
        m.put(ASSyntax.createAtom("a"), ASSyntax.createAtom("b"));
        ASSyntax.parseTerm(m.toString());
    }

    @Test
    @Disabled
    public void testParseSet() throws ParseException {
        SetTermImpl s = new SetTermImpl();
        s.add(ASSyntax.createAtom("a"));
        s.add(ASSyntax.createAtom("b"));
        ASSyntax.parseTerm(s.toString());
    }

}
