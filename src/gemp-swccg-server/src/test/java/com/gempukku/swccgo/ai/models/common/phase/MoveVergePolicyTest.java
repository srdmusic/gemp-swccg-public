package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoveVergePolicyTest {
    @Test
    public void noVergeProducesNoResult() {
        MoveVergePolicy.Evaluation result =
                MoveVergePolicy.evaluate(false, false, false,
                        "Move Death Star to parsec 7");

        assertEquals(MoveVergePolicy.Branch.NONE, result.branch());
        assertFalse(result.contribution().applies());
        assertFalse(result.hardVeto());
    }

    @Test
    public void orbitScarifHasPriorityOverParsecText() {
        MoveVergePolicy.Evaluation result =
                MoveVergePolicy.evaluate(true, false, false,
                        "Move from parsec 4 to ORBIT Scarif at parsec 7");

        assertContribution(result, MoveVergePolicy.Branch.ORBIT_SCARIF,
                300.0f,
                "V79 DEATH STAR ORBIT SCARIF: prefer arriving at Scarif (+300 objective preference)");
        assertEquals(
                "move from parsec 4 to orbit scarif at parsec 7",
                result.actionLower());
    }

    @Test
    public void lastParsecMatchIsTheDestination() {
        MoveVergePolicy.Evaluation result =
                MoveVergePolicy.evaluate(true, false, false,
                        "Move Death Star at parsec 4 to parsec 6");

        assertContribution(result,
                MoveVergePolicy.Branch.ONE_HOP_FROM_SCARIF,
                300.0f,
                "V79 DEATH STAR → parsec 6 (1 hop from Scarif at 7)");
        assertEquals(Integer.valueOf(6), result.destinationParsec());
    }

    @Test
    public void parsecSevenGetsArrivalSetupScore() {
        MoveVergePolicy.Evaluation result =
                MoveVergePolicy.evaluate(true, false, false, "to parsec 7");

        assertContribution(result, MoveVergePolicy.Branch.PARSEC_SEVEN,
                300.0f,
                "V79 DEATH STAR → parsec 7 (Scarif's parsec) — take orbit option next!");
    }

    @Test
    public void parsecEightIsOneHop() {
        MoveVergePolicy.Evaluation result =
                MoveVergePolicy.evaluate(true, false, false, "to parsec 8");

        assertContribution(result,
                MoveVergePolicy.Branch.ONE_HOP_FROM_SCARIF,
                300.0f,
                "V79 DEATH STAR → parsec 8 (1 hop from Scarif at 7)");
    }

    @Test
    public void higherParsecsOutsideOneHopUseTowardScore() {
        for (int parsec : new int[]{5, 9, 12}) {
            MoveVergePolicy.Evaluation result = MoveVergePolicy.evaluate(
                    true, false, false, "to parsec " + parsec);
            assertEquals(MoveVergePolicy.Branch.TOWARD_SCARIF,
                    result.branch());
            assertEquals(300.0f, result.contribution().delta(), 0.0f);
            assertEquals(Integer.valueOf(parsec),
                    result.destinationParsec());
            assertEquals(
                    "V79 DEATH STAR → parsec " + parsec
                            + " (toward Scarif)",
                    result.contribution().reason());
        }
    }

    @Test
    public void parsecsZeroThroughFourUseWrongDirectionScore() {
        for (int parsec = 0; parsec <= 4; parsec++) {
            MoveVergePolicy.Evaluation result = MoveVergePolicy.evaluate(
                    true, false, false, "to parsec " + parsec);
            assertEquals(MoveVergePolicy.Branch.WRONG_DIRECTION,
                    result.branch());
            assertEquals(-300.0f, result.contribution().delta(), 0.0f);
            assertEquals(Integer.valueOf(parsec),
                    result.destinationParsec());
            assertEquals(
                    "V79 DEATH STAR → parsec " + parsec
                            + " — WRONG DIRECTION (Scarif is at 7)",
                    result.contribution().reason());
        }
    }

    @Test
    public void noParsecUsesDefaultMoveScore() {
        for (String displayText : new String[]{null, "Move using hyperspeed"}) {
            MoveVergePolicy.Evaluation result = MoveVergePolicy.evaluate(
                    true, false, false, displayText);
            assertContribution(result, MoveVergePolicy.Branch.DEFAULT_MOVE,
                    300.0f,
                    "V79 DEATH STAR MOVE: Verge active, default move");
            assertNull(result.destinationParsec());
        }
    }

    @Test
    public void failedLaterParseKeepsLastSuccessfulDestination() {
        MoveVergePolicy.Evaluation result = MoveVergePolicy.evaluate(
                true, false, false,
                "from parsec 4 to parsec 6 then parsec 999999999999999999999");

        assertEquals(MoveVergePolicy.Branch.ONE_HOP_FROM_SCARIF,
                result.branch());
        assertEquals(Integer.valueOf(6), result.destinationParsec());
    }

    @Test
    public void preFlipOrbitHoldIsABoundedPreference() {
        MoveVergePolicy.Evaluation result = MoveVergePolicy.evaluate(
                true, true, false, "Move using hyperspeed");

        assertEquals(MoveVergePolicy.Branch.PRE_FLIP_HOLD,
                result.branch());
        assertTrue(result.contribution().applies());
        assertEquals(-300.0f, result.contribution().delta(), 0.0f);
        assertFalse(result.hardVeto());
        assertNull(result.hardVetoReason());
    }

    @Test
    public void postFlipReleasesEverywhereBecauseTheBackRequiresALeaderNotOrbit() {
        for (boolean atScarif : new boolean[]{false, true}) {
            MoveVergePolicy.Evaluation result = MoveVergePolicy.evaluate(
                    true, atScarif, true, "Move toward Scarif");

            assertEquals(MoveVergePolicy.Branch.POST_FLIP_RELEASE,
                    result.branch());
            assertFalse(result.contribution().applies());
            assertFalse(result.hardVeto());
            assertNull(result.hardVetoReason());
        }
    }

    @Test
    public void nullParsecChoiceProducesNoContribution() {
        MoveVergePolicy.ParsecChoiceEvaluation result =
                MoveVergePolicy.evaluateParsecChoice(null);

        assertEquals(MoveVergePolicy.ParsecChoiceBranch.NONE,
                result.branch());
        assertFalse(result.contribution().applies());
        assertNull(result.parsec());
        assertNull(result.distanceFromScarif());
    }

    @Test
    public void parsecChoicePreservesExactV79Boundaries() {
        assertParsecChoice(
                MoveVergePolicy.evaluateParsecChoice(7),
                MoveVergePolicy.ParsecChoiceBranch.PARSEC_SEVEN,
                7, 0, 300.0f,
                "V79 PARSEC 7 (Scarif!) — pick this");

        for (int parsec : new int[]{6, 8}) {
            assertParsecChoice(
                    MoveVergePolicy.evaluateParsecChoice(parsec),
                    MoveVergePolicy.ParsecChoiceBranch.ONE_HOP_FROM_SCARIF,
                    parsec, 1, 300.0f,
                    "V79 PARSEC " + parsec + " (1 hop from Scarif)");
        }

        for (int parsec : new int[]{5, 9}) {
            assertParsecChoice(
                    MoveVergePolicy.evaluateParsecChoice(parsec),
                    MoveVergePolicy.ParsecChoiceBranch.TOWARD_SCARIF,
                    parsec, Math.abs(parsec - 7), 300.0f,
                    "V79 PARSEC " + parsec + " (toward Scarif)");
        }

        for (int parsec : new int[]{0, 4}) {
            assertParsecChoice(
                    MoveVergePolicy.evaluateParsecChoice(parsec),
                    MoveVergePolicy.ParsecChoiceBranch.WRONG_DIRECTION,
                    parsec, Math.abs(parsec - 7), -300.0f,
                    "V79 PARSEC " + parsec + " — WRONG DIRECTION");
        }
    }

    @Test
    public void destinationChoicePreservesExactV79ScoresAndReasons() {
        assertParsecChoice(
                MoveVergePolicy.evaluateDestinationChoice(true),
                MoveVergePolicy.ParsecChoiceBranch.ORBIT_SCARIF,
                null, null, 300.0f,
                "V79 ORBIT SCARIF: preferred objective destination (+300)");
        assertParsecChoice(
                MoveVergePolicy.evaluateDestinationChoice(false),
                MoveVergePolicy.ParsecChoiceBranch.OTHER_DESTINATION,
                null, null, -200.0f,
                "V79 destination not Scarif — avoid");
    }

    @Test
    public void fallbackChoicePreservesV103FloorAndReasonFormatting() {
        assertParsecChoice(
                MoveVergePolicy.evaluateParsecFallback(7),
                MoveVergePolicy.ParsecChoiceBranch.FALLBACK_PARSEC,
                7, 0, 300.0f,
                "V103 PARSEC FALLBACK: parsec 7 (dist 0 to Scarif) → +300");
        assertParsecChoice(
                MoveVergePolicy.evaluateParsecFallback(2),
                MoveVergePolicy.ParsecChoiceBranch.FALLBACK_PARSEC,
                2, 5, 50.0f,
                "V103 PARSEC FALLBACK: parsec 2 (dist 5 to Scarif) → +50");
        assertParsecChoice(
                MoveVergePolicy.evaluateParsecFallback(1),
                MoveVergePolicy.ParsecChoiceBranch.FALLBACK_PARSEC,
                1, 6, 0.0f,
                "V103 PARSEC FALLBACK: parsec 1 (dist 6 to Scarif) → +0");

        MoveVergePolicy.ParsecChoiceEvaluation none =
                MoveVergePolicy.evaluateParsecFallback(null);
        assertEquals(MoveVergePolicy.ParsecChoiceBranch.NONE,
                none.branch());
        assertFalse(none.contribution().applies());
    }

    private static void assertContribution(
            MoveVergePolicy.Evaluation result,
            MoveVergePolicy.Branch branch,
            float delta,
            String reason) {
        assertEquals(branch, result.branch());
        assertTrue(result.contribution().applies());
        assertEquals(delta, result.contribution().delta(), 0.0f);
        assertEquals(reason, result.contribution().reason());
        assertFalse(result.hardVeto());
    }

    private static void assertParsecChoice(
            MoveVergePolicy.ParsecChoiceEvaluation result,
            MoveVergePolicy.ParsecChoiceBranch branch,
            Integer parsec,
            Integer distanceFromScarif,
            float delta,
            String reason) {
        assertEquals(branch, result.branch());
        assertTrue(result.contribution().applies());
        assertEquals(parsec, result.parsec());
        assertEquals(distanceFromScarif, result.distanceFromScarif());
        assertEquals(delta, result.contribution().delta(), 0.0f);
        assertEquals(reason, result.contribution().reason());
    }
}
