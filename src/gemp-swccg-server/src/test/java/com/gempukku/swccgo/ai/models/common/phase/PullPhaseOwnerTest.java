package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseFinalizer;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Exact compatibility-wire translation through the single PULL finalizer lane. */
public class PullPhaseOwnerTest {

    @Test
    public void firstSeenTieWireIsPreservedAndEachLaneRunsOnce() {
        Owned owned = parent("Choose action or Pass", false);
        AtomicInteger compatibilityCalls = new AtomicInteger();
        AtomicInteger finalizerCalls = new AtomicInteger();

        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(),
                (route, facts, assessment) -> {
                    compatibilityCalls.incrementAndGet();
                    return "0";
                },
                (snapshot, contract, intent, random, history) -> {
                    finalizerCalls.incrementAndGet();
                    return ResponseFinalizer.finalize(snapshot, contract, intent, random, history);
                });

        assertAccepted(result, "0");
        assertEquals(1, compatibilityCalls.get());
        assertEquals(1, finalizerCalls.get());
    }

    @Test
    public void exactParentPassIsPreserved() {
        Owned owned = parent("Choose action or Pass", false);
        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(), (route, facts, assessment) -> "");

        assertAccepted(result, "");
    }

    @Test
    public void arbitraryMultiSelectPreservesCommaOrder() {
        Owned owned = child(DecisionOrigin.PULL_DEPLOY_CHILD, PullRoute.PULL_DEPLOY_CHILD);
        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(),
                (route, facts, assessment) -> "temp2,temp0");

        assertAccepted(result, "temp2,temp0");
    }

    @Test
    public void duplicateCopiesSelectByExactWireOrdinal() {
        Owned owned = child(DecisionOrigin.PULL_TAKE_CHILD, PullRoute.PULL_TAKE_CHILD);
        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(),
                (route, facts, assessment) -> "temp1");

        assertEquals(1002, owned.facts().candidateCards().get(1).permanentCardId());
        assertAccepted(result, "temp1");
    }

    @Test
    public void childCancelPreservesExactEmptyWire() {
        Owned owned = child(DecisionOrigin.PULL_TAKE_CHILD, PullRoute.PULL_TAKE_CHILD);
        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(), (route, facts, assessment) -> "");

        assertAccepted(result, "");
    }

    @Test
    public void destinationUsesExactCurrentCardWire() {
        Map<String, String[]> params = PullTestFixtures.destination(false);
        Owned owned = owned(AwaitingDecisionType.CARD_SELECTION, "choose destination",
                params, PullRoute.PULL_DESTINATION);
        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(), (route, facts, assessment) -> "302");

        assertAccepted(result, "302");
    }

    @Test
    public void failedVerifyBypassesCompatibilityAndFinalizesEmptyExactlyOnce() {
        Map<String, String[]> params = PullTestFixtures.failedVerify();
        Owned owned = owned(AwaitingDecisionType.ARBITRARY_CARDS, "verify",
                params, PullRoute.PULL_FAILED_VERIFY);
        AtomicInteger compatibilityCalls = new AtomicInteger();
        AtomicInteger finalizerCalls = new AtomicInteger();

        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(),
                (route, facts, assessment) -> {
                    compatibilityCalls.incrementAndGet();
                    return "temp0";
                },
                (snapshot, contract, intent, random, history) -> {
                    finalizerCalls.incrementAndGet();
                    return ResponseFinalizer.finalize(snapshot, contract, intent, random, history);
                });

        assertAccepted(result, "");
        assertEquals(0, compatibilityCalls.get());
        assertEquals(1, finalizerCalls.get());
    }

    @Test
    public void unknownCompatibilityWireIsRejectedBeforeFinalizer() {
        Owned owned = child(DecisionOrigin.PULL_DEPLOY_CHILD, PullRoute.PULL_DEPLOY_CHILD);
        AtomicInteger finalizerCalls = new AtomicInteger();
        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(),
                (route, facts, assessment) -> "temp9",
                (snapshot, contract, intent, random, history) -> {
                    finalizerCalls.incrementAndGet();
                    return ResponseFinalizer.finalize(snapshot, contract, intent, random, history);
                });

        assertTypedRejection(result);
        assertEquals(0, finalizerCalls.get());
    }

    @Test
    public void ambiguousCandidateWireIsRejectedBeforeFinalizer() {
        Owned owned = child(DecisionOrigin.PULL_DEPLOY_CHILD, PullRoute.PULL_DEPLOY_CHILD);
        Map<String, List<String>> raw = new LinkedHashMap<>(
                owned.snapshot().rawDecision().parameters());
        raw.put("cardId", List.of("temp0", "temp0", "temp2"));
        DecisionSnapshot ambiguous = new DecisionSnapshot(
                owned.snapshot().decisionFacts(), owned.snapshot().actionFacts(),
                owned.snapshot().serviceFacts(),
                owned.snapshot().objectiveFacts(),
                new DecisionSnapshot.RawDecision(owned.snapshot().rawDecision().source(), raw),
                owned.snapshot().snapshotVersion());
        AtomicInteger finalizerCalls = new AtomicInteger();

        AiDecisionResult result = PullPhaseOwner.decide(
                ambiguous, RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(),
                (route, facts, assessment) -> "temp0",
                (snapshot, contract, intent, random, history) -> {
                    finalizerCalls.incrementAndGet();
                    return ResponseFinalizer.finalize(snapshot, contract, intent, random, history);
                });

        assertTypedRejection(result);
        assertEquals(0, finalizerCalls.get());
    }

    @Test
    public void correctedWireIsRejectedInsteadOfReplacingLegacyChoice() {
        Map<String, String[]> params = PullTestFixtures.child(DecisionOrigin.PULL_DEPLOY_CHILD);
        PullTestFixtures.put(params, "selectable", "false", "true", "true");
        Owned owned = owned(AwaitingDecisionType.ARBITRARY_CARDS, "choose", params,
                PullRoute.PULL_DEPLOY_CHILD);

        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(),
                (route, facts, assessment) -> "temp0");

        assertTypedRejection(result);
    }

    @Test
    public void policyDeniedPassDoesNotInvokeFinalizerOrRngFallback() {
        Owned owned = parent("Choose mandatory action", true);
        AtomicInteger finalizerCalls = new AtomicInteger();

        AiDecisionResult result = PullPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.route(),
                owned.facts(), owned.assessment(),
                (route, facts, assessment) -> "",
                (snapshot, contract, intent, random, history) -> {
                    finalizerCalls.incrementAndGet();
                    return ResponseFinalizer.finalize(snapshot, contract, intent, random, history);
                });

        assertTypedRejection(result);
        assertEquals(0, finalizerCalls.get());
    }

    private static Owned parent(String text, boolean noPass) {
        Map<String, String[]> params = PullTestFixtures.parent(
                DecisionActionSemantic.PULL_DEPLOY_FROM_PILE);
        PullTestFixtures.put(params, "noPass", String.valueOf(noPass));
        return owned(AwaitingDecisionType.CARD_ACTION_CHOICE, text, params, PullRoute.PULL_PARENT);
    }

    private static Owned child(DecisionOrigin origin, PullRoute route) {
        return owned(AwaitingDecisionType.ARBITRARY_CARDS, "choose cards",
                PullTestFixtures.child(origin), route);
    }

    private static Owned owned(AwaitingDecisionType type,
                               String text,
                               Map<String, String[]> params,
                               PullRoute route) {
        DecisionSnapshot snapshot = PullTestFixtures.snapshot(type, text, params);
        PullFacts facts = PullFacts.parse(snapshot, PullTestFixtures.input(type, params), route).value();
        return new Owned(snapshot, route, facts, PullAssessment.compatibility(facts));
    }

    private static void assertAccepted(AiDecisionResult result, String wire) {
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals(wire, result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, result.mutationMode());
        assertTrue(result.fromTypedFinalizer());
        assertNotNull(result.trackerMutation());
        assertEquals(wire, result.trackerMutation().wireResponse());
    }

    private static void assertTypedRejection(AiDecisionResult result) {
        assertEquals(AiDecisionResult.Status.TYPED_REJECTION, result.status());
        assertEquals(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                result.rejectionCode());
        assertNull(result.wireResponse());
        assertNull(result.trackerMutation());
    }

    private record Owned(DecisionSnapshot snapshot,
                         PullRoute route,
                         PullFacts facts,
                         PullAssessment assessment) {
    }
}
