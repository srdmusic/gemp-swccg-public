package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md):
 * completeness status of one emitted DecisionTrace envelope.
 *
 * Every swallowed capture error marks the record INCOMPLETE (with an ordered typed
 * TraceCaptureFailure list). Silent truncation is forbidden: an INCOMPLETE trace must
 * never compare as authoritative fixture evidence, and a strict fixture sink fails on it.
 */
public enum TraceStatus {
    COMPLETE,
    INCOMPLETE
}
