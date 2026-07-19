package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoveWinnabilityPolicyTest {
    @Test
    public void destinationMatchingPreservesCaseInsensitiveSubstring() {
        assertTrue(MoveWinnabilityPolicy.actionTargetsLocation(
                "move vader to cloud city: carbonite chamber",
                "Cloud City: Carbonite Chamber"));
        assertFalse(MoveWinnabilityPolicy.actionTargetsLocation(
                "move vader to cloud city: carbonite chamber",
                "Cloud City: Downtown Plaza"));
    }

    @Test
    public void winnableContestedMoveProducesNoContributionOrVeto() {
        assertNone(MoveWinnabilityPolicy.contested(
                "Vader", "Carbonite Chamber",
                10.0f, 5.0f, 9.0f, true));
    }

    @Test
    public void unwinnableMoveBelowSixPowerGapGetsBasePenalty() {
        MoveWinnabilityPolicy.Evaluation result =
                MoveWinnabilityPolicy.contested(
                        "Vader", "Carbonite Chamber",
                        10.0f, 5.0f, 15.9f, false);

        assertUnwinnable(result, -800.0f);
    }

    @Test
    public void exactSixPowerGapGetsSeverePenalty() {
        MoveWinnabilityPolicy.Evaluation result =
                MoveWinnabilityPolicy.contested(
                        "Vader", "Carbonite Chamber",
                        10.0f, 5.0f, 16.0f, false);

        assertUnwinnable(result, -1500.0f);
        assertEquals(
                "V137 UNWINNABLE MOVE: Vader → Carbonite Chamber contested"
                        + " — even the full group (10 pwr/5 abil) loses to opp 16 pwr"
                        + " (shared canWinAt false)",
                result.vetoReason());
        assertEquals(
                "V137 UNWINNABLE MOVE: Vader → Carbonite Chamber contested"
                        + " — even the full group (10 pwr/5 abil) loses to opp 16 pwr;"
                        + " don't waste move force",
                result.reason());
    }

    @Test
    public void nonBattlegroundNeverGetsAntiSoloPenalty() {
        assertNone(MoveWinnabilityPolicy.uncontestedBattleground(
                "Asajj", "Guest Quarters", false, 0, 1));
    }

    @Test
    public void projectedPairAvoidsAntiSoloPenalty() {
        assertNone(MoveWinnabilityPolicy.uncontestedBattleground(
                "Asajj", "Beldon's Eye", true, 1, 1));
    }

    @Test
    public void projectedSoloGetsExactBattlegroundPenalty() {
        MoveWinnabilityPolicy.Evaluation result =
                MoveWinnabilityPolicy.uncontestedBattleground(
                        "Asajj", "Beldon's Eye", true, 0, 1);

        assertEquals(
                MoveWinnabilityPolicy.Branch.ANTI_SOLO_BATTLEGROUND,
                result.branch());
        assertTrue(result.applies());
        assertFalse(result.canWinVeto());
        assertNull(result.vetoReason());
        assertEquals(-500.0f, result.delta(), 0.0f);
        assertEquals(1, result.projectedCharactersAtDestination());
        assertEquals(
                "V137 ANTI-SOLO BG: Asajj → Beldon's Eye would be SOLO"
                        + " at a battleground (uncontested now, opp can reinforce/attack"
                        + " next turn) — don't park alone",
                result.reason());
    }

    private static void assertUnwinnable(
            MoveWinnabilityPolicy.Evaluation result,
            float delta) {
        assertEquals(MoveWinnabilityPolicy.Branch.UNWINNABLE,
                result.branch());
        assertTrue(result.applies());
        assertTrue(result.canWinVeto());
        assertEquals(delta, result.delta(), 0.0f);
        assertEquals(0, result.projectedCharactersAtDestination());
    }

    private static void assertNone(
            MoveWinnabilityPolicy.Evaluation result) {
        assertEquals(MoveWinnabilityPolicy.Branch.NONE, result.branch());
        assertFalse(result.applies());
        assertFalse(result.canWinVeto());
        assertNull(result.vetoReason());
        assertNull(result.reason());
        assertEquals(0.0f, result.delta(), 0.0f);
        assertEquals(0, result.projectedCharactersAtDestination());
    }
}
