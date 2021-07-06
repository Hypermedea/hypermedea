package org.hypermedea.planning;

import fr.uga.pddl4j.parser.Domain;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import org.junit.Test;

/**
 * @author Victor Charpenay
 */
public class TermDomainWrapperTest {

    public static final String TEST_DOMAIN_STRUCTURE = "domain(\"hanoi\", [\n" +
            "  action(\"move\",\n" +
            "    and(smaller(\"?disc\", \"?to\"), on(\"?disc\", \"?from\"), clear(\"?disc\"), clear(\"?to\")),\n" +
            "    and(clear(\"?from\"), on(\"?disc\", \"?to\"), not(on(\"?disc\", \"?from\")), not(clear(\"?to\"))))\n" +
            "])";

    @Test
    public void testGetDomain() throws ParseException {
        Structure term = ASSyntax.parseStructure(TEST_DOMAIN_STRUCTURE);
        TermDomainWrapper w = new TermDomainWrapper(term);

        Domain d = w.getDomain();

        assert d != null; // TODO re-parse domain and check components
    }

}