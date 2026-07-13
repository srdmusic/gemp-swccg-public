package com.gempukku.swccgo.ai.models.common.finalization;

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
