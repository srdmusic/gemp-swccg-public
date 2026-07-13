package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4A2b (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md
 * "Payload ownership", BLOCK_RESPONSE): the one DIRECT inherited shared-tracker
 * blockLastActionOnCancel(...) call at the HeuristicAiBase.decide(...) empty
 * CARD_SELECTION/ARBITRARY_CARDS boundary, observed AFTER the legacy call ran. Exactly
 * one event per direct legacy call when a session is active. The INTERNAL
 * blockLastActionOnCancel(...) call inside DecisionTracker.recordDecision(...) (the
 * three-cancel streak) stays FOLDED into that call's single RECORD_RESPONSE event per
 * the packet's one-call/one-event law and receives no nested hook.
 *
 * Payload: the exact call subject (decisionType, decisionText verbatim from the call
 * arguments, never reconstructed), the EXACT boolean the legacy call returned, and the
 * complete decision-affecting before/after snapshots, which carry the last-action
 * key/response the block consumes and the canonical turn-block rows it writes. The
 * outcome is derived from and constructor-validated against snapshot equality, and the
 * legacy return is a FULL BICONDITIONAL with the outcome (reviewer m00441): a
 * successful block always clears the last-action pair and writes a turn-block row
 * (true never pairs with NO_OP), and every false return exits before any mutation
 * (false never pairs with CHANGED).
 *
 * OWNER IS FIXED to HEURISTIC_SHARED: the only reachable external
 * blockLastActionOnCancel(...) call in production is the inherited HeuristicAiBase
 * tracker path; neither outer bot calls it on its outer tracker.
 */
public record TrackerBlockResponseEvent(
    TrackerOwner owner,
    String decisionType,
    String decisionText,
    boolean blocked,
    DecisionTrackerSnapshot before,
    DecisionTrackerSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public TrackerBlockResponseEvent {
        Objects.requireNonNull(owner, "owner");
        if (owner != TrackerOwner.HEURISTIC_SHARED) {
            throw new IllegalArgumentException(
                "BLOCK_RESPONSE observes the inherited HeuristicAiBase tracker only; no outer"
                    + " tracker has a reachable external blockLastActionOnCancel(...) call"
                    + " (owner was " + owner + ")");
        }
        Objects.requireNonNull(decisionType, "decisionType");
        Objects.requireNonNull(decisionText, "decisionText");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (blocked != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("legacy return " + blocked
                + " contradicts outcome " + outcome
                + ": a successful block always mutates (clears the last-action pair and"
                + " writes a turn-block row) and every false return exits before any mutation");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static TrackerBlockResponseEvent of(TrackerOwner owner, String decisionType,
                                               String decisionText, boolean blocked,
                                               DecisionTrackerSnapshot before,
                                               DecisionTrackerSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new TrackerBlockResponseEvent(owner, decisionType, decisionText, blocked,
            before, after, changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
