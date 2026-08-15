package com.gempukku.swccgo.ai.models.common.strategy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EndorOperationsTacticalPolicyTest {
    @Test
    public void postFlipReinforcementDominatesEmptySiteSpread() {
        assertEquals(300.0f,
                EndorOperationsTacticalPolicy.postFlipPlanAdjustment(
                        true, true, true, true, false),
                0.0f);
        assertEquals(-250.0f,
                EndorOperationsTacticalPolicy.postFlipPlanAdjustment(
                        true, true, false, true, true),
                0.0f);
        assertEquals(0.0f,
                EndorOperationsTacticalPolicy.postFlipPlanAdjustment(
                        true, false, false, true, true),
                0.0f);
        assertTrue(5.0f
                + EndorOperationsTacticalPolicy.postFlipPlanAdjustment(
                    true, true, true, true, false)
                > 227.0f
                + EndorOperationsTacticalPolicy.postFlipPlanAdjustment(
                    true, true, false, true, true));
    }

    @Test
    public void bunkerPullBootstrapsEndorShieldOnly() {
        assertEquals(300.0f,
                EndorOperationsTacticalPolicy
                    .endorShieldBootstrapAdjustment(
                        "Endor: Bunker",
                        "Deploy Endor Shield from Reserve Deck"),
                0.0f);
        assertEquals(0.0f,
                EndorOperationsTacticalPolicy
                    .endorShieldBootstrapAdjustment(
                        "Endor: Bunker",
                        "Deploy a weapon from Reserve Deck"),
                0.0f);
        assertEquals(0.0f,
                EndorOperationsTacticalPolicy
                    .endorShieldBootstrapAdjustment(
                        "Endor: Landing Platform",
                        "Deploy Endor Shield from Reserve Deck"),
                0.0f);
    }

    @Test
    public void reservesAShieldSlotWhileTheEndorSpaceRouteRemainsPending() {
        assertTrue(EndorOperationsTacticalPolicy
                .shouldReserveShieldSlotForBattleOrder(
                        true, false, true, false, true));
        assertFalse(EndorOperationsTacticalPolicy
                .shouldReserveShieldSlotForBattleOrder(
                        true, false, true, false, false));
        assertFalse(EndorOperationsTacticalPolicy
                .shouldReserveShieldSlotForBattleOrder(
                        true, true, true, false, true));
    }

    @Test
    public void postFlipPursuesAnOpenEndorSystem() {
        assertTrue(EndorOperationsTacticalPolicy.shouldPursueEndorSystem(
                true, true, true, false, 0.0f));
        assertFalse(EndorOperationsTacticalPolicy.shouldPursueEndorSystem(
                true, false, true, false, 0.0f));
        assertFalse(EndorOperationsTacticalPolicy.shouldPursueEndorSystem(
                false, true, true, false, 0.0f));
        assertFalse(EndorOperationsTacticalPolicy.shouldPursueEndorSystem(
                true, true, true, true, 0.0f));
        assertFalse(EndorOperationsTacticalPolicy.shouldPursueEndorSystem(
                true, true, true, false, 1.0f));
    }

    @Test
    public void cheapAdmiralGarrisonsBunkerInsteadOfMobileBoba() {
        assertEquals(300.0f,
                EndorOperationsTacticalPolicy.bunkerGarrisonAdjustment(
                        true, false, true, true, 2, false),
                0.0f);
        assertEquals(-300.0f,
                EndorOperationsTacticalPolicy.bunkerGarrisonAdjustment(
                        true, false, true, false, 4, true),
                0.0f);
        assertEquals(0.0f,
                EndorOperationsTacticalPolicy.bunkerGarrisonAdjustment(
                        true, true, true, false, 4, true),
                0.0f);
    }

    @Test
    public void recognizesBothObjectiveFaces() {
        assertTrue(EndorOperationsTacticalPolicy.isEndorOperations(
                "8_167", "anything"));
        assertTrue(EndorOperationsTacticalPolicy.isEndorOperations(
                null, "Imperial Outpost"));
        assertFalse(EndorOperationsTacticalPolicy.isEndorOperations(
                "226_12", "You've Got To Be Kidding"));
    }
}
