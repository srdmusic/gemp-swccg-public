package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ActivateAmountPolicyTest {

    @Test
    public void fullActivationUsesRawMaximum() {
        assertResult(0, 7, 20, 40, 7,
                ActivateAmountPolicy.Mode.ACTIVATE_FULL);
    }

    @Test
    public void destinyBufferKeepsFourReserveCards() {
        assertResult(0, 7, 6, 40, 2,
                ActivateAmountPolicy.Mode.KEEP_FOUR_FOR_DESTINY);
    }

    @Test
    public void replayBoundaryActivatesSevenOfElevenOnAQuietBoard() {
        assertResult(0, 11, 11, 22, 7,
                ActivateAmountPolicy.Mode.KEEP_FOUR_FOR_DESTINY);
    }

    @Test
    public void lowLifeKeepsTwoCards() {
        assertResult(0, 7, 20, 10, 5,
                ActivateAmountPolicy.Mode.KEEP_TWO_AT_LOW_LIFE);
    }

    @Test
    public void positiveRangeKeepsAtLeastOne() {
        assertResult(0, 7, 2, 40, 1,
                ActivateAmountPolicy.Mode.KEEP_FOUR_FOR_DESTINY);
    }

    @Test
    public void rawMinimumClampRunsAfterKeepFourFloor() {
        assertResult(4, 7, 3, 40, 4,
                ActivateAmountPolicy.Mode.KEEP_FOUR_FOR_DESTINY);
    }

    @Test
    public void lowerLowLifeCapDoesNotReplaceExistingKeepFourMode() {
        assertResult(0, 7, 6, 10, 2,
                ActivateAmountPolicy.Mode.KEEP_FOUR_FOR_DESTINY);
    }

    @Test
    public void malformedBoundsAreRejected() {
        assertInvalid(-1, 3);
        assertInvalid(0, 0);
        assertInvalid(4, 3);
    }

    private static void assertResult(int min, int max, int reserve, int life,
                                     int amount,
                                     ActivateAmountPolicy.Mode mode) {
        ActivateAmountPolicy.Result result = ActivateAmountPolicy.assess(
                new ActivateAmountPolicy.Input(min, max, reserve, life));
        assertEquals(amount, result.amount());
        assertEquals(mode, result.mode());
    }

    private static void assertInvalid(int min, int max) {
        try {
            new ActivateAmountPolicy.Input(min, max, 10, 20);
            fail("expected invalid activation bounds");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
