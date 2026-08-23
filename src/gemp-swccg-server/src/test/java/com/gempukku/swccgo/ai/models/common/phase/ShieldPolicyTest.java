package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.ShieldFacts;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ShieldPolicyTest {

    @Test
    public void stackedPileSourcesAndShieldMajorityKeepLegacyRouting() {
        assertTrue(ShieldPolicy.isStackedPileShieldSource("Knowledge And Defense (V)"));
        assertTrue(ShieldPolicy.isStackedPileShieldSource("ANGER, FEAR, AGGRESSION"));
        assertFalse(ShieldPolicy.isStackedPileShieldSource("A Tragedy Has Occurred"));
        assertFalse(ShieldPolicy.isStackedPileShieldSource(null));

        assertFalse(ShieldPolicy.isShieldSelection(0, 0));
        assertTrue(ShieldPolicy.isShieldSelection(1, 2));
        assertFalse(ShieldPolicy.isShieldSelection(1, 3));
        assertTrue(ShieldPolicy.isShieldSelection(2, 3));
    }

    @Test
    public void validatedTopLevelPlayCardRequiresOneFullyAlignedSource() {
        String[] actionIds = {"deploy", "shield", "pass"};
        String[] actionTexts = {"Deploy", "Play a card", "Pass"};
        String[] sourceCardIds = {"7", "8", "9"};

        assertEquals("8", ShieldPolicy.selectedTopLevelPlayCardSourceId(
                "ACTION_CHOICE", actionIds, actionTexts, sourceCardIds, "shield"));
        assertEquals("8", ShieldPolicy.selectedTopLevelPlayCardSourceId(
                "CARD_ACTION_CHOICE", actionIds, actionTexts, sourceCardIds, "shield"));
        assertNull(ShieldPolicy.selectedTopLevelPlayCardSourceId(
                "CARD_SELECTION", actionIds, actionTexts, sourceCardIds, "shield"));
        assertNull(ShieldPolicy.selectedTopLevelPlayCardSourceId(
                "ACTION_CHOICE", actionIds, new String[] {"Play a card"},
                sourceCardIds, "shield"));
        assertNull(ShieldPolicy.selectedTopLevelPlayCardSourceId(
                "ACTION_CHOICE", new String[] {"shield", "shield"},
                new String[] {"Play a card", "Play a card"},
                new String[] {"8", "9"}, "shield"));
        assertNull(ShieldPolicy.selectedTopLevelPlayCardSourceId(
                "ACTION_CHOICE", actionIds, actionTexts, sourceCardIds, "deploy"));
    }

    @Test
    public void fourthSlotDefaultsClosedAndRequiresThePreferredMenuCard() {
        ShieldPolicy.FourthSlotPick closed = ShieldPolicy.fourthSlotPick(
                Side.DARK, facts(false, false, 2, false, 1, false, false), title -> true);
        assertNull(closed.preferred());
        assertFalse(closed.pursue());
        assertEquals(ShieldPolicy.FourthSlotTrigger.CLOSED, closed.trigger());

        ShieldPolicy.FourthSlotPick absent = ShieldPolicy.fourthSlotPick(
                Side.DARK, facts(true, true, 2, true, 3, true, true), title -> false);
        assertEquals("Battle Order", absent.preferred());
        assertFalse(absent.pursue());
        assertEquals(ShieldPolicy.FourthSlotTrigger.BATTLE_ORDER_PLAN, absent.trigger());

        ShieldPolicy.FourthSlotPick offered = ShieldPolicy.fourthSlotPick(
                Side.LIGHT, facts(true, true, 2, true, 3, true, true), title -> true);
        assertEquals("Battle Plan", offered.preferred());
        assertTrue(offered.pursue());
    }

    @Test
    public void nonBattlegroundThreatPreemptsBattlePlanAndNoLongerRequiresDrainBonus() {
        ShieldFacts.FourthSlotFacts allTriggers =
                facts(true, true, 0, false, 3, true, true);
        assertPick(Side.DARK, allTriggers, "Come Here You Big Coward",
                ShieldPolicy.FourthSlotTrigger.NON_BATTLEGROUND_DRAIN);
        assertPick(Side.LIGHT, allTriggers, "Simple Tricks And Nonsense",
                ShieldPolicy.FourthSlotTrigger.NON_BATTLEGROUND_DRAIN);

        ShieldFacts.FourthSlotFacts battlePlanAndDrainCap =
                facts(true, true, 0, true, 3, true, false);
        assertPick(Side.DARK, battlePlanAndDrainCap, "Battle Order",
                ShieldPolicy.FourthSlotTrigger.BATTLE_ORDER_PLAN);
        assertPick(Side.LIGHT, battlePlanAndDrainCap, "Battle Plan",
                ShieldPolicy.FourthSlotTrigger.BATTLE_ORDER_PLAN);

        ShieldFacts.FourthSlotFacts drainCap =
                facts(false, true, 0, true, 3, true, false);
        assertPick(Side.DARK, drainCap, "Resistance",
                ShieldPolicy.FourthSlotTrigger.DRAIN_CAP);
        assertPick(Side.LIGHT, drainCap, "Ultimatum",
                ShieldPolicy.FourthSlotTrigger.DRAIN_CAP);

        ShieldFacts.FourthSlotFacts nonBattlegroundDrain =
                facts(false, true, 1, false, 1, false, true);
        assertPick(Side.DARK, nonBattlegroundDrain, "Come Here You Big Coward",
                ShieldPolicy.FourthSlotTrigger.NON_BATTLEGROUND_DRAIN);
        assertPick(Side.LIGHT, nonBattlegroundDrain, "Simple Tricks And Nonsense",
                ShieldPolicy.FourthSlotTrigger.NON_BATTLEGROUND_DRAIN);

        ShieldFacts.FourthSlotFacts redundantBattleOrder =
                new ShieldFacts.FourthSlotFacts(
                        true, true, 0, true, 3,
                        true, false, true);
        assertPick(Side.DARK, redundantBattleOrder, "Resistance",
                ShieldPolicy.FourthSlotTrigger.DRAIN_CAP);
    }

    @Test
    public void nonBattlegroundShieldRequiresItsPrintedOccupationWindow() {
        ShieldPolicy.FourthSlotPick noOwnBattleground =
                ShieldPolicy.fourthSlotPick(
                        Side.LIGHT,
                        facts(false, false, 0, false, 0, false, true),
                        false, title -> true);
        assertEquals(ShieldPolicy.FourthSlotTrigger.CLOSED,
                noOwnBattleground.trigger());

        ShieldPolicy.FourthSlotPick opponentAtTwoBattlegrounds =
                ShieldPolicy.fourthSlotPick(
                        Side.LIGHT,
                        facts(false, true, 2, false, 1, false, true),
                        false, title -> true);
        assertEquals(ShieldPolicy.FourthSlotTrigger.CLOSED,
                opponentAtTwoBattlegrounds.trigger());
    }

    @Test
    public void historicalNonBattlegroundDrainOpensTheSameShieldRoute() {
        ShieldFacts.FourthSlotFacts noCurrentDrain =
                facts(false, true, 1, false, 1, false, false);

        ShieldPolicy.FourthSlotPick historical =
                ShieldPolicy.fourthSlotPick(
                        Side.LIGHT, noCurrentDrain, true, title -> true);

        assertEquals("Simple Tricks And Nonsense", historical.preferred());
        assertTrue(historical.pursue());
        assertEquals(ShieldPolicy.FourthSlotTrigger.NON_BATTLEGROUND_DRAIN,
                historical.trigger());
    }

    @Test
    public void noCurrentOrHistoricalNonBattlegroundThreatPreservesOldPriority() {
        ShieldFacts.FourthSlotFacts battlePlanFacts =
                facts(true, true, 1, false, 2, false, false);
        assertPick(Side.LIGHT, battlePlanFacts, "Battle Plan",
                ShieldPolicy.FourthSlotTrigger.BATTLE_ORDER_PLAN);

        ShieldPolicy.FourthSlotPick closed = ShieldPolicy.fourthSlotPick(
                Side.LIGHT,
                facts(false, true, 1, false, 1, false, false),
                false, title -> true);
        assertEquals(ShieldPolicy.FourthSlotTrigger.CLOSED, closed.trigger());
        assertFalse(closed.pursue());
    }

    @Test
    public void stackedPileParentPreservesAdditiveOrderAndExactScores() {
        PolicyResult blocked = ShieldPolicy.stackedPileParent("A", 3, false, closed(),
                true, 2, true, 1);
        assertOperations(blocked, "V124", -3000.0f, "V102", -2000.0f);
        assertEquals(
                "V124 K&D 4TH-SLOT BLOCK: 3 shields already on table, no V105/V107 trigger — don't activate K&D for 4th shield",
                blocked.operations().get(0).reason());

        PolicyResult paced = ShieldPolicy.stackedPileParent("A", 3, false,
                pick("Battle Order"), false, 0, true, 2);
        assertOperations(paced, "V29.1-stacked-pile", -40.0f);

        PolicyResult available = ShieldPolicy.stackedPileParent("A", 2, true, closed(),
                false, 0, false, 1);
        assertOperations(available, "SHIELDS-stacked-pile-available", 50.0f);

        PolicyResult thirdHeld = ShieldPolicy.stackedPileParent(
                "A", 2, false, closed(), false, 0, false, 2);
        assertOperations(thirdHeld, "V112-third-slot-reserve", -3000.0f,
                "SHIELDS-stacked-pile-available", 50.0f);
    }

    @Test
    public void eopReservesFourthShieldSlotUntilBattleOrderIsLive() {
        PolicyResult reserved = ShieldPolicy.stackedPileParent(
                "A", 3, pick("Come Here You Big Coward"),
                false, 0, false, 2, true);
        assertEquals(2, reserved.operations().size());
        assertEquals("SHIELDS-EOP-BATTLE-ORDER-RESERVE",
                reserved.operations().get(0).ruleArmId().id());
        assertBits(-300.0f, reserved.operations().get(0).delta());
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                reserved.operations().get(0).domainId());
        assertEquals("SHIELDS-stacked-pile-available",
                reserved.operations().get(1).ruleArmId().id());
        assertBits(50.0f, reserved.operations().get(1).delta());
        assertEquals(TraceDomainId.SHIELDS,
                reserved.operations().get(1).domainId());

        PolicyResult ordinary = ShieldPolicy.stackedPileParent(
                "A", 2, closed(), false, 0, false, 2, false);
        assertOperations(ordinary,
                "SHIELDS-stacked-pile-available", 50.0f);
    }

    // ==== Hoth repair #2 (2026-07-27): corrected Battle Order gate ====

    @Test
    public void correctedGateReleasesThirdSlotHoldWhenBattleOrderLive() {
        // T11: the V112 parent hold must not block the activation it is
        // reserving for once the corrected gate is live.
        PolicyResult released = ShieldPolicy.stackedPileParent(
                "A", 2, true, closed(), false, 0, false, 3);
        assertOperations(released, "SHIELDS-stacked-pile-available", 50.0f);

        PolicyResult held = ShieldPolicy.stackedPileParent(
                "A", 2, false, closed(), false, 0, false, 3);
        assertOperations(held, "V112-third-slot-reserve", -3000.0f,
                "SHIELDS-stacked-pile-available", 50.0f);
    }

    @Test
    public void urgentNonBattlegroundShieldReleasesThirdSlotParentReserve() {
        PolicyResult released = ShieldPolicy.stackedPileParent(
                "A", 2, false,
                pick("Simple Tricks And Nonsense",
                        ShieldPolicy.FourthSlotTrigger.NON_BATTLEGROUND_DRAIN),
                false, 0, false, 1);

        assertOperations(released,
                "V106-non-bg-shield-parent", 2000.0f,
                "SHIELDS-stacked-pile-available", 50.0f);
    }

    @Test
    public void thirdSlotHoldAndEopReserveNeverDoubleStack() {
        // T13: V112 and the EOP reserve are an if/else pair — exactly one
        // -3000 even when both keys are simultaneously true.
        PolicyResult both = ShieldPolicy.stackedPileParent(
                "A", 2, false, closed(), false, 0, false, 2, true);
        assertOperations(both, "V112-third-slot-reserve", -3000.0f,
                "SHIELDS-stacked-pile-available", 50.0f);
    }

    @Test
    public void unknownBattleOrderGateFollowsCorrectedLaw() {
        // T7-shaped at the policy surface: live gate passes, dead gate vetoes.
        assertTrue(ShieldPolicy.unknownBattleOrderGate("A", "Battle Order", true)
                .operations().isEmpty());
        assertOperations(ShieldPolicy.unknownBattleOrderGate(
                "A", "Battle Order", false), "V112", -9999.0f);
        assertTrue(ShieldPolicy.unknownBattleOrderGate("A", "Other Shield", false)
                .operations().isEmpty());
    }

    @Test
    public void earlyBonusRequiresTwoShieldsSoAllegationsStaysFirst() {
        // T9 boundary at the policy surface: an opened gate on turn 1 with
        // zero shields must NOT add the +200 early bonus (base 80 + ready 50
        // = 130 < Secret Plans 275 < Allegations 325). At slot 3 the reserve
        // route totals 80+50+200+2000 and dominates.
        PolicyResult turnOneOpen = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Order", 80.0f, 2, 1, 0,
                closed(), true, ShieldPolicy.CandidateRoute.RESERVE);
        assertOperations(turnOneOpen, "V51-battle-order-ready", 50.0f);

        PolicyResult slotThreeOpen = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Order", 80.0f, 2, 3, 2,
                closed(), true, ShieldPolicy.CandidateRoute.RESERVE);
        assertOperations(slotThreeOpen,
                "V112-third-slot-selection", 2000.0f,
                "V51-battle-order-ready", 50.0f,
                "V51-battle-order-early", 200.0f);
    }

    @Test
    public void pressureOnlyBattleOrderDoesNotBorrowTurnOneSelfExemption() {
        PolicyResult pressureOnly = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Order", 80.0f, 2, 1, 0,
                closed(), true, false, false,
                ShieldPolicy.CandidateRoute.RESERVE);
        assertOperations(pressureOnly,
                "V53-shield-min-turn", -5000.0f,
                "V51-battle-order-ready", 50.0f);
    }

    @Test
    public void redundancyGateStillWinsOverTheCorrectedGate() {
        // T12: an equivalent card on table vetoes before any gate scoring.
        PolicyResult redundant = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Order", 80.0f, 2, 3, 2,
                closed(), true, true, ShieldPolicy.CandidateRoute.DEDICATED);
        assertOperations(redundant,
                "SHIELD.BATTLE_ORDER_PLAN.REDUNDANT", -9999.0f);
    }

    @Test
    public void thirdSlotReserveReleasesWhenEquivalentAlreadyOnTable() {
        // Review integration (2026-07-27): when a Battle Order/Plan
        // equivalent is already on table the third-slot reserve is spent —
        // a non-Battle-Order shield at two shields must NOT take the -5000
        // hold, or slot three stalls for the rest of the game.
        PolicyResult released = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Other Shield", 80.0f, 2, 3, 2,
                closed(), false, true, ShieldPolicy.CandidateRoute.DEDICATED);
        assertTrue(released.operations().isEmpty());

        PolicyResult stillHeld = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Other Shield", 80.0f, 2, 3, 2,
                closed(), false, false, ShieldPolicy.CandidateRoute.DEDICATED);
        assertOperations(stillHeld,
                "V112-third-slot-selection", -5000.0f);
    }

    @Test
    public void defensiveWindowPreservesOpponentTurnPrecedence() {
        assertOperations(ShieldPolicy.defensiveShieldWindow("A", false, true, 1),
                "SHIELDS-opponent-turn", -10.0f);
        assertOperations(ShieldPolicy.defensiveShieldWindow("A", true, true, 1),
                "V29.1-shield-window", -40.0f);
        assertOperations(ShieldPolicy.defensiveShieldWindow("A", true, false, 1),
                "SHIELDS-window-available", 50.0f);
    }

    @Test
    public void mixedSelectionKeepsV112AndV117Boundaries() {
        assertTrue(ShieldPolicy.unknownBattleOrderGate("A", "Other Shield", false)
                .operations().isEmpty());
        assertTrue(ShieldPolicy.unknownBattleOrderGate("A", "Battle Order", true)
                .operations().isEmpty());
        assertOperations(ShieldPolicy.unknownBattleOrderGate(
                "A", "Battle Plan", false), "V112", -9999.0f);

        assertTrue(ShieldPolicy.unknownFourthSlot("A", 2, "Resistance", closed())
                .operations().isEmpty());
        assertOperations(ShieldPolicy.unknownFourthSlot(
                "A", 3, "Resistance", closed()), "V117", -9999.0f);
        assertOperations(ShieldPolicy.unknownFourthSlot(
                "A", 3, "Ultimatum", pick("Battle Plan")), "V117", -9999.0f);
        assertOperations(ShieldPolicy.unknownFourthSlot(
                "A", 3, "Battle Plan (V)", pick("Battle Plan")), "V117", 2000.0f);
    }

    @Test
    public void candidatePolicyOrdersTimingThenFourthSlotThenBattleOrder() {
        PolicyResult blocked = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Order", 80.0f, 2, 1, 3, closed(), false,
                ShieldPolicy.CandidateRoute.DEDICATED);
        assertOperations(blocked,
                "V53-shield-min-turn", -5000.0f,
                "V105-V107-selection", -5000.0f,
                "V51-battle-order-gate", -9999.0f);

        PolicyResult preferred = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Order", 80.0f, 2, 1, 3,
                pick("Battle Order"), true,
                ShieldPolicy.CandidateRoute.DEDICATED);
        assertOperations(preferred,
                "V105-V107-selection", 2000.0f,
                "V51-battle-order-early", 200.0f);

        PolicyResult rejectedBase = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Order", -50.0f, 2, 2, 2, closed(), true,
                ShieldPolicy.CandidateRoute.DEDICATED);
        // Consolidated lineage: the Shield twin live-replay repair's global
        // third-slot reserve (V112) is live law, so a non-live Battle Order
        // (score not above -50) at two shields is still held. The EOP-era
        // empty expectation predates that rule.
        assertOperations(rejectedBase,
                "V112-third-slot-selection", -5000.0f);

        PolicyResult redundant = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Order", -50.0f, 2, 2, 3,
                pick("Battle Order"), true, true,
                ShieldPolicy.CandidateRoute.DEDICATED);
        assertOperations(redundant,
                "SHIELD.BATTLE_ORDER_PLAN.REDUNDANT", -9999.0f);
        assertOperations(ShieldPolicy.battleOrderPlanRedundancyGate(
                "A", "Battle Order", true),
                "SHIELD.BATTLE_ORDER_PLAN.REDUNDANT", -9999.0f);
    }

    @Test
    public void reserveSelectionKeepsReadyBonusAndGuardedEarlyBonus() {
        assertOperations(ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Order", 80.0f, 2, 2, 2, closed(), false,
                ShieldPolicy.CandidateRoute.RESERVE),
                "V112-third-slot-selection", -5000.0f,
                "V51-battle-order-gate", -9999.0f);
        assertOperations(ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Plan", 80.0f, 2, 1, 2, closed(), true,
                ShieldPolicy.CandidateRoute.RESERVE),
                "V112-third-slot-selection", 2000.0f,
                "V51-battle-order-ready", 50.0f,
                "V51-battle-order-early", 200.0f);
        assertOperations(ShieldPolicy.shieldCandidateAdjustments(
                "A", "Battle Plan", -50.0f, 2, 1, 2, closed(), true,
                ShieldPolicy.CandidateRoute.RESERVE),
                "V112-third-slot-selection", -5000.0f,
                "V51-battle-order-ready", 50.0f);
        assertOperations(ShieldPolicy.shieldCandidateAdjustments(
                "A", "Ultimatum", 50.0f, 0, 1, 2, closed(), false,
                ShieldPolicy.CandidateRoute.RESERVE),
                "V112-third-slot-selection", -5000.0f);
    }

    @Test
    public void earlyPreferredFourthSlotCannotReviveMinimumTurnVeto() {
        PolicyResult earlyPreferred = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Come Here You Big Coward", 80.0f, 2, 1, 3,
                pick("Come Here You Big Coward"), false,
                ShieldPolicy.CandidateRoute.DEDICATED);
        assertOperations(earlyPreferred,
                "V53-shield-min-turn", -5000.0f,
                "V105-V107-selection", 2000.0f);
        assertBits(-3000.0f, sum(earlyPreferred));

        assertOperations(ShieldPolicy.shieldCandidateAdjustments(
                        "B", "Simple Tricks And Nonsense", 80.0f, 2, 1, 3,
                        pick("Simple Tricks And Nonsense"), false,
                        ShieldPolicy.CandidateRoute.RESERVE),
                "V53-shield-min-turn", -5000.0f,
                "V105-V107-selection", 2000.0f);

        assertOperations(ShieldPolicy.shieldCandidateAdjustments(
                        "A", "Resistance", 50.0f, 0, 3, 3,
                        closed(), false, ShieldPolicy.CandidateRoute.DEDICATED),
                "V105-V107-selection", -5000.0f);
        assertOperations(ShieldPolicy.shieldCandidateAdjustments(
                        "A", "Resistance", 50.0f, 0, 3, 3,
                        pick("Battle Order"), false,
                        ShieldPolicy.CandidateRoute.DEDICATED),
                "V105-V107-selection", -5000.0f);
    }

    @Test
    public void exactUrgentShieldBeatsMinimumTurnAndThirdSlotHolds() {
        ShieldPolicy.FourthSlotPick urgent = pick(
                "Simple Tricks And Nonsense",
                ShieldPolicy.FourthSlotTrigger.NON_BATTLEGROUND_DRAIN);

        PolicyResult exact = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Simple Tricks And Nonsense", 80.0f, 2, 1, 2,
                urgent, false, ShieldPolicy.CandidateRoute.DEDICATED);
        assertOperations(exact,
                "V106-non-bg-shield-selection", 2000.0f);

        PolicyResult otherShield = ShieldPolicy.shieldCandidateAdjustments(
                "B", "Other Shield", 80.0f, 0, 1, 2,
                urgent, false, ShieldPolicy.CandidateRoute.DEDICATED);
        assertOperations(otherShield,
                "V106-non-bg-shield-selection", -5000.0f);
    }

    @Test
    public void absentUrgentThreatKeepsMinimumTurnAndThirdSlotHolds() {
        PolicyResult unchanged = ShieldPolicy.shieldCandidateAdjustments(
                "A", "Simple Tricks And Nonsense", 80.0f, 2, 1, 2,
                closed(), false, ShieldPolicy.CandidateRoute.DEDICATED);

        assertOperations(unchanged,
                "V53-shield-min-turn", -5000.0f,
                "V112-third-slot-selection", -5000.0f);
    }

    @Test
    public void onePolicyResultNeverRepeatsAnActionRuleContribution() {
        List<PolicyResult> results = List.of(
                ShieldPolicy.stackedPileParent("A", 3, false, closed(), true, 2, true, 1),
                ShieldPolicy.shieldCandidateAdjustments(
                        "B", "Battle Order", 80.0f, 2, 1, 3,
                        pick("Battle Order"), true,
                        ShieldPolicy.CandidateRoute.DEDICATED),
                ShieldPolicy.shieldCandidateAdjustments(
                        "C", "Battle Plan", 80.0f, 2, 1, 2, closed(), true,
                        ShieldPolicy.CandidateRoute.RESERVE));
        for (PolicyResult result : results) {
            Set<String> contributions = new HashSet<>();
            for (PolicyOperation operation : result.operations()) {
                assertTrue(contributions.add(
                        operation.actionId() + ":" + operation.ruleArmId().id()));
                assertEquals(TraceDomainId.SHIELDS, operation.domainId());
            }
        }
    }

    private static ShieldFacts.FourthSlotFacts facts(
            boolean bothTheaters,
            boolean anyBattleground,
            int opponentBattlegrounds,
            boolean opponentHasDrainBonus,
            int ownBattlegrounds,
            boolean opponentCanDrainThree,
            boolean opponentDrainsNonBattleground) {
        return new ShieldFacts.FourthSlotFacts(bothTheaters, anyBattleground,
                opponentBattlegrounds, opponentHasDrainBonus, ownBattlegrounds,
                opponentCanDrainThree, opponentDrainsNonBattleground);
    }

    private static ShieldPolicy.FourthSlotPick closed() {
        return new ShieldPolicy.FourthSlotPick(
                null, false, ShieldPolicy.FourthSlotTrigger.CLOSED);
    }

    private static ShieldPolicy.FourthSlotPick pick(String title) {
        return new ShieldPolicy.FourthSlotPick(
                title, true, ShieldPolicy.FourthSlotTrigger.BATTLE_ORDER_PLAN);
    }

    private static ShieldPolicy.FourthSlotPick pick(
            String title, ShieldPolicy.FourthSlotTrigger trigger) {
        return new ShieldPolicy.FourthSlotPick(title, true, trigger);
    }

    private static void assertPick(Side side, ShieldFacts.FourthSlotFacts facts,
                                   String title, ShieldPolicy.FourthSlotTrigger trigger) {
        ShieldPolicy.FourthSlotPick pick =
                ShieldPolicy.fourthSlotPick(side, facts, candidate -> true);
        assertEquals(title, pick.preferred());
        assertTrue(pick.pursue());
        assertEquals(trigger, pick.trigger());
    }

    private static void assertOperations(PolicyResult result, Object... expected) {
        assertEquals(expected.length / 2, result.operations().size());
        for (int i = 0; i < result.operations().size(); i++) {
            PolicyOperation operation = result.operations().get(i);
            assertEquals(expected[i * 2], operation.ruleArmId().id());
            assertBits((Float) expected[i * 2 + 1], operation.delta());
            assertEquals(TraceDomainId.SHIELDS, operation.domainId());
            assertEquals(PolicyOperationKind.ADD, operation.kind());
            assertTrue(operation.outputKind() == TraceOutputKind.ORDERING
                    || operation.outputKind() == TraceOutputKind.VETO
                    || operation.outputKind() == TraceOutputKind.BANDED);
        }
    }

    private static float sum(PolicyResult result) {
        float total = 0.0f;
        for (PolicyOperation operation : result.operations()) {
            total += operation.delta();
        }
        return total;
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }
}
