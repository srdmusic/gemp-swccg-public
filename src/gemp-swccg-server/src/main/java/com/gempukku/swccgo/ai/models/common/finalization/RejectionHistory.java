package com.gempukku.swccgo.ai.models.common.finalization;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FINALIZER LANE F3 / REJECTION HISTORY (2026-07-13) ═══
// Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md §F3 —
// "Rejection remains typed data. F3 does not perform retries itself."
// Audit seam: finalize(..., RejectionHistory history) — the audit's required
// replacement for the V163/V169 additive retry counters ("Typed rejection
// history plus explicit assessment", audit P0 #4).
//
// Immutable, ordered record of the wire responses already REJECTED for the SAME
// decision. The finalizer consumes it as data (stamped into FinalizedResponse
// so a trace can reconstruct the retry position); the bounded-retry OWNER — the
// F2 mediator increment, an engine change held for Steve's explicit approval —
// is the only party that appends to it and decides when to stop.
// ═══════════════════════════════════════════════════════════
public record RejectionHistory(List<Attempt> attempts) {

    /** One rejected attempt: the exact wire response and the typed reason. */
    public record Attempt(String wireResponse, FinalizedResponse.RejectReason reason, String detail) {
        public Attempt {
            Objects.requireNonNull(wireResponse, "wireResponse");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
        }
    }

    public RejectionHistory {
        Objects.requireNonNull(attempts, "attempts");
        attempts = List.copyOf(attempts);
    }

    public static RejectionHistory empty() {
        return new RejectionHistory(List.of());
    }

    /**
     * FINALIZER RUNTIME (2026-07-13,
     * Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md §2): the one
     * immutable append. Returns a NEW history with the rejected attempt added; never mutates
     * the existing list. Detail must be nonblank. The F2 mediator retry loop is the only
     * caller: it appends the exact returned wire, {@code ENGINE_DECISION_INVALID}, and the
     * engine's nonblank warning after a checked rejection, so the single retry receives
     * count 1 carrying the first rejected wire.
     */
    public RejectionHistory append(String wireResponse, FinalizedResponse.RejectReason reason,
                                   String detail) {
        Objects.requireNonNull(wireResponse, "wireResponse");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(detail, "detail");
        if (detail.isBlank()) {
            throw new IllegalArgumentException("append detail must be nonblank");
        }
        List<Attempt> next = new ArrayList<>(attempts);
        next.add(new Attempt(wireResponse, reason, detail));
        return new RejectionHistory(next);
    }

    public int size() {
        return attempts.size();
    }

    /** Whether this exact wire response was already rejected for this decision. */
    public boolean containsWire(String wireResponse) {
        for (Attempt attempt : attempts) {
            if (attempt.wireResponse().equals(wireResponse)) {
                return true;
            }
        }
        return false;
    }
}
