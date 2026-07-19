package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoveThreatPolicyTest {
    private static final int FAVORABLE = 4;
    private static final int DANGER = -6;

    @Test
    public void opponentPowerGatePreservesStrictGreaterThanZero() {
        assertNoThreat(0.0f);
        assertNoThreat(-1.0f);
        assertNoThreat(Float.NaN);
        assertTrue(evaluate(1.0f, 0.0f).applies());
    }

    @Test
    public void exactBoundariesPreserveThreatLevels() {
        assertLevel(MoveThreatPolicy.ThreatLevel.CRUSH, 8.0f);
        assertLevel(MoveThreatPolicy.ThreatLevel.FAVORABLE,
                Math.nextDown(8.0f));
        assertLevel(MoveThreatPolicy.ThreatLevel.FAVORABLE, 4.0f);
        assertLevel(MoveThreatPolicy.ThreatLevel.RISKY,
                Math.nextDown(4.0f));
        assertLevel(MoveThreatPolicy.ThreatLevel.RISKY, -4.0f);
        assertLevel(MoveThreatPolicy.ThreatLevel.DANGEROUS,
                Math.nextDown(-4.0f));
        assertLevel(MoveThreatPolicy.ThreatLevel.DANGEROUS, -6.0f);
        assertLevel(MoveThreatPolicy.ThreatLevel.RETREAT,
                Math.nextDown(-6.0f));
    }

    @Test
    public void crushPreservesReasonAndDelta() {
        MoveThreatPolicy.Evaluation result = evaluate(1.0f, 8.0f);

        assertEquals("V37.1 STAY AND CRUSH: Power +8 — DESTROY them!",
                result.reason());
        assertFloat(-1500.0f, result.delta());
        assertFalse(result.claimSurvivalRank());
    }

    @Test
    public void favorablePreservesReasonAndDelta() {
        MoveThreatPolicy.Evaluation result = evaluate(1.0f, 4.0f);

        assertEquals("V37.1 STAY AND FIGHT: Power +4 — hold position!",
                result.reason());
        assertFloat(-1500.0f, result.delta());
        assertFalse(result.claimSurvivalRank());
    }

    @Test
    public void riskyPreservesReasonAndDelta() {
        MoveThreatPolicy.Evaluation result = evaluate(1.0f, -4.0f);

        assertEquals("V37.1 CONTESTED: Even power (-4) — hold position!",
                result.reason());
        assertFloat(-500.0f, result.delta());
        assertFalse(result.claimSurvivalRank());
    }

    @Test
    public void dangerousPreservesReasonAndDelta() {
        MoveThreatPolicy.Evaluation result = evaluate(1.0f, -6.0f);

        assertEquals("Dangerous location - retreat recommended (-6)",
                result.reason());
        assertFloat(20.0f, result.delta());
        assertFalse(result.claimSurvivalRank());
    }

    @Test
    public void retreatPreservesReasonDeltaAndSurvivalClaim() {
        MoveThreatPolicy.Evaluation result = evaluate(1.0f, -7.0f);

        assertEquals("Strategic retreat - badly outmatched (-7)",
                result.reason());
        assertFloat(150.0f, result.delta());
        assertTrue(result.claimSurvivalRank());
    }

    @Test
    public void reasonsPreserveJavaIntegerTruncation() {
        assertEquals("V37.1 STAY AND CRUSH: Power +8 — DESTROY them!",
                evaluate(1.0f, 8.9f).reason());
        assertEquals("Strategic retreat - badly outmatched (-6)",
                evaluate(1.0f, -6.9f).reason());
    }

    @Test
    public void nanPowerDifferencePreservesRetreatFallthrough() {
        MoveThreatPolicy.Evaluation result =
                evaluate(1.0f, Float.NaN);

        assertEquals(MoveThreatPolicy.ThreatLevel.RETREAT,
                result.level());
        assertEquals("Strategic retreat - badly outmatched (0)",
                result.reason());
        assertFloat(150.0f, result.delta());
        assertTrue(result.claimSurvivalRank());
    }

    private static MoveThreatPolicy.Evaluation evaluate(
            float opponentPower, float powerDiff) {
        return MoveThreatPolicy.evaluate(
                opponentPower, powerDiff, FAVORABLE, DANGER);
    }

    private static void assertNoThreat(float opponentPower) {
        MoveThreatPolicy.Evaluation result = evaluate(opponentPower, 0.0f);
        assertFalse(result.applies());
        assertNull(result.level());
        assertNull(result.reason());
        assertFloat(0.0f, result.delta());
        assertFalse(result.claimSurvivalRank());
    }

    private static void assertLevel(
            MoveThreatPolicy.ThreatLevel expected, float powerDiff) {
        assertEquals(expected, evaluate(1.0f, powerDiff).level());
    }

    private static void assertFloat(float expected, float actual) {
        assertEquals(expected, actual, 0.0001f);
    }
}
