package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShieldStrategyTest {

    @Test
    public void pacingTableAndTurnZeroBugRemainFrozenForStructuralExtraction() {
        ShieldStrategy strategy = new ShieldStrategy(Side.DARK);

        assertEquals(0, strategy.shieldsAllowedThisTurn(0));
        assertEquals(2, strategy.shieldsAllowedThisTurn(1));
        assertEquals(3, strategy.shieldsAllowedThisTurn(2));
        assertEquals(4, strategy.shieldsAllowedThisTurn(3));
        assertEquals(4, strategy.shieldsAllowedThisTurn(20));
        assertTrue(strategy.atPacingCap(0));
        assertFalse(strategy.atPacingCap(1));
    }

    @Test
    public void shieldAndActivationStateKeepTheirSeparateLiveCounters() {
        ShieldStrategy strategy = new ShieldStrategy(Side.DARK);

        strategy.recordShieldPlayed("unknown-one", "Unknown One");
        assertEquals(3, strategy.shieldsRemaining());
        assertFalse(strategy.atPacingCap(1));

        strategy.recordShieldPlayed("unknown-two", "Unknown Two");
        assertEquals(2, strategy.shieldsRemaining());
        assertTrue(strategy.atPacingCap(1));

        assertFalse(strategy.atKnDActivationCap(1));
        strategy.recordKnDActivation(1);
        strategy.recordKnDActivation(1);
        assertTrue(strategy.atKnDActivationCap(1));
        assertFalse(strategy.atKnDActivationCap(2));

        strategy.reset();
        assertEquals(4, strategy.shieldsRemaining());
        assertFalse(strategy.atKnDActivationCap(1));
    }

    @Test
    public void reservePathUnknownBlueprintCrossTalkRemainsBugForBug() {
        ShieldStrategy strategy = new ShieldStrategy(Side.DARK);

        assertBits(50.0f, strategy.scoreShield("not_a_shield", "not_a_shield", 3));
        assertBits(50.0f, strategy.scoreShield("601_67", "601_67", 3));
    }

    @Test
    public void storedMinimumTurnRemainsUnconsultedInThisStructuralPhase() {
        ShieldStrategy strategy = new ShieldStrategy(Side.DARK);

        assertBits(80.0f, strategy.scoreShield("13_54", "Battle Order", 1));
        assertBits(80.0f, strategy.scoreShield("13_54", "Battle Order", 2));
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }
}
