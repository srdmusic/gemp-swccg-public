package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LateEstablishPolicyTest {

    @Test
    public void turnFourKeepsLegacyGroundGuardAndCap() {
        assertFalse(LateEstablishPolicy.allowsWeakSolo(
                facts(4, 19, true, true, true, 4.0f)));
        assertEquals(2, LateEstablishPolicy.groundEstablishLimit(4, 19));
    }

    @Test
    public void turnFiveBelowTwentyReleasesExactAbilityFourGroundSolo() {
        assertTrue(LateEstablishPolicy.allowsWeakSolo(
                facts(5, 19, true, true, true, 4.0f)));
        assertEquals(3, LateEstablishPolicy.groundEstablishLimit(5, 19));
    }

    @Test
    public void opponentLostPileTwentyKeepsLegacyGroundGuardAndCap() {
        assertFalse(LateEstablishPolicy.allowsWeakSolo(
                facts(5, 20, true, true, true, 4.0f)));
        assertEquals(2, LateEstablishPolicy.groundEstablishLimit(5, 20));
    }

    @Test
    public void releaseRequiresEmptyGroundExactLegalityAndAbilityFour() {
        assertFalse(LateEstablishPolicy.allowsWeakSolo(
                facts(5, 19, false, true, true, 4.0f)));
        assertFalse(LateEstablishPolicy.allowsWeakSolo(
                facts(5, 19, true, false, true, 4.0f)));
        assertFalse(LateEstablishPolicy.allowsWeakSolo(
                facts(5, 19, true, true, false, 4.0f)));
        assertFalse(LateEstablishPolicy.allowsWeakSolo(
                facts(5, 19, true, true, true, Math.nextDown(4.0f))));
    }

    @Test
    public void spaceNeverReceivesGroundSpreadRelease() {
        assertFalse(LateEstablishPolicy.allowsWeakSolo(
                facts(5, 19, true, true, true, 4.0f, false)));
    }

    @Test
    public void unknownOpponentLostPileFailsClosed() {
        assertFalse(LateEstablishPolicy.allowsWeakSolo(
                facts(5, -1, true, true, true, 4.0f)));
        assertEquals(2, LateEstablishPolicy.groundEstablishLimit(5, -1));
    }

    private static LateEstablishPolicy.CandidateFacts facts(
            int turn, int opponentLostPile, boolean empty,
            boolean engineDeployable, boolean affordable,
            float projectedAbility) {
        return facts(turn, opponentLostPile, empty, engineDeployable,
                affordable, projectedAbility, true);
    }

    private static LateEstablishPolicy.CandidateFacts facts(
            int turn, int opponentLostPile, boolean empty,
            boolean engineDeployable, boolean affordable,
            float projectedAbility, boolean ground) {
        return new LateEstablishPolicy.CandidateFacts(
                turn, opponentLostPile, ground, empty,
                engineDeployable, affordable, projectedAbility);
    }
}
