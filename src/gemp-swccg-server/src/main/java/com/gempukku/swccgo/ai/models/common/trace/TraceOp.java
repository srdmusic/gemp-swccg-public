package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE HOOK (2026-07-13, Handoffs/CODEX_MINIMAL_DECISION_TRACE_HOOK_2026-07-13.md):
 * operation kinds for the append-only per-decision trace.
 *
 * INITIAL/ADD/SET/HARD_VETO/MERGE are recorded by EvaluatedAction's existing score/veto
 * choke points. RANK/SELECT are recorded by CombinedEvaluator (bucket walk, epilogues,
 * winner). FINALIZE marks a finalization boundary; in this increment that is
 * CombinedEvaluator's pre-final winner. The router (route id, frozen input, fallback path)
 * and DecisionSafety (final response, correction reason) are the NEXT increment: they
 * reuse SELECT/FINALIZE ops with their own evaluatorId/ruleId/detail values, so no schema
 * change is needed to record them later.
 */
public enum TraceOp {
    INITIAL,
    ADD,
    SET,
    HARD_VETO,
    MERGE,
    RANK,
    SELECT,
    FINALIZE
}
