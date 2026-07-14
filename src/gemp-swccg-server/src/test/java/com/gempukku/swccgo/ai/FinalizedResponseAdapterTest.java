package com.gempukku.swccgo.ai;

import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.ResponseIntent;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * FINALIZER RUNTIME (2026-07-13, Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md
 * §8 + §1): the pure FinalizedResponse -> AiDecisionResult adapter and the AiDecisionResult
 * envelope invariants. No RNG, no tracker, no mediator. ACCEPTED/CORRECTED/FORCED map to
 * WIRE_RESPONSE carrying the finalizer's tracker mutation as the OUTER_COMMON descriptor;
 * REJECTED maps to TYPED_REJECTION with the exact typed reason and detail.
 */
public class FinalizedResponseAdapterTest {

    private static FinalizedResponse acceptedWire(String decisionId, String wire) {
        return new FinalizedResponse(
                new ResponseIntent.Pass(),
                FinalizedResponse.Status.ACCEPTED,
                wire,
                List.of(),
                null, null, null,
                new FinalizedResponse.TrackerMutationRequest(decisionId, wire),
                0);
    }

    @Test
    public void acceptedMapsToWireResponseWithOuterCommonDescriptor() {
        FinalizedResponse fr = acceptedWire("42", "3");
        AiDecisionResult result = FinalizedResponseAdapter.toDecisionResult(fr, "42");

        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals("3", result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, result.mutationMode());
        assertTrue("from a typed finalizer", result.fromTypedFinalizer());
        assertSame("copies the finalizer tracker mutation descriptor",
                fr.trackerMutation(), result.trackerMutation());
        assertEquals("42", result.decisionId());
    }

    @Test
    public void acceptedExplicitNonePreservesTypedWireWithoutTrackerDescriptor() {
        FinalizedResponse fr = acceptedWire("42", "3");
        AiDecisionResult result = FinalizedResponseAdapter.toDecisionResult(fr, "42",
                AiDecisionResult.MutationMode.NONE);

        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals("3", result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.NONE, result.mutationMode());
        assertTrue("typed-finalizer origin is retained", result.fromTypedFinalizer());
        assertNull("NONE carries no tracker mutation", result.trackerMutation());
        assertEquals("42", result.decisionId());
        assertEquals("the pure adapter does not alter the finalizer value", "3",
                fr.trackerMutation().wireResponse());
    }

    @Test
    public void noneTypedFinalizerRejectsTrackerDescriptor() {
        FinalizedResponse fr = acceptedWire("42", "3");
        try {
            AiDecisionResult.finalizerWire("3", "42", AiDecisionResult.MutationMode.NONE,
                    fr.trackerMutation());
            fail("NONE typed-finalizer result must not carry a tracker descriptor");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("NONE"));
        }
    }

    @Test
    public void rejectedMapsToTypedRejectionWithExactReason() {
        FinalizedResponse fr = new FinalizedResponse(
                new ResponseIntent.Pass(),
                FinalizedResponse.Status.REJECTED,
                null,
                List.of(),
                new FinalizedResponse.Rejection(FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK,
                        "no legal fallback"),
                null, null, null,
                0);
        AiDecisionResult result = FinalizedResponseAdapter.toDecisionResult(fr, "9");

        assertEquals(AiDecisionResult.Status.TYPED_REJECTION, result.status());
        assertEquals(FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK, result.rejectionCode());
        assertEquals("no legal fallback", result.rejectionDetail());
        assertNull("typed rejection carries no wire", result.wireResponse());
        assertNull("typed rejection carries no mutation mode", result.mutationMode());
        assertNull("typed rejection carries no tracker mutation", result.trackerMutation());
    }

    @Test
    public void legacyWireIsNoneModeAndNotTypedFinalizer() {
        AiDecisionResult result = AiDecisionResult.legacyWire("", "1");
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals("", result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.NONE, result.mutationMode());
        assertFalse(result.fromTypedFinalizer());
        assertNull(result.trackerMutation());
    }

    @Test
    public void withWireResponsePreservesLifecycleMetadataAndReplacesWire() {
        AiDecisionResult legacyOuter = AiDecisionResult.wire("5",
                AiDecisionResult.MutationMode.OUTER_COMMON, "1");
        AiDecisionResult overridden = legacyOuter.withWireResponse("7");

        assertEquals("7", overridden.wireResponse());
        assertEquals("mode preserved", AiDecisionResult.MutationMode.OUTER_COMMON,
                overridden.mutationMode());
        assertEquals("decision id preserved", "1", overridden.decisionId());
        assertFalse("typed-finalizer flag preserved", overridden.fromTypedFinalizer());
        assertEquals("original is untouched", "5", legacyOuter.wireResponse());
    }

    @Test
    public void withWireResponseRebuildsTrackerMutationWire() {
        FinalizedResponse fr = acceptedWire("1", "5");
        AiDecisionResult finalizerResult = FinalizedResponseAdapter.toDecisionResult(fr, "1");
        AiDecisionResult overridden = finalizerResult.withWireResponse("8");

        assertEquals("8", overridden.wireResponse());
        assertEquals("tracker mutation wire rebuilt to the override", "8",
                overridden.trackerMutation().wireResponse());
        assertEquals("tracker mutation decision id preserved", "1",
                overridden.trackerMutation().decisionId());
    }

    @Test
    public void typedRejectionRequiresNonblankDetail() {
        try {
            AiDecisionResult.typedRejection(FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK, "  ", "1");
            fail("blank rejection detail must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("nonblank"));
        }
    }
}
