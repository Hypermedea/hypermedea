package org.hypermedea.pddl.planners;

class PlannerNotFoundException extends RuntimeException {

    PlannerNotFoundException(String plannerName) {
        super(String.format("Planner '%s' could not be found.", plannerName));
    }

}
