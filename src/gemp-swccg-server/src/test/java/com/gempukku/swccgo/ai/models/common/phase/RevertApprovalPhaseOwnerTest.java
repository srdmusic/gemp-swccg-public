package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseFinalizer;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Exact V44/V67j compatibility selection through one typed finalizer lane. */
public class RevertApprovalPhaseOwnerTest {

    @Test
    public void singleFinalizerLaneReceivesExactHistoryAndReturnsNoneWire() {
        Map<String, String[]> params = parameters("No", "Allow revert");
        DecisionSnapshot snapshot = snapshot(params);
        RejectionHistory history = RejectionHistory.empty().append(
                "0", FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID,
                "prior checked rejection");
        AtomicInteger calls = new AtomicInteger();

        AiDecisionResult result = RevertApprovalPhaseOwner.decide(
                snapshot, history, RevertApprovalPhaseOwner.legacySelection(params.get("results")),
                (actualSnapshot, contract, intent, random, actualHistory) -> {
                    calls.incrementAndGet();
                    assertSame(snapshot, actualSnapshot);
                    assertSame(history, actualHistory);
                    return ResponseFinalizer.finalize(
                            actualSnapshot, contract, intent, random, actualHistory);
                });

        assertEquals(1, calls.get());
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals("1", result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.NONE, result.mutationMode());
        assertTrue(result.fromTypedFinalizer());
        assertNull(result.trackerMutation());
    }

    @Test
    public void emptyCandidatesReturnExactTypedBoundsRejection() {
        Map<String, String[]> params = parameters();
        AiDecisionResult result = RevertApprovalPhaseOwner.decide(
                snapshot(params), RejectionHistory.empty(),
                RevertApprovalPhaseOwner.legacySelection(params.get("results")));

        assertEquals(AiDecisionResult.Status.TYPED_REJECTION, result.status());
        assertEquals(FinalizedResponse.RejectReason.ORDINAL_OUT_OF_BOUNDS,
                result.rejectionCode());
        assertNull(result.wireResponse());
        assertNull(result.mutationMode());
        assertNull(result.trackerMutation());
    }

    @Test
    public void legacySelectionPreservesPositivePredicateAndDefaultLabel() {
        assertEquals(new RevertApprovalPhaseOwner.LegacySelection(1, "Accept"),
                RevertApprovalPhaseOwner.legacySelection(new String[]{"No", "Accept"}));
        assertEquals(new RevertApprovalPhaseOwner.LegacySelection(0, "Okay"),
                RevertApprovalPhaseOwner.legacySelection(new String[]{"Okay", "No"}));
        assertEquals(new RevertApprovalPhaseOwner.LegacySelection(0, "(default index 0)"),
                RevertApprovalPhaseOwner.legacySelection(new String[]{"No", "Decline"}));
        assertEquals(new RevertApprovalPhaseOwner.LegacySelection(0, "(default index 0)"),
                RevertApprovalPhaseOwner.legacySelection(null));
    }

    private static Map<String, String[]> parameters(String... results) {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("results", results);
        return params;
    }

    private static DecisionSnapshot snapshot(Map<String, String[]> params) {
        return PullTestFixtures.snapshot(AwaitingDecisionType.MULTIPLE_CHOICE,
                "Opponent requests a revert. Allow revert?", params);
    }
}
