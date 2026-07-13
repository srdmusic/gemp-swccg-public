package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Map;
import java.util.Objects;

/**
 * TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", SINGLE_RESPONSE_RECORD): the heuristic-memory write
 * boundary of HeuristicAiBase.updateSingleDecisionLoop(...). The only suppressing guard
 * is the entry guard (null decision/response or an empty currentStateHash, the
 * ThrowingHand suppression law); past it, owned writes always execute on BOTH exits:
 * the empty-response-key path resets the four last-decision fields (an executed write,
 * NO_OP only when they were already reset), and the main path writes the repeat count
 * and last-decision key/response/hash. Any INTERNALLY created local-response block
 * (repeat count reaching the loop threshold) stays FOLDED into this one external
 * boundary event; it is never a seventh top-level mutator event.
 *
 * Payload: the exact decision (type and verbatim text), the raw response, the tracking
 * response (null exactly when the caller passed null; the legacy fold then keys on the
 * raw response), and the complete before/after snapshots, whose localBlockedResponses
 * delta carries the folded block.
 */
public record HeuristicSingleResponseRecordEvent(
    String decisionType,
    String decisionText,
    String rawResponse,
    String trackingResponse,
    HeuristicMemorySnapshot before,
    HeuristicMemorySnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public HeuristicSingleResponseRecordEvent {
        Objects.requireNonNull(decisionType, "decisionType");
        Objects.requireNonNull(decisionText, "decisionText");
        Objects.requireNonNull(rawResponse, "rawResponse");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        if (before.currentStateHash().isEmpty()) {
            throw new IllegalArgumentException(
                "an empty currentStateHash returns before any owned write and emits no event");
        }
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        String responseKey = trackingResponse != null ? trackingResponse : rawResponse;
        if (responseKey.isEmpty()) {
            if (!after.lastDecisionKey().isEmpty() || !after.lastDecisionResponse().isEmpty()
                    || !after.lastDecisionStateHash().isEmpty()
                    || after.lastDecisionRepeatCount() != 0) {
                throw new IllegalArgumentException(
                    "an empty response key resets all four last-decision fields");
            }
            if (!before.localBlockedResponses().equals(after.localBlockedResponses())) {
                throw new IllegalArgumentException(
                    "the empty-response-key path never touches local blocks");
            }
        } else {
            String truncated = decisionText.length() > 60
                ? decisionText.substring(0, 60) : decisionText;
            String key = decisionType + ":" + truncated;
            if (!key.equals(after.lastDecisionKey())
                    || !responseKey.equals(after.lastDecisionResponse())
                    || !before.currentStateHash().equals(after.lastDecisionStateHash())) {
                throw new IllegalArgumentException(
                    "the main path always records this decision key, response key, and state hash");
            }
            boolean sameDecision = key.equals(before.lastDecisionKey())
                    && responseKey.equals(before.lastDecisionResponse())
                    && before.currentStateHash().equals(before.lastDecisionStateHash());
            int expectedRepeat = sameDecision ? before.lastDecisionRepeatCount() + 1 : 1;
            if (after.lastDecisionRepeatCount() != expectedRepeat) {
                throw new IllegalArgumentException("repeat count " + after.lastDecisionRepeatCount()
                    + " must be " + expectedRepeat + " (sameDecision=" + sameDecision + ")");
            }
        }
        if (!frozenRemainder(before).equals(frozenRemainder(after))) {
            throw new IllegalArgumentException("SINGLE_RESPONSE_RECORD may not claim changes"
                + " outside the last-decision fields and the folded local blocks");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static HeuristicSingleResponseRecordEvent of(String decisionType, String decisionText,
                                                        String rawResponse, String trackingResponse,
                                                        HeuristicMemorySnapshot before,
                                                        HeuristicMemorySnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new HeuristicSingleResponseRecordEvent(decisionType, decisionText, rawResponse,
            trackingResponse, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }

    /** Copy with every component this helper may legally write zeroed out. */
    private static HeuristicMemorySnapshot frozenRemainder(HeuristicMemorySnapshot s) {
        return new HeuristicMemorySnapshot(s.currentStateHash(), s.blockStateHash(),
            "", "", "", 0, s.currentTurnNumber(),
            s.lastActionChoiceText(), s.lastActionChoiceCardId(), s.lastActionChoiceBlueprintId(),
            s.failedSearchActionTexts(), s.failedSearchCardIds(), s.failedSearchBlueprintIds(),
            Map.of(), s.recentDecisionResponses(),
            s.recentReassignmentTurns(), s.reassignmentCounts());
    }
}
