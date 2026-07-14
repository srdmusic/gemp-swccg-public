package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DrawReserveAssessmentTest {

    @Test
    public void baseAddsEachLegacyComponentInSequence() {
        assertEquals(0, reserve(false, false, false, false, false, false, 0));
        assertEquals(1, reserve(true, false, false, false, false, false, 0));
        assertEquals(2, reserve(true, true, false, false, false, false, 0));
        assertEquals(3, reserve(true, true, true, false, false, false, 0));
        assertEquals(4, reserve(true, true, true, true, false, false, 0));
        assertEquals(6, reserve(true, true, true, true, true, false, 0));
        assertEquals(7, reserve(true, true, true, true, true, true, 0));
        assertEquals(9, reserve(true, true, true, true, true, true, 2));
    }

    @Test
    public void baseCapsAtTenBeforeCorridorCharactersAreAdded() {
        DrawReserveAssessment capped = DrawReserveAssessment.base(
                true, true, true, true, true, true, 20);

        assertEquals(10, capped.forceToReserve());
        assertEquals(14, capped.plusCorridorCharacters(4).forceToReserve());
    }

    @Test
    public void baseDoesNotFloorNegativeMaintenance() {
        assertEquals(-3, reserve(false, false, false, false, false, false, -3));
    }

    @Test
    public void corridorAdditionUsesOrdinaryJavaIntOverflow() {
        DrawReserveAssessment assessment = new DrawReserveAssessment(Integer.MAX_VALUE);

        assertEquals(Integer.MIN_VALUE,
                assessment.plusCorridorCharacters(1).forceToReserve());
    }

    private static int reserve(boolean drawTheirFire,
                               boolean firstStrike,
                               boolean contestedAny,
                               boolean turnFourOrLater,
                               boolean imperialArrestOrder,
                               boolean vergeNeedsDeathStarMove,
                               int maintenanceObligation) {
        return DrawReserveAssessment.base(
                drawTheirFire,
                firstStrike,
                contestedAny,
                turnFourOrLater,
                imperialArrestOrder,
                vergeNeedsDeathStarMove,
                maintenanceObligation).forceToReserve();
    }
}
