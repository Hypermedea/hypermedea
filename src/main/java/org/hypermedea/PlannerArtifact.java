package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.encoding.Encoder;
import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.planners.statespace.AbstractStateSpacePlanner;
import fr.uga.pddl4j.planners.statespace.StateSpacePlannerFactory;
import fr.uga.pddl4j.util.Plan;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.planning.TermDomainWrapper;
import org.hypermedea.planning.TermProblemWrapper;
import org.hypermedea.planning.PlanJasonWrapper;

/**
 * Artifact to help agents build plans based on PDDL abstractions. The PDDL language includes:
 * <ul>
 *     <li>a domain definition, including a list of (durative) actions, (derived) predicates and optional constraints</li>
 *     <li>a problem definition, including a reference to one or more domain definitions, an initial state and a goal state</li>
 * </ul>
 *
 * @author Victor Charpenay, Jehad Melad
 */
public class PlannerArtifact extends Artifact {

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
     * @param domainStructure a Jason structure defining a PDDL domain
     * @param problemStructure a Jason structure defining a PDDL problem
     * @param plan a Jason plan serialized as string
     */
    @OPERATION
    public void buildPlan(String domainStructure, String problemStructure, OpFeedbackParam<String> plan) {
        Domain domain = null;
        Problem pb = null;

        try {
            Structure domainTerm = ASSyntax.parseStructure(domainStructure);
            Structure problemTerm = ASSyntax.parseStructure(problemStructure);

            domain = new TermDomainWrapper(domainTerm).getDomain();

            pb = new TermProblemWrapper(problemTerm).getProblem();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (domain == null || pb == null) {
            failed("The provided domain/problem definition is not valid");
            return;
        }

        CodedProblem cpb = Encoder.encode(domain, pb);

        Plan p = planner.search(cpb);

        PlanJasonWrapper w = new PlanJasonWrapper(pb.getName().toString(), p, cpb);
        plan.set(w.toString());
    }

}
