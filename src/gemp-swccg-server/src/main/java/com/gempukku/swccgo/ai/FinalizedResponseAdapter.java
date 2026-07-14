package com.gempukku.swccgo.ai;

import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;

/**
 * FINALIZER RUNTIME (2026-07-13, Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md
 * §8): the one PURE adapter from a {@link FinalizedResponse} to an {@link AiDecisionResult}.
 *
 * <ul>
 *   <li>{@code ACCEPTED}, {@code CORRECTED}, {@code FORCED} (non-null wire) become
 *       {@code WIRE_RESPONSE}, carrying the finalizer's tracker mutation request copied into
 *       the closed {@code OUTER_COMMON} lifecycle descriptor — it is NEVER applied here.</li>
 *   <li>{@code REJECTED} becomes {@code TYPED_REJECTION} with the exact typed reason and detail.</li>
 * </ul>
 *
 * This is a production seam with focused tests, but no phase owner calls {@code ResponseFinalizer}
 * yet. The adapter draws no RNG, mutates no tracker, and never calls the mediator.
 */
public final class FinalizedResponseAdapter {

    private FinalizedResponseAdapter() {
        // static access only
    }

    /**
     * @param finalized a pure finalizer verdict
     * @param decisionId the diagnostic decision id; for a wire result it MUST equal the
     *                   finalizer tracker mutation's decision id (the {@code AiDecisionResult}
     *                   invariant enforces the match)
     */
    public static AiDecisionResult toDecisionResult(FinalizedResponse finalized, String decisionId) {
        switch (finalized.status()) {
            case ACCEPTED:
            case CORRECTED:
            case FORCED:
                return AiDecisionResult.finalizerWire(finalized.wireResponse(), decisionId,
                    finalized.trackerMutation());
            case REJECTED:
                return AiDecisionResult.typedRejection(finalized.rejection().reason(),
                    finalized.rejection().detail(), decisionId);
            default:
                throw new IllegalStateException("unhandled finalizer status " + finalized.status());
        }
    }
}
