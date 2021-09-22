package org.hypermedea;

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

}
