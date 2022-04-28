package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.util.Plan;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for either a native planner implementation (called as a subprocess) or a PDDL4J planner, accessing
 * Java data structures.
 */
public abstract class PlannerWrapper {

    protected final List<String> constantIndex = new ArrayList<>();

    /**
     * Search a valid plan that solves the given problem.
     *
     * @param domain a PDDL domain
     * @param problem a PDDL problem
     * @return a plan solving the given problem
     */
    public abstract Plan search(Domain domain, Problem problem);

    /**
     * Get an index of constants maintained by the wrapper, as used by the underlying planner.
     *
     * FIXME this method is stateful: it depends on previous calls to search()
     *
     * @return a list of constants (whose indices are used in the plan returned by {@link #search(Domain, Problem)})
     */
    public List<String> getConstantIndex() {
        return constantIndex;
    }

}
