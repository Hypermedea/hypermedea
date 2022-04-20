package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.encoding.Encoder;
import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.util.Plan;

/**
 * Wrapper for either a native planner implementation (called as a subprocess) or a PDDL4J planner, accessing
 * Java data structures.
 */
public abstract class PlannerWrapper {

    /**
     * Convenience method that builds an instance of {@link CodedProblem} and calls
     * {@link #search(Domain, Problem, CodedProblem)} with the coded problem passed as argument.
     *
     * @param domain a PDDL domain
     * @param problem a PDDL problem
     * @return a plan solving the given problem
     */
    public Plan search(Domain domain, Problem problem) {
        return search(domain, problem, Encoder.encode(domain, problem));
    }

    /**
     * Search a valid plan that solves the given problem.
     * To encode the problem and domain definitions, use {@link Encoder#encode(Domain, Problem)}}.
     *
     * @param domain a PDDL domain
     * @param problem a PDDL problem
     * @param codedProblem an indexed representation of the given problem (each PDDL definition maps to an integer)
     * @return a plan solving the given problem
     */
    public abstract Plan search(Domain domain, Problem problem, CodedProblem codedProblem);

}
