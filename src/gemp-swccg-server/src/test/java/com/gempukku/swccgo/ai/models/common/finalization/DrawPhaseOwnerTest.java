package com.gempukku.swccgo.ai.models.common.finalization;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.phase.DrawPhaseOwner;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Typed finalizer ownership contract for canonical top-level DRAW decisions. */
public class DrawPhaseOwnerTest {

    @Test
    public void candidateWinnerReturnsExactWireAndInvokesEvaluatorOnce() {
        DecisionSnapshot snapshot = snapshot(3, false, "Choose draw action or Pass");
        AtomicInteger evaluations = new AtomicInteger();

        AiDecisionResult result = DrawPhaseOwner.decide(
                snapshot,
                RejectionHistory.empty(),
                () -> {
                    evaluations.incrementAndGet();
                    return DrawPhaseOwner.Evaluation.candidate("1");
                });

        assertAcceptedWire(result, "1", snapshot.decisionFacts().decisionId());
        assertEquals(1, evaluations.get());
    }

    @Test
    public void passReturnsExactEmptyWireAndInvokesEvaluatorOnce() {
        DecisionSnapshot snapshot = snapshot(2, false, "Choose draw action or Pass");
        AtomicInteger evaluations = new AtomicInteger();

        AiDecisionResult result = DrawPhaseOwner.decide(
                snapshot,
                RejectionHistory.empty(),
                () -> {
                    evaluations.incrementAndGet();
                    return DrawPhaseOwner.Evaluation.passResult();
                });

        assertAcceptedWire(result, "", snapshot.decisionFacts().decisionId());
        assertEquals(1, evaluations.get());
    }

    @Test
    public void priorRejectionsDoNotTriggerRetryOrChangeExactWinnerWire() {
        DecisionSnapshot snapshot = snapshot(3, false, "Choose draw action or Pass");
        RejectionHistory history = RejectionHistory.empty()
                .append("0", FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID,
                        "first engine rejection")
                .append("1", FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID,
                        "second engine rejection");
        AtomicInteger evaluations = new AtomicInteger();

        FinalizedResponse finalized = ResponseFinalizer.finalize(
                snapshot,
                ResponseContract.from(snapshot),
                new ResponseIntent.CandidateOrdinal(2),
                new java.util.Random(0),
                history);
        assertEquals(2, finalized.priorRejectionCount());

        AiDecisionResult result = DrawPhaseOwner.decide(
                snapshot,
                history,
                () -> {
                    evaluations.incrementAndGet();
                    return DrawPhaseOwner.Evaluation.candidate("2");
                });

        assertEquals(2, history.size());
        assertAcceptedWire(result, "2", snapshot.decisionFacts().decisionId());
        assertEquals(1, evaluations.get());
        assertEquals("history remains immutable", 2, history.size());
    }

    @Test
    public void missingWinnerIsTypedRejection() {
        DecisionSnapshot snapshot = snapshot(2, false, "Choose draw action or Pass");

        AiDecisionResult result = DrawPhaseOwner.decide(
                snapshot,
                RejectionHistory.empty(),
                () -> DrawPhaseOwner.Evaluation.candidate("9"));

        assertTypedWinnerRejection(result);
    }

    @Test
    public void ambiguousWinnerIsTypedRejection() {
        DecisionSnapshot snapshot = withActionIds(
                snapshot(2, false, "Choose draw action or Pass"),
                List.of("1", "1"));

        AiDecisionResult result = DrawPhaseOwner.decide(
                snapshot,
                RejectionHistory.empty(),
                () -> DrawPhaseOwner.Evaluation.candidate("1"));

        assertTypedWinnerRejection(result);
    }

    @Test
    public void policyDeniedPassCannotUseRandomOrFallThroughAsForcedWire() {
        DecisionSnapshot snapshot = snapshot(2, true, "Choose action to perform");

        AiDecisionResult result = DrawPhaseOwner.decide(
                snapshot,
                RejectionHistory.empty(),
                DrawPhaseOwner.Evaluation::passResult);

        assertEquals(AiDecisionResult.Status.TYPED_REJECTION, result.status());
        assertNull("forbidden forced fallback must not escape as wire", result.wireResponse());
    }

    private static DecisionSnapshot snapshot(int actionCount, boolean noPass, String text) {
        return EngineDecisionFixtures.snapshotOf(
                new EngineDecisionFixtures.RecordingCardActionChoice(
                        text, EngineDecisionFixtures.actions(actionCount), noPass));
    }

    private static DecisionSnapshot withActionIds(DecisionSnapshot snapshot,
                                                  List<String> actionIds) {
        Map<String, List<String>> parameters =
                new LinkedHashMap<>(snapshot.rawDecision().parameters());
        parameters.put("actionId", actionIds);
        DecisionSnapshot.RawDecision rawDecision = new DecisionSnapshot.RawDecision(
                snapshot.rawDecision().source(), parameters);
        return new DecisionSnapshot(
                snapshot.decisionFacts(),
                snapshot.actionFacts(),
                snapshot.serviceFacts(),
                rawDecision,
                snapshot.snapshotVersion());
    }

    private static void assertAcceptedWire(AiDecisionResult result,
                                           String expectedWire,
                                           String expectedDecisionId) {
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals(expectedWire, result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, result.mutationMode());
        assertTrue(result.fromTypedFinalizer());
        assertEquals(expectedDecisionId, result.decisionId());
        assertNotNull(result.trackerMutation());
        assertEquals(expectedWire, result.trackerMutation().wireResponse());
        assertEquals(expectedDecisionId, result.trackerMutation().decisionId());
    }

    private static void assertTypedWinnerRejection(AiDecisionResult result) {
        assertEquals(AiDecisionResult.Status.TYPED_REJECTION, result.status());
        assertEquals(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                result.rejectionCode());
        assertTrue(result.rejectionDetail().contains("missing or ambiguous"));
        assertNull(result.wireResponse());
        assertNull(result.trackerMutation());
    }
}
