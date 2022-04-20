package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.encoding.Encoder;
import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.parser.Symbol;
import fr.uga.pddl4j.util.Plan;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.pddl.PlanJasonWrapper;
import org.hypermedea.pddl.TermDomainWrapper;
import org.hypermedea.pddl.TermProblemWrapper;
import org.hypermedea.pddl.TermWrapperException;
import org.hypermedea.pddl.planners.PlannerWrapper;
import org.hypermedea.pddl.planners.PlannerWrapperFactory;

import java.util.HashMap;
import java.util.Map;

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

    public static final String DEFAULT_PLANNER = PlannerWrapperFactory.FF_PLANNER;

    private PlannerWrapper planner;

    /**
     * Initialize the planner artifact with the default planner (see {@link #DEFAULT_PLANNER}).
     */
    public void init() {
        init(DEFAULT_PLANNER);
    }

    /**
     * Initialize the planner artifact with a custom planner.
     *
     * @param plannerName either one of the PDDL4J planners ({@code ff} or {@code hsp}, see {@link PlannerWrapperFactory})
     *                    or a path to an executable program (e.g. {@code /opt/ff})
     */
    public void init(String plannerName) {
        planner = PlannerWrapperFactory.create(plannerName);
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

        Map<Symbol, Term> dictionary = new HashMap<>();

        try {
            Structure domainTerm = ASSyntax.parseStructure(domainStructure);
            Structure problemTerm = ASSyntax.parseStructure(problemStructure);

            TermDomainWrapper domainWrapper = new TermDomainWrapper(domainTerm);
            TermProblemWrapper problemWrapper = new TermProblemWrapper(problemTerm);

            domain = domainWrapper.getDomain();
            pb = problemWrapper.getProblem();

            // TODO use instead CodedProblem.toString(Object)
            dictionary.putAll(domainWrapper.getDictionary());
            dictionary.putAll(problemWrapper.getDictionary());
        } catch (ParseException | TermWrapperException e) {
            e.printStackTrace();
        }

        if (domain == null || pb == null) {
            failed("the provided domain/problem definition is not valid");
            return;
        }

        // TODO internalize coded problem
        CodedProblem cpb = Encoder.encode(domain, pb);

        Plan p = planner.search(domain, pb, cpb);

        PlanJasonWrapper w = new PlanJasonWrapper(p, cpb.getConstants(), dictionary);
        plan.set(w.toString());
    }

    /**
     * operation to serialize a specification in the PDDL format (mostly useful for debugging).
     *
     * @param domainOrProblemStructure a Jason structure defining either a PDDL domain or a PDDL problem
     * @param pddlString a PDDL serialization of the definition
     */
    @OPERATION
    public void getAsPDDL(String domainOrProblemStructure, OpFeedbackParam<String> pddlString) {
        Structure term = null;
        try {
            term = ASSyntax.parseStructure(domainOrProblemStructure);
        } catch (ParseException e) {
            e.printStackTrace();
            failed(String.format("not a valid structure: %s.", domainOrProblemStructure));
        }

        if (term != null) {
            try {
                Object def = null;

                if (term.getFunctor().equals("domain")) def = new TermDomainWrapper(term).getDomain();
                else if (term.getFunctor().equals("problem")) def = new TermProblemWrapper(term).getProblem();
                else throw new TermWrapperException(term, "expected a domain or problem structure");

                pddlString.set(def.toString());
            } catch (TermWrapperException e) {
                e.printStackTrace();
                failed(String.format("not a valid domain or problem definition: %s.", term));
            }
        }
    }

}
