package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", RECENT_RESPONSE_APPEND): the heuristic-memory write
 * boundary of HeuristicAiBase.recordRecentDecisionResponse(...). Guard returns (null
 * decision, empty tracking response, empty currentStateHash, wrong decision type) emit
 * nothing; past the guards the ordered deque append always executes, preceded by
 * map-entry creation when the key is new and followed by FIFO eviction past six
 * entries, all inside this one event.
 *
 * Payload: the exact decision key (the legacy helper's own key, never reconstructed),
 * the appended response, the evicted rows, the ordered deque before/after for that key
 * (all derived from the snapshots), and the complete before/after snapshots. A deque of
 * identical entries evicting into an identical deque is a real NO_OP with a real
 * evicted row, never event suppression.
 */
public record HeuristicRecentResponseAppendEvent(
    String decisionKey,
    String appendedResponse,
    List<String> dequeBefore,
    List<String> dequeAfter,
    List<String> evictedRows,
    HeuristicMemorySnapshot before,
    HeuristicMemorySnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public HeuristicRecentResponseAppendEvent {
        Objects.requireNonNull(decisionKey, "decisionKey");
        Objects.requireNonNull(appendedResponse, "appendedResponse");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        if (appendedResponse.isEmpty()) {
            throw new IllegalArgumentException(
                "the legacy guard rejects an empty tracking response");
        }
        if (before.currentStateHash().isEmpty()) {
            throw new IllegalArgumentException(
                "an empty currentStateHash returns before any owned write and emits no event");
        }
        dequeBefore = frozenCopy(dequeBefore, "dequeBefore");
        dequeAfter = frozenCopy(dequeAfter, "dequeAfter");
        evictedRows = frozenCopy(evictedRows, "evictedRows");
        List<String> beforeInMap = before.recentDecisionResponses().get(decisionKey);
        if (!dequeBefore.equals(beforeInMap != null ? beforeInMap : List.of())) {
            throw new IllegalArgumentException("dequeBefore " + dequeBefore
                + " inconsistent with the before snapshot entry for " + decisionKey);
        }
        if (!dequeAfter.equals(after.recentDecisionResponses().get(decisionKey))) {
            throw new IllegalArgumentException("dequeAfter " + dequeAfter
                + " inconsistent with the after snapshot entry for " + decisionKey);
        }
        if (dequeAfter.isEmpty()
                || !appendedResponse.equals(dequeAfter.get(dequeAfter.size() - 1))) {
            throw new IllegalArgumentException(
                "the appended response must be the last ordered deque entry");
        }
        if (evictedRows.size() + dequeAfter.size() != dequeBefore.size() + 1) {
            throw new IllegalArgumentException("eviction arithmetic broken: " + evictedRows.size()
                + " evicted + " + dequeAfter.size() + " kept != " + dequeBefore.size() + " + 1");
        }
        List<String> reconstructed = new ArrayList<>(evictedRows);
        reconstructed.addAll(dequeAfter.subList(0, dequeAfter.size() - 1));
        if (!reconstructed.equals(dequeBefore)) {
            throw new IllegalArgumentException(
                "evicted rows must be the exact FIFO prefix of the before deque");
        }
        if (!mapWithoutKey(before.recentDecisionResponses(), decisionKey)
                .equals(mapWithoutKey(after.recentDecisionResponses(), decisionKey))) {
            throw new IllegalArgumentException(
                "this boundary touches only its own decision key's deque");
        }
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (!frozenRemainder(before).equals(frozenRemainder(after))) {
            throw new IllegalArgumentException(
                "RECENT_RESPONSE_APPEND may not claim changes outside recentDecisionResponses");
        }
    }

    /** Factory used by the recording choke point: derives deques, evictions, and outcome. */
    public static HeuristicRecentResponseAppendEvent of(String decisionKey, String appendedResponse,
                                                        HeuristicMemorySnapshot before,
                                                        HeuristicMemorySnapshot after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(decisionKey, "decisionKey");
        List<String> dequeBefore = before.recentDecisionResponses().get(decisionKey);
        if (dequeBefore == null) {
            dequeBefore = List.of();
        }
        List<String> dequeAfter = after.recentDecisionResponses().get(decisionKey);
        if (dequeAfter == null) {
            throw new IllegalArgumentException(
                "the executed append must leave a deque for " + decisionKey);
        }
        int evictedCount = dequeBefore.size() + 1 - dequeAfter.size();
        if (evictedCount < 0 || evictedCount > dequeBefore.size()) {
            throw new IllegalArgumentException("eviction arithmetic broken for " + decisionKey
                + ": before " + dequeBefore.size() + ", after " + dequeAfter.size());
        }
        List<String> evicted = new ArrayList<>(dequeBefore.subList(0, evictedCount));
        boolean changed = !before.equals(after);
        return new HeuristicRecentResponseAppendEvent(decisionKey, appendedResponse,
            dequeBefore, dequeAfter, evicted, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }

    private static List<String> frozenCopy(List<String> values, String name) {
        List<String> copy = new ArrayList<>(Objects.requireNonNull(values, name));
        for (String value : copy) {
            Objects.requireNonNull(value, name + " element");
        }
        return Collections.unmodifiableList(copy);
    }

    private static Map<String, List<String>> mapWithoutKey(Map<String, List<String>> map,
                                                           String key) {
        TreeMap<String, List<String>> copy = new TreeMap<>(map);
        copy.remove(key);
        return copy;
    }

    /** Copy with the one component this helper may legally write zeroed out. */
    private static HeuristicMemorySnapshot frozenRemainder(HeuristicMemorySnapshot s) {
        return new HeuristicMemorySnapshot(s.currentStateHash(), s.blockStateHash(),
            s.lastDecisionStateHash(), s.lastDecisionKey(), s.lastDecisionResponse(),
            s.lastDecisionRepeatCount(), s.currentTurnNumber(),
            s.lastActionChoiceText(), s.lastActionChoiceCardId(), s.lastActionChoiceBlueprintId(),
            s.failedSearchActionTexts(), s.failedSearchCardIds(), s.failedSearchBlueprintIds(),
            s.localBlockedResponses(), Map.of(),
            s.recentReassignmentTurns(), s.reassignmentCounts());
    }
}
