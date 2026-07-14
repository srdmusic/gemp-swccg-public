package com.gempukku.swccgo.ai;

import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;

import java.util.Objects;

/**
 * FINALIZER RUNTIME (2026-07-13, Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md
 * §8): the one PURE adapter from a {@link FinalizedResponse} to an {@link AiDecisionResult}.
 *
 * <ul>
 *   <li>{@code ACCEPTED}, {@code CORRECTED}, {@code FORCED} (non-null wire) become
 *       {@code WIRE_RESPONSE}. The default overload carries the finalizer's tracker mutation
 *       request as {@code OUTER_COMMON}; the explicit {@code NONE} overload carries no tracker
 *       mutation request.</li>
 *   <li>{@code REJECTED} becomes {@code TYPED_REJECTION} with the exact typed reason and detail.</li>
 * </ul>
 *
 * DRAW and PULL already own typed finalization directly. The V44/V67j revert route is the first
 * production owner using this adapter with explicit {@code NONE}. The adapter draws no RNG,
 * mutates no tracker, and never calls the mediator.
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
        return toDecisionResult(finalized, decisionId, AiDecisionResult.MutationMode.OUTER_COMMON);
    }

    /**
     * Map a finalizer verdict with explicit post-acceptance mutation ownership. Rejections
     * carry neither a mode nor a tracker descriptor regardless of the requested accepted mode.
     */
    public static AiDecisionResult toDecisionResult(FinalizedResponse finalized, String decisionId,
                                                    AiDecisionResult.MutationMode acceptedMutationMode) {
        Objects.requireNonNull(finalized, "finalized");
        Objects.requireNonNull(acceptedMutationMode, "acceptedMutationMode");
        switch (finalized.status()) {
            case ACCEPTED:
            case CORRECTED:
            case FORCED:
                return AiDecisionResult.finalizerWire(finalized.wireResponse(), decisionId,
                    acceptedMutationMode,
                    acceptedMutationMode == AiDecisionResult.MutationMode.OUTER_COMMON
                        ? finalized.trackerMutation() : null);
            case REJECTED:
                return AiDecisionResult.typedRejection(finalized.rejection().reason(),
                    finalized.rejection().detail(), decisionId);
            default:
                throw new IllegalStateException("unhandled finalizer status " + finalized.status());
        }
    }
}
