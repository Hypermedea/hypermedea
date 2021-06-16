package hypermedia;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.encoding.Encoder;
import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.parser.Symbol;
import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.planners.statespace.AbstractStateSpacePlanner;
import fr.uga.pddl4j.planners.statespace.StateSpacePlannerFactory;
import fr.uga.pddl4j.util.Plan;
import planning.PlanJasonWrapper;

/**
 * Artifact to help agents build plans based on PDDL abstractions. The PDDL language includes:
 * <ul>
 *     <li>a domain definition, including a list of (durative) actions, (derived) predicates and optional constraints</li>
 *     <li>a problem definition, including a reference to one or more domain definitions, an initial state and a goal state</li>
 * </ul>
 *
 * @author Victor Charpenay, Jehad Melad
 */
public class PlannerArtifact {

    private final Planner planner;

    public PlannerArtifact() {
        final StateSpacePlannerFactory stateSpacePlannerFactory = StateSpacePlannerFactory.getInstance();
        final Planner.Name plannerName = AbstractStateSpacePlanner.DEFAULT_PLANNER;
        planner = stateSpacePlannerFactory.getPlanner(plannerName);
        // TODO parameterize choice of planner
    }

    /**
     * operation to turn a Jason abstract specification of a PDDL domain/problem into a Jason plan
     * that agents can add to their plan library.
     *
     * @param domainTerm a Jason term defining a PDDL domain
     * @param problemTerm a Jason term defining a PDDL problem
     * @param plan a Jason plan serialized as string
     */
    @OPERATION
    public void buildPlan(Object domainTerm, Object problemTerm, OpFeedbackParam<String> plan) {
        Symbol domainName = new Symbol(Symbol.Kind.PROBLEM, "test-domain");
        Domain domain = new Domain(domainName);
        // TODO build domain from terms

        Symbol pbName = new Symbol(Symbol.Kind.PROBLEM, "test-problem");
        Problem pb = new Problem(pbName);
        // TODO build Problem from terms
        // TODO list objects from initialState

        CodedProblem cpb = Encoder.encode(domain, pb);

        Plan p = planner.search(cpb);

        PlanJasonWrapper w = new PlanJasonWrapper(pbName.toString(), p, cpb);
        plan.set(w.toString());
    }

}
