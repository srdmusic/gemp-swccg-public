package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.cards.set10.light.Card10_010;
import com.gempukku.swccgo.cards.set209.light.Card209_015;
import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CaptureObjectivePolicyTest {

    @Test
    public void virtualHutWinsOnlyTheExactTigihSetupTie() {
        PolicyOperation preferred = only(
                CaptureObjectivePolicy.scoreSetupHut(
                        new CaptureObjectivePolicy.SetupHutFacts(
                                "virtual", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                                "214_19", true)));

        assertOperation(preferred, PolicyOperationKind.ADD,
                "SETUP.TIGIH.PREFER_VIRTUAL_HUT",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.ORDERING, 300.0f);
        assertEmpty(CaptureObjectivePolicy.scoreSetupHut(
                new CaptureObjectivePolicy.SetupHutFacts(
                        "base", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                        "8_71", true)));
        assertEmpty(CaptureObjectivePolicy.scoreSetupHut(
                new CaptureObjectivePolicy.SetupHutFacts(
                        "only-virtual",
                        CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                        "214_19", false)));
        assertEmpty(CaptureObjectivePolicy.scoreSetupHut(
                new CaptureObjectivePolicy.SetupHutFacts(
                        "wrong-objective",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        "214_19", true)));
    }

    @Test
    public void guaranteedCaptureParentAndDestinationUseBoundedPreference() {
        for (CaptureObjectivePolicy.ObjectiveKind objective
                : CaptureObjectivePolicy.ObjectiveKind.values()) {
            PolicyOperation parent = only(
                    CaptureObjectivePolicy.scoreCaptureRoute(
                            new CaptureObjectivePolicy.CaptureRouteFacts(
                                    "parent", objective,
                                    CaptureObjectivePolicy.CaptureRouteStep.PARENT,
                                    true)));
            PolicyOperation destination = only(
                    CaptureObjectivePolicy.scoreCaptureRoute(
                            new CaptureObjectivePolicy.CaptureRouteFacts(
                                    "destination", objective,
                                    CaptureObjectivePolicy.CaptureRouteStep.DESTINATION,
                                    true)));

            assertOperation(parent, PolicyOperationKind.ADD,
                    "MOVE.OBJECTIVE.CAPTURE_ROUTE_PARENT",
                    TraceDomainId.OBJECTIVE_INTENT, TraceOutputKind.BANDED,
                    300.0f);
            assertOperation(destination, PolicyOperationKind.ADD,
                    "MOVE.OBJECTIVE.CAPTURE_ROUTE_DESTINATION",
                    TraceDomainId.OBJECTIVE_INTENT, TraceOutputKind.BANDED,
                    300.0f);
        }
        assertEmpty(CaptureObjectivePolicy.scoreCaptureRoute(
                new CaptureObjectivePolicy.CaptureRouteFacts(
                        "not-guaranteed",
                        CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                        CaptureObjectivePolicy.CaptureRouteStep.PARENT,
                    false)));
    }

    @Test
    public void guaranteedCaptureDeployUsesBoundedObjectiveDomain() {
        PolicyOperation parent = only(
                CaptureObjectivePolicy.scoreDeployCaptureRoute(
                        new CaptureObjectivePolicy.DeployCaptureFacts(
                                "parent",
                                CaptureObjectivePolicy.ObjectiveKind.BHBM,
                                CaptureObjectivePolicy.CaptureRouteStep.PARENT,
                                true)));
        PolicyOperation destination = only(
                CaptureObjectivePolicy.scoreDeployCaptureRoute(
                        new CaptureObjectivePolicy.DeployCaptureFacts(
                                "destination",
                                CaptureObjectivePolicy.ObjectiveKind.BHBM,
                                CaptureObjectivePolicy.CaptureRouteStep.DESTINATION,
                                true)));

        assertOperation(parent, PolicyOperationKind.ADD,
                "DEPLOY.OBJECTIVE.CAPTURE_ROUTE_PARENT",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f);
        assertOperation(destination, PolicyOperationKind.ADD,
                "DEPLOY.OBJECTIVE.CAPTURE_ROUTE_DESTINATION",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f);
        assertEmpty(CaptureObjectivePolicy.scoreDeployCaptureRoute(
                new CaptureObjectivePolicy.DeployCaptureFacts(
                        "no-capture",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        CaptureObjectivePolicy.CaptureRouteStep.PARENT,
                        false)));
    }

    @Test
    public void bhbmEmperorDownloadInfluencesBothSidesWhenAffordable() {
        PolicyOperation payoff = only(
                CaptureObjectivePolicy.scoreEmperorDownload(
                        new CaptureObjectivePolicy.EmperorDownloadFacts(
                                "back",
                                CaptureObjectivePolicy.ObjectiveKind.BHBM,
                                true,
                                true)));
        assertOperation(payoff, PolicyOperationKind.ADD,
                "PULL.OBJECTIVE.BHBM.EMPEROR",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f);
        PolicyOperation setup = only(
                CaptureObjectivePolicy.scoreEmperorDownload(
                        new CaptureObjectivePolicy.EmperorDownloadFacts(
                                "front",
                                CaptureObjectivePolicy.ObjectiveKind.BHBM,
                                false,
                                true)));
        assertOperation(setup, PolicyOperationKind.ADD,
                "PULL.OBJECTIVE.BHBM.EMPEROR_SETUP",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f);
        assertEmpty(CaptureObjectivePolicy.scoreEmperorDownload(
                new CaptureObjectivePolicy.EmperorDownloadFacts(
                        "wrong-objective",
                        CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                        true,
                        true)));
        PolicyOperation unaffordable = only(
                CaptureObjectivePolicy.scoreEmperorDownload(
                        new CaptureObjectivePolicy.EmperorDownloadFacts(
                                "unaffordable",
                                CaptureObjectivePolicy.ObjectiveKind.BHBM,
                                true,
                                false)));
        assertOperation(unaffordable,
                PolicyOperationKind.ADD,
                "PULL.OBJECTIVE.BHBM.EMPEROR_RESERVE",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, -300.0f);
    }

    @Test
    public void tigihCrossoverPrefersToWaitAtStrictFourteen() {
        PolicyOperation deferAtFourteen = only(
                CaptureObjectivePolicy.scorePayoff(
                        new CaptureObjectivePolicy.PayoffFacts(
                                "defer", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                                true, false, 14.0f)));
        assertOperation(deferAtFourteen, PolicyOperationKind.ADD,
                "OBJECTIVE.TIGIH.CROSSOVER_TIMING_DEFER",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, -300.0f);

        PolicyOperation safeTiming = only(
                CaptureObjectivePolicy.scorePayoff(
                        new CaptureObjectivePolicy.PayoffFacts(
                                "safe", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                                true, true, 0.0f)));
        assertOperation(safeTiming, PolicyOperationKind.ADD,
                "OBJECTIVE.TIGIH.CROSSOVER_ATTEMPT",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f);
    }

    @Test
    public void guaranteedTotalAboveFourteenOverridesTigihTimingDefer() {
        PolicyOperation guaranteed = only(
                CaptureObjectivePolicy.scorePayoff(
                        new CaptureObjectivePolicy.PayoffFacts(
                                "guaranteed",
                                CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                                true, false, Math.nextUp(14.0f))));

        assertOperation(guaranteed, PolicyOperationKind.ADD,
                "OBJECTIVE.TIGIH.CROSSOVER_ATTEMPT",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f);
    }

    @Test
    public void bhbmDuelReceivesBoundedPreferenceWhenExactActionIsReady() {
        PolicyOperation duel = only(
                CaptureObjectivePolicy.scorePayoff(
                        new CaptureObjectivePolicy.PayoffFacts(
                                "duel", CaptureObjectivePolicy.ObjectiveKind.BHBM,
                                true, false, 0.0f)));

        assertOperation(duel, PolicyOperationKind.ADD,
                "OBJECTIVE.BHBM.DUEL_ATTEMPT",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f);
        assertEmpty(CaptureObjectivePolicy.scorePayoff(
                new CaptureObjectivePolicy.PayoffFacts(
                        "not-ready",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        false, true, 20.0f)));
    }

    @Test
    public void bhbmForceDripUrgencyUsesOneBoundedPositiveSignal() {
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                bhbmUrgencyFacts("age-zero", 0)));

        PolicyOperation ageOne = only(
                CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                        bhbmUrgencyFacts("age-one", 1)));
        PolicyOperation ageTwo = only(
                CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                        bhbmUrgencyFacts("age-two", 2)));
        PolicyOperation ageSeven = only(
                CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                        bhbmUrgencyFacts("age-seven", 7)));
        PolicyOperation capped = only(
                CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                        bhbmUrgencyFacts("capped", 8)));
        PolicyOperation pastCap = only(
                CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                        bhbmUrgencyFacts("past-cap", Integer.MAX_VALUE)));
        PolicyOperation duel = only(
                CaptureObjectivePolicy.scorePayoff(
                        new CaptureObjectivePolicy.PayoffFacts(
                                "duel",
                                CaptureObjectivePolicy.ObjectiveKind.BHBM,
                                true, false, 0.0f)));

        assertOperation(ageOne, PolicyOperationKind.ADD,
                "OBJECTIVE.BHBM.FORCE_DRIP_TRIO_URGENCY",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f);
        assertRawFloat(300.0f, ageTwo.delta());
        assertRawFloat(300.0f, ageSeven.delta());
        assertRawFloat(300.0f, capped.delta());
        assertRawFloat(300.0f, pastCap.delta());
        assertRawFloat(300.0f, duel.delta());
    }

    @Test
    public void bhbmForceDripUrgencyFailsClosedAtEverySourceBoundary() {
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "tigih", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                        true, true, true, 4, true, true, true, true, true, true,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "front", CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        false, true, true, 4, true, true, true, true, true, true,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "force-drip-cancelled",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, false, true, 4, true, true, true, true, true, true,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "unknown-age",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, false, 4, true, true, true, true, true, true,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "unknown-board",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, false, true, true, true, true, true,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "unsafe", CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, false, true, true, true, true,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "wrong-throne",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, true, false, true, true, true,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "light-card-10-010",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, true, true, false, true, true,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "opponent-candidate",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, true, true, true, false, true,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "unstable-back",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, true, true, true, true, false,
                        2, 3, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "partial-trio",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, true, true, true, true, true,
                        1, 2, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "no-count-increase",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, true, true, true, true, true,
                        2, 2, true)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "breaks-stable-back",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, true, true, true, true, true,
                        2, 3, false)));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "invalid-source-count",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, true, true, true, true, true,
                        3, 4, true)));
    }

    @Test
    public void thereIsAnotherCancelsTheBhbmForceDripUrgency() {
        Card209_015 source = new Card209_015();

        assertEquals(Side.LIGHT, source.getSide());
        assertTrue(source.getGameText().contains(
                "Opponent loses no Force to their Objective."));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "there-is-another",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, false, true, 4, true, true, true,
                        true, true, true,
                        2, 3, true)));
    }

    @Test
    public void card10_010IsLightOwnedRelocationNotDarkSelfProgress() {
        Card10_010 source = new Card10_010();

        assertEquals(Side.LIGHT, source.getSide());
        assertTrue(source.getGameText().contains(
                "during your move phase may relocate Vader (with Luke)"));
        assertEmpty(CaptureObjectivePolicy.scoreBhbmForceDripUrgency(
                new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                        "card-10-010",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        true, true, true, 4, true, true, true,
                        false, true, true,
                        1, 3, true)));
    }

    @Test
    public void conflictBonusCannotRescueAnUnsafeProjection() {
        PolicyOperation safe = only(
                CaptureObjectivePolicy.scoreConflictBattle(
                        new CaptureObjectivePolicy.ConflictBattleFacts(
                                "safe", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                                true, true)));
        assertOperation(safe, PolicyOperationKind.ADD,
                "BATTLE.OBJECTIVE.TIGIH.CONFLICT_BUILDUP",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, 300.0f);

        assertEmpty(CaptureObjectivePolicy.scoreConflictBattle(
                new CaptureObjectivePolicy.ConflictBattleFacts(
                        "unsafe", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                        true, false)));
        assertEmpty(CaptureObjectivePolicy.scoreConflictBattle(
                new CaptureObjectivePolicy.ConflictBattleFacts(
                        "inactive", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                        false, true)));
        assertEmpty(CaptureObjectivePolicy.scoreConflictBattle(
                new CaptureObjectivePolicy.ConflictBattleFacts(
                        "dark", CaptureObjectivePolicy.ObjectiveKind.BHBM,
                                true, true)));
    }

    @Test
    public void soleVirtualHutCaptureEnablerGetsBoundedBattleHold() {
        PolicyOperation hold = only(
                CaptureObjectivePolicy
                    .holdSoleVirtualCaptureEnablerBattle(
                        new CaptureObjectivePolicy
                            .CaptureEnablerBattleFacts(
                                "hold",
                                CaptureObjectivePolicy
                                    .ObjectiveKind.TIGIH,
                                true)));
        assertOperation(hold, PolicyOperationKind.ADD,
                "BATTLE.OBJECTIVE.TIGIH.VIRTUAL_HUT_ENABLER_HOLD",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED, -300.0f);
        assertEmpty(CaptureObjectivePolicy
                .holdSoleVirtualCaptureEnablerBattle(
                    new CaptureObjectivePolicy
                        .CaptureEnablerBattleFacts(
                            "other-route",
                            CaptureObjectivePolicy
                                .ObjectiveKind.TIGIH,
                            false)));
        assertEmpty(CaptureObjectivePolicy
                .holdSoleVirtualCaptureEnablerBattle(
                    new CaptureObjectivePolicy
                        .CaptureEnablerBattleFacts(
                            "dark",
                            CaptureObjectivePolicy
                                .ObjectiveKind.BHBM,
                            true)));
    }

    @Test
    public void breakingLastStableBackStateGetsBoundedHold() {
        for (CaptureObjectivePolicy.ObjectiveKind objective
                : CaptureObjectivePolicy.ObjectiveKind.values()) {
            PolicyOperation hold = only(
                    CaptureObjectivePolicy.scoreStableBackHold(
                            new CaptureObjectivePolicy.StableBackFacts(
                                    "hold", objective, true, true, true)));

            assertOperation(hold, PolicyOperationKind.ADD,
                    "OBJECTIVE.CAPTURE_STATE.STABLE_BACK_HOLD",
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED, -300.0f);
        }
        assertEmpty(CaptureObjectivePolicy.scoreStableBackHold(
                new CaptureObjectivePolicy.StableBackFacts(
                        "front", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                        false, true, true)));
        assertEmpty(CaptureObjectivePolicy.scoreStableBackHold(
                new CaptureObjectivePolicy.StableBackFacts(
                        "preserved", CaptureObjectivePolicy.ObjectiveKind.TIGIH,
                        true, true, false)));
    }

    @Test
    public void criticalTargetAndPayoffRetentionUseBoundedPreference() {
        for (CaptureObjectivePolicy.ObjectiveKind objective
                : CaptureObjectivePolicy.ObjectiveKind.values()) {
            for (CaptureObjectivePolicy.CriticalRole role
                    : CaptureObjectivePolicy.CriticalRole.values()) {
                PolicyOperation retain = only(
                        CaptureObjectivePolicy.scoreCriticalRetention(
                                new CaptureObjectivePolicy.RetentionFacts(
                                        "retain", objective, role, true)));

                assertOperation(retain, PolicyOperationKind.ADD,
                        "FORCE_LOSS.OBJECTIVE.CAPTURE_CRITICAL",
                        TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.BANDED, -300.0f);
            }
        }
        assertEmpty(CaptureObjectivePolicy.scoreCriticalRetention(
                new CaptureObjectivePolicy.RetentionFacts(
                        "free-loss",
                        CaptureObjectivePolicy.ObjectiveKind.BHBM,
                        CaptureObjectivePolicy.CriticalRole.CAPTURE_PIECE,
                        false)));
    }

    private static PolicyOperation only(PolicyResult result) {
        assertEquals("CAPTURE_OBJECTIVE_POLICY", result.producerId());
        assertEquals(1, result.operations().size());
        return result.operations().get(0);
    }

    private static void assertEmpty(PolicyResult result) {
        assertEquals("CAPTURE_OBJECTIVE_POLICY", result.producerId());
        assertTrue(result.operations().isEmpty());
    }

    private static void assertOperation(
            PolicyOperation operation,
            PolicyOperationKind kind,
            String ruleId,
            TraceDomainId domainId,
            TraceOutputKind outputKind,
            float delta) {
        assertEquals(kind, operation.kind());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(domainId, operation.domainId());
        assertEquals(outputKind, operation.outputKind());
        assertRawFloat(delta, operation.delta());
    }

    private static void assertRawFloat(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }

    private static CaptureObjectivePolicy.BhbmForceDripUrgencyFacts
    bhbmUrgencyFacts(String actionId, int turnsObservedSinceFlip) {
        return new CaptureObjectivePolicy.BhbmForceDripUrgencyFacts(
                actionId,
                CaptureObjectivePolicy.ObjectiveKind.BHBM,
                true,
                true,
                true,
                turnsObservedSinceFlip,
                true,
                true,
                true,
                true,
                true,
                true,
                2,
                3,
                true);
    }
}
