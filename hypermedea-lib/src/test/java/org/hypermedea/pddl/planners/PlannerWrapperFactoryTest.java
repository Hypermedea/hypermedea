package org.hypermedea.pddl.planners;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlannerWrapperFactoryTest {

    @Test(expected = PlannerNotFoundException.class)
    public void testNotFound() {
        PlannerWrapperFactory.create("not-found");
    }

    @Test
    public void testFF() {
        PlannerWrapper p = PlannerWrapperFactory.create(PlannerWrapperFactory.FF_PLANNER);

        assertEquals(p.getClass(), DefaultPlannerWrapper.class);
    }

    @Test
    public void testFFNative() {
        PlannerWrapper p = PlannerWrapperFactory.create(PlannerWrapperFactory.FF_NATIVE_PLANNER);

        assertEquals(p.getClass(), FFWrapper.class);
    }

}
