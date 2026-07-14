package com.gempukku.swccgo.ai.models.common.finalization;

import java.util.List;
import java.util.Objects;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FINALIZER LANE F3 / FINALIZED RESPONSE (2026-07-13) ═══
// Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md §F3.
// Audit: Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md §"Smallest
// consolidation seam" — "FinalizedResponse must contain the chosen intent, exact
// wire response, corrections, veto or forced-choice reason, deterministic random
// draw metadata, and exactly one tracker mutation request. The finalizer is
// pure. The caller applies the mutation only after the engine accepts the
// response."
//
// PURE SHADOW value: nothing here reaches the engine, tracker, or strategic
// memory. The single trackerMutation field is a REQUEST descriptor, applied by
// the (future) caller exactly once after engine acceptance — the audit's answer
// to "tracker mutation has multiple choke points" (P1).
// ═══════════════════════════════════════════════════════════
public record FinalizedResponse(
        ResponseIntent intent,
        Status status,
        /** Exact wire response; null iff status == REJECTED. Pass is "", never null. */
        String wireResponse,
        /** Ordered deterministic corrections applied to the intent (empty when none). */
        List<Correction> corrections,
        /** Typed rejection; non-null iff status == REJECTED. */
        Rejection rejection,
        /** Typed forced-choice reason; non-null iff status == FORCED (m00336 gate
         *  P0 #2 on 92965934b: no FORCED result may exist without saying WHY the
         *  intent was unsendable — the audit's "veto or forced-choice reason"). */
        ForcedChoice forcedChoice,
        /** Deterministic random draw metadata; non-null iff a draw was consumed
         *  (only the FORCED status may consume one, and at most one). */
        RandomDraw randomDraw,
        /** Exactly one tracker mutation request; non-null iff a wire response
         *  exists. The finalizer never applies it. */
        TrackerMutationRequest trackerMutation,
        /** How many prior rejected attempts the caller reported for this decision
         *  (RejectionHistory.size()) — retry position, owned by the F2 caller. */
        int priorRejectionCount) {

    public enum Status {
        /** The intent was legal as proposed; wire is its direct encoding. */
        ACCEPTED,
        /** Deterministic corrections were applied (no RNG). */
        CORRECTED,
        /** The intent was unsendable; a legal fallback was chosen — via the ONE
         *  injected RandomGenerator draw where a set of equally-legal candidates
         *  exists, or deterministically (engine default / first-N fill) where a
         *  canonical fallback exists. */
        FORCED,
        /** No legal deterministic outcome exists; typed data for the caller.
         *  F3 performs no retries (packet rule). */
        REJECTED
    }

    /** Typed reason for a deterministic correction. Aligned with the trace layer's
     *  TraceCorrection.Kind.SELECTABLE_CLAMP where one exists. */
    public enum CorrectionReason {
        /** Locked preselected ids dropped from a card response: ARBITRARY output
         *  contains ONLY selectable delta ids (packet F3 rule; engine truth:
         *  ArbitraryCardsSelectionDecision.java:255 rejects non-selectable ids).
         *  The LEGACY clamp resends them (DecisionSafety.java:222-227) and
         *  produces engine-rejected wire — a documented divergence. */
        PRESELECTED_DELTA_ONLY,
        /** Unknown / non-selectable / out-of-bounds card ordinals dropped
         *  (same philosophy as DecisionSafety.java:177-253 SAFETY CLAMP,
         *  extended to CARD_SELECTION decisions that carry no selectable[]
         *  array, which the legacy clamp silently skips —
         *  DecisionSafety.java:198). */
        SELECTABLE_CLAMP,
        /** Duplicate ordinals collapsed (engine rejects duplicate cards:
         *  CardsSelectionDecision.java:76-77). */
        DUPLICATE_DROPPED,
        /** Trailing ids dropped to satisfy the engine maximum
         *  (CardsSelectionDecision.java:70-71). */
        MAX_CLAMP,
        /** Deterministic first-selectable fill to satisfy the engine minimum. */
        MANDATORY_REBUILD
    }

    /** Typed reason a FORCED fallback replaced the proposed intent (m00336 gate
     *  P0 #2). Exactly the two ways a Pass intent can be unsendable; every
     *  FORCED-producing path in ResponseFinalizer supplies one. */
    public enum ForceReason {
        /** The V148 pass-legality semantic (ResponseContract.policyPassAllowed —
         *  min==0 AND (!noPass OR cancel text)) DENIES the strategy decline, so a
         *  legal choice was made instead — even where the concrete engine decision
         *  would have accepted the empty wire (the CARD_ACTION_CHOICE
         *  contradiction shape). The seeded/deterministic analog of legacy
         *  must-choose (DecisionSafety.java:87-93 + 147-164). */
        POLICY_PASS_DENIED,
        /** Policy ALLOWS the decline (policyPassAllowed=true) but the concrete
         *  engine decision rejects the empty wire response
         *  (ACTION_CHOICE/MULTIPLE_CHOICE/INTEGER), so the least-commitment legal
         *  fallback encodes it: seeded candidate draw, engine default, or nearest
         *  bound. */
        PASS_NOT_WIRE_ENCODABLE
    }

    /** Typed reason for a rejection. */
    public enum RejectReason {
        /** The concrete engine decision rejects an empty wire response
         *  (ACTION_CHOICE: ActionSelectionDecision.java:130-132; MULTIPLE_CHOICE /
         *  INTEGER: unparseable empty). */
        EMPTY_WIRE_REJECTED,
        /** Ordinal outside the candidate bounds. For MULTIPLE_CHOICE this is the
         *  guard the engine LACKS today — it indexes _possibleResults before any
         *  bounds check and throws unchecked ArrayIndexOutOfBoundsException
         *  (MultipleChoiceAwaitingDecision.java:59-70, audit P0 #1; F1 owns the
         *  engine-side repair). */
        ORDINAL_OUT_OF_BOUNDS,
        /** INTEGER value outside the engine bounds (IntegerAwaitingDecision.java:35-47). */
        INTEGER_OUT_OF_BOUNDS,
        /** Too few sendable ids exist to satisfy the engine minimum, and no
         *  deterministic fill is possible — where legacy would emit
         *  SAFETY_CRITICAL_NO_OPTIONS and guess "0" (DecisionSafety.java:166-174). */
        BELOW_MINIMUM_UNCORRECTABLE,
        /** No legal fallback candidate exists for a forced choice. */
        NO_LEGAL_FALLBACK,
        /** The intent variant does not fit the concrete decision type (e.g.
         *  IntegerValue against a CARD_SELECTION). */
        INTENT_TYPE_MISMATCH,
        /** A fact the verdict depends on is UNKNOWN in the snapshot — the
         *  finalizer's declared unknown policy is typed rejection, never a
         *  fabricated default (facts-model law). */
        CONTRACT_FACT_UNKNOWN,
        /** FINALIZER RUNTIME (2026-07-13,
         *  Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md §2): the
         *  ENGINE-OWNED reason a submitted wire was rejected by the concrete engine
         *  decision (checked DecisionResultInvalidException). The mediator retry loop is the
         *  ONLY party that stamps this into RejectionHistory; ResponseFinalizer NEVER produces
         *  it (the finalizer reasons about unsendable intents, not engine-returned rejections). */
        ENGINE_DECISION_INVALID
    }

    public record Correction(CorrectionReason reason, String detail) {
        public Correction {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
            if (detail.isBlank()) {
                throw new IllegalArgumentException("correction detail must be nonblank");
            }
        }
    }

    /** Typed forced-choice reason + optional human detail (m00336 gate P0 #2:
     *  "typed ForceReason (enum + optional detail)"). */
    public record ForcedChoice(ForceReason reason, String detail) {
        public ForcedChoice {
            Objects.requireNonNull(reason, "reason");
            if (detail != null && detail.isBlank()) {
                throw new IllegalArgumentException("forced-choice detail must be null or nonblank");
            }
        }
    }

    public record Rejection(RejectReason reason, String detail) {
        public Rejection {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
            if (detail.isBlank()) {
                throw new IllegalArgumentException("rejection detail must be nonblank");
            }
        }
    }

    /** Deterministic random draw metadata: with a fixed injected RandomGenerator,
     *  (bound, value) reproduces the draw exactly (packet gate: "Fixed RNG makes
     *  every fallback deterministic"). */
    public record RandomDraw(int bound, int value) {
        public RandomDraw {
            if (bound <= 0) {
                throw new IllegalArgumentException("bound must be > 0, was " + bound);
            }
            if (value < 0 || value >= bound) {
                throw new IllegalArgumentException("value " + value + " outside draw bound " + bound);
            }
        }
    }

    /** The single tracker mutation the caller may apply AFTER engine acceptance
     *  (audit: "exactly one tracker mutation request"). Descriptor only. */
    public record TrackerMutationRequest(String decisionId, String wireResponse) {
        public TrackerMutationRequest {
            Objects.requireNonNull(decisionId, "decisionId");
            Objects.requireNonNull(wireResponse, "wireResponse");
        }
    }

    public FinalizedResponse {
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(corrections, "corrections");
        corrections = List.copyOf(corrections);
        if (priorRejectionCount < 0) {
            throw new IllegalArgumentException("priorRejectionCount must be >= 0");
        }
        if (status == Status.REJECTED) {
            if (wireResponse != null || trackerMutation != null) {
                throw new IllegalArgumentException("REJECTED carries no wire response and no tracker mutation");
            }
            Objects.requireNonNull(rejection, "REJECTED requires a typed rejection");
        } else {
            Objects.requireNonNull(wireResponse, "non-REJECTED requires a wire response (pass is \"\")");
            Objects.requireNonNull(trackerMutation, "non-REJECTED requires exactly one tracker mutation request");
            if (rejection != null) {
                throw new IllegalArgumentException("only REJECTED carries a rejection");
            }
            if (!wireResponse.equals(trackerMutation.wireResponse())) {
                throw new IllegalArgumentException("tracker mutation must record the exact wire response");
            }
        }
        // m00336 gate P0 #2 INVARIANT, both directions: a FORCED result cannot
        // exist without its typed reason, and no other status may carry one.
        if (status == Status.FORCED) {
            Objects.requireNonNull(forcedChoice, "FORCED requires a typed forced-choice reason");
        } else if (forcedChoice != null) {
            throw new IllegalArgumentException("only FORCED carries a forced-choice reason");
        }
        if (randomDraw != null && status != Status.FORCED) {
            throw new IllegalArgumentException("only FORCED may consume a random draw");
        }
        if (status == Status.CORRECTED && corrections.isEmpty()) {
            throw new IllegalArgumentException("CORRECTED requires at least one typed correction");
        }
        if (status == Status.ACCEPTED && !corrections.isEmpty()) {
            throw new IllegalArgumentException("ACCEPTED must carry no corrections");
        }
    }
}
