package pl;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import jason.pl.PlanLibrary;

import java.util.HashSet;
import java.util.Set;

public class build_plan extends DefaultInternalAction {

    private class Operator {

        public final Plan plan;

        public final LogicalFormula precondition;

        public final LogicalFormula effect;

        public Operator(Plan p) {
            this.plan = p;
            this.precondition = p.getContext();

            if (p.getTrigger().isAchvGoal()) this.effect = p.getTrigger().getLiteral();
            else this.effect = ASSyntax.createAtom("false");
        }

    }

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (args.length != 1) {
            String msg = build_plan.class.getName() + " expects a single argument, " + args.length + " received";
            throw new IllegalArgumentException(msg);
        }

        if (!args[0].isLiteral()) {
            String msg = build_plan.class.getName() + " expects a 'goal' argument as literal, " + args[0] + " received";
            throw new IllegalArgumentException(msg);
        }

        Literal goal = (Literal) args[0];

        getDomain(ts.getAg().getPL());
        getProblem(ts.getAg().getBB());

        return null;
    }

    private Set<Operator> getDomain(PlanLibrary pl) {
        Set<Operator> ops = new HashSet<>();
        for (Plan p : pl.getPlans()) ops.add(new Operator(p));
        return ops;
    }

    private void getProblem(BeliefBase bb) {

    }

    private void plan() {
        // simple planning: backward chaining with loop detection
    }

}
