package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.util.BitOp;
import fr.uga.pddl4j.util.Plan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper for a PDDL planner provided as a native program (called in a subprocess).
 */
public abstract class NativePlannerWrapper extends PlannerWrapper {

    public static final String DOMAIN_TMP_LOCATION = "/tmp/domain.pddl";

    public static final String PROBLEM_TMP_LOCATION = "/tmp/problem.pddl";

    private final String location;

    public NativePlannerWrapper(String loc) {
        this.location = loc;
    }

    @Override
    public Plan search(Domain domain, Problem problem, CodedProblem codedProblem) {
        try {
            FileWriter dw = new FileWriter(DOMAIN_TMP_LOCATION);
            dw.write(domain.toString());
            dw.close();

            FileWriter pw = new FileWriter(PROBLEM_TMP_LOCATION);
            pw.write(problem.toString());
            pw.close();

            ProcessBuilder psb = new ProcessBuilder(location, "-o", DOMAIN_TMP_LOCATION, "-f", PROBLEM_TMP_LOCATION);
            psb.redirectError(new File("error.log"));

            Process ps = psb.start();
            ps.waitFor();

            return parsePlan(ps.getInputStream(), codedProblem);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Parse the output of the native program and build a {@link Plan object}.
     *
     * @param in the character stream output by the underlying native program
     * @return a PDDL plan
     */
    protected abstract Plan parsePlan(InputStream in, CodedProblem codedProblem) throws IOException;

    /**
     * Build
     *
     * @param name
     * @param params
     * @return
     */
    protected BitOp buildOp(String name, String[] params, CodedProblem codedProblem) {
        BitOp op = new BitOp(name.toLowerCase(), params.length);

        for (int i = 0; i < params.length; i++) {
            String p = params[i].toLowerCase();
            op.setValueOfParameter(i, codedProblem.getConstants().indexOf(p));
        }

        return op;
    }

}
