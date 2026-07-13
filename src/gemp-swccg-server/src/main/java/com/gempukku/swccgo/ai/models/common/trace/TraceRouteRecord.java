package com.gempukku.swccgo.ai.models.common.trace;

import java.util.List;
import java.util.Objects;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Route record"): one selected typed route id plus the ORDERED route evidence and the
 * bypass/fallback reason.
 *
 * Route evidence entries are derived from frozen input facts only (decision type, text
 * predicates, obligation flags) — never a score, assessment, winner, or mutable service
 * result. When control fell through lanes (e.g. evaluator lane replaced by raw-noPass
 * emergency), every observation stays in orderedEvidence and `selected` is the lane that
 * actually produced the response; the earlier lane is named in bypassOrFallbackReason.
 */
public record TraceRouteRecord(TraceRoute selected, List<String> orderedEvidence,
                               String bypassOrFallbackReason) {

    public TraceRouteRecord {
        Objects.requireNonNull(selected, "selected");
        Objects.requireNonNull(orderedEvidence, "orderedEvidence");
        orderedEvidence = List.copyOf(orderedEvidence);
    }
}
