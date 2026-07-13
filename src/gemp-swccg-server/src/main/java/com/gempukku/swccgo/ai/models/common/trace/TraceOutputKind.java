package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Operation record"): closed output-kind enum for migrated arms.
 *
 * The three kinds are the registry's own KIND taxonomy
 * (resources/DOMAIN_REGISTRY_2026-07-12.md, per plan §4): VETO / ORDERING / BANDED.
 * Closed set — never a free string.
 */
public enum TraceOutputKind {
    VETO,
    ORDERING,
    BANDED
}
