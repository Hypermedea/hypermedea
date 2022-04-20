package org.hypermedea.pddl.planners;

import fr.uga.pddl4j.planners.Planner;
import fr.uga.pddl4j.planners.statespace.StateSpacePlannerFactory;

public class PlannerWrapperFactory {

    public static final String FF_PLANNER = "ff";

    public static final String HSP_PLANNER = "hsp";

    public static PlannerWrapper create(String plannerName) {
        switch (plannerName.toLowerCase()) {
            case FF_PLANNER:
            case HSP_PLANNER:
                final StateSpacePlannerFactory stateSpacePlannerFactory = StateSpacePlannerFactory.getInstance();
                final Planner.Name name = Planner.Name.valueOf(plannerName.toUpperCase());
                Planner p = stateSpacePlannerFactory.getPlanner(name);

                return new DefaultPlannerWrapper(p);

            default:
                return new NativePlannerWrapper(plannerName);
        }
    }

}
