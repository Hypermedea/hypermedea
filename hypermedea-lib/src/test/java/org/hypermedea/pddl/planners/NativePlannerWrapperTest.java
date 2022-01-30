package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.util.Plan;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.pddl.TermDomainWrapper;
import org.hypermedea.pddl.TermProblemWrapper;
import org.hypermedea.pddl.TermWrapperException;
import org.junit.Test;

import static org.hypermedea.pddl.TermDomainWrapperTest.TEST_DOMAIN_STRUCTURE;
import static org.hypermedea.pddl.TermProblemWrapperTest.TEST_PROBLEM_STRUCTURE;
import static org.junit.Assert.assertEquals;

public class NativePlannerWrapperTest {

    @Test
    public void testSearch() throws ParseException, TermWrapperException {
        Structure domainTerm = ASSyntax.parseStructure(TEST_DOMAIN_STRUCTURE);
        Structure pbTerm = ASSyntax.parseStructure(TEST_PROBLEM_STRUCTURE);

        Domain domain = new TermDomainWrapper(domainTerm).getDomain();
        Problem pb = new TermProblemWrapper(pbTerm).getProblem();

        PlannerWrapper planner = new FFWrapper();
        Plan p = planner.search(domain, pb);

        assertEquals(p.actions().size(), 1);
        assertEquals(p.actions().get(0).getArity(), 3);
    }

}
