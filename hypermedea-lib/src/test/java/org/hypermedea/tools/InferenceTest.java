package org.hypermedea.tools;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Plan;
import jason.asSyntax.Rule;
import jason.asSyntax.parser.ParseException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

public class InferenceTest {

    @Test
    public void testRulesToPlans() throws ParseException {
        Rule r1 = ASSyntax.parseRule("p(X, Y) :- q(X, Y) .");
        Rule r2 = ASSyntax.parseRule("p(X, Y) :- q(X, Y) & r(Y, Z) .");
        Rule r3 = ASSyntax.parseRule("p(X, Y) :- q(X, Y) & r(Y, Z) & s(Z, Y) .");

        Collection<Plan> plans = Inference.rulesToPlans(Arrays.asList(r1, r2, r3));
        for (Plan p : plans) System.out.println(p.toASString());
    }

}
