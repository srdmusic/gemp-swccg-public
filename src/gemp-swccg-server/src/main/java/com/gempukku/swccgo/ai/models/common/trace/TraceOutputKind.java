package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Operation record"): closed output-kind enum for migrated arms.
 *
 * The three kinds are the registry's own KIND taxonomy
 * (resources/DOMAIN_REGISTRY_2026-07-12.md, per plan §4): VETO / ORDERING / BANDED.
 * Closed set — never a free string.
 *
 * TRACE-V2 GATE P1-4 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): plus exactly TWO
 * explicit identity sentinels that are NOT registry kinds — LEGACY_UNTAGGED for
 * unmigrated legacy arms and COMBINED_EVALUATOR for framework merge/rank/select ops.
 * Operation identity is mandatory on every dimension; null never means anything.
 */
public enum TraceOutputKind {
    /** Sentinel: unmigrated legacy arm (visible debt, never guessed metadata). */
    LEGACY_UNTAGGED,
    /** Sentinel: CombinedEvaluator framework operation (merge/rank/select/synthetic pass). */
    COMBINED_EVALUATOR,
    VETO,
    ORDERING,
    BANDED
}
