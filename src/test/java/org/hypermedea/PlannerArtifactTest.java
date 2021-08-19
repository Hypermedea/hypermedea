package org.hypermedea;

import cartago.ArtifactConfigurationFailedException;
import cartago.OpFeedbackParam;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.parser.ParseException;
import org.junit.Test;

import static org.hypermedea.pddl.TermDomainWrapperTest.TEST_DOMAIN_STRUCTURE;
import static org.hypermedea.pddl.TermProblemWrapperTest.TEST_PROBLEM_STRUCTURE;

public class PlannerArtifactTest {

    @Test
    public void testBuildPlan() throws ParseException {
        PlannerArtifact a = new PlannerArtifact();

        OpFeedbackParam<String> res = new OpFeedbackParam<>();

        a.buildPlan(TEST_DOMAIN_STRUCTURE, TEST_PROBLEM_STRUCTURE, res);
        String plan = res.get();

        assert plan != null;

        ASSyntax.parsePlan(plan); // should parse
    }

    @Test
    public void testNonDefaultPlanner() throws ParseException, ArtifactConfigurationFailedException {
        PlannerArtifact a1 = new PlannerArtifact();
        a1.init("HSP");
        PlannerArtifact a2 = new PlannerArtifact();
        a2.init("FF");

        OpFeedbackParam<String> res = new OpFeedbackParam<>();

        a1.buildPlan(TEST_DOMAIN_STRUCTURE, TEST_PROBLEM_STRUCTURE, res);
        String plan1 = res.get();

        a2.buildPlan(TEST_DOMAIN_STRUCTURE, TEST_PROBLEM_STRUCTURE, res);
        String plan2 = res.get();

        assert plan1 != null && plan2 != null && plan1.equals(plan2);
    }

}
