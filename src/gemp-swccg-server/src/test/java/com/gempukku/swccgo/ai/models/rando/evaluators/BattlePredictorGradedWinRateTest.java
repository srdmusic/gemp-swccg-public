package com.gempukku.swccgo.ai.models.rando.evaluators;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ADDED 2026-08-08 (passivity fix, m01683): predictBattleFullIntel used to
 * return a BINARY winRate (1.0/0.5/0.0), so any projected 1-point deficit read
 * as certain defeat and tripped the V76 -800 block. It now grades by margin
 * over the maximum destiny swing, clamped to [0.05, 0.95], and Monte Carlo
 * ties count as half a win. Covers BOTH mirrors (rando + chosenone).
 */
public class BattlePredictorGradedWinRateTest {

    private static final float EPSILON = 0.0001f;

    // 1 draw each side, both averages 3, opponent fixed at 10+3.
    private static float fullIntelWinRate(int myPower) {
        return BattlePredictor.predictBattleFullIntel(
                myPower, 1, 3.0f, 10, 1, 3.0f).winProbability;
    }

    @Test
    public void fullIntelWinRateIsGradedMonotoneAndClamped() {
        float losing = fullIntelWinRate(6);     // margin -4
        float tie = fullIntelWinRate(10);       // margin 0
        float small = fullIntelWinRate(12);     // margin +2
        float solid = fullIntelWinRate(16);     // margin +6

        // maxSwing = 6 * (1 + 1) = 12; winRate = 0.5 + margin / 24.
        assertEquals(0.5f - 4.0f / 24.0f, losing, EPSILON);
        assertEquals(0.5f, tie, EPSILON);
        assertEquals(0.5f + 2.0f / 24.0f, small, EPSILON);
        assertEquals(0.75f, solid, EPSILON);

        assertTrue("must be monotone in margin",
                losing < tie && tie < small && small < solid);

        // A projected loss is no longer a flat 0.0 certain defeat.
        assertTrue(losing > 0.05f);

        // Clamps: intel never claims certainty.
        assertEquals(0.95f, fullIntelWinRate(100), EPSILON);
        assertEquals(0.05f, BattlePredictor.predictBattleFullIntel(
                0, 1, 3.0f, 100, 1, 3.0f).winProbability, EPSILON);
    }

    @Test
    public void fullIntelTieIsHalfWinWithNoDamage() {
        BattlePredictor.BattleOutcome outcome =
                BattlePredictor.predictBattleFullIntel(
                        10, 1, 3.0f, 10, 1, 3.0f);
        assertEquals(0.5f, outcome.winProbability, EPSILON);
        assertEquals(0.0f, outcome.expectedDamageDealt, EPSILON);
        assertEquals(0.0f, outcome.expectedDamageTaken, EPSILON);
    }

    @Test
    public void monteCarloTiesCountAsHalfWin() {
        // Zero draws on both sides makes every simulation deterministic.
        assertEquals(0.5f, BattlePredictor.predictBattle(
                5, 0, 5, 0).winProbability, EPSILON);
        assertEquals(1.0f, BattlePredictor.predictBattle(
                10, 0, 5, 0).winProbability, EPSILON);
        assertEquals(0.0f, BattlePredictor.predictBattle(
                5, 0, 10, 0).winProbability, EPSILON);
    }

    @Test
    public void chosenOneMirrorGradesIdentically() {
        for (int myPower : new int[] {0, 6, 10, 12, 16, 100}) {
            assertEquals(
                    BattlePredictor.predictBattleFullIntel(
                            myPower, 1, 3.0f, 10, 1, 3.0f).winProbability,
                    com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .BattlePredictor.predictBattleFullIntel(
                                    myPower, 1, 3.0f, 10, 1, 3.0f)
                            .winProbability,
                    EPSILON);
        }
        assertEquals(0.5f,
                com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .BattlePredictor.predictBattle(5, 0, 5, 0)
                        .winProbability,
                EPSILON);
    }
}
