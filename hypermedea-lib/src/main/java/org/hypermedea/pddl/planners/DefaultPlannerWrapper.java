package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.encoding.Encoder;
import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.util.Plan;

/**
 * Default implementation of {@link PlannerWrapper}, taking a {@link fr.uga.pddl4j.planners.Planner} as argument.
 */
public class DefaultPlannerWrapper extends PlannerWrapper {

    private final Planner planner;

    public DefaultPlannerWrapper(Planner planner) {
        this.planner = planner;
    }

    @Override
    public Plan search(Domain domain, Problem problem) {
        CodedProblem pb = Encoder.encode(domain, problem);
        return planner.search(pb);
    }
}
