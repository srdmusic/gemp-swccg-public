package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PullSpecificActionPolicyTest {
    private static final String ACTION_ID = "pull-42";

    @Test
    public void wmaopAndLostPileLightsaberGatesKeepExactBlockedBranches() {
        assertEmpty(PullSpecificActionPolicy.scoreWmaopGate(
                new PullSpecificActionFacts.Gate(ACTION_ID, false, "unused")));
        assertResult(PullSpecificActionPolicy.scoreWmaopGate(
                        new PullSpecificActionFacts.Gate(ACTION_ID, true,
                                "Vader is not ready")),
                expected("V142", TraceOutputKind.VETO, -2000.0f,
                        "V142 WMAOP BLOCK: Vader is not ready — hold the interrupt"));

        assertEmpty(PullSpecificActionPolicy.scoreLostPileLightsaberGate(
                new PullSpecificActionFacts.Gate(ACTION_ID, false, "unused")));
        assertResult(PullSpecificActionPolicy.scoreLostPileLightsaberGate(
                        new PullSpecificActionFacts.Gate(ACTION_ID, true,
                                "ignored by fixed reason")),
                expected("V147", TraceOutputKind.VETO, -2000.0f,
                        "V147 IAYF: Vader's Lightsaber NOT in Lost Pile — don't waste 1 Force on failed search, use free Reserve download"));
    }

    @Test
    public void welcomeHomeOnlyVetoesTheSaveForBattleBranch() {
        assertEmpty(PullSpecificActionPolicy.scoreWelcomeHome(
                new PullSpecificActionFacts.WelcomeHome(
                        ACTION_ID, false, "Tyranus is ready")));
        assertResult(PullSpecificActionPolicy.scoreWelcomeHome(
                        new PullSpecificActionFacts.WelcomeHome(
                                ACTION_ID, true, "Tyranus is ready")),
                expected("V155-welcome-home-save", TraceOutputKind.VETO,
                        -2000.0f,
                        "V155 WELCOME HOME: Tyranus is ready — SAVE this card for battle (Tyranus ability-number mode), don't waste the pull"));
    }

    @Test
    public void youAreBeatenOnlyVetoesSearchMode() {
        assertEmpty(PullSpecificActionPolicy.scoreYouAreBeatenSearch(
                new PullSpecificActionFacts.YouAreBeatenSearch(
                        ACTION_ID, false)));
        assertResult(PullSpecificActionPolicy.scoreYouAreBeatenSearch(
                        new PullSpecificActionFacts.YouAreBeatenSearch(
                                ACTION_ID, true)),
                expected("V144-you-are-beaten-search", TraceOutputKind.VETO,
                        -2000.0f,
                        "V144 YOU ARE BEATEN: Mode 2 (IAYF search) — never use this mode, save for battle freeze or Cancel Uncontrollable Fury"));
    }

    @Test
    public void v144SplitOwnersPreserveSearchThenFreezeAndMinusFifteenHundred() {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "v144-split-owner-order");
        ledger.register(PullSpecificActionPolicy.scoreYouAreBeatenSearch(
                new PullSpecificActionFacts.YouAreBeatenSearch(
                        ACTION_ID, true)));
        ledger.register(BattleActionTextPolicy.scoreYouAreBeatenMode(
                new BattleActionTextFacts.YouAreBeatenModeFacts(
                        ACTION_ID, true, true)));

        var operations = ledger.operationsFor(ACTION_ID);
        assertEquals(2, operations.size());
        assertEquals("V144-you-are-beaten-search",
                operations.get(0).ruleArmId().id());
        assertEquals("V144-you-are-beaten-freeze",
                operations.get(1).ruleArmId().id());
        assertEquals(Float.floatToRawIntBits(-1500.0f),
                Float.floatToRawIntBits(
                        operations.get(0).delta() + operations.get(1).delta()));
    }

    @Test
    public void emptyPilePrecedesLowLostPileAndContinuesTheAction() {
        assertEvaluation(PullSpecificActionPolicy.scorePileSearch(
                        new PullSpecificActionFacts.PileSearch(ACTION_ID,
                                PullSpecificActionFacts.PileKind.LOST, 0)),
                PullSpecificActionPolicy.AdapterStep.CONTINUE_ACTION,
                expected("V23-empty-lost", TraceOutputKind.VETO, -300.0f,
                        "V23 EMPTY PILE: Lost Pile is empty — search will fail!"));
        assertEvaluation(PullSpecificActionPolicy.scorePileSearch(
                        new PullSpecificActionFacts.PileSearch(ACTION_ID,
                                PullSpecificActionFacts.PileKind.USED, 0)),
                PullSpecificActionPolicy.AdapterStep.CONTINUE_ACTION,
                expected("V23-empty-used", TraceOutputKind.VETO, -300.0f,
                        "V23 EMPTY PILE: Used Pile is empty — search will fail!"));
    }

    @Test
    public void lowLostPileWarnsAtOneAndTwoThenFallsThrough() {
        assertEvaluation(PullSpecificActionPolicy.scorePileSearch(
                        new PullSpecificActionFacts.PileSearch(ACTION_ID,
                                PullSpecificActionFacts.PileKind.LOST, 1)),
                PullSpecificActionPolicy.AdapterStep.FALL_THROUGH,
                expected("V23-low-lost", TraceOutputKind.VETO, -100.0f,
                        "V23 LOW PILE: Lost Pile only has 1 cards — risky search"));
        assertEvaluation(PullSpecificActionPolicy.scorePileSearch(
                        new PullSpecificActionFacts.PileSearch(ACTION_ID,
                                PullSpecificActionFacts.PileKind.LOST, 2)),
                PullSpecificActionPolicy.AdapterStep.FALL_THROUGH,
                expected("V23-low-lost", TraceOutputKind.VETO, -100.0f,
                        "V23 LOW PILE: Lost Pile only has 2 cards — risky search"));
    }

    @Test
    public void safePileSearchesFallThroughWithoutOperations() {
        assertEvaluation(PullSpecificActionPolicy.scorePileSearch(
                        new PullSpecificActionFacts.PileSearch(ACTION_ID,
                                PullSpecificActionFacts.PileKind.LOST, 3)),
                PullSpecificActionPolicy.AdapterStep.FALL_THROUGH);
        assertEvaluation(PullSpecificActionPolicy.scorePileSearch(
                        new PullSpecificActionFacts.PileSearch(ACTION_ID,
                                PullSpecificActionFacts.PileKind.USED, 1)),
                PullSpecificActionPolicy.AdapterStep.FALL_THROUGH);
    }

    @Test
    public void tdigwattExhaustionControlsTheAdapterStep() {
        assertEvaluation(PullSpecificActionPolicy.scoreTdigwatt(
                        new PullSpecificActionFacts.ExhaustedSearch(
                                ACTION_ID, true)),
                PullSpecificActionPolicy.AdapterStep.FALL_THROUGH);
        assertEvaluation(PullSpecificActionPolicy.scoreTdigwatt(
                        new PullSpecificActionFacts.ExhaustedSearch(
                                ACTION_ID, false)),
                PullSpecificActionPolicy.AdapterStep.CONTINUE_ACTION,
                expected("V24-tdigwatt-exhausted", TraceOutputKind.VETO,
                        -400.0f,
                        "V24 TDIGWATT: All targets already pulled — search will fail!"));
    }

    @Test
    public void sorryLocationAlwaysEmitsOneExactAvailabilityResult() {
        assertResult(PullSpecificActionPolicy.scoreSorryLocation(
                        new PullSpecificActionFacts.SorryLocation(
                                ACTION_ID, true)),
                expected("V24.6-sorry-ready", TraceOutputKind.ORDERING,
                        250.0f,
                        "V24.6 I'M SORRY: CC sites still in reserve — pull one NOW for more drains + occupation!"));
        assertResult(PullSpecificActionPolicy.scoreSorryLocation(
                        new PullSpecificActionFacts.SorryLocation(
                                ACTION_ID, false)),
                expected("V24.6-sorry-exhausted", TraceOutputKind.VETO,
                        -300.0f,
                        "V24.6 I'M SORRY: All CC interior sites already pulled — search will fail!"));
    }

    @Test
    public void iayfPresenceRequiresBothApplicabilityAndMissingVader() {
        assertEmpty(PullSpecificActionPolicy.scoreIayfPresence(
                new PullSpecificActionFacts.IayfPresence(
                        ACTION_ID, false, false)));
        assertEmpty(PullSpecificActionPolicy.scoreIayfPresence(
                new PullSpecificActionFacts.IayfPresence(
                        ACTION_ID, false, true)));
        assertEmpty(PullSpecificActionPolicy.scoreIayfPresence(
                new PullSpecificActionFacts.IayfPresence(
                        ACTION_ID, true, true)));
        assertResult(PullSpecificActionPolicy.scoreIayfPresence(
                        new PullSpecificActionFacts.IayfPresence(
                                ACTION_ID, true, false)),
                expected("V29.8-iayf-presence", TraceOutputKind.VETO,
                        -500.0f,
                        "V29.8 IAYF: Vader NOT on table — can't deploy lightsaber from ANY source!"));
    }

    @Test
    public void iayfMissingVaderPrecedesWrongPileAndArmingBranches() {
        assertResult(PullSpecificActionPolicy.scoreIayfReserve(
                        iayf(false, true, true, false, false, false)),
                expected("V29.7-iayf-presence", TraceOutputKind.VETO,
                        -500.0f,
                        "V29.7 IAYF: Vader NOT on table — can't deploy lightsaber!"));
    }

    @Test
    public void iayfWrongReservePrecedesWrongLostAndArmingBranches() {
        assertResult(PullSpecificActionPolicy.scoreIayfReserve(
                        iayf(true, true, true, false, false, false)),
                expected("V37-iayf-wrong-reserve", TraceOutputKind.VETO,
                        -600.0f,
                        "V37 IAYF: Lightsaber NOT in Reserve Deck — WILL FAIL! Check Lost Pile instead."));
    }

    @Test
    public void iayfWrongLostPrecedesArmingBranches() {
        assertResult(PullSpecificActionPolicy.scoreIayfReserve(
                        iayf(true, true, true, true, false, false)),
                expected("V37-iayf-wrong-lost", TraceOutputKind.VETO,
                        -400.0f,
                        "V37 IAYF: Lightsaber NOT in Lost Pile — check Reserve instead."));
        assertResult(PullSpecificActionPolicy.scoreIayfReserve(
                        iayf(true, false, true, false, false, true)),
                expected("V37-iayf-wrong-lost", TraceOutputKind.VETO,
                        -400.0f,
                        "V37 IAYF: Lightsaber NOT in Lost Pile — check Reserve instead."));
    }

    @Test
    public void iayfUnarmedReasonUsesLostModeBeforeReserveMode() {
        assertResult(PullSpecificActionPolicy.scoreIayfReserve(
                        iayf(true, true, false, true, false, false)),
                expected("V37-iayf-unarmed", TraceOutputKind.ORDERING,
                        600.0f,
                        "V37 IAYF: Vader UNARMED — retrieve lightsaber from Reserve NOW!"));
        assertResult(PullSpecificActionPolicy.scoreIayfReserve(
                        iayf(true, false, true, false, true, false)),
                expected("V37-iayf-unarmed", TraceOutputKind.ORDERING,
                        600.0f,
                        "V37 IAYF: Vader UNARMED — retrieve lightsaber from Lost Pile NOW!"));
        assertResult(PullSpecificActionPolicy.scoreIayfReserve(
                        iayf(true, true, true, true, true, false)),
                expected("V37-iayf-unarmed", TraceOutputKind.ORDERING,
                        600.0f,
                        "V37 IAYF: Vader UNARMED — retrieve lightsaber from Lost Pile NOW!"));
    }

    @Test
    public void iayfArmedBranchAlwaysScoresSpareAfterSafetyChecks() {
        assertResult(PullSpecificActionPolicy.scoreIayfReserve(
                        iayf(true, true, false, true, false, true)),
                expected("V35.8-iayf-spare", TraceOutputKind.ORDERING,
                        50.0f,
                        "V35.8 IAYF: Vader armed — spare lightsaber retrieval"));
        assertResult(PullSpecificActionPolicy.scoreIayfReserve(
                        iayf(true, false, false, false, false, true)),
                expected("V35.8-iayf-spare", TraceOutputKind.ORDERING,
                        50.0f,
                        "V35.8 IAYF: Vader armed — spare lightsaber retrieval"));
    }

    @Test
    public void crushMissingTargetAndDuplicateVetoesAreAdditiveAndOrdered() {
        Expected missing = expected("V29.7-crush-empty", TraceOutputKind.VETO,
                -400.0f,
                "V29.7 CRUSH: No I Have You Now or Evader in reserve — WILL FAIL!");

        assertResult(crush(false, PullSpecificActionFacts.DuplicateState.NONE),
                missing);
        assertResult(crush(false,
                        PullSpecificActionFacts.DuplicateState.BOTH_IN_HAND),
                missing,
                expected("V29.9-crush-both", TraceOutputKind.VETO, -300.0f,
                        "V29.9 CRUSH DUPLICATE: Both IHYN and Evader already in hand — pulling another is wasteful!"));
        assertResult(crush(false,
                        PullSpecificActionFacts.DuplicateState.FIRST_IN_HAND_SECOND_MISSING),
                missing,
                expected("V29.9-crush-ihyn", TraceOutputKind.VETO, -250.0f,
                        "V29.9 CRUSH DUPLICATE: IHYN already in hand, no Evader in reserve — save Crush!"));
        assertResult(crush(false,
                        PullSpecificActionFacts.DuplicateState.SECOND_IN_HAND_FIRST_MISSING),
                missing,
                expected("V29.9-crush-evader", TraceOutputKind.VETO, -250.0f,
                        "V29.9 CRUSH DUPLICATE: Evader already in hand, no IHYN in reserve — save Crush!"));
    }

    @Test
    public void crushDuplicateBranchesRemainIndependentWhenTargetExists() {
        assertEmpty(crush(true, PullSpecificActionFacts.DuplicateState.NONE));
        assertResult(crush(true,
                        PullSpecificActionFacts.DuplicateState.BOTH_IN_HAND),
                expected("V29.9-crush-both", TraceOutputKind.VETO, -300.0f,
                        "V29.9 CRUSH DUPLICATE: Both IHYN and Evader already in hand — pulling another is wasteful!"));
        assertResult(crush(true,
                        PullSpecificActionFacts.DuplicateState.FIRST_IN_HAND_SECOND_MISSING),
                expected("V29.9-crush-ihyn", TraceOutputKind.VETO, -250.0f,
                        "V29.9 CRUSH DUPLICATE: IHYN already in hand, no Evader in reserve — save Crush!"));
        assertResult(crush(true,
                        PullSpecificActionFacts.DuplicateState.SECOND_IN_HAND_FIRST_MISSING),
                expected("V29.9-crush-evader", TraceOutputKind.VETO, -250.0f,
                        "V29.9 CRUSH DUPLICATE: Evader already in hand, no IHYN in reserve — save Crush!"));
    }

    @Test
    public void everyOtherNamedReserveSourceHasItsExactMissingTargetVeto() {
        assertNamedSource(PullSpecificActionFacts.ReserveSourceKind.YOU_ARE_BEATEN,
                "V29.7-you-are-beaten-empty",
                "V29.7 YOU ARE BEATEN: No I Am Your Father in reserve — WILL FAIL!");
        assertNamedSource(PullSpecificActionFacts.ReserveSourceKind.BLAST_POINTS,
                "V29.7-blast-points-empty",
                "V29.7 BLAST POINTS: No Ghhhk or Hyperwave Scan in reserve — WILL FAIL!");
        assertNamedSource(PullSpecificActionFacts.ReserveSourceKind.HUNT_DOWN,
                "V29.7-hunt-down-empty",
                "V29.7 HUNT DOWN: No locations left in reserve — WILL FAIL!");
        assertNamedSource(PullSpecificActionFacts.ReserveSourceKind.IMPERIAL_COMMAND,
                "V29.7-imperial-command-empty",
                "V29.7 IMPERIAL COMMAND: No admirals/generals in reserve — WILL FAIL!");
        assertNamedSource(PullSpecificActionFacts.ReserveSourceKind.ENDOR_SHIELD,
                "V29.7-endor-shield-empty",
                "V29.7 ENDOR SHIELD: No admirals in reserve — WILL FAIL!");
        assertNamedSource(PullSpecificActionFacts.ReserveSourceKind.VISAGE,
                "V29.7-visage-empty",
                "V29.7 VISAGE: No lightsabers in reserve — WILL FAIL!");
        assertNamedSource(PullSpecificActionFacts.ReserveSourceKind.KIR_KANOS,
                "V29.7-kir-kanos-empty",
                "V29.7 KIR KANOS: No Royal Guards in reserve — WILL FAIL!");
    }

    @Test
    public void noneSourceAndNonCrushDuplicateStateEmitNothing() {
        assertEmpty(PullSpecificActionPolicy.scoreNamedReserveSource(
                named(PullSpecificActionFacts.ReserveSourceKind.NONE,
                        false, PullSpecificActionFacts.DuplicateState.BOTH_IN_HAND)));
        assertEmpty(PullSpecificActionPolicy.scoreNamedReserveSource(
                named(PullSpecificActionFacts.ReserveSourceKind.YOU_ARE_BEATEN,
                        true, PullSpecificActionFacts.DuplicateState.BOTH_IN_HAND)));
    }

    @Test
    public void reserveRiskBoundariesAreThreeFourEightAndNine() {
        assertResult(PullSpecificActionPolicy.scoreReserveRisk(
                        new PullSpecificActionFacts.ReserveRisk(ACTION_ID, 3)),
                expected("V37-reserve-intel", TraceOutputKind.VETO, -200.0f,
                        "V37 RESERVE INTEL RISK: Only 3 cards in reserve — search reveals almost everything to opponent!"));
        assertResult(PullSpecificActionPolicy.scoreReserveRisk(
                        new PullSpecificActionFacts.ReserveRisk(ACTION_ID, 4)),
                expected("V37-reserve-caution", TraceOutputKind.VETO, -50.0f,
                        "V37 RESERVE CAUTION: 4 cards in reserve — opponent will see deck composition"));
        assertResult(PullSpecificActionPolicy.scoreReserveRisk(
                        new PullSpecificActionFacts.ReserveRisk(ACTION_ID, 8)),
                expected("V37-reserve-caution", TraceOutputKind.VETO, -50.0f,
                        "V37 RESERVE CAUTION: 8 cards in reserve — opponent will see deck composition"));
        assertEmpty(PullSpecificActionPolicy.scoreReserveRisk(
                new PullSpecificActionFacts.ReserveRisk(ACTION_ID, 9)));
    }

    @Test
    public void masterfulMoveNoCharacterPrecedesEarlyTurnCheck() {
        assertResult(PullSpecificActionPolicy.scoreMasterfulMove(
                        new PullSpecificActionFacts.MasterfulMove(
                                ACTION_ID, false, 1)),
                expected("V24.9-masterful-no-character",
                        TraceOutputKind.VETO, -500.0f,
                        "V24.9 MASTERFUL MOVE: No characters on table — Ghhhk has nothing to protect! Save force for deployment!"));
        assertResult(PullSpecificActionPolicy.scoreMasterfulMove(
                        new PullSpecificActionFacts.MasterfulMove(
                                ACTION_ID, false, 3)),
                expected("V24.9-masterful-no-character",
                        TraceOutputKind.VETO, -500.0f,
                        "V24.9 MASTERFUL MOVE: No characters on table — Ghhhk has nothing to protect! Save force for deployment!"));
    }

    @Test
    public void masterfulMoveEarlyTurnBoundaryIsTwoThenThree() {
        assertResult(PullSpecificActionPolicy.scoreMasterfulMove(
                        new PullSpecificActionFacts.MasterfulMove(
                                ACTION_ID, true, 2)),
                expected("V24.9-masterful-early", TraceOutputKind.VETO,
                        -300.0f,
                        "V24.9 MASTERFUL MOVE: Too early (turn 2) — prioritize getting Executor out!"));
        assertEmpty(PullSpecificActionPolicy.scoreMasterfulMove(
                new PullSpecificActionFacts.MasterfulMove(
                        ACTION_ID, true, 3)));
    }

    @Test
    public void effectSearchPreservesWoklingPrecedenceAndGenericFallback() {
        assertResult(PullSpecificActionPolicy.scoreEffectSearch(
                        new PullSpecificActionFacts.EffectSearch(
                                ACTION_ID, true)),
                expected("V53-wokling-search", TraceOutputKind.VETO,
                        -9999.0f,
                        "V53 BLOCK WOKLING: Don't waste 3 force searching for effects!"));
        assertResult(PullSpecificActionPolicy.scoreEffectSearch(
                        new PullSpecificActionFacts.EffectSearch(
                                ACTION_ID, false)),
                expected("PULL-effect-search", TraceOutputKind.ORDERING,
                        30.0f, "Search for Effect from Reserve Deck"));
    }

    @Test
    public void admiralGeneralPullPreservesMutuallyExclusiveOrderAndObjectiveDomain() {
        assertResult(PullSpecificActionPolicy.scoreAdmiralGeneralPull(
                        new PullSpecificActionFacts.AdmiralGeneralPull(
                                ACTION_ID, false, true)),
                expected("V29.7-admiral-general-empty", TraceOutputKind.VETO,
                        -300.0f,
                        "V29.7 NO TARGET: No admirals/generals in Reserve Deck — skip!"));
        assertResult(PullSpecificActionPolicy.scoreAdmiralGeneralPull(
                        new PullSpecificActionFacts.AdmiralGeneralPull(
                                ACTION_ID, true, true)),
                expectedObjective("PULL-executor-chain", TraceOutputKind.ORDERING,
                        300.0f,
                        "OBJECTIVE: prefer an admiral pilot for the Executor-to-Bespin route (+300 bounded preference)"));
        assertResult(PullSpecificActionPolicy.scoreAdmiralGeneralPull(
                        new PullSpecificActionFacts.AdmiralGeneralPull(
                                ACTION_ID, true, false)),
                expected("V29.7-admiral-general-first",
                        TraceOutputKind.ORDERING, 250.0f,
                        "V29.7 PULL FIRST: Retrieve admiral/general into hand before deploying!"));
    }

    @Test
    public void executorChainPreferenceSharesTheObjectiveIntentCap() {
        PolicyOperation executorChain = PullSpecificActionPolicy
                .scoreAdmiralGeneralPull(
                        new PullSpecificActionFacts.AdmiralGeneralPull(
                                ACTION_ID, true, true))
                .operations().getFirst();
        PolicyResult sharedCap = new PolicyResult(
                "objective-cap-sharing",
                List.of(
                        executorChain,
                        PolicyOperation.add(
                                ACTION_ID,
                                TraceRuleId.of("TEST.SECOND.OBJECTIVE"),
                                TraceDomainId.OBJECTIVE_INTENT,
                                TraceOutputKind.BANDED,
                                300.0f,
                                "Second bounded objective preference")));

        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                sharedCap.operations().get(0).domainId());
        assertEquals(300.0f,
                sharedCap.operations().get(0).delta(), 0.0f);
        assertEquals(0.0f,
                sharedCap.operations().get(1).delta(), 0.0f);
    }

    @Test
    public void veersHothUploadVetoesOnlyExactEmptySearch() {
        assertResult(PullSpecificActionPolicy.scoreVeersHothUpload(
                new PullSpecificActionFacts.VeersHothUpload(
                        ACTION_ID, true, false)),
                expected("V29.7-veers-hoth-upload-empty",
                        TraceOutputKind.VETO, -2000.0f,
                        "V29.7 VEERS: No Blizzard 1 or 6th Marker in Reserve Deck, so the reveal would fail"));
        assertEmpty(PullSpecificActionPolicy.scoreVeersHothUpload(
                new PullSpecificActionFacts.VeersHothUpload(
                        ACTION_ID, true, true)));
        assertEmpty(PullSpecificActionPolicy.scoreVeersHothUpload(
                new PullSpecificActionFacts.VeersHothUpload(
                        ACTION_ID, false, false)));
    }

    private static PullSpecificActionFacts.IayfReserve iayf(
            boolean vaderOnTable, boolean reserveMode, boolean lostMode,
            boolean saberInReserve, boolean saberInLost,
            boolean vaderArmed) {
        return new PullSpecificActionFacts.IayfReserve(
                ACTION_ID, vaderOnTable, reserveMode, lostMode,
                saberInReserve, saberInLost, vaderArmed);
    }

    private static PolicyResult crush(
            boolean targetAvailable,
            PullSpecificActionFacts.DuplicateState duplicateState) {
        return PullSpecificActionPolicy.scoreNamedReserveSource(
                named(PullSpecificActionFacts.ReserveSourceKind.CRUSH_THE_REBELLION,
                        targetAvailable, duplicateState));
    }

    private static PullSpecificActionFacts.NamedReserveSource named(
            PullSpecificActionFacts.ReserveSourceKind kind,
            boolean targetAvailable,
            PullSpecificActionFacts.DuplicateState duplicateState) {
        return new PullSpecificActionFacts.NamedReserveSource(
                ACTION_ID, kind, targetAvailable, duplicateState);
    }

    private static void assertNamedSource(
            PullSpecificActionFacts.ReserveSourceKind kind,
            String ruleId, String reason) {
        assertEmpty(PullSpecificActionPolicy.scoreNamedReserveSource(
                named(kind, true, PullSpecificActionFacts.DuplicateState.NONE)));
        assertResult(PullSpecificActionPolicy.scoreNamedReserveSource(
                        named(kind, false,
                                PullSpecificActionFacts.DuplicateState.NONE)),
                expected(ruleId, TraceOutputKind.VETO, -400.0f, reason));
    }

    private static void assertEvaluation(
            PullSpecificActionPolicy.Evaluation evaluation,
            PullSpecificActionPolicy.AdapterStep adapterStep,
            Expected... expected) {
        assertEquals(adapterStep, evaluation.adapterStep());
        assertResult(evaluation.result(), expected);
    }

    private static void assertEmpty(PolicyResult result) {
        assertResult(result);
    }

    private static void assertResult(PolicyResult result,
                                     Expected... expected) {
        assertEquals("PULL_SPECIFIC_ACTION_POLICY", result.producerId());
        assertEquals(expected.length, result.operations().size());
        for (int i = 0; i < expected.length; i++) {
            PolicyOperation operation = result.operations().get(i);
            Expected value = expected[i];
            assertEquals(ACTION_ID, operation.actionId());
            assertEquals(value.ruleId, operation.ruleArmId().id());
            assertEquals(value.domainId, operation.domainId());
            assertEquals(value.outputKind, operation.outputKind());
            assertEquals(PolicyOperationKind.ADD, operation.kind());
            assertEquals(Float.floatToRawIntBits(value.delta),
                    Float.floatToRawIntBits(operation.delta()));
            assertEquals(value.reason, operation.reason());
        }
    }

    private static Expected expected(String ruleId,
                                     TraceOutputKind outputKind,
                                     float delta, String reason) {
        return new Expected(ruleId, TraceDomainId.PULL_SEARCH,
                outputKind, delta, reason);
    }

    private static Expected expectedObjective(
            String ruleId, TraceOutputKind outputKind,
            float delta, String reason) {
        return new Expected(ruleId, TraceDomainId.OBJECTIVE_INTENT,
                outputKind, delta, reason);
    }

    private static final class Expected {
        private final String ruleId;
        private final TraceDomainId domainId;
        private final TraceOutputKind outputKind;
        private final float delta;
        private final String reason;

        private Expected(String ruleId, TraceDomainId domainId,
                         TraceOutputKind outputKind, float delta,
                         String reason) {
            this.ruleId = ruleId;
            this.domainId = domainId;
            this.outputKind = outputKind;
            this.delta = delta;
            this.reason = reason;
        }
    }
}
