package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TdigwattObjectivePolicyTest {
    private static final int CLASSIC_CARD_ID = 101;
    private static final int VIRTUAL_CARD_ID = 202;

    @Test
    public void classicFrontRequiresDarkDealAndBothOccupations() {
        assertFalse(TdigwattObjectivePolicy.classicFlipReady(
                classicFront(false, true, true)));
        assertFalse(TdigwattObjectivePolicy.classicFlipReady(
                classicFront(true, false, true)));
        assertFalse(TdigwattObjectivePolicy.classicFlipReady(
                classicFront(true, true, false)));

        assertTrue(
                "Occupation remains sufficient even though no Dark control fact is required",
                TdigwattObjectivePolicy.classicFlipReady(
                        classicFront(true, true, true)));
    }

    @Test
    public void classicBackFailsOnlyOnTheThreeSourceTriggers() {
        assertTrue(
                "Dark Deal merely absent and both occupations gone are not back-side triggers",
                TdigwattObjectivePolicy.classicStableBack(
                        classicBack(
                                false, false, false,
                                false, false, false)));
        assertFalse(TdigwattObjectivePolicy.classicStableBack(
                classicBack(
                        false, false, false,
                        true, false, false)));
        assertFalse(TdigwattObjectivePolicy.classicStableBack(
                classicBack(
                        true, true, true,
                        false, true, false)));
        assertFalse(TdigwattObjectivePolicy.classicStableBack(
                classicBack(
                        true, true, true,
                        false, false, true)));
    }

    @Test
    public void virtualFrontAndBackUseStrictSourceBoundaries() {
        for (int dark = 0; dark <= 3; dark++) {
            for (int light = 0; light <= 3; light++) {
                assertEquals(
                        "front boundary dark=" + dark
                                + " light=" + light,
                        dark >= 3 && light < 3,
                        TdigwattObjectivePolicy.virtualFlipReady(
                                virtualFront(dark, light)));
                assertEquals(
                        "back boundary dark=" + dark
                                + " light=" + light,
                        light <= dark,
                        TdigwattObjectivePolicy.virtualStableBack(
                                virtualBack(dark, light)));
            }
        }
    }

    @Test
    public void virtualBackStaysStableAtEveryTieZeroThroughThree() {
        for (int tie = 0; tie <= 3; tie++) {
            var facts = virtualBack(tie, tie);
            assertEquals(
                    0,
                    TdigwattObjectivePolicy
                            .virtualStableBackCushion(facts));
            assertTrue(
                    "tie " + tie + "-" + tie
                            + " must remain on the back",
                    TdigwattObjectivePolicy
                            .virtualStableBack(facts));
        }
    }

    @Test
    public void pullListsRemainPrintIsolated() {
        assertTrue(sourceLegalPull(
                classicIdentity(false),
                TdigwattObjectiveFacts.PullTarget.BESPIN_SYSTEM,
                false));
        assertTrue(sourceLegalPull(
                classicIdentity(false),
                TdigwattObjectiveFacts.PullTarget.BESPIN_CLOUD_CITY,
                false));
        assertTrue(sourceLegalPull(
                classicIdentity(false),
                TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                false));
        assertTrue(sourceLegalPull(
                classicIdentity(false),
                TdigwattObjectiveFacts.PullTarget
                        .CLOUD_CITY_OCCUPATION,
                false));
        assertFalse(sourceLegalPull(
                classicIdentity(false),
                TdigwattObjectiveFacts.PullTarget.VADERS_BOUNTY,
                false));
        assertFalse(sourceLegalPull(
                classicIdentity(true),
                TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                false));

        assertTrue(sourceLegalPull(
                virtualIdentity(false),
                TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                false));
        assertTrue(sourceLegalPull(
                virtualIdentity(false),
                TdigwattObjectiveFacts.PullTarget.VADERS_BOUNTY,
                false));
        assertTrue(sourceLegalPull(
                virtualIdentity(false),
                TdigwattObjectiveFacts.PullTarget.BESPIN_SYSTEM,
                true));
        assertFalse(sourceLegalPull(
                virtualIdentity(false),
                TdigwattObjectiveFacts.PullTarget.BESPIN_SYSTEM,
                false));
        assertFalse(sourceLegalPull(
                virtualIdentity(false),
                TdigwattObjectiveFacts.PullTarget.BESPIN_CLOUD_CITY,
                true));
        assertFalse(sourceLegalPull(
                virtualIdentity(false),
                TdigwattObjectiveFacts.PullTarget
                        .CLOUD_CITY_OCCUPATION,
                false));
    }

    @Test
    public void pullMustBelongToExactPhysicalObjectiveAndReserveCard() {
        var identity = virtualIdentity(false);
        var wrongSource = pullFacts(
                identity,
                TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                false, true,
                identity.physicalCardId() + 1);
        var notInReserve = pullFacts(
                identity,
                TdigwattObjectiveFacts.PullTarget.DARK_DEAL,
                false, false,
                identity.physicalCardId());

        assertFalse(TdigwattObjectivePolicy.sourceLegalPull(
                wrongSource));
        assertFalse(TdigwattObjectivePolicy.sourceLegalPull(
                notInReserve));
    }

    @Test
    public void deployPriorityAdvancesAndCompletesEachFrontSeparately() {
        assertEquals(
                TdigwattObjectivePolicy.DeployPriority
                        .ADVANCE_FRONT,
                TdigwattObjectivePolicy.deployPriority(
                        classicFront(false, true, false),
                        classicFront(true, true, false)));
        assertEquals(
                TdigwattObjectivePolicy.DeployPriority
                        .COMPLETE_FRONT,
                TdigwattObjectivePolicy.deployPriority(
                        classicFront(true, true, false),
                        classicFront(true, true, true)));

        assertEquals(
                TdigwattObjectivePolicy.DeployPriority
                        .ADVANCE_FRONT,
                TdigwattObjectivePolicy.deployPriority(
                        virtualFront(1, 0),
                        virtualFront(2, 0)));
        assertEquals(
                TdigwattObjectivePolicy.DeployPriority
                        .COMPLETE_FRONT,
                TdigwattObjectivePolicy.deployPriority(
                        virtualFront(2, 2),
                        virtualFront(3, 2)));
        assertEquals(
                TdigwattObjectivePolicy.DeployPriority
                        .ADVANCE_FRONT,
                TdigwattObjectivePolicy.deployPriority(
                        virtualFront(2, 3),
                        virtualFront(3, 3)));
    }

    @Test
    public void virtualBackDeployProtectsTieButClassicInventsNoRule() {
        assertEquals(
                TdigwattObjectivePolicy.DeployPriority
                        .PROTECT_STABLE_BACK,
                TdigwattObjectivePolicy.deployPriority(
                        virtualBack(2, 2),
                        virtualBack(3, 2)));
        assertEquals(
                TdigwattObjectivePolicy.DeployPriority.NONE,
                TdigwattObjectivePolicy.deployPriority(
                        classicBack(
                                true, true, true,
                                false, false, false),
                        classicBack(
                                true, true, true,
                                false, false, false)));
    }

    @Test
    public void projectionsCannotCrossPhysicalObjectiveIdentity() {
        var before = virtualFront(2, 0);
        var otherIdentity =
                new TdigwattObjectiveFacts.ObjectiveIdentity(
                        VIRTUAL_CARD_ID + 1,
                        TdigwattObjectiveFacts
                                .VIRTUAL_BLUEPRINT_ID,
                        false);
        var after = new TdigwattObjectiveFacts.VirtualState(
                otherIdentity, 3, 0);

        try {
            TdigwattObjectivePolicy.deployPriority(
                    before, after);
            fail("expected physical objective mismatch");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void virtualLandoMoveReservesTheExactEngineComputedCost() {
        assertEquals(
                1,
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(
                                landoMoveFacts(
                                        virtualIdentity(false),
                                        VIRTUAL_CARD_ID,
                                        true, true, true,
                                        true, true, 1)));
        assertEquals(
                3,
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(
                                landoMoveFacts(
                                        virtualIdentity(true),
                                        VIRTUAL_CARD_ID,
                                        true, true, true,
                                        true, true, 3)));
        assertEquals(
                0,
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(
                                landoMoveFacts(
                                        virtualIdentity(true),
                                        VIRTUAL_CARD_ID,
                                        true, true, true,
                                        true, true, 0)));
        assertEquals(
                0,
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(
                                landoMoveFacts(
                                        classicIdentity(false),
                                        CLASSIC_CARD_ID,
                                        true, true, true,
                                        true, true, 1)));
        assertEquals(
                0,
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(
                                landoMoveFacts(
                                        virtualIdentity(false),
                                        VIRTUAL_CARD_ID + 1,
                                        true, true, true,
                                        true, true, 1)));
        assertEquals(
                0,
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(
                                landoMoveFacts(
                                        virtualIdentity(false),
                                        VIRTUAL_CARD_ID,
                                        true, true, true,
                                        false, true, 1)));
        assertEquals(
                0,
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(
                                landoMoveFacts(
                                        virtualIdentity(false),
                                        VIRTUAL_CARD_ID,
                                        true, false, true,
                                        true, true, 1)));
        assertEquals(
                0,
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(
                                landoMoveFacts(
                                        virtualIdentity(false),
                                        VIRTUAL_CARD_ID,
                                        true, true, true,
                                        true, false, 1)));
    }

    @Test
    public void backSideBattlePayoffsMatchEachPrinting() {
        assertBattlePayoff(
                battleFacts(classicIdentity(false),
                        true, true, true, true, true),
                0, 0);
        assertBattlePayoff(
                battleFacts(classicIdentity(true),
                        true, true, false, false, false),
                2, 0);
        assertBattlePayoff(
                battleFacts(classicIdentity(true),
                        true, true, true, false, false),
                4, 0);
        assertBattlePayoff(
                battleFacts(virtualIdentity(true),
                        true, true, true, true, false),
                2, 1);
        assertBattlePayoff(
                battleFacts(virtualIdentity(true),
                        false, false, false, true, true),
                0, 2);
        assertBattlePayoff(
                battleFacts(virtualIdentity(true),
                        false, false, false, false, true),
                0, 0);
    }

    @Test
    public void classicRetentionKeepsForceLossBoardNeutralAndProtectsForfeit() {
        var frontBefore = classicFront(true, true, true);
        var frontAfterLoss = frontBefore;
        var frontAfterForfeit = classicFront(true, true, false);
        var frontMargins =
                TdigwattObjectivePolicy.retentionMargins(
                        frontBefore,
                        frontAfterLoss,
                        frontAfterForfeit);

        assertEquals(
                0, frontMargins.forceLoss().frontProgressLost());
        assertFalse(
                frontMargins.forceLoss()
                        .protectionRequired());
        assertEquals(
                1, frontMargins.forfeit().frontProgressLost());
        assertTrue(
                frontMargins.forfeit()
                        .preventsReadyFrontFlip());

        var backBefore = classicBack(
                true, false, false,
                false, false, false);
        var forceLossUnchanged = backBefore;
        var opponentControlsSystem = classicBack(
                true, false, false,
                false, true, false);
        var backMargins =
                TdigwattObjectivePolicy.retentionMargins(
                        backBefore,
                        forceLossUnchanged,
                        opponentControlsSystem);

        assertFalse(
                backMargins.forceLoss().protectionRequired());
        assertTrue(backMargins.forfeit().breaksStableBack());
        assertEquals(
                1,
                backMargins.forfeit()
                        .stableBackCushionLost());
    }

    @Test
    public void virtualRetentionProtectsFrontThresholdAndBackTie() {
        var frontMargins =
                TdigwattObjectivePolicy.retentionMargins(
                        virtualFront(3, 2),
                        virtualFront(3, 2),
                        virtualFront(3, 3));
        assertFalse(
                frontMargins.forceLoss()
                        .protectionRequired());
        assertTrue(
                frontMargins.forfeit()
                        .preventsReadyFrontFlip());

        var backMargins =
                TdigwattObjectivePolicy.retentionMargins(
                        virtualBack(1, 1),
                        virtualBack(1, 1),
                        virtualBack(1, 2));
        assertFalse(
                backMargins.forceLoss()
                        .protectionRequired());
        assertTrue(
                backMargins.forfeit().breaksStableBack());
        assertEquals(
                0,
                backMargins.forceLoss()
                        .stableBackCushionLost());
        assertEquals(
                1,
                backMargins.forfeit()
                        .stableBackCushionLost());
    }

    @Test
    public void virtualRetentionAlsoReportsNonterminalCushionErosion() {
        var margins =
                TdigwattObjectivePolicy.retentionMargins(
                        virtualBack(3, 1),
                        virtualBack(3, 1),
                        virtualBack(3, 2));

        assertEquals(
                0,
                margins.forceLoss()
                        .stableBackCushionLost());
        assertFalse(margins.forceLoss().breaksStableBack());
        assertEquals(
                1,
                margins.forfeit()
                        .stableBackCushionLost());
        assertTrue(margins.forfeit().protectionRequired());
        assertFalse(margins.forfeit().breaksStableBack());
    }

    private static TdigwattObjectiveFacts.ObjectiveIdentity
            classicIdentity(boolean back) {
        return new TdigwattObjectiveFacts.ObjectiveIdentity(
                CLASSIC_CARD_ID,
                TdigwattObjectiveFacts.CLASSIC_BLUEPRINT_ID,
                back);
    }

    private static TdigwattObjectiveFacts.ObjectiveIdentity
            virtualIdentity(boolean back) {
        return new TdigwattObjectiveFacts.ObjectiveIdentity(
                VIRTUAL_CARD_ID,
                TdigwattObjectiveFacts.VIRTUAL_BLUEPRINT_ID,
                back);
    }

    private static TdigwattObjectiveFacts.ClassicState
            classicFront(
                    boolean darkDeal,
                    boolean systemOccupation,
                    boolean cloudCityOccupation) {
        return new TdigwattObjectiveFacts.ClassicState(
                classicIdentity(false),
                darkDeal,
                systemOccupation,
                cloudCityOccupation,
                false, false, false);
    }

    private static TdigwattObjectiveFacts.ClassicState
            classicBack(
                    boolean darkDeal,
                    boolean systemOccupation,
                    boolean cloudCityOccupation,
                    boolean canceled,
                    boolean opponentControlsBespin,
                    boolean bespinBlownAway) {
        return new TdigwattObjectiveFacts.ClassicState(
                classicIdentity(true),
                darkDeal,
                systemOccupation,
                cloudCityOccupation,
                canceled,
                opponentControlsBespin,
                bespinBlownAway);
    }

    private static TdigwattObjectiveFacts.VirtualState
            virtualFront(int dark, int light) {
        return new TdigwattObjectiveFacts.VirtualState(
                virtualIdentity(false), dark, light);
    }

    private static TdigwattObjectiveFacts.VirtualState
            virtualBack(int dark, int light) {
        return new TdigwattObjectiveFacts.VirtualState(
                virtualIdentity(true), dark, light);
    }

    private static boolean sourceLegalPull(
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            TdigwattObjectiveFacts.PullTarget target,
            boolean specialEdition) {
        return TdigwattObjectivePolicy.sourceLegalPull(
                pullFacts(
                        objective, target, specialEdition,
                        true, objective.physicalCardId()));
    }

    private static TdigwattObjectiveFacts.PullFacts pullFacts(
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            TdigwattObjectiveFacts.PullTarget target,
            boolean specialEdition,
            boolean inReserve,
            int actionSourceCardId) {
        return new TdigwattObjectiveFacts.PullFacts(
                objective,
                actionSourceCardId,
                303,
                specialEdition ? "5_164" : "test_print",
                target,
                specialEdition,
                inReserve);
    }

    private static TdigwattObjectiveFacts.LandoMoveFacts
            landoMoveFacts(
                    TdigwattObjectiveFacts.ObjectiveIdentity objective,
                    int actionSourceCardId,
                    boolean offered,
                    boolean exactRouteKnown,
                    boolean destination,
                    boolean useful,
                    boolean formationSafe,
                    int requiredForceCost) {
        return new TdigwattObjectiveFacts.LandoMoveFacts(
                objective,
                actionSourceCardId,
                offered,
                exactRouteKnown,
                destination,
                useful,
                formationSafe,
                requiredForceCost);
    }

    private static TdigwattObjectiveFacts.BattleFacts
            battleFacts(
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

    private static void assertBattlePayoff(
            TdigwattObjectiveFacts.BattleFacts facts,
            int totalDestiny,
            int landoAdjustments) {
        var payoff =
                TdigwattObjectivePolicy.battlePayoff(facts);
        assertEquals(
                totalDestiny,
                payoff.totalBattleDestinyBonus());
        assertEquals(
                landoAdjustments,
                payoff.landoDestinyAdjustments());
        assertEquals(
                totalDestiny > 0 || landoAdjustments > 0,
                payoff.eligible());
    }
}
