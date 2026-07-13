package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4A1, m00372 Option A (accepted m00373; matrix RECORD_RESPONSE row): the
 * outer decision tracker's recordDecision(...) call, observed AFTER the legacy call ran
 * (the matrix correction: the retired hook recorded an intention BEFORE the call).
 *
 * Payload: the call subject (decision type/id, the exact decision key from the
 * tracker's pure package-local traceDecisionKey() seam, and the exact response value
 * the legacy call received) plus the COMPLETE decision-affecting owner snapshot before
 * and after (ordered sequence rows, repeat/loop counts, last-action pair, cancel
 * key/count, canonical turn blocks). The outcome is derived from and constructor-
 * validated against snapshot equality: CHANGED iff before != after. history is excluded
 * by ruling, so a history-only append is honestly NO_OP over the decision-affecting
 * state.
 *
 * TRACE STAGE 4A2b (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md
 * "Authorized implementation shape"): the 4A1 outer-owners-only invariant is
 * INTENTIONALLY expanded: HEURISTIC_SHARED is now accepted for the one inherited
 * HeuristicAiBase recordDecision(...) call, REUSING this record instead of minting a
 * duplicate. The shared call records the heuristic trackingResponse while the outer
 * call later records the bot's final result, so the two owners' response values can
 * legitimately differ on one decision; both records are real current behavior and are
 * never coalesced. The internal cancel-block helper recordDecision(...) can invoke
 * stays FOLDED into this single event (no nested BLOCK_RESPONSE).
 */
public record TrackerRecordResponseEvent(
    TrackerOwner owner,
    String decisionType,
    String decisionId,
    String decisionKey,
    String response,
    DecisionTrackerSnapshot before,
    DecisionTrackerSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public TrackerRecordResponseEvent {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(decisionType, "decisionType");
        Objects.requireNonNull(decisionId, "decisionId");
        Objects.requireNonNull(decisionKey, "decisionKey");
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static TrackerRecordResponseEvent of(TrackerOwner owner, String decisionType,
                                                String decisionId, String decisionKey,
                                                String response,
                                                DecisionTrackerSnapshot before,
                                                DecisionTrackerSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new TrackerRecordResponseEvent(owner, decisionType, decisionId, decisionKey,
            response, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
