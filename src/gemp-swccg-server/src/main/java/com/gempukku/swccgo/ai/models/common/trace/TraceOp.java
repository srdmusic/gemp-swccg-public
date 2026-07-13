package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Operation record"): operation kinds for the append-only per-decision trace.
 *
 * INITIAL/ADD/SET/HARD_VETO/MERGE are recorded by EvaluatedAction's existing score/veto
 * choke points. RANK/SELECT are recorded by CombinedEvaluator (bucket walk, epilogues,
 * winner). The former FINALIZE op is gone: finalization data (pre-safety winner, pass
 * eligibility, corrections, final response) lives in the envelope's typed
 * TraceFinalization record, because CombinedEvaluator's selected action is explicitly
 * NOT the AI's final answer.
 */
public enum TraceOp {
    INITIAL,
    ADD,
    SET,
    HARD_VETO,
    MERGE,
    RANK,
    SELECT
}
