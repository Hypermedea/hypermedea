package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.util.BitOp;
import fr.uga.pddl4j.util.Plan;
import fr.uga.pddl4j.util.SequentialPlan;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper for a PDDL planner provided as a native program (called in a subprocess).
 *
 * The program's output is parsed according to the rules given in <a href="https://github.com/KCL-Planning/VAL">VAL</a>.
 * A plan is a sequence of steps {@code 0: STEP1\n 1: STEP2\n...} where each step is one of:
 * <ul>
 *     <li>NAME ARG1 ARG2 .. ARGN</li>
 *     <li>(NAME ARG1 ARG2 .. ARGN) (TODO)</li>
 *     <li>NAME [ARG1 ARG2 .. ARGN] (TODO)</li>
 * </ul>
 *
 * @author Victor Charpenay
 */
public class NativePlannerWrapper extends PlannerWrapper {

    public static final String DOMAIN_TMP_LOCATION = "/tmp/domain.pddl";

    public static final String PROBLEM_TMP_LOCATION = "/tmp/problem.pddl";

    private final String location;

    public NativePlannerWrapper(String loc) {
        File program = new File(loc);

        if (!program.exists()) throw new PlannerNotFoundException(loc);
        // TODO check program is executable

        this.location = loc;
    }

    @Override
    public Plan search(Domain domain, Problem problem) {
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

            return parsePlan(ps.getInputStream());
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
    protected Plan parsePlan(InputStream in) throws IOException {
        Plan p = new SequentialPlan();

        BufferedReader lineReader = new BufferedReader(new InputStreamReader(in));

        // regex should match a number, followed by an action name and a list of parameters
        Pattern stepPattern = Pattern.compile("\\d+: (?<name>\\w+)(?<params>( \\w+)*)$");

        Boolean endLine = false;

        while (!endLine) {
            String l = lineReader.readLine();

            if (l == null) {
                endLine = true;
            } else {
                Matcher m = stepPattern.matcher(l);

                if (m.find()) {
                    String name = m.group("name");
                    String[] params = m.group("params").stripLeading().split(" ");

                    p.add(p.size(), buildOp(name, params));
                }
            }
        }

        return p;
    }

    /**
     * Build a compact representation of an operation {@code name(params...)}
     * based on the indexed representation of the problem.
     *
     * @param name operation name
     * @param params operation parameters
     * @return a compact representation of the operation
     */
    protected BitOp buildOp(String name, String[] params) {
        BitOp op = new BitOp(name.toLowerCase(), params.length);

        for (int i = 0; i < params.length; i++) {
            String p = params[i].toLowerCase();
            Integer indexValue = constantIndex.indexOf(p);

            if (indexValue < 0) {
                constantIndex.add(p);
                indexValue = getConstantIndex().size() - 1;
            }

            op.setValueOfParameter(i, indexValue);
        }

        return op;
    }

}
