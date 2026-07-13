package com.gempukku.swccgo.ai.models.common.trace;

import java.util.Objects;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "State-event observation"): one typed AI-side mutation event OBSERVED during the one
 * legacy run. The trace observes the legacy mutation; it never applies a second event and
 * never calls decisionMade.
 *
 * Increment 2 records only the OUTER decide() choke points (decision-tracker record,
 * strategic-event record, pending-concede set). The inner choke points (heuristic
 * tracker, objective/deck/strategy refresh, deploy-plan lifecycle, retry budgets,
 * barrier memory, move state, opponent intelligence) are the contract's landing
 * increment 4 and remain deferred — their Kind constants exist so the envelope shape
 * is stable when those hooks land.
 */
public record TraceIntendedStateEvent(Kind kind, String detail) {

    public enum Kind {
        DECISION_TRACKER_RECORD,
        HEURISTIC_TRACKER_RECORD,
        STRATEGY_REFRESH,
        OBJECTIVE_REFRESH,
        DECK_ORACLE_REFRESH,
        DEPLOY_PLAN_EVENT,
        RETRY_BUDGET,
        BARRIER_MEMORY,
        MOVE_STATE,
        OPPONENT_INTEL,
        STRATEGIC_EVENT_RECORD,
        PENDING_CONCEDE
    }

    public TraceIntendedStateEvent {
        Objects.requireNonNull(kind, "kind");
    }
}
