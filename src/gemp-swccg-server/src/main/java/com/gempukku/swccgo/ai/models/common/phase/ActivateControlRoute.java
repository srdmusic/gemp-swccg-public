package com.gempukku.swccgo.ai.models.common.phase;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: ACTIVATE/CONTROL OPTION 2 / LIVE ROUTE (2026-07-13) ═══
// Packet: Handoffs/CODEX_ACTIVATE_CONTROL_PHASE_B_PACKET_2026-07-13.md §1.
//
// The closed set of routes the pure shadow resolver returns. Six OWNED routes plus
// LEGACY_UNOWNED, which is the deliberate bypass for any decision this phase does
// not own (absent/unknown origin, wrong phase, or wrong wire shape). The two
// production bot adapters consume these routes after the existing chaos gate.
// ═══════════════════════════════════════════════════════════
public enum ActivateControlRoute {
    ACTIVATE_TOP_LEVEL,
    ACTIVATE_AMOUNT,
    ACTIVATE_ALLOWANCE,
    ACTIVATE_ZERO_CONFIRM,
    ACTIVATE_ACK,
    CONTROL_TOP_LEVEL,
    LEGACY_UNOWNED
}
