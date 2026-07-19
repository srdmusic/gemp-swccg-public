package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoveAbilityPolicyTest {
    @Test
    public void destinyDangerUsesExactRemainingAbilityBoundaries() {
        assertAnalysis(
                MoveAbilityPolicy.analyze(2, 7.0f, 4.0f),
                MoveAbilityPolicy.Branch.DESTINY_DANGER,
                3.0f);
        assertAnalysis(
                MoveAbilityPolicy.analyze(2, 8.0f, 4.0f),
                MoveAbilityPolicy.Branch.NONE,
                4.0f);
        assertAnalysis(
                MoveAbilityPolicy.analyze(2, 4.0f, 4.0f),
                MoveAbilityPolicy.Branch.NONE,
                0.0f);
        assertAnalysis(
                MoveAbilityPolicy.analyze(1, 3.0f, 3.0f),
                MoveAbilityPolicy.Branch.SOLO_ESCAPE,
                0.0f);
    }

    @Test
    public void soloEscapeStopsAtFourAbilityAndRequiresOneCharacter() {
        assertEquals(MoveAbilityPolicy.Branch.SOLO_ESCAPE,
                MoveAbilityPolicy.analyze(1, 3.9f, 3.9f).branch());
        assertEquals(MoveAbilityPolicy.Branch.NONE,
                MoveAbilityPolicy.analyze(1, 4.0f, 4.0f).branch());
        assertEquals(MoveAbilityPolicy.Branch.NONE,
                MoveAbilityPolicy.analyze(0, 3.0f, 0.0f).branch());
    }

    @Test
    public void destinyDangerWithoutEnemyGetsBasePenalty() {
        MoveAbilityPolicy.Evaluation result =
                MoveAbilityPolicy.destinyDanger(
                        "Emperor Palpatine",
                        "Cloud City: Downtown Plaza",
                        7.0f,
                        3.0f,
                        0.0f);

        assertEvaluation(
                result,
                MoveAbilityPolicy.Branch.DESTINY_DANGER,
                -300.0f,
                false);
        assertEquals(
                "V32 ABILITY DANGER: Moving Emperor Palpatine away drops"
                        + " ability from 7 to 3 (< 4) at Cloud City: Downtown Plaza!"
                        + " NO BATTLE DESTINY!",
                result.reason());
    }

    @Test
    public void destinyDangerWithEnemyGetsDisasterPenaltyAndSuffix() {
        MoveAbilityPolicy.Evaluation result =
                MoveAbilityPolicy.destinyDanger(
                        "Emperor Palpatine",
                        "Cloud City: Downtown Plaza",
                        7.0f,
                        3.0f,
                        8.9f);

        assertEvaluation(
                result,
                MoveAbilityPolicy.Branch.DESTINY_DANGER,
                -500.0f,
                false);
        assertTrue(result.reason().endsWith(" ENEMY POWER=8"));
    }

    @Test
    public void soloEscapeProducesExactContribution() {
        MoveAbilityPolicy.Evaluation result =
                MoveAbilityPolicy.soloEscape("Fel", 3.0f);

        assertEvaluation(
                result,
                MoveAbilityPolicy.Branch.SOLO_ESCAPE,
                50.0f,
                false);
        assertEquals(
                "V32 ABILITY SOLO ESCAPE: Fel alone with ability 3 < 4"
                        + " — move to join allies!",
                result.reason());
    }

    @Test
    public void uncontestedGateUsesExactZeroEquality() {
        assertTrue(MoveAbilityPolicy.isUncontested(0.0f));
        assertTrue(MoveAbilityPolicy.isUncontested(-0.0f));
        assertFalse(MoveAbilityPolicy.isUncontested(0.1f));
        assertFalse(MoveAbilityPolicy.isUncontested(-0.1f));
        assertFalse(MoveAbilityPolicy.isUncontested(Float.NaN));
    }

    @Test
    public void joinGateExcludesUndercoverAndReadyObjectiveWork() {
        assertTrue(MoveAbilityPolicy.canJoinGroup(false, false));
        assertFalse(MoveAbilityPolicy.canJoinGroup(true, false));
        assertFalse(MoveAbilityPolicy.canJoinGroup(false, true));
        assertFalse(MoveAbilityPolicy.canJoinGroup(true, true));
    }

    @Test
    public void joinGroupProducesExactR2Contribution() {
        MoveAbilityPolicy.Evaluation result =
                MoveAbilityPolicy.joinGroup(
                        "Fel",
                        3.0f,
                        "Beach",
                        "Forest",
                        5.0f);

        assertEvaluation(
                result,
                MoveAbilityPolicy.Branch.JOIN_GROUP,
                250.0f,
                true);
        assertEquals(
                "V156 JOIN-GROUP: Fel (ability 3) solo at uncontested Beach"
                        + " — join Forest (stack reaches ability 5)!",
                result.reason());
    }

    private static void assertAnalysis(
            MoveAbilityPolicy.Analysis result,
            MoveAbilityPolicy.Branch branch,
            float abilityAfterMove) {
        assertEquals(branch, result.branch());
        assertEquals(abilityAfterMove, result.abilityAfterMove(), 0.0f);
    }

    private static void assertEvaluation(
            MoveAbilityPolicy.Evaluation result,
            MoveAbilityPolicy.Branch branch,
            float delta,
            boolean claimDoctrine) {
        assertEquals(branch, result.branch());
        assertTrue(result.applies());
        assertEquals(delta, result.delta(), 0.0f);
        assertEquals(claimDoctrine, result.claimDoctrine());
    }
}
