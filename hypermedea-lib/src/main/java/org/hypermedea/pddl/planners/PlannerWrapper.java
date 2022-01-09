package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.util.Plan;

/**
 * Wrapper for either a native planner implementation (called as a subprocess) or a PDDL4J planner, accessing
 * Java data structures.
 */
public abstract class PlannerWrapper {

    public abstract Plan search(Domain domain, Problem problem);

}
