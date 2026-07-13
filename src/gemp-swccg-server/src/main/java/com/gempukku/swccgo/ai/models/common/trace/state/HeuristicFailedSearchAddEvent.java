package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", FAILED_SEARCH_ADD): the heuristic-memory write boundary
 * of HeuristicAiBase.handleFailedSearchVerification(...). A failed verification match
 * emits nothing; a throwing reserve-deck getter propagates uncaught exactly as legacy
 * code does, before any capture, so no event and no state change exist on that path.
 * Once the exact unsuccessful-search verification matches, every non-empty prior-action
 * identity is added to its membership set; when all three identities are empty no owned
 * write executes and no event is emitted. Repeated additions are a real NO_OP.
 *
 * Payload: the exact prior action tuple whose available identities were added, the
 * complete sorted per-set deltas (derived from the snapshots: each delta is either
 * empty or exactly its prior identity), and the complete before/after snapshots. These
 * are memberships only, no counters (4A0 matrix correction).
 */
public record HeuristicFailedSearchAddEvent(
    String priorActionText,
    String priorCardId,
    String priorBlueprintId,
    List<String> addedActionTexts,
    List<String> addedCardIds,
    List<String> addedBlueprintIds,
    HeuristicMemorySnapshot before,
    HeuristicMemorySnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public HeuristicFailedSearchAddEvent {
        Objects.requireNonNull(priorActionText, "priorActionText");
        Objects.requireNonNull(priorCardId, "priorCardId");
        Objects.requireNonNull(priorBlueprintId, "priorBlueprintId");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        if (priorActionText.isEmpty() && priorCardId.isEmpty() && priorBlueprintId.isEmpty()) {
            throw new IllegalArgumentException(
                "an all-empty prior action tuple executes no add and emits no event");
        }
        addedActionTexts = validatedDelta(priorActionText,
            before.failedSearchActionTexts(), after.failedSearchActionTexts(),
            addedActionTexts, "addedActionTexts");
        addedCardIds = validatedDelta(priorCardId,
            before.failedSearchCardIds(), after.failedSearchCardIds(),
            addedCardIds, "addedCardIds");
        addedBlueprintIds = validatedDelta(priorBlueprintId,
            before.failedSearchBlueprintIds(), after.failedSearchBlueprintIds(),
            addedBlueprintIds, "addedBlueprintIds");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (!frozenRemainder(before).equals(frozenRemainder(after))) {
            throw new IllegalArgumentException(
                "FAILED_SEARCH_ADD may not claim changes outside the three failed-search sets");
        }
    }

    /** Factory used by the recording choke point: derives the deltas and the outcome. */
    public static HeuristicFailedSearchAddEvent of(String priorActionText, String priorCardId,
                                                   String priorBlueprintId,
                                                   HeuristicMemorySnapshot before,
                                                   HeuristicMemorySnapshot after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        boolean changed = !before.equals(after);
        return new HeuristicFailedSearchAddEvent(priorActionText, priorCardId, priorBlueprintId,
            sortedDiff(before.failedSearchActionTexts(), after.failedSearchActionTexts()),
            sortedDiff(before.failedSearchCardIds(), after.failedSearchCardIds()),
            sortedDiff(before.failedSearchBlueprintIds(), after.failedSearchBlueprintIds()),
            before, after, changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }

    private static List<String> sortedDiff(List<String> beforeSet, List<String> afterSet) {
        List<String> added = new ArrayList<>();
        for (String value : afterSet) {
            if (!beforeSet.contains(value)) {
                added.add(value);
            }
        }
        Collections.sort(added);
        return added;
    }

    /** One membership set: the delta must be exactly the executed add, nothing else. */
    private static List<String> validatedDelta(String priorValue, List<String> beforeSet,
                                               List<String> afterSet, List<String> claimed,
                                               String name) {
        Objects.requireNonNull(claimed, name);
        if (!afterSet.containsAll(beforeSet)) {
            throw new IllegalArgumentException(
                "failed-search sets only grow at this boundary (" + name + ")");
        }
        List<String> derived = sortedDiff(beforeSet, afterSet);
        if (!derived.equals(claimed)) {
            throw new IllegalArgumentException(name + " " + claimed
                + " inconsistent with the before/after set delta " + derived);
        }
        if (priorValue.isEmpty()) {
            if (!derived.isEmpty()) {
                throw new IllegalArgumentException(
                    "an empty prior identity executes no add, but " + name + " is " + derived);
            }
        } else {
            if (!afterSet.contains(priorValue)) {
                throw new IllegalArgumentException("the executed add must leave " + priorValue
                    + " a member of the after set (" + name + ")");
            }
            if (!derived.isEmpty() && !derived.equals(List.of(priorValue))) {
                throw new IllegalArgumentException(name + " may contain only the prior identity "
                    + priorValue + ", got " + derived);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(claimed));
    }

    /** Copy with the three membership sets this helper may legally write zeroed out. */
    private static HeuristicMemorySnapshot frozenRemainder(HeuristicMemorySnapshot s) {
        return new HeuristicMemorySnapshot(s.currentStateHash(), s.blockStateHash(),
            s.lastDecisionStateHash(), s.lastDecisionKey(), s.lastDecisionResponse(),
            s.lastDecisionRepeatCount(), s.currentTurnNumber(),
            s.lastActionChoiceText(), s.lastActionChoiceCardId(), s.lastActionChoiceBlueprintId(),
            List.of(), List.of(), List.of(),
            s.localBlockedResponses(), s.recentDecisionResponses(),
            s.recentReassignmentTurns(), s.reassignmentCounts());
    }
}
