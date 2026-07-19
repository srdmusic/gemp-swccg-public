package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoveBlockedResponsePolicyTest {
    @Test
    public void blockedMatchRequiresNonEmptyExactSetMembership() {
        assertFalse(MoveBlockedResponsePolicy.matches(
                null, "7", "Move using landspeed"));
        assertFalse(MoveBlockedResponsePolicy.matches(
                Collections.emptySet(), "7", "Move using landspeed"));
        assertFalse(MoveBlockedResponsePolicy.matches(
                Set.of("8"), "7", "Move using landspeed"));
        assertFalse(MoveBlockedResponsePolicy.matches(
                Set.of("move using landspeed"),
                "7", "Move using landspeed"));
    }

    @Test
    public void blockedMatchPreservesIdOrTextBehavior() {
        assertTrue(MoveBlockedResponsePolicy.matches(
                Set.of("7"), "7", "Move using landspeed"));
        assertTrue(MoveBlockedResponsePolicy.matches(
                Set.of("Move using landspeed"),
                "7", "Move using landspeed"));
        assertTrue(MoveBlockedResponsePolicy.matches(
                Set.of("7", "Move using landspeed"),
                "7", "Move using landspeed"));
    }

    @Test
    public void notBlockedClassificationIgnoresPowerFacts() {
        MoveBlockedResponsePolicy.Evaluation result =
                MoveBlockedResponsePolicy.classify(
                        false, true, 1.0f, 99.0f);

        assertEquals(MoveBlockedResponsePolicy.Outcome.NOT_BLOCKED,
                result.outcome());
        assertNull(result.reason());
        assertRawFloat(0.0f, result.delta());
        assertFalse(result.powerFactsAvailable());
        assertRawFloat(0.0f, result.ourPower());
        assertRawFloat(0.0f, result.opponentPower());
    }

    @Test
    public void blockedWithoutPowerFactsPreservesHardBlock() {
        MoveBlockedResponsePolicy.Evaluation result =
                MoveBlockedResponsePolicy.classify(
                        true, false, 0.0f, 0.0f);

        assertHardBlock(result);
        assertFalse(result.powerFactsAvailable());
    }

    @Test
    public void strictGreaterOpponentPowerPreservesEndangeredFallthrough() {
        MoveBlockedResponsePolicy.Evaluation result =
                MoveBlockedResponsePolicy.classify(
                        true, true, 6.0f, Math.nextUp(6.0f));

        assertEquals(
                MoveBlockedResponsePolicy.Outcome.ENDANGERED_FALLTHROUGH,
                result.outcome());
        assertNull(result.reason());
        assertRawFloat(0.0f, result.delta());
        assertTrue(result.powerFactsAvailable());
        assertRawFloat(6.0f, result.ourPower());
        assertRawFloat(Math.nextUp(6.0f), result.opponentPower());
    }

    @Test
    public void equalOrLowerOpponentPowerPreservesHardBlock() {
        assertHardBlock(MoveBlockedResponsePolicy.classify(
                true, true, 6.0f, 6.0f));
        assertHardBlock(MoveBlockedResponsePolicy.classify(
                true, true, 6.0f, 5.0f));
    }

    @Test
    public void nanPowerPreservesHardBlock() {
        assertHardBlock(MoveBlockedResponsePolicy.classify(
                true, true, Float.NaN, 7.0f));
        assertHardBlock(MoveBlockedResponsePolicy.classify(
                true, true, 6.0f, Float.NaN));
    }

    @Test
    public void endangeredRetryBudgetPreservesThreeSoftThenHardBoundary() {
        MoveBlockedResponsePolicy.RetryBudget budget =
                new MoveBlockedResponsePolicy.RetryBudget();

        for (int attempt = 1; attempt <= 3; attempt++) {
            MoveBlockedResponsePolicy.RetryEvaluation result =
                    budget.evaluate(7, "Move using landspeed");
            assertFalse(result.hardBlock());
            assertRawFloat(-250.0f, result.delta());
            assertEquals(attempt, result.attempt());
            assertEquals(3, result.retryBudget());
            assertEquals(
                    "BLOCKED (loop prevention) — soft (V169: endangered mover, retreat must stay possible)",
                    result.reason());
        }

        MoveBlockedResponsePolicy.RetryEvaluation exhausted =
                budget.evaluate(7, "Move using landspeed");
        assertTrue(exhausted.hardBlock());
        assertRawFloat(-100000.0f, exhausted.delta());
        assertEquals(4, exhausted.attempt());
        assertEquals(
                "BLOCKED (loop prevention) — hard veto (V169 retry budget exhausted: no safe destination materialized)",
                exhausted.reason());
    }

    @Test
    public void endangeredRetryBudgetResetsByTurnAndKeepsKeysIndependent() {
        MoveBlockedResponsePolicy.RetryBudget budget =
                new MoveBlockedResponsePolicy.RetryBudget();

        assertEquals(1, budget.evaluate(3, "first").attempt());
        assertEquals(2, budget.evaluate(3, "first").attempt());
        assertEquals(1, budget.evaluate(3, "second").attempt());
        assertEquals(1, budget.evaluate(4, "first").attempt());
    }

    private static void assertHardBlock(
            MoveBlockedResponsePolicy.Evaluation result) {
        assertEquals(MoveBlockedResponsePolicy.Outcome.HARD_BLOCK,
                result.outcome());
        assertEquals(
                "CANCEL-LOOP BLOCK: this move led to repeated Done-cancels — try something else (LADDER VETO)",
                result.reason());
        assertRawFloat(-100000.0f, result.delta());
    }

    private static void assertRawFloat(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
