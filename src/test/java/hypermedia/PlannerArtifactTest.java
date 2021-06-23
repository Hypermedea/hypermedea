package hypermedia;

import cartago.OpFeedbackParam;
import org.junit.Test;

import static planning.TermDomainWrapperTest.TEST_DOMAIN_STRUCTURE;
import static planning.TermProblemWrapperTest.TEST_PROBLEM_STRUCTURE;

public class PlannerArtifactTest {

    @Test
    public void testBuildPlan() {
        PlannerArtifact a = new PlannerArtifact();

        OpFeedbackParam<String> res = new OpFeedbackParam<>();

        a.buildPlan(TEST_DOMAIN_STRUCTURE, TEST_PROBLEM_STRUCTURE, res);

        assert res.get() != null;
    }

}
