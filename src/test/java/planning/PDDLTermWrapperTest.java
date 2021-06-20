package planning;

import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import org.junit.Test;

/**
 * @author Victor Charpenay
 */
public class PDDLTermWrapperTest {

    public static final String TEST_DOMAIN_STRUCTURE = "domain(\"hanoi\", [\n" +
            "  action(\"move\",\n" +
            "    and(smaller(\"?to\", \"?disc\"), on(\"?disc\", \"?from\"), clear(\"?disc\"), clear(\"?to\")),\n" +
            "    and(clear(\"?from\"), on(\"?disc\", \"?to\"), not(on(\"?disc\", \"?from\")), not(clear(\"?to\"))))\n" +
            "])";

    public static final String TEST_DOMAIN_PDDL = "";

    public static final String TEST_PROBLEM_STRUCTURE = "problem(\"test\", \"hanoi\", [\n" +
            "  smaller(disk1, disk2),\n" +
            "  smaller(disk3, disk2),\n" +
            "  smaller(disk1, disk3),\n" +
            "  on(disk1, disk2),\n" +
            "  clear(disk1),\n" +
            "  clear(disk3)\n" +
            "], clear(disk2))";

    @Test
    public void testGetDomain() throws ParseException {
        Structure term = ASSyntax.parseStructure(TEST_DOMAIN_STRUCTURE);
        PDDLTermWrapper w = new PDDLTermWrapper(term, ASSyntax.createAtom(""));

        Domain d = w.getDomain();

        assert d != null; // TODO re-parse domain and check components
    }

    @Test
    public void testGetProblem() throws ParseException {
        Structure term = ASSyntax.parseStructure(TEST_PROBLEM_STRUCTURE);
        PDDLTermWrapper w = new PDDLTermWrapper(ASSyntax.createAtom(""), term);

        Problem pb = w.getProblem();

        assert pb != null; // TODO re-parse problem and check components
    }

}
