package com.gempukku.swccgo.ai.models.common.trace.state;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table"): the mirrored StrategyController lives once per bot, so
 * its observed owner is one of exactly two closed scopes. An event carries the owner
 * whose controller state it observed; the two are never merged in the trace.
 *
 * This is a DISTINCT owner enum from TrackerOwner by design (the decision trackers and
 * the strategy controllers are separate owners); it is never reused for tracker events
 * and never expanded to a shared scope, because there is no shared StrategyController.
 */
public enum StrategyControllerOwner {
    RANDO,
    CHOSENONE
}
