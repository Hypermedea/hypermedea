package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.pddl.TermDomainWrapper;
import org.hypermedea.pddl.TermProblemWrapper;
import org.hypermedea.pddl.TermWrapperException;
import org.junit.Test;

import static org.hypermedea.pddl.TermDomainWrapperTest.TEST_DOMAIN_STRUCTURE;
import static org.hypermedea.pddl.TermProblemWrapperTest.TEST_PROBLEM_STRUCTURE;

public class NativePlannerWrapperTest {

    @Test
    public void testSearch() throws ParseException, TermWrapperException {
        Structure domainTerm = ASSyntax.parseStructure(TEST_DOMAIN_STRUCTURE);
        Structure pbTerm = ASSyntax.parseStructure(TEST_PROBLEM_STRUCTURE);

        Domain domain = new TermDomainWrapper(domainTerm).getDomain();
        Problem pb = new TermProblemWrapper(pbTerm).getProblem();

        String loc = NativePlannerWrapperTest.class.getClassLoader().getResource("ff").getFile();

        PlannerWrapper planner = new NativePlannerWrapper(loc);
        planner.search(domain, pb);
    }

}
