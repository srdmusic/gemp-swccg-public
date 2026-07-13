package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", STATE_UPDATE): the heuristic-memory write boundary of
 * HeuristicAiBase.updateDecisionTrackerState(...), observed as one event at helper exit.
 * A guard return (null game/player, or a caught game-state getter throw) emits nothing;
 * once the guards pass, the turn and state-hash assignments always execute, so exactly
 * one event exists per completed helper call when a session is active.
 *
 * Payload: the exact five state-read values (the same legacy arguments the nested
 * 4A2b-owned shared decisionTracker.updateState(...) call receives; that nested call
 * stays a SEPARATE TrackerUpdateStateEvent recorded first, never coalesced), the
 * complete before/after heuristic snapshots, and the exact pruned
 * recentReassignmentTurns rows. Turn rollback clears BOTH reassignment maps and makes
 * pruning a no-op on the emptied map; normal advance prunes only expired
 * recentReassignmentTurns rows while reassignmentCounts persists (4A0 matrix
 * correction). A state-hash change clears local blocks, recent responses, and the
 * repeat count only; failed-search sets, the action tuple, and the last-decision
 * key/response/hash are retained (the packet's reused-controller retention rule).
 */
public record HeuristicStateUpdateEvent(
    int handSize,
    int forcePile,
    int reserveDeck,
    int turn,
    int cardsInPlay,
    Map<String, Integer> prunedReassignmentTurns,
    HeuristicMemorySnapshot before,
    HeuristicMemorySnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public HeuristicStateUpdateEvent {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(prunedReassignmentTurns, "prunedReassignmentTurns");
        if (handSize < 0 || forcePile < 0 || reserveDeck < 0 || turn < 0 || cardsInPlay < 0) {
            throw new IllegalArgumentException("state-read values must be >= 0: "
                + handSize + ":" + forcePile + ":" + reserveDeck + ":" + turn + ":" + cardsInPlay);
        }
        TreeMap<String, Integer> prunedCopy = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : prunedReassignmentTurns.entrySet()) {
            prunedCopy.put(Objects.requireNonNull(entry.getKey(), "pruned key"),
                Objects.requireNonNull(entry.getValue(), "pruned value"));
        }
        prunedReassignmentTurns = Collections.unmodifiableMap(prunedCopy);
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        // the helper always leaves currentTurnNumber, currentStateHash, and
        // blockStateHash at these exact values on every completed call
        if (after.currentTurnNumber() != turn) {
            throw new IllegalArgumentException("after.currentTurnNumber "
                + after.currentTurnNumber() + " must equal the turn argument " + turn);
        }
        String expectedHash = handSize + ":" + forcePile + ":" + reserveDeck + ":" + turn
            + ":" + cardsInPlay;
        if (!expectedHash.equals(after.currentStateHash())) {
            throw new IllegalArgumentException("after.currentStateHash "
                + after.currentStateHash() + " must equal the joined state reads " + expectedHash);
        }
        if (!after.blockStateHash().equals(after.currentStateHash())) {
            throw new IllegalArgumentException(
                "the helper always leaves blockStateHash equal to currentStateHash, got "
                    + after.blockStateHash() + " vs " + after.currentStateHash());
        }
        boolean rollback = turn < before.currentTurnNumber();
        if (rollback) {
            if (!prunedReassignmentTurns.isEmpty()) {
                throw new IllegalArgumentException(
                    "turn rollback clears both reassignment maps before pruning runs on the"
                        + " emptied map; pruned rows must be empty, got " + prunedReassignmentTurns);
            }
            if (!after.recentReassignmentTurns().isEmpty() || !after.reassignmentCounts().isEmpty()) {
                throw new IllegalArgumentException(
                    "turn rollback must leave both reassignment maps empty");
            }
        } else {
            Map<String, Integer> derived = derivePrunedRows(before, after);
            if (!derived.equals(prunedReassignmentTurns)) {
                throw new IllegalArgumentException("pruned rows " + prunedReassignmentTurns
                    + " inconsistent with the before/after recentReassignmentTurns delta " + derived);
            }
            for (Map.Entry<String, Integer> entry : after.recentReassignmentTurns().entrySet()) {
                if (!entry.getValue().equals(before.recentReassignmentTurns().get(entry.getKey()))) {
                    throw new IllegalArgumentException(
                        "this boundary never adds or rewrites recentReassignmentTurns rows: "
                            + entry.getKey());
                }
            }
            if (!after.reassignmentCounts().equals(before.reassignmentCounts())) {
                throw new IllegalArgumentException(
                    "reassignmentCounts persists through normal advance (4A0 matrix correction)");
            }
        }
        if (!frozenRemainder(before).equals(frozenRemainder(after))) {
            throw new IllegalArgumentException(
                "STATE_UPDATE may not claim changes to memory its helper never writes"
                    + " (last-decision key/response/hash, action tuple, failed-search sets)");
        }
    }

    /** Factory used by the recording choke point: derives pruned rows and the outcome. */
    public static HeuristicStateUpdateEvent of(int handSize, int forcePile, int reserveDeck,
                                               int turn, int cardsInPlay,
                                               HeuristicMemorySnapshot before,
                                               HeuristicMemorySnapshot after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        boolean rollback = turn < before.currentTurnNumber();
        Map<String, Integer> pruned = rollback ? Map.of() : derivePrunedRows(before, after);
        boolean changed = !before.equals(after);
        return new HeuristicStateUpdateEvent(handSize, forcePile, reserveDeck, turn, cardsInPlay,
            pruned, before, after, changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }

    /** Rows present before but absent after: only pruning removes them on a normal advance. */
    private static Map<String, Integer> derivePrunedRows(HeuristicMemorySnapshot before,
                                                         HeuristicMemorySnapshot after) {
        TreeMap<String, Integer> pruned = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : before.recentReassignmentTurns().entrySet()) {
            if (!after.recentReassignmentTurns().containsKey(entry.getKey())) {
                pruned.put(entry.getKey(), entry.getValue());
            }
        }
        return pruned;
    }

    /** Copy with every component this helper may legally write zeroed out. */
    private static HeuristicMemorySnapshot frozenRemainder(HeuristicMemorySnapshot s) {
        return new HeuristicMemorySnapshot("", "", s.lastDecisionStateHash(),
            s.lastDecisionKey(), s.lastDecisionResponse(), 0, 0,
            s.lastActionChoiceText(), s.lastActionChoiceCardId(), s.lastActionChoiceBlueprintId(),
            s.failedSearchActionTexts(), s.failedSearchCardIds(), s.failedSearchBlueprintIds(),
            Map.of(), Map.of(), Map.of(), Map.of());
    }
}
