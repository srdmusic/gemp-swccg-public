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
                1500.0f,
                "V79 DEATH STAR ORBIT SCARIF: arrive at Scarif — must take this!");
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
                1000.0f,
                "V79 DEATH STAR → parsec 6 (1 hop from Scarif at 7)");
        assertEquals(Integer.valueOf(6), result.destinationParsec());
    }

    @Test
    public void parsecSevenGetsArrivalSetupScore() {
        MoveVergePolicy.Evaluation result =
                MoveVergePolicy.evaluate(true, false, false, "to parsec 7");

        assertContribution(result, MoveVergePolicy.Branch.PARSEC_SEVEN,
                1200.0f,
                "V79 DEATH STAR → parsec 7 (Scarif's parsec) — take orbit option next!");
    }

    @Test
    public void parsecEightIsOneHop() {
        MoveVergePolicy.Evaluation result =
                MoveVergePolicy.evaluate(true, false, false, "to parsec 8");

        assertContribution(result,
                MoveVergePolicy.Branch.ONE_HOP_FROM_SCARIF,
                1000.0f,
                "V79 DEATH STAR → parsec 8 (1 hop from Scarif at 7)");
    }

    @Test
    public void higherParsecsOutsideOneHopUseTowardScore() {
        for (int parsec : new int[]{5, 9, 12}) {
            MoveVergePolicy.Evaluation result = MoveVergePolicy.evaluate(
                    true, false, false, "to parsec " + parsec);
            assertEquals(MoveVergePolicy.Branch.TOWARD_SCARIF,
                    result.branch());
            assertEquals(700.0f, result.contribution().delta(), 0.0f);
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
                    500.0f,
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
    public void preFlipOrbitHoldsWithoutScoreOrVeto() {
        MoveVergePolicy.Evaluation result = MoveVergePolicy.evaluate(
                true, true, false, "Move using hyperspeed");

        assertEquals(MoveVergePolicy.Branch.PRE_FLIP_HOLD,
                result.branch());
        assertFalse(result.contribution().applies());
        assertFalse(result.hardVeto());
    }

    @Test
    public void postFlipOrbitProducesExactHardVeto() {
        MoveVergePolicy.Evaluation result = MoveVergePolicy.evaluate(
                true, true, true, "Orbit Scarif");

        assertEquals(MoveVergePolicy.Branch.POST_FLIP_HOLD,
                result.branch());
        assertFalse(result.contribution().applies());
        assertTrue(result.hardVeto());
        assertEquals(
                "V79b FLIP-BACK GUARD: objective flipped + Death Star orbiting Scarif"
                        + " — leaving orbit un-satisfies 'Death Star orbiting Scarif'; stay parked",
                result.hardVetoReason());
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
}
