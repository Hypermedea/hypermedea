package org.hypermedea.pddl;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.planners.ProblemFactory;
import fr.uga.pddl4j.planners.statespace.StateSpacePlanner;
import fr.uga.pddl4j.planners.statespace.StateSpacePlannerFactory;
import fr.uga.pddl4j.util.Plan;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Test domain taken from https://fai.cs.uni-saarland.de/hoffmann/ff-domains.html.
 *
 * @author Victor Charpenay
 */
public class PlanJasonWrapperTest {

    private static final String TEST_DOMAIN = "test-domain.pddl";

    private static final String TEST_PROBLEM = "test-problem.pddl";

    private CodedProblem problem;

    private Plan plan;

    @Before
    public void parsePDDL() throws URISyntaxException, IOException {
        ClassLoader cl = PlanJasonWrapperTest.class.getClassLoader();

        URL domainURL = cl.getResource(TEST_DOMAIN);
        File domain = new File(domainURL.toURI());

        URL pbURL = cl.getResource(TEST_PROBLEM);
        File pb = new File(pbURL.toURI());

        ProblemFactory f = ProblemFactory.getInstance();
        f.parse(domain, pb);

        problem = f.encode();

        Planner p = StateSpacePlannerFactory.getInstance().getPlanner(StateSpacePlanner.DEFAULT_PLANNER);

        plan = p.search(problem);
    }

    @Test
    public void testToString() {
        PlanJasonWrapper w = new PlanJasonWrapper("test", plan, problem);

        assert w.toString().equals("!test : true <- move(disk1, disk2, disk3) .");
    }

}
