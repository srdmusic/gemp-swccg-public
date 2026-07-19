package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoveBuddyProtectionPolicyTest {
    @Test
    public void buddyPairRequiresExactlyTwoCharactersIncludingMover() {
        assertTrue(MoveBuddyProtectionPolicy.hasBuddyPair(2, true));
        assertFalse(MoveBuddyProtectionPolicy.hasBuddyPair(2, false));
        assertFalse(MoveBuddyProtectionPolicy.hasBuddyPair(1, true));
        assertFalse(MoveBuddyProtectionPolicy.hasBuddyPair(3, true));
    }

    @Test
    public void powerAnalysisUsesExactVulnerabilityAndPresenceBoundaries() {
        assertTrue(MoveBuddyProtectionPolicy.needsPowerAnalysis(5, 6, 0.0f));
        assertFalse(MoveBuddyProtectionPolicy.needsPowerAnalysis(6, 6, 0.0f));
        assertFalse(MoveBuddyProtectionPolicy.needsPowerAnalysis(8, 6, -1.0f));
        assertTrue(MoveBuddyProtectionPolicy.needsPowerAnalysis(8, 6, 0.1f));
    }

    @Test
    public void safeAllyWithoutEnemyProducesNoContribution() {
        assertNone(MoveBuddyProtectionPolicy.evaluate(
                "Cloud City: Downtown Plaza",
                "Lando Calrissian",
                6,
                6,
                8.0f,
                0.0f));
    }

    @Test
    public void vulnerableAllyWithoutEnemyGetsBaseProtectionPenalty() {
        MoveBuddyProtectionPolicy.Evaluation result =
                MoveBuddyProtectionPolicy.evaluate(
                        "Cloud City: Downtown Plaza",
                        "Lando Calrissian",
                        5,
                        6,
                        8.0f,
                        0.0f);

        assertBuddy(result, -150.0f, false);
        assertEquals(
                "V27 BUDDY PROTECT: Moving away leaves Lando Calrissian"
                        + " (power 5) ALONE at Cloud City: Downtown Plaza!",
                result.reason());
    }

    @Test
    public void enemyThatOverpowersAllyGetsCriticalPenalty() {
        MoveBuddyProtectionPolicy.Evaluation result =
                MoveBuddyProtectionPolicy.evaluate(
                        "Cloud City: Downtown Plaza",
                        "Lando Calrissian",
                        4,
                        6,
                        10.0f,
                        7.0f);

        assertBuddy(result, -400.0f, true);
        assertEquals(
                "V27 BUDDY PROTECT: Moving away leaves Lando Calrissian"
                        + " (power 4) ALONE at Cloud City: Downtown Plaza!"
                        + " ENEMY POWER=7!",
                result.reason());
    }

    @Test
    public void enemyThatDoesNotOverpowerAllyGetsPresencePenalty() {
        MoveBuddyProtectionPolicy.Evaluation result =
                MoveBuddyProtectionPolicy.evaluate(
                        "Cloud City: Downtown Plaza",
                        "Lando Calrissian",
                        8,
                        6,
                        10.0f,
                        7.0f);

        assertBuddy(result, -250.0f, true);
    }

    @Test
    public void exactDoublePowerBoundaryClaimsDoomedEscape() {
        MoveBuddyProtectionPolicy.Evaluation result =
                MoveBuddyProtectionPolicy.evaluate(
                        "Farm",
                        "C-3PO",
                        3,
                        6,
                        6.0f,
                        12.0f);

        assertDoomed(result, 6, 12);
    }

    @Test
    public void exactTenPowerDifferenceClaimsDoomedEscape() {
        MoveBuddyProtectionPolicy.Evaluation result =
                MoveBuddyProtectionPolicy.evaluate(
                        "Farm",
                        "C-3PO",
                        3,
                        6,
                        15.0f,
                        25.0f);

        assertDoomed(result, 15, 25);
    }

    @Test
    public void justBelowBothDoomedBoundariesKeepsBuddyProtection() {
        MoveBuddyProtectionPolicy.Evaluation result =
                MoveBuddyProtectionPolicy.evaluate(
                        "Farm",
                        "C-3PO",
                        20,
                        6,
                        10.0f,
                        19.9f);

        assertBuddy(result, -250.0f, true);
    }

    @Test
    public void failedOurPowerReadDefaultStillPreservesDoomedBranch() {
        MoveBuddyProtectionPolicy.Evaluation result =
                MoveBuddyProtectionPolicy.evaluate(
                        "Farm",
                        "C-3PO",
                        3,
                        6,
                        0.0f,
                        1.9f);

        assertDoomed(result, 0, 1);
    }

    private static void assertNone(
            MoveBuddyProtectionPolicy.Evaluation result) {
        assertEquals(MoveBuddyProtectionPolicy.Branch.NONE, result.branch());
        assertFalse(result.applies());
        assertEquals(0.0f, result.delta(), 0.0f);
        assertFalse(result.claimSurvival());
        assertFalse(result.enemyThreat());
        assertNull(result.reason());
    }

    private static void assertBuddy(
            MoveBuddyProtectionPolicy.Evaluation result,
            float delta,
            boolean enemyThreat) {
        assertEquals(MoveBuddyProtectionPolicy.Branch.BUDDY_PROTECT,
                result.branch());
        assertTrue(result.applies());
        assertEquals(delta, result.delta(), 0.0f);
        assertFalse(result.claimSurvival());
        assertEquals(enemyThreat, result.enemyThreat());
    }

    private static void assertDoomed(
            MoveBuddyProtectionPolicy.Evaluation result,
            int ourPower,
            int theirPower) {
        assertEquals(MoveBuddyProtectionPolicy.Branch.DOOMED_ESCAPE,
                result.branch());
        assertTrue(result.applies());
        assertEquals(200.0f, result.delta(), 0.0f);
        assertTrue(result.claimSurvival());
        assertTrue(result.enemyThreat());
        assertEquals(
                "V59 DOOMED: Farm is a lost position (us " + ourPower
                        + " vs enemy " + theirPower
                        + ") — ESCAPE the valuable character!",
                result.reason());
    }
}
