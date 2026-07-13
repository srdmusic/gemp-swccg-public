package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", REASSIGNMENT_RECORD): the heuristic-memory write
 * boundary of HeuristicAiBase.recordRecentReassignment(...). Guard returns (nulls,
 * empty response, no current turn, wrong decision type, pass response, unparseable
 * index, non-reassignment action, empty key) emit nothing; once the non-empty key
 * branch is entered, the recentReassignmentTurns put and the reassignmentCounts
 * increment always execute together, both FOLDED into this one event. The nested count
 * is never a separate event.
 *
 * Payload: the closed key variant with the exact legacy map key (precedence is card,
 * then non-sentinel blueprint, then extracted text), the recorded turn, and both map
 * deltas (prior/new turn row and prior/new count, derived from the snapshots), plus
 * the complete before/after snapshots.
 */
public record HeuristicReassignmentRecordEvent(
    Variant variant,
    String key,
    int turn,
    Integer turnBefore,
    int turnAfter,
    Integer countBefore,
    int countAfter,
    HeuristicMemorySnapshot before,
    HeuristicMemorySnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    /** Closed key variants in legacy precedence order: card, blueprint, then text. */
    public enum Variant {
        CARD("card:"),
        BLUEPRINT("blueprint:"),
        TEXT("text:");

        private final String prefix;

        Variant(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }

        static Variant fromKey(String key) {
            Objects.requireNonNull(key, "key");
            for (Variant variant : values()) {
                if (key.startsWith(variant.prefix) && key.length() > variant.prefix.length()) {
                    return variant;
                }
            }
            throw new IllegalArgumentException(
                "key must carry a closed variant prefix with a non-empty value, got " + key);
        }
    }

    public HeuristicReassignmentRecordEvent {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        if (Variant.fromKey(key) != variant) {
            throw new IllegalArgumentException("variant " + variant
                + " inconsistent with the key prefix of " + key);
        }
        if (turn <= 0) {
            throw new IllegalArgumentException(
                "the legacy guard requires a positive current turn, got " + turn);
        }
        if (before.currentTurnNumber() != turn || after.currentTurnNumber() != turn) {
            throw new IllegalArgumentException(
                "this helper never writes currentTurnNumber; both snapshots must carry " + turn);
        }
        if (turnAfter != turn) {
            throw new IllegalArgumentException("the put always records the current turn "
                + turn + ", got " + turnAfter);
        }
        if (!Objects.equals(turnBefore, before.recentReassignmentTurns().get(key))
                || !Integer.valueOf(turnAfter).equals(after.recentReassignmentTurns().get(key))) {
            throw new IllegalArgumentException(
                "turn delta inconsistent with the snapshots for " + key);
        }
        if (!Objects.equals(countBefore, before.reassignmentCounts().get(key))
                || !Integer.valueOf(countAfter).equals(after.reassignmentCounts().get(key))) {
            throw new IllegalArgumentException(
                "count delta inconsistent with the snapshots for " + key);
        }
        int expectedCount = countBefore == null ? 1 : countBefore + 1;
        if (countAfter != expectedCount) {
            throw new IllegalArgumentException("the folded increment must move the count to "
                + expectedCount + ", got " + countAfter);
        }
        if (!mapWithoutKey(before.recentReassignmentTurns(), key)
                .equals(mapWithoutKey(after.recentReassignmentTurns(), key))
                || !mapWithoutKey(before.reassignmentCounts(), key)
                    .equals(mapWithoutKey(after.reassignmentCounts(), key))) {
            throw new IllegalArgumentException(
                "this boundary touches only its own key in the two reassignment maps");
        }
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (!frozenRemainder(before).equals(frozenRemainder(after))) {
            throw new IllegalArgumentException(
                "REASSIGNMENT_RECORD may not claim changes outside the two reassignment maps");
        }
    }

    /** Factory used by the recording choke point: derives variant, deltas, and outcome. */
    public static HeuristicReassignmentRecordEvent of(String key, int turn,
                                                      HeuristicMemorySnapshot before,
                                                      HeuristicMemorySnapshot after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Integer turnAfter = after.recentReassignmentTurns().get(key);
        Integer countAfter = after.reassignmentCounts().get(key);
        if (turnAfter == null || countAfter == null) {
            throw new IllegalArgumentException(
                "the executed writes must leave both map rows for " + key);
        }
        boolean changed = !before.equals(after);
        return new HeuristicReassignmentRecordEvent(Variant.fromKey(key), key, turn,
            before.recentReassignmentTurns().get(key), turnAfter,
            before.reassignmentCounts().get(key), countAfter,
            before, after, changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }

    private static Map<String, Integer> mapWithoutKey(Map<String, Integer> map, String key) {
        TreeMap<String, Integer> copy = new TreeMap<>(map);
        copy.remove(key);
        return copy;
    }

    /** Copy with the two reassignment maps this helper may legally write zeroed out. */
    private static HeuristicMemorySnapshot frozenRemainder(HeuristicMemorySnapshot s) {
        return new HeuristicMemorySnapshot(s.currentStateHash(), s.blockStateHash(),
            s.lastDecisionStateHash(), s.lastDecisionKey(), s.lastDecisionResponse(),
            s.lastDecisionRepeatCount(), s.currentTurnNumber(),
            s.lastActionChoiceText(), s.lastActionChoiceCardId(), s.lastActionChoiceBlueprintId(),
            s.failedSearchActionTexts(), s.failedSearchCardIds(), s.failedSearchBlueprintIds(),
            s.localBlockedResponses(), s.recentDecisionResponses(),
            Map.of(), Map.of());
    }
}
