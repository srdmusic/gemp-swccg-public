package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Operation record"): closed typed domain identity for migrated arms.
 *
 * Constants are EXACTLY the semantic domains of the authoritative registry
 * (resources/DOMAIN_REGISTRY_2026-07-12.md §1 "Domain overview" — one owner per domain
 * is the migration law). A new constant is added ONLY when the registry adds a domain;
 * this enum is a closed set, never a string escape hatch, and identity is never parsed
 * from reasoning prose.
 */
public enum TraceDomainId {
    SETUP_STARTING,
    ACTIVATION_AMOUNT,
    FORCE_BUDGET,
    DRAIN_CONTROL,
    DEPLOY_SEQUENCING,
    DEPLOY_SITING,
    DEPLOY_ATTACH,
    SOLO_FORMATION,
    BATTLE_INITIATION,
    BATTLE_WEAPONS,
    BATTLE_FORFEIT,
    MOVE,
    DRAW_COUNT,
    FORCE_LOSS_PAYMENT,
    SHIELDS,
    PULL_SEARCH,
    OBJECTIVE_INTENT,
    LOOP_SAFETY,
    PASS_CANCEL,
    RESPONSE_ROUTING,
    DECK_PLAYBOOK,
    FACT_SERVICES
}
