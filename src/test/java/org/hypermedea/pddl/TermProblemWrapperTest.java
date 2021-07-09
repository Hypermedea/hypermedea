package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.Problem;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import org.junit.Test;

/**
 * @author Victor Charpenay
 */
public class TermProblemWrapperTest {

    public static final String TEST_PROBLEM_STRUCTURE = "problem(\"test\", \"hanoi\", [\n" +
            "  smaller(disk1, disk2),\n" +
            "  smaller(disk3, disk2),\n" +
            "  smaller(disk1, disk3),\n" +
            "  on(disk1, disk2),\n" +
            "  clear(disk1),\n" +
            "  clear(disk3)\n" +
            "], clear(disk2))";

    @Test
    public void testGetProblem() throws ParseException, TermWrapperException {
        Structure term = ASSyntax.parseStructure(TEST_PROBLEM_STRUCTURE);
        TermProblemWrapper w = new TermProblemWrapper(term);

        Problem pb = w.getProblem();

        assert pb != null; // TODO re-parse problem and check components
    }

}
