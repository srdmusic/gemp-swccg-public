package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ActivateAmountPolicyTest {

    @Test
    public void fullActivationUsesRawMaximum() {
        assertResult(0, 7, 20, 40, false, 7,
                ActivateAmountPolicy.Mode.ACTIVATE_FULL);
    }

    @Test
    public void battlePlanKeepsThreeReserveCards() {
        assertResult(0, 7, 6, 40, true, 3,
                ActivateAmountPolicy.Mode.KEEP_THREE_FOR_BATTLE);
    }

    @Test
    public void lowLifeKeepsTwoCards() {
        assertResult(0, 7, 20, 10, false, 5,
                ActivateAmountPolicy.Mode.KEEP_TWO_AT_LOW_LIFE);
    }

    @Test
    public void positiveRangeKeepsAtLeastOne() {
        assertResult(0, 7, 2, 40, true, 1,
                ActivateAmountPolicy.Mode.KEEP_THREE_FOR_BATTLE);
    }

    @Test
    public void rawMinimumClampRunsAfterKeepThreeFloor() {
        assertResult(4, 7, 3, 40, true, 4,
                ActivateAmountPolicy.Mode.KEEP_THREE_FOR_BATTLE);
    }

    @Test
    public void lowerLowLifeCapDoesNotReplaceExistingKeepThreeMode() {
        assertResult(0, 7, 6, 10, true, 3,
                ActivateAmountPolicy.Mode.KEEP_THREE_FOR_BATTLE);
    }

    @Test
    public void malformedBoundsAreRejected() {
        assertInvalid(-1, 3);
        assertInvalid(0, 0);
        assertInvalid(4, 3);
    }

    private static void assertResult(int min, int max, int reserve, int life,
                                     boolean battlePlausible, int amount,
                                     ActivateAmountPolicy.Mode mode) {
        ActivateAmountPolicy.Result result = ActivateAmountPolicy.assess(
                new ActivateAmountPolicy.Input(min, max, reserve, life, battlePlausible));
        assertEquals(amount, result.amount());
        assertEquals(mode, result.mode());
    }

    private static void assertInvalid(int min, int max) {
        try {
            new ActivateAmountPolicy.Input(min, max, 10, 20, false);
            fail("expected invalid activation bounds");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
