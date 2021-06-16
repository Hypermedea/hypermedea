package planning;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.util.BitOp;
import fr.uga.pddl4j.util.Plan;

/**
 * Wrapper for PDDL plans output by the planner artifact, to be serialized as Jason plans.
 */
public class PlanJasonWrapper {

    private final String name;

    private final Plan plan;

    private final CodedProblem problem;

    public PlanJasonWrapper(String name, Plan plan, CodedProblem problem) {
        this.name = name;
        this.plan = plan;
        this.problem = problem;
    }

    @Override
    public String toString() {
        StringBuilder list = new StringBuilder();

        for (int i = 0; i < plan.size(); i++) {
            BitOp op = plan.actions().get(i);
            StringBuilder args = new StringBuilder();

            for (int j = 0; j < op.getArity(); j++) {
                String arg = problem.getConstants().get(op.getValueOfParameter(j));
                args.append(arg);

                if (j < op.getArity() - 1) args.append(", ");
            }

            String a = String.format("%s(%s)", op.getName(), args);
            list.append(a);

            if (i < plan.size() - 1) list.append("; ");
        }

        return String.format("!%s : true <- %s .", name, list);
    }

}
