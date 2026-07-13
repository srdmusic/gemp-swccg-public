package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", ACTION_CHOICE_REMEMBER): the heuristic-memory write
 * boundary of HeuristicAiBase.updateLastActionChoiceText(...). Guard returns (wrong
 * decision type, empty result, missing action texts, unparseable result, out-of-range
 * index) emit nothing; once the in-range branch is entered, the three tuple assignments
 * always execute, so exactly one event exists per executed write when a session is
 * active. Rewriting an identical tuple is a real NO_OP, never event suppression.
 *
 * Payload: the exact decision type, result, and parsed index, plus the prior/new
 * lastActionChoice tuple carried by the complete before/after snapshots. Sentinel
 * blueprint values "inplay" and "rules" become empty strings exactly as the legacy
 * code folds them, so no after snapshot may carry a sentinel blueprint id.
 */
public record HeuristicActionChoiceRememberEvent(
    String decisionType,
    String result,
    int index,
    HeuristicMemorySnapshot before,
    HeuristicMemorySnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public HeuristicActionChoiceRememberEvent {
        Objects.requireNonNull(decisionType, "decisionType");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        if (!"ACTION_CHOICE".equals(decisionType) && !"CARD_ACTION_CHOICE".equals(decisionType)) {
            throw new IllegalArgumentException(
                "the legacy guard admits only ACTION_CHOICE and CARD_ACTION_CHOICE, got "
                    + decisionType);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("the legacy guard rejects an empty result");
        }
        int parsed;
        try {
            parsed = Integer.parseInt(result);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "an unparseable result never reaches the write boundary: " + result);
        }
        if (parsed != index || index < 0) {
            throw new IllegalArgumentException("index " + index
                + " must be the non-negative parse of the result " + result);
        }
        if ("inplay".equals(after.lastActionChoiceBlueprintId())
                || "rules".equals(after.lastActionChoiceBlueprintId())) {
            throw new IllegalArgumentException(
                "sentinel blueprint values become empty strings exactly as legacy code does, got "
                    + after.lastActionChoiceBlueprintId());
        }
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (!frozenRemainder(before).equals(frozenRemainder(after))) {
            throw new IllegalArgumentException(
                "ACTION_CHOICE_REMEMBER may not claim changes outside the lastActionChoice tuple");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static HeuristicActionChoiceRememberEvent of(String decisionType, String result,
                                                        int index,
                                                        HeuristicMemorySnapshot before,
                                                        HeuristicMemorySnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new HeuristicActionChoiceRememberEvent(decisionType, result, index, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }

    /** Copy with the three tuple components this helper may legally write zeroed out. */
    private static HeuristicMemorySnapshot frozenRemainder(HeuristicMemorySnapshot s) {
        return new HeuristicMemorySnapshot(s.currentStateHash(), s.blockStateHash(),
            s.lastDecisionStateHash(), s.lastDecisionKey(), s.lastDecisionResponse(),
            s.lastDecisionRepeatCount(), s.currentTurnNumber(),
            "", "", "",
            s.failedSearchActionTexts(), s.failedSearchCardIds(), s.failedSearchBlueprintIds(),
            s.localBlockedResponses(), s.recentDecisionResponses(),
            s.recentReassignmentTurns(), s.reassignmentCounts());
    }
}
