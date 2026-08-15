package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.common.DestinyType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TdigwattObjectiveScoringPolicyTest {
    private static final int CLASSIC_ID = 101;
    private static final int VIRTUAL_ID = 202;
    private static final float PASS_SCORE = 8.0f;
    private static final float MAX_OBJECTIVE_INFLUENCE = 300.0f;
    private static final float TACTICAL_OVERRIDE = 350.0f;

    @Test
    public void exactPullParentBeatsPassAndProvenExhaustionHardStops() {
        var ready = TdigwattObjectiveScoringPolicy.scorePullParent(
                new TdigwattObjectiveScoringPolicy.PullParentFacts(
                        "classic-parent",
                        classicIdentity(false),
                        CLASSIC_ID,
                        true,
                        true,
                        List.of(pull(
                                classicIdentity(false),
                                CLASSIC_ID,
                                TdigwattObjectiveFacts.PullTarget
                                        .DARK_DEAL,
                                false,
                                true))));
        assertEquals(
                TdigwattObjectiveScoringPolicy.Outcome
                        .PULL_PARENT_READY,
                ready.outcome());
        PolicyOperation readyOperation = only(ready);
        assertAdd(
                readyOperation,
                "TDIGWATT.109_12.PULL.PARENT.READY",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.ORDERING,
                MAX_OBJECTIVE_INFLUENCE);
        assertTrue(readyOperation.delta() > PASS_SCORE);

        var exhausted =
                TdigwattObjectiveScoringPolicy.scorePullParent(
                        new TdigwattObjectiveScoringPolicy
                                .PullParentFacts(
                                    "classic-parent",
                                    classicIdentity(false),
                                    CLASSIC_ID,
                                    true,
                                    true,
                                    List.of()));
        assertEquals(
                TdigwattObjectiveScoringPolicy.Outcome
                        .PULL_PARENT_EXHAUSTED,
                exhausted.outcome());
        assertHardVeto(
                only(exhausted),
                "TDIGWATT.109_12.PULL.PARENT.EXHAUSTED",
                TraceDomainId.PULL_SEARCH);
    }

    @Test
    public void pullParentUnknownNearMatchAndWrongSourceStayNeutral() {
        var identity = virtualIdentity(false);
        assertNeutral(TdigwattObjectiveScoringPolicy.scorePullParent(
                new TdigwattObjectiveScoringPolicy.PullParentFacts(
                        "unknown-action",
                        identity,
                        VIRTUAL_ID,
                        false,
                        true,
                        List.of())));
        assertNeutral(TdigwattObjectiveScoringPolicy.scorePullParent(
                new TdigwattObjectiveScoringPolicy.PullParentFacts(
                        "wrong-source",
                        identity,
                        VIRTUAL_ID + 1,
                        true,
                        true,
                        List.of())));
        assertNeutral(TdigwattObjectiveScoringPolicy.scorePullParent(
                new TdigwattObjectiveScoringPolicy.PullParentFacts(
                        "incomplete-scan",
                        identity,
                        VIRTUAL_ID,
                        true,
                        false,
                        List.of())));
    }

    @Test
    public void pullChildKeepsClassicAndVirtualPrintListsIsolated() {
        var classicLegal = scoreChild(
                pull(classicIdentity(false), CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget
                                .CLOUD_CITY_OCCUPATION,
                        false, true),
                true, true);
        assertAdd(
                only(classicLegal),
                "TDIGWATT.109_12.PULL.CHILD.LEGAL",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                TdigwattObjectiveScoringPolicy.PULL_CHILD_BONUS);

        var classicWrongPrint = scoreChild(
                pull(classicIdentity(false), CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget
                                .VADERS_BOUNTY,
                        false, true),
                true, true);
        assertHardVeto(
                only(classicWrongPrint),
                "TDIGWATT.109_12.PULL.CHILD.PRINT_ISOLATION",
                TraceDomainId.DECK_PLAYBOOK);

        var virtualLegal = scoreChild(
                pull(virtualIdentity(false), VIRTUAL_ID,
                        TdigwattObjectiveFacts.PullTarget
                                .VADERS_BOUNTY,
                        false, true),
                true, true);
        assertAdd(
                only(virtualLegal),
                "TDIGWATT.226_12.PULL.CHILD.LEGAL",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                TdigwattObjectiveScoringPolicy.PULL_CHILD_BONUS);

        var virtualClassicBespin = scoreChild(
                pull(virtualIdentity(false), VIRTUAL_ID,
                        TdigwattObjectiveFacts.PullTarget
                                .BESPIN_SYSTEM,
                        false, true),
                true, true);
        assertHardVeto(
                only(virtualClassicBespin),
                "TDIGWATT.226_12.PULL.CHILD.PRINT_ISOLATION",
                TraceDomainId.DECK_PLAYBOOK);

        assertAdd(
                only(scoreChild(
                        pull(virtualIdentity(false), VIRTUAL_ID,
                                TdigwattObjectiveFacts.PullTarget
                                        .BESPIN_SYSTEM,
                                true, true),
                        true, true)),
                "TDIGWATT.226_12.PULL.CHILD.LEGAL",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                TdigwattObjectiveScoringPolicy.PULL_CHILD_BONUS);
    }

    @Test
    public void pullChildVetoRequiresExactPhysicalSourceProvenance() {
        var wrongPrint = pull(
                virtualIdentity(false),
                VIRTUAL_ID,
                TdigwattObjectiveFacts.PullTarget
                        .CLOUD_CITY_OCCUPATION,
                false,
                true);
        assertNeutral(scoreChild(wrongPrint, false, true));
        assertNeutral(scoreChild(wrongPrint, true, false));

        var wrongPhysicalSource = pull(
                virtualIdentity(false),
                VIRTUAL_ID + 1,
                TdigwattObjectiveFacts.PullTarget
                        .CLOUD_CITY_OCCUPATION,
                false,
                true);
        assertNeutral(scoreChild(
                wrongPhysicalSource, true, true));

        var notInReserve = pull(
                virtualIdentity(false),
                VIRTUAL_ID,
                TdigwattObjectiveFacts.PullTarget
                        .CLOUD_CITY_OCCUPATION,
                false,
                false);
        assertNeutral(scoreChild(notInReserve, true, true));

        var impossibleClassicBackAction = pull(
                classicIdentity(true),
                CLASSIC_ID,
                TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                false,
                true);
        assertNeutral(scoreChild(
                impossibleClassicBackAction, true, true));
    }

    @Test
    public void deployPreferencesArePositiveBoundedAndExact() {
        var advance = TdigwattObjectiveScoringPolicy.scoreDeploy(
                "classic-advance",
                classicFront(false, true, false),
                classicFront(true, true, false),
                true);
        var complete = TdigwattObjectiveScoringPolicy.scoreDeploy(
                "classic-complete",
                classicFront(true, true, false),
                classicFront(true, true, true),
                true);
        var protect = TdigwattObjectiveScoringPolicy.scoreDeploy(
                "virtual-protect",
                virtualBack(2, 2),
                virtualBack(3, 2),
                true);

        assertAdd(
                only(advance),
                "TDIGWATT.109_12.DEPLOY.ADVANCE_FRONT",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                TdigwattObjectiveScoringPolicy
                        .DEPLOY_ADVANCE_BONUS);
        assertAdd(
                only(complete),
                "TDIGWATT.109_12.DEPLOY.COMPLETE_FRONT",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                TdigwattObjectiveScoringPolicy
                        .DEPLOY_COMPLETE_BONUS);
        assertAdd(
                only(protect),
                "TDIGWATT.226_12.DEPLOY.PROTECT_STABLE_BACK",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                TdigwattObjectiveScoringPolicy
                        .DEPLOY_STABLE_BACK_BONUS);

        assertTrue(only(advance).delta() > PASS_SCORE);
        assertTrue(only(advance).delta()
                <= MAX_OBJECTIVE_INFLUENCE);
        assertTrue(only(complete).delta()
                <= MAX_OBJECTIVE_INFLUENCE);
        assertTrue(only(protect).delta()
                <= MAX_OBJECTIVE_INFLUENCE);

        assertNeutral(TdigwattObjectiveScoringPolicy.scoreDeploy(
                "no-progress",
                virtualFront(2, 1),
                virtualFront(2, 1),
                true));
        assertNeutral(TdigwattObjectiveScoringPolicy.scoreDeploy(
                "unknown-projection",
                virtualFront(2, 1),
                virtualFront(3, 1),
                false));
    }

    @Test
    public void engineDeployUsesExactSourcePrintAndPersistence() {
        var classicDarkDeal = TdigwattObjectiveScoringPolicy
                .scoreEngineDeploy(engineDeploy(
                        classicIdentity(false),
                        CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                        true,
                        true));
        assertAdd(
                only(classicDarkDeal),
                "TDIGWATT.109_12.DEPLOY.ENGINE.DARK_DEAL",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                TdigwattObjectiveScoringPolicy
                        .ENGINE_DEPLOY_BONUS);

        var virtualDarkDeal = TdigwattObjectiveScoringPolicy
                .scoreEngineDeploy(engineDeploy(
                        virtualIdentity(false),
                        VIRTUAL_ID,
                        TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                        true,
                        true));
        assertNeutral(virtualDarkDeal);

        var classicOccupation = TdigwattObjectiveScoringPolicy
                .scoreEngineDeploy(engineDeploy(
                        classicIdentity(false),
                        CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget
                                .CLOUD_CITY_OCCUPATION,
                        true,
                        true));
        assertAdd(
                only(classicOccupation),
                "TDIGWATT.109_12.DEPLOY.ENGINE.CLOUD_CITY_OCCUPATION",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                TdigwattObjectiveScoringPolicy
                        .ENGINE_DEPLOY_BONUS);

        assertTrue(TdigwattObjectiveScoringPolicy
                        .ENGINE_DEPLOY_BONUS
                > PASS_SCORE);
        assertTrue(TdigwattObjectiveScoringPolicy
                        .ENGINE_DEPLOY_BONUS
                <= TdigwattObjectiveScoringPolicy
                        .DEPLOY_COMPLETE_BONUS);

        var unstableDarkDeal =
                TdigwattObjectiveScoringPolicy
                    .scoreEngineDeploy(engineDeploy(
                        classicIdentity(false),
                        CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget
                            .DARK_DEAL,
                        true,
                        true,
                        true,
                        false));
        assertHardVeto(
                only(unstableDarkDeal),
                "TDIGWATT.109_12.DEPLOY.ENGINE.DARK_DEAL.CANCELS",
                TraceDomainId.DEPLOY_SITING);

        var unstableOccupation =
                TdigwattObjectiveScoringPolicy
                    .scoreEngineDeploy(engineDeploy(
                        classicIdentity(false),
                        CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget
                            .CLOUD_CITY_OCCUPATION,
                        true,
                        true,
                        true,
                        false));
        assertHardVeto(
                only(unstableOccupation),
                "TDIGWATT.109_12.DEPLOY.ENGINE.CLOUD_CITY_OCCUPATION.CANCELS",
                TraceDomainId.DEPLOY_SITING);

        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreEngineDeploy(engineDeploy(
                        classicIdentity(false),
                        CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                        true,
                        true,
                        false,
                        false)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreEngineDeploy(engineDeploy(
                        virtualIdentity(false),
                        VIRTUAL_ID,
                        TdigwattObjectiveFacts.PullTarget
                                .CLOUD_CITY_OCCUPATION,
                        true,
                        true)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreEngineDeploy(engineDeploy(
                        classicIdentity(false),
                        CLASSIC_ID + 1,
                        TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                        true,
                        true)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreEngineDeploy(engineDeploy(
                        classicIdentity(false),
                        CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                        false,
                        true)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreEngineDeploy(engineDeploy(
                        classicIdentity(false),
                        CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                        true,
                        false)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreEngineDeploy(engineDeploy(
                        classicIdentity(false),
                        CLASSIC_ID,
                        TdigwattObjectiveFacts.PullTarget.VADERS_BOUNTY,
                        true,
                        true)));
    }

    @Test
    public void virtualLandoScoresOnlyAnExactSafeUsefulFundedRoute() {
        var valid = new TdigwattObjectiveScoringPolicy
                .LandoActionFacts(
                    "lando-parent",
                    landoMove(
                            virtualIdentity(false),
                            VIRTUAL_ID,
                            true, true, true,
                            true, true, 1),
                    true,
                    1);
        assertAdd(
                only(TdigwattObjectiveScoringPolicy
                        .scoreVirtualLandoParent(valid)),
                "TDIGWATT.226_12.LANDO.PARENT",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.ORDERING,
                TdigwattObjectiveScoringPolicy.LANDO_PARENT_BONUS);
        assertAdd(
                only(TdigwattObjectiveScoringPolicy
                        .scoreVirtualLandoDestination(
                            new TdigwattObjectiveScoringPolicy
                                    .LandoDestinationFacts(
                                        valid, true))),
                "TDIGWATT.226_12.LANDO.DESTINATION",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                TdigwattObjectiveScoringPolicy
                        .LANDO_DESTINATION_BONUS);

        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoParent(
                    new TdigwattObjectiveScoringPolicy
                            .LandoActionFacts(
                                "unfunded",
                                valid.move(),
                                true,
                                0)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoParent(
                    new TdigwattObjectiveScoringPolicy
                            .LandoActionFacts(
                                "unknown-parent",
                                valid.move(),
                                false,
                                1)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoDestination(
                    new TdigwattObjectiveScoringPolicy
                            .LandoDestinationFacts(
                                valid, false)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoParent(
                    new TdigwattObjectiveScoringPolicy
                            .LandoActionFacts(
                                "unsafe-route",
                                landoMove(
                                    virtualIdentity(false),
                                    VIRTUAL_ID,
                                    true, true, true,
                                    true, false, 1),
                                true,
                                1)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoParent(
                    new TdigwattObjectiveScoringPolicy
                            .LandoActionFacts(
                                "classic-near-match",
                                landoMove(
                                    classicIdentity(false),
                                    CLASSIC_ID,
                                    true, true, true,
                                    true, true, 1),
                                true,
                                1)));
    }

    @Test
    public void exactVirtualLandoRouteIsModestAndTacticallyOverrideable() {
        var valid = new TdigwattObjectiveScoringPolicy
                .LandoActionFacts(
                    "lando-parent",
                    landoMove(
                            virtualIdentity(false),
                            VIRTUAL_ID,
                            true, true, true,
                            true, true, 1),
                    true,
                    1);
        var parent = TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoParent(valid);
        var destination = TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoDestination(
                    new TdigwattObjectiveScoringPolicy
                            .LandoDestinationFacts(valid, true));

        assertEquals(
                MAX_OBJECTIVE_INFLUENCE,
                only(parent).delta(),
                0.0f);
        assertEquals(
                MAX_OBJECTIVE_INFLUENCE,
                only(destination).delta(),
                0.0f);
        assertTrue(only(parent).delta()
                - TACTICAL_OVERRIDE < 0.0f);
        assertTrue(only(destination).delta()
                - TACTICAL_OVERRIDE < 0.0f);

        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoParent(
                    new TdigwattObjectiveScoringPolicy
                            .LandoActionFacts(
                                "unsafe-route",
                                landoMove(
                                    virtualIdentity(false),
                                    VIRTUAL_ID,
                                    true, true, true,
                                    true, false, 1),
                                true,
                                1)));
    }

    @Test
    public void controlBudgetAppliesBoundedPenaltyToExactSpendCrossingReserve() {
        var move = landoMove(
                virtualIdentity(false),
                VIRTUAL_ID,
                true, true, true,
                true, true, 2);
        var discouraged = TdigwattObjectiveScoringPolicy
                .preserveVirtualLandoControlForce(
                    new TdigwattObjectiveScoringPolicy
                            .ControlSpendFacts(
                                "optional-spend",
                                move,
                                true,
                                false,
                                2,
                                3));
        assertEquals(
                TdigwattObjectiveScoringPolicy.Outcome
                        .CONTROL_LANDO_FORCE_RESERVED,
                discouraged.outcome());
        assertAdd(
                only(discouraged),
                "TDIGWATT.226_12.CONTROL.LANDO_FORCE_RESERVE",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                -300.0f);
        assertTrue(only(discouraged).delta()
                + TACTICAL_OVERRIDE > 0.0f);

        assertNeutral(controlSpend(
                move, true, false, 1, 3));
        assertNeutral(controlSpend(
                move, true, false, null, 3));
        assertNeutral(controlSpend(
                move, false, false, 2, 3));
        assertNeutral(controlSpend(
                move, true, true, 2, 3));
        assertNeutral(controlSpend(
                landoMove(
                        virtualIdentity(false),
                        VIRTUAL_ID,
                        true, false, true,
                        true, true, 2),
                true, false, 2, 3));
        assertNeutral(controlSpend(
                landoMove(
                        virtualIdentity(false),
                        VIRTUAL_ID,
                        true, true, true,
                        true, true, 0),
                true, false, 1, 3));
        assertNeutral(controlSpend(
                move, true, false, 1, 1));
    }

    @Test
    public void backSideBattlePayoffNeverOverridesBaseSafety() {
        var classicPayoff =
                TdigwattObjectiveScoringPolicy.scoreBackSideBattle(
                    battleScoring(
                        battle(
                            classicIdentity(true),
                            true, true, true,
                            false, false),
                        true, true, false));
        assertAdd(
                only(classicPayoff),
                "TDIGWATT.109_12.BACK.BATTLE_PAYOFF",
                TraceDomainId.BATTLE_INITIATION,
                TraceOutputKind.BANDED,
                160.0f);

        var virtualPayoff =
                TdigwattObjectiveScoringPolicy.scoreBackSideBattle(
                    battleScoring(
                        battle(
                            virtualIdentity(true),
                            true, true, false,
                            true, true),
                        true, true, false));
        assertAdd(
                only(virtualPayoff),
                "TDIGWATT.226_12.BACK.BATTLE_PAYOFF",
                TraceDomainId.BATTLE_INITIATION,
                TraceOutputKind.BANDED,
                120.0f);
        assertTrue(only(classicPayoff).delta()
                <= TdigwattObjectiveScoringPolicy
                    .MAX_BATTLE_PAYOFF_BONUS);

        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreBackSideBattle(
                    battleScoring(
                        battle(
                            classicIdentity(true),
                            true, true, true,
                            false, false),
                        true, false, false)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreBackSideBattle(
                    battleScoring(
                        battle(
                            classicIdentity(true),
                            true, true, true,
                            false, false),
                        true, true, true)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreBackSideBattle(
                    battleScoring(
                        battle(
                            classicIdentity(false),
                            true, true, true,
                            false, false),
                        true, true, false)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreBackSideBattle(
                    battleScoring(
                        battle(
                            virtualIdentity(true),
                            true, true, false,
                            true, true),
                        false, true, false)));
    }

    @Test
    public void virtualLandoDestinyChoosesHelpfulExactDirection() {
        var ownDraw = destinyAdjustment(
                TdigwattObjectiveFacts
                    .DestinyDrawOwner.YOURS);
        var parent =
                TdigwattObjectiveScoringPolicy
                    .scoreVirtualLandoDestinyAdjustment(
                        destinyAdjustmentScoring(
                            ownDraw,
                            TdigwattObjectiveScoringPolicy
                                .DestinyAdjustmentChoice
                                .PARENT,
                            true));
        assertAdd(
                only(parent),
                "TDIGWATT.226_12.BACK.LANDO_DESTINY.PARENT",
                TraceDomainId.BATTLE_WEAPONS,
                TraceOutputKind.ORDERING,
                TdigwattObjectiveScoringPolicy
                    .LANDO_DESTINY_ADJUSTMENT_BONUS);
        assertTrue(only(parent).delta() > PASS_SCORE);

        assertAdd(
                only(TdigwattObjectiveScoringPolicy
                    .scoreVirtualLandoDestinyAdjustment(
                        destinyAdjustmentScoring(
                            ownDraw,
                            TdigwattObjectiveScoringPolicy
                                .DestinyAdjustmentChoice
                                .ADD_ONE,
                            true))),
                "TDIGWATT.226_12.BACK.LANDO_DESTINY.DIRECTION",
                TraceDomainId.BATTLE_WEAPONS,
                TraceOutputKind.ORDERING,
                TdigwattObjectiveScoringPolicy
                    .LANDO_DESTINY_ADJUSTMENT_BONUS);
        assertHardVeto(
                only(TdigwattObjectiveScoringPolicy
                    .scoreVirtualLandoDestinyAdjustment(
                        destinyAdjustmentScoring(
                            ownDraw,
                            TdigwattObjectiveScoringPolicy
                                .DestinyAdjustmentChoice
                                .SUBTRACT_ONE,
                            true))),
                "TDIGWATT.226_12.BACK.LANDO_DESTINY.WRONG_DIRECTION",
                TraceDomainId.BATTLE_WEAPONS);

        var opponentDraw = destinyAdjustment(
                TdigwattObjectiveFacts
                    .DestinyDrawOwner.OPPONENTS);
        assertAdd(
                only(TdigwattObjectiveScoringPolicy
                    .scoreVirtualLandoDestinyAdjustment(
                        destinyAdjustmentScoring(
                            opponentDraw,
                            TdigwattObjectiveScoringPolicy
                                .DestinyAdjustmentChoice
                                .SUBTRACT_ONE,
                            true))),
                "TDIGWATT.226_12.BACK.LANDO_DESTINY.DIRECTION",
                TraceDomainId.BATTLE_WEAPONS,
                TraceOutputKind.ORDERING,
                TdigwattObjectiveScoringPolicy
                    .LANDO_DESTINY_ADJUSTMENT_BONUS);
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoDestinyAdjustment(
                    destinyAdjustmentScoring(
                        ownDraw,
                        TdigwattObjectiveScoringPolicy
                            .DestinyAdjustmentChoice
                            .PARENT,
                        false)));

        var unknownPurpose = destinyAdjustment(
                TdigwattObjectiveFacts
                    .DestinyDrawOwner.YOURS,
                DestinyType.DESTINY);
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoDestinyAdjustment(
                    destinyAdjustmentScoring(
                        unknownPurpose,
                        TdigwattObjectiveScoringPolicy
                            .DestinyAdjustmentChoice.PARENT,
                        true)));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreVirtualLandoDestinyAdjustment(
                    destinyAdjustmentScoring(
                        unknownPurpose,
                        TdigwattObjectiveScoringPolicy
                            .DestinyAdjustmentChoice.ADD_ONE,
                        true)));
    }

    @Test
    public void retentionPenaltyNeedsPositiveMarginAndLegalAlternative() {
        var classicBefore = classicFront(true, true, true);
        var classicAfter = classicBefore;
        var forceLoss =
                TdigwattObjectiveScoringPolicy.scoreForceLoss(
                    "lose-dark-deal",
                    classicBefore,
                    classicAfter,
                    true,
                    true);
        assertNeutral(forceLoss);

        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreForceLoss(
                    "unavoidable",
                    classicBefore,
                    classicAfter,
                    true,
                    false));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreForceLoss(
                    "zero-margin",
                    classicBefore,
                    classicBefore,
                    true,
                    true));
        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreForceLoss(
                    "unknown-projection",
                    classicBefore,
                    classicAfter,
                    false,
                    true));

        var forfeit =
                TdigwattObjectiveScoringPolicy.scoreForfeit(
                    "lose-tie",
                    virtualBack(1, 1),
                    virtualBack(1, 2),
                    true,
                    true);
        assertEquals(
                TdigwattObjectiveScoringPolicy.Outcome
                        .FORFEIT_RETAIN,
                forfeit.outcome());
        assertAdd(
                only(forfeit),
                "TDIGWATT.226_12.FORFEIT.RETAIN",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                -300.0f);

        assertNeutral(TdigwattObjectiveScoringPolicy
                .scoreForfeit(
                    "classic-sticky-back",
                    classicBack(
                        true, false, false,
                        false, false, false),
                    classicBack(
                        false, false, false,
                        false, false, false),
                    true,
                    true));
    }

    private static TdigwattObjectiveScoringPolicy.Evaluation scoreChild(
            TdigwattObjectiveFacts.PullFacts candidate,
            boolean exactChildDecision,
            boolean exactClassification) {
        return TdigwattObjectiveScoringPolicy.scorePullChild(
                new TdigwattObjectiveScoringPolicy.PullChildFacts(
                        "pull-child",
                        candidate,
                        exactChildDecision,
                        exactClassification));
    }

    private static TdigwattObjectiveScoringPolicy.EngineDeployFacts
            engineDeploy(
                    TdigwattObjectiveFacts.ObjectiveIdentity objective,
                    int objectiveSourceId,
                    TdigwattObjectiveFacts.PullTarget target,
                    boolean exactEngineOffer,
                    boolean exactTargetClassification) {
        return engineDeploy(
                objective,
                objectiveSourceId,
                target,
                exactEngineOffer,
                exactTargetClassification,
                true,
                true);
    }

    private static TdigwattObjectiveScoringPolicy.EngineDeployFacts
            engineDeploy(
                    TdigwattObjectiveFacts.ObjectiveIdentity objective,
                    int objectiveSourceId,
                    TdigwattObjectiveFacts.PullTarget target,
                    boolean exactEngineOffer,
                    boolean exactTargetClassification,
                    boolean sourcePersistenceExact,
                    boolean sourceEffectPersists) {
        return new TdigwattObjectiveScoringPolicy.EngineDeployFacts(
                "engine-deploy",
                objective,
                objectiveSourceId,
                target,
                exactEngineOffer,
                exactTargetClassification,
                sourcePersistenceExact,
                sourceEffectPersists);
    }

    private static TdigwattObjectiveScoringPolicy.Evaluation controlSpend(
            TdigwattObjectiveFacts.LandoMoveFacts move,
            boolean exactOptionalSpend,
            boolean landoSourceAction,
            Integer exactCost,
            int liveForce) {
        return TdigwattObjectiveScoringPolicy
                .preserveVirtualLandoControlForce(
                    new TdigwattObjectiveScoringPolicy
                            .ControlSpendFacts(
                                "control-spend",
                                move,
                                exactOptionalSpend,
                                landoSourceAction,
                                exactCost,
                                liveForce));
    }

    private static TdigwattObjectiveScoringPolicy
            .BattleScoringFacts battleScoring(
                    TdigwattObjectiveFacts.BattleFacts battle,
                    boolean exactBattle,
                    boolean baseSafe,
                    boolean unsafeVeto) {
        return new TdigwattObjectiveScoringPolicy
                .BattleScoringFacts(
                    "battle",
                    battle,
                    exactBattle,
                    baseSafe,
                    unsafeVeto);
    }

    private static TdigwattObjectiveFacts
            .DestinyAdjustmentFacts destinyAdjustment(
                    TdigwattObjectiveFacts
                        .DestinyDrawOwner owner) {
        return destinyAdjustment(
                owner, DestinyType.BATTLE_DESTINY);
    }

    private static TdigwattObjectiveFacts
            .DestinyAdjustmentFacts destinyAdjustment(
                    TdigwattObjectiveFacts
                        .DestinyDrawOwner owner,
                    DestinyType destinyType) {
        return new TdigwattObjectiveFacts
                .DestinyAdjustmentFacts(
                    virtualIdentity(true),
                    VIRTUAL_ID,
                    battle(
                        virtualIdentity(true),
                        false, false, false,
                        true, true),
                    owner,
                    destinyType,
                    2);
    }

    private static TdigwattObjectiveScoringPolicy
            .DestinyAdjustmentScoringFacts
            destinyAdjustmentScoring(
                    TdigwattObjectiveFacts
                        .DestinyAdjustmentFacts facts,
                    TdigwattObjectiveScoringPolicy
                        .DestinyAdjustmentChoice choice,
                    boolean exact) {
        return new TdigwattObjectiveScoringPolicy
                .DestinyAdjustmentScoringFacts(
                    "destiny-adjustment",
                    facts,
                    exact,
                    choice);
    }

    private static TdigwattObjectiveFacts.PullFacts pull(
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            int actionSourceId,
            TdigwattObjectiveFacts.PullTarget target,
            boolean specialEdition,
            boolean inReserve) {
        return new TdigwattObjectiveFacts.PullFacts(
                objective,
                actionSourceId,
                actionSourceId + 1000,
                "candidate-" + target,
                target,
                specialEdition,
                inReserve);
    }

    private static TdigwattObjectiveFacts.LandoMoveFacts landoMove(
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            int actionSourceId,
            boolean sourceAvailable,
            boolean exactRoute,
            boolean destinationExists,
            boolean useful,
            boolean safe,
            int forceCost) {
        return new TdigwattObjectiveFacts.LandoMoveFacts(
                objective,
                actionSourceId,
                sourceAvailable,
                exactRoute,
                destinationExists,
                useful,
                safe,
                forceCost);
    }

    private static TdigwattObjectiveFacts.BattleFacts battle(
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            boolean alien,
            boolean imperial,
            boolean ugnaught,
            boolean lando,
            boolean lobot) {
        return new TdigwattObjectiveFacts.BattleFacts(
                objective,
                alien,
                imperial,
                ugnaught,
                lando,
                lobot);
    }

    private static TdigwattObjectiveFacts.ObjectiveIdentity
            classicIdentity(boolean back) {
        return new TdigwattObjectiveFacts.ObjectiveIdentity(
                CLASSIC_ID,
                TdigwattObjectiveFacts.CLASSIC_BLUEPRINT_ID,
                back);
    }

    private static TdigwattObjectiveFacts.ObjectiveIdentity
            virtualIdentity(boolean back) {
        return new TdigwattObjectiveFacts.ObjectiveIdentity(
                VIRTUAL_ID,
                TdigwattObjectiveFacts.VIRTUAL_BLUEPRINT_ID,
                back);
    }

    private static TdigwattObjectiveFacts.ClassicState classicFront(
            boolean darkDeal,
            boolean system,
            boolean cloudCity) {
        return new TdigwattObjectiveFacts.ClassicState(
                classicIdentity(false),
                darkDeal,
                system,
                cloudCity,
                false,
                false,
                false);
    }

    private static TdigwattObjectiveFacts.ClassicState classicBack(
            boolean darkDeal,
            boolean system,
            boolean cloudCity,
            boolean canceled,
            boolean opponentControlsSystem,
            boolean blownAway) {
        return new TdigwattObjectiveFacts.ClassicState(
                classicIdentity(true),
                darkDeal,
                system,
                cloudCity,
                canceled,
                opponentControlsSystem,
                blownAway);
    }

    private static TdigwattObjectiveFacts.VirtualState virtualFront(
            int dark,
            int light) {
        return new TdigwattObjectiveFacts.VirtualState(
                virtualIdentity(false), dark, light);
    }

    private static TdigwattObjectiveFacts.VirtualState virtualBack(
            int dark,
            int light) {
        return new TdigwattObjectiveFacts.VirtualState(
                virtualIdentity(true), dark, light);
    }

    private static PolicyOperation only(
            TdigwattObjectiveScoringPolicy.Evaluation evaluation) {
        assertEquals(1, evaluation.result().operations().size());
        return evaluation.result().operations().get(0);
    }

    private static void assertNeutral(
            TdigwattObjectiveScoringPolicy.Evaluation evaluation) {
        assertEquals(
                TdigwattObjectiveScoringPolicy.Outcome.NEUTRAL,
                evaluation.outcome());
        assertTrue(evaluation.result().operations().isEmpty());
    }

    private static void assertAdd(
            PolicyOperation operation,
            String ruleId,
            TraceDomainId domain,
            TraceOutputKind outputKind,
            float delta) {
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(domain, operation.domainId());
        assertEquals(outputKind, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(delta, operation.delta(), 0.0f);
    }

    private static void assertHardVeto(
            PolicyOperation operation,
            String ruleId,
            TraceDomainId domain) {
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(domain, operation.domainId());
        assertEquals(TraceOutputKind.VETO, operation.outputKind());
        assertEquals(
                PolicyOperationKind.HARD_VETO,
                operation.kind());
        assertEquals(0.0f, operation.delta(), 0.0f);
    }
}
