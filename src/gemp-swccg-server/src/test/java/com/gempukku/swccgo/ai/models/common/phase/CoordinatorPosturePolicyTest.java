package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoordinatorPosturePolicyTest {

    @Test
    public void scoresRetainExactBandsAndAdditivity() {
        assertEquals(40, CoordinatorPosturePolicy.score(
                true, false, false, true, false, false, false));
        assertEquals(40, CoordinatorPosturePolicy.score(
                true, false, false, false, true, false, false));
        assertEquals(30, CoordinatorPosturePolicy.score(
                false, true, false, false, true, false, false));
        assertEquals(-30, CoordinatorPosturePolicy.score(
                false, false, true, false, true, false, false));
        assertEquals(20, CoordinatorPosturePolicy.score(
                false, false, true, false, false, true, false));
        assertEquals(60, CoordinatorPosturePolicy.score(
                false, false, false, false, false, false, true));
        assertEquals(120, CoordinatorPosturePolicy.score(
                true, true, true, true, true, true, true));
        assertEquals(0, CoordinatorPosturePolicy.score(
                true, true, true, false, false, false, false));
    }
}
