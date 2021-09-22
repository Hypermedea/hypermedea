package org.hypermedea.pddl;

import fr.uga.pddl4j.parser.Symbol;
import fr.uga.pddl4j.util.BitOp;
import fr.uga.pddl4j.util.Plan;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.Term;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for PDDL plans output by the planner artifact, to be serialized as Jason plans.
 */
public class PlanJasonWrapper {

    public static final String DEFAULT_GOAL_NAME = "runPlan";

    private final Plan plan;

    private final List<String> indices;

    private final Map<Symbol, Term> labels;

    /**
     * create a wrapper to serialize a Jason plan from a PDDL plan.
     *
     * @param plan the input PDDL plan
     * @param indices a list of symbol strings (symbols are encoded by list index in the input plan)
     */
    public PlanJasonWrapper(Plan plan, List<String> indices) {
        this.plan = plan;
        this.indices = indices;
        this.labels = new HashMap<>();
    }

    /**
     * create a wrapper to serialize a Jason plan from a PDDL plan.
     *
     * @param plan the input PDDL plan
     * @param indices a list of symbol strings (symbols are encoded by list index in the input plan)
     * @param labels a map from symbols to their original label in Jason
     */
    public PlanJasonWrapper(Plan plan, List<String> indices, Map<Symbol, Term> labels) {
        this.plan = plan;
        this.indices = indices;
        this.labels = labels;
    }

    @Override
    public String toString() {
        StringBuilder list = new StringBuilder();

        if (plan == null || plan.size() == 0) {
            list.append("fail");
        } else {
            for (int i = 0; i < plan.size(); i++) {
                BitOp op = plan.actions().get(i);
                StringBuilder args = new StringBuilder();

                for (int j = 0; j < op.getArity(); j++) {
                    String arg = indices.get(op.getValueOfParameter(j));

                    args.append(getLabelForSymbol(arg));

                    if (j < op.getArity() - 1) args.append(", ");
                }

                String a = String.format("%s(%s)", getLabelForSymbol(op.getName()), args);
                list.append(a);

                if (i < plan.size() - 1) list.append("; ");
            }
        }

        return String.format("+!%s : true <- %s .", getProblemName(), list);
    }

    private Term getProblemName() {
        for (Symbol s : labels.keySet()) {
            if (s.getKind().equals(Symbol.Kind.PROBLEM)) return labels.get(s);
        }

        return ASSyntax.createAtom(DEFAULT_GOAL_NAME);
    }

    private Term getLabelForSymbol(String symbolImage) {
        for (Symbol s : labels.keySet()) {
            if (s.getImage().equals(symbolImage)) return labels.get(s);
        }

        return ASSyntax.createAtom(symbolImage);
    }

}
