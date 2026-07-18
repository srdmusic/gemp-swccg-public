package com.gempukku.swccgo.ai.models.common.policy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TurnResourcePlanTest {
    @Test
    public void exposesCommittedSpendableAndShortfallFacts() {
        TurnResourcePlan solvent = new TurnResourcePlan(12, 2, 3, 1, 2, 1);
        assertEquals(9, solvent.committedForce());
        assertEquals(3, solvent.spendableNow());
        assertEquals(0, solvent.shortfall());

        TurnResourcePlan shortPlan = new TurnResourcePlan(4, 2, 3, 1, 2, 1);
        assertEquals(9, shortPlan.committedForce());
        assertEquals(0, shortPlan.spendableNow());
        assertEquals(5, shortPlan.shortfall());
    }

    @Test
    public void equalityBoundaryLeavesNoSpendableForceAndNoShortfall() {
        TurnResourcePlan exact = new TurnResourcePlan(9, 2, 3, 1, 2, 1);
        assertEquals(0, exact.spendableNow());
        assertEquals(0, exact.shortfall());
    }

    @Test
    public void rejectsNegativeFacts() {
        try {
            new TurnResourcePlan(5, -1, 0, 0, 0, 0);
            fail("expected negative obligation rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
