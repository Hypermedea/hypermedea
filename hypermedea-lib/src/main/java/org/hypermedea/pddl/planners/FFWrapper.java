package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.encoding.CodedProblem;
import fr.uga.pddl4j.util.Plan;
import fr.uga.pddl4j.util.SequentialPlan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper for <a href="https://fai.cs.uni-saarland.de/hoffmann/ff.html">the FF(-X) planner by Joerg Hoffmann</a> (Linux).
 */
public class FFWrapper extends NativePlannerWrapper {

    public static final String FF_BINARY_LOCATION = FFWrapper.class.getClassLoader().getResource("ff").getFile();

    public FFWrapper() {
        super(FF_BINARY_LOCATION);
    }

    @Override
    protected Plan parsePlan(InputStream in, CodedProblem cpb) throws IOException {
        Plan p = new SequentialPlan();

        BufferedReader lineReader = new BufferedReader(new InputStreamReader(in));

        Boolean stepLine = false;
        Boolean endLine = false;

        // regex should match a number, followed by an action name and a list of parameters
        Pattern stepPattern = Pattern.compile("\\d+: (?<name>\\w+)(?<params>( \\w+)*)$");

        while (!endLine) {
            String l = lineReader.readLine();

            if (l == null || (stepLine && l.isEmpty())) endLine = true;
            else if (l.startsWith("step")) stepLine = true;

            if (stepLine && !endLine) {
                Matcher m = stepPattern.matcher(l);
                if (m.find()) {
                    String name = m.group("name");
                    String[] params = m.group("params").stripLeading().split(" ");

                    p.add(p.size(), buildOp(name, params, cpb));
                } else {
                    // TODO warn something's wrong
                }
            }
        }

        return p;
    }

}
