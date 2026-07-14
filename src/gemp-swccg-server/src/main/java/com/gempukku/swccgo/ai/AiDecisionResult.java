package com.gempukku.swccgo.ai;

import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;

import java.util.Objects;

/**
 * FINALIZER RUNTIME (2026-07-13, Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md
 * §1): the one small immutable envelope a mediator-facing AI call returns. It is EITHER a
 * concrete wire response the mediator submits to the engine, OR a typed pre-engine rejection
 * that never reaches {@code decisionMade}.
 *
 * <p>Construction rejects contradictory states so rejection can never masquerade as an
 * unowned route (null / empty Pass / ordinal zero / exception message).
 *
 * <p>{@link MutationMode} describes ONLY post-acceptance mutation ownership — it does NOT
 * describe trace ownership. Every mediator-facing Rando/ChosenOne result, both {@code NONE}
 * and {@code OUTER_COMMON}, keeps a deferred trace lifecycle until a disposition callback
 * closes it (packet §1 lines 119-122); Curator can replace a wrapped {@code NONE} response,
 * so the trace must stay open past computation. The mode is DATA, never a callback closure.
 */
public final class AiDecisionResult {

    /** Closed status: a submittable wire response, or a typed pre-engine rejection. */
    public enum Status {
        WIRE_RESPONSE,
        TYPED_REJECTION
    }

    /**
     * Post-acceptance mutation ownership for a {@code WIRE_RESPONSE}:
     * <ul>
     *   <li>{@code NONE} — a direct interceptor (or legacy raw-string) response: the accepted
     *       callback applies no outer tracker / strategic mutation.</li>
     *   <li>{@code OUTER_COMMON} — a common-boundary response: the accepted callback applies the
     *       outer Rando/ChosenOne tracker record + strategic events exactly once.</li>
     * </ul>
     */
    public enum MutationMode {
        NONE,
        OUTER_COMMON
    }

    private final Status status;
    /** Exact wire response; non-null iff {@code WIRE_RESPONSE}. Pass remains {@code ""}. */
    private final String wireResponse;
    /** Typed rejection code; non-null iff {@code TYPED_REJECTION}. */
    private final FinalizedResponse.RejectReason rejectionCode;
    /** Nonblank rejection detail; non-null iff {@code TYPED_REJECTION}. */
    private final String rejectionDetail;
    /** Accepted-mutation mode; non-null iff {@code WIRE_RESPONSE}. */
    private final MutationMode mutationMode;
    /** Immutable tracker mutation request; non-null only for an OUTER_COMMON typed-finalizer wire. */
    private final FinalizedResponse.TrackerMutationRequest trackerMutation;
    /** Immutable diagnostic decision id. Object identity remains the mediator's retry/terminal key. */
    private final String decisionId;
    /** Whether this came from a typed finalizer (true) or legacy raw-string compatibility (false). */
    private final boolean fromTypedFinalizer;

    private AiDecisionResult(Status status, String wireResponse,
                             FinalizedResponse.RejectReason rejectionCode, String rejectionDetail,
                             MutationMode mutationMode,
                             FinalizedResponse.TrackerMutationRequest trackerMutation,
                             String decisionId, boolean fromTypedFinalizer) {
        this.status = Objects.requireNonNull(status, "status");
        this.decisionId = Objects.requireNonNull(decisionId, "decisionId");
        this.wireResponse = wireResponse;
        this.rejectionCode = rejectionCode;
        this.rejectionDetail = rejectionDetail;
        this.mutationMode = mutationMode;
        this.trackerMutation = trackerMutation;
        this.fromTypedFinalizer = fromTypedFinalizer;
        if (status == Status.WIRE_RESPONSE) {
            Objects.requireNonNull(wireResponse, "WIRE_RESPONSE requires a wire response (pass is \"\")");
            Objects.requireNonNull(mutationMode, "WIRE_RESPONSE requires an accepted-mutation mode");
            if (rejectionCode != null || rejectionDetail != null) {
                throw new IllegalArgumentException("WIRE_RESPONSE carries no rejection code or detail");
            }
            if (fromTypedFinalizer) {
                if (mutationMode == MutationMode.OUTER_COMMON) {
                    Objects.requireNonNull(trackerMutation,
                        "an OUTER_COMMON typed-finalizer wire requires a tracker mutation request");
                    if (!trackerMutation.wireResponse().equals(wireResponse)) {
                        throw new IllegalArgumentException(
                            "tracker mutation must record the exact wire response");
                    }
                    if (!trackerMutation.decisionId().equals(decisionId)) {
                        throw new IllegalArgumentException(
                            "tracker mutation decision id must match the result");
                    }
                } else if (trackerMutation != null) {
                    throw new IllegalArgumentException(
                        "a NONE typed-finalizer wire carries no tracker mutation request");
                }
            } else if (trackerMutation != null) {
                throw new IllegalArgumentException(
                    "a legacy compatibility wire result carries no tracker mutation request");
            }
        } else { // TYPED_REJECTION
            Objects.requireNonNull(rejectionCode, "TYPED_REJECTION requires a typed rejection code");
            if (rejectionDetail == null || rejectionDetail.isBlank()) {
                throw new IllegalArgumentException("TYPED_REJECTION requires nonblank detail");
            }
            if (wireResponse != null || mutationMode != null || trackerMutation != null) {
                throw new IllegalArgumentException(
                    "TYPED_REJECTION carries no wire, mutation mode, or tracker mutation");
            }
        }
    }

    /** Legacy raw-string wire response with mutation mode {@code NONE} (the default adapter). */
    public static AiDecisionResult legacyWire(String wireResponse, String decisionId) {
        return new AiDecisionResult(Status.WIRE_RESPONSE, wireResponse, null, null,
            MutationMode.NONE, null, decisionId, false);
    }

    /** Legacy raw-string wire response with an explicit mutation mode (Rando/ChosenOne routes). */
    public static AiDecisionResult wire(String wireResponse, MutationMode mutationMode, String decisionId) {
        return new AiDecisionResult(Status.WIRE_RESPONSE, wireResponse, null, null,
            mutationMode, null, decisionId, false);
    }

    /** Typed-finalizer wire response carrying its immutable tracker mutation descriptor. */
    public static AiDecisionResult finalizerWire(String wireResponse, String decisionId,
                                                 FinalizedResponse.TrackerMutationRequest trackerMutation) {
        return new AiDecisionResult(Status.WIRE_RESPONSE, wireResponse, null, null,
            MutationMode.OUTER_COMMON, trackerMutation, decisionId, true);
    }

    /** Typed-finalizer wire response with explicit accepted-mutation ownership. */
    public static AiDecisionResult finalizerWire(String wireResponse, String decisionId,
                                                 MutationMode mutationMode,
                                                 FinalizedResponse.TrackerMutationRequest trackerMutation) {
        return new AiDecisionResult(Status.WIRE_RESPONSE, wireResponse, null, null,
            mutationMode, trackerMutation, decisionId, true);
    }

    /** Typed pre-engine rejection carrying the exact typed reason and nonblank detail. */
    public static AiDecisionResult typedRejection(FinalizedResponse.RejectReason code, String detail,
                                                  String decisionId) {
        return new AiDecisionResult(Status.TYPED_REJECTION, null, code, detail,
            null, null, decisionId, true);
    }

    /**
     * Curator override (packet §6): replace ONLY the wire response, preserving all lifecycle
     * metadata (status, mutation mode, decision id, typed-finalizer flag). When a typed
     * finalizer descriptor is present, it is rebuilt with the override wire so the accepted
     * mutation records the actual Curator choice. Pure: returns a NEW instance.
     */
    public AiDecisionResult withWireResponse(String overrideWire) {
        if (status != Status.WIRE_RESPONSE) {
            throw new IllegalStateException("only a WIRE_RESPONSE result can be overridden");
        }
        Objects.requireNonNull(overrideWire, "overrideWire");
        FinalizedResponse.TrackerMutationRequest overriddenMutation = (trackerMutation == null)
            ? null
            : new FinalizedResponse.TrackerMutationRequest(trackerMutation.decisionId(), overrideWire);
        return new AiDecisionResult(status, overrideWire, null, null,
            mutationMode, overriddenMutation, decisionId, fromTypedFinalizer);
    }

    public Status status() {
        return status;
    }

    public String wireResponse() {
        return wireResponse;
    }

    public FinalizedResponse.RejectReason rejectionCode() {
        return rejectionCode;
    }

    public String rejectionDetail() {
        return rejectionDetail;
    }

    public MutationMode mutationMode() {
        return mutationMode;
    }

    public FinalizedResponse.TrackerMutationRequest trackerMutation() {
        return trackerMutation;
    }

    public String decisionId() {
        return decisionId;
    }

    public boolean fromTypedFinalizer() {
        return fromTypedFinalizer;
    }
}
