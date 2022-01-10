package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.parser.Domain;
import fr.uga.pddl4j.parser.Problem;
import fr.uga.pddl4j.util.Plan;
import fr.uga.pddl4j.util.SequentialPlan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Wrapper for a PDDL planner provided as a native program (called in a subprocess).
 *
 * By default, Hypermedea includes <a href="https://fai.cs.uni-saarland.de/hoffmann/ff.html">the FF(-X) planner by Joerg Hoffmann</a> (Linux).
 */
public class NativePlannerWrapper extends PlannerWrapper {

    public static final String DOMAIN_TMP_LOCATION = "/tmp/domain.pddl";

    public static final String PROBLEM_TMP_LOCATION = "/tmp/problem.pddl";

    private final String location;

    public NativePlannerWrapper(String loc) {
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

            // TODO handle relative path
            ProcessBuilder psb = new ProcessBuilder(location, "-o " + DOMAIN_TMP_LOCATION, "-f " + PROBLEM_TMP_LOCATION);
            psb.redirectError(new File("error.log"));

            Process ps = null;
            ps = psb.start();
            ps.waitFor();

            // TODO parse result and build plan
            ps.getInputStream();

            Plan p = new SequentialPlan();
            p.add(0, null);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

}
