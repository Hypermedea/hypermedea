package org.hypermedea.tools;

import jason.asSyntax.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Inference {

    public static Collection<Plan> rulesToPlans(Collection<Rule> rules) {
        Collection<Plan> plans = new HashSet<>();

        for (Rule r : rules) {
            Literal h = r.getHead();
            LogicalFormula b = r.getBody();

            Collection<Literal> conjuncts = collectConjuncts(b);

            for (Literal l : conjuncts) {
                Set<Literal> context = new HashSet<>(conjuncts);
                context.remove(l);

                LogicalFormula contextExpr = buildConjunction(context);

                Plan p = buildPlan(h, l, contextExpr);
                plans.add(p);
            }
        }

        // TODO add negative plans (to retract inferred statements)

        return plans;
    }

    private static Collection<Literal> collectConjuncts(LogicalFormula f) {
        if (!(f instanceof LogExpr)) {
            Collection<Literal> conjuncts = new HashSet<>();
            conjuncts.add((Literal) f);
            return conjuncts;
        }

        LogExpr expr = (LogExpr) f;
        // left associativity of &: left-hand side always literal (or disjunction)
        Collection<Literal> conjuncts = collectConjuncts(expr.getRHS());

        LogicalFormula lhs = expr.getLHS();
        if (lhs instanceof Literal) conjuncts.add((Literal) lhs);
        // TODO if not pure conjunction, get DNF first

        return conjuncts;
    }

    private static LogicalFormula buildConjunction(Collection<Literal> conjuncts) {
        if (conjuncts.isEmpty()) return null;

        Literal l = conjuncts.stream().findAny().get();
        if (conjuncts.size() == 1) return l;

        conjuncts.remove(l);
        return new LogExpr(l, LogExpr.LogicalOp.and, buildConjunction(conjuncts));
    }

    private static Plan buildPlan(Literal head, Literal bodyTrigger, LogicalFormula bodyCondition) {
        Trigger trigger = new Trigger(Trigger.TEOperator.add, Trigger.TEType.belief, bodyTrigger);

        PlanBody action = new PlanBodyImpl(PlanBody.BodyType.addBel, head);

        PlanBody body = action;
        if (bodyCondition != null) {
            InternalActionLiteral loop = new InternalActionLiteral(".foreach");
            loop.addTerms(bodyCondition, action);
            body = new PlanBodyImpl(PlanBody.BodyType.action, loop);
        }

        return new Plan(null, trigger, null, body);
    }

}
