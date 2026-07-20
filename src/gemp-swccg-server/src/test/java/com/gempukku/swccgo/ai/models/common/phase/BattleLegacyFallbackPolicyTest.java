package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BattleLegacyFallbackPolicyTest {

    @Test
    public void locationBandsPreserveExactBoundariesAndAdditiveOrder() {
        assertEquals(80, location(4, false, false));
        assertEquals(100, location(8, false, false));
        assertEquals(135, location(8, true, true));

        assertEquals(20, location(3, true, false));
        assertEquals(45, location(-5, true, true));

        assertEquals(-60, location(-6, true, false));
        assertEquals(-35, location(-7, true, true));
    }

    @Test
    public void boardFallbackKeepsFavorableNeutralAndDangerBands() {
        assertEquals(80, board(4));
        assertEquals(80, board(12));
        assertEquals(0, board(3));
        assertEquals(0, board(-5));
        assertEquals(-60, board(-6));
    }

    @Test
    public void adapterSuppliedScoresAndThresholdsRemainExplicitInputs() {
        assertEquals(101, BattleActionTextPolicy.scoreLegacyFallbackLocation(
                91, 5, -3, 5, true, false));
        assertEquals(91, BattleActionTextPolicy.scoreLegacyFallbackBoard(
                91, 5, -3, 5));
        assertEquals(-60, BattleActionTextPolicy.scoreLegacyFallbackBoard(
                91, 5, -3, -3));
    }

    @Test
    public void legacyWeaponFireScoreRemainsExact() {
        assertEquals(50, BattleWeaponsPolicy.scoreLegacyFallbackFireWeapon());
    }

    private static int location(float powerAdvantage,
                                boolean battleground,
                                boolean contestedWinning) {
        return BattleActionTextPolicy.scoreLegacyFallbackLocation(
                80, 4, -6, powerAdvantage, battleground,
                contestedWinning);
    }

    private static int board(float boardAdvantage) {
        return BattleActionTextPolicy.scoreLegacyFallbackBoard(
                80, 4, -6, boardAdvantage);
    }
}
