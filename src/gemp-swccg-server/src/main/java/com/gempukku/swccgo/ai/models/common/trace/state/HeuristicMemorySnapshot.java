package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Snapshot Boundary"): the one immutable, canonical view of the retained heuristic
 * memory owned directly by HeuristicAiBase, captured by the owner's pure private
 * accessor before and after each of the six owner boundaries. Exact before/after
 * equality defines CHANGED vs NO_OP.
 *
 * Canonical form is guaranteed by construction: the three failed-search sets are sorted
 * lists; every outer map is canonicalized by sorted key; HashSet-backed map values
 * (localBlockedResponses) are sorted; deque-backed map values (recentDecisionResponses)
 * preserve insertion order exactly; both reassignment maps use sorted keys. No snapshot
 * order may depend on HashMap or HashSet iteration. Every collection is defensively
 * copied and frozen.
 *
 * EXCLUDED by the packet: the private DecisionTracker (the separate 4A2b owner), static
 * keyword tables, transient method locals, and game/service references. There is no
 * reachable heuristic-memory clear/reset owner in the source; a reused controller
 * retains failed-search sets, the last action tuple, and the last-decision fields, and
 * this snapshot records that retention exactly without inventing a reset.
 */
public record HeuristicMemorySnapshot(
    String currentStateHash,
    String blockStateHash,
    String lastDecisionStateHash,
    String lastDecisionKey,
    String lastDecisionResponse,
    int lastDecisionRepeatCount,
    int currentTurnNumber,
    String lastActionChoiceText,
    String lastActionChoiceCardId,
    String lastActionChoiceBlueprintId,
    List<String> failedSearchActionTexts,
    List<String> failedSearchCardIds,
    List<String> failedSearchBlueprintIds,
    Map<String, List<String>> localBlockedResponses,
    Map<String, List<String>> recentDecisionResponses,
    Map<String, Integer> recentReassignmentTurns,
    Map<String, Integer> reassignmentCounts) {

    public HeuristicMemorySnapshot {
        Objects.requireNonNull(currentStateHash, "currentStateHash");
        Objects.requireNonNull(blockStateHash, "blockStateHash");
        Objects.requireNonNull(lastDecisionStateHash, "lastDecisionStateHash");
        Objects.requireNonNull(lastDecisionKey, "lastDecisionKey");
        Objects.requireNonNull(lastDecisionResponse, "lastDecisionResponse");
        if (lastDecisionRepeatCount < 0) {
            throw new IllegalArgumentException(
                "lastDecisionRepeatCount must be >= 0, was " + lastDecisionRepeatCount);
        }
        if (currentTurnNumber < 0) {
            throw new IllegalArgumentException(
                "currentTurnNumber must be >= 0, was " + currentTurnNumber);
        }
        Objects.requireNonNull(lastActionChoiceText, "lastActionChoiceText");
        Objects.requireNonNull(lastActionChoiceCardId, "lastActionChoiceCardId");
        Objects.requireNonNull(lastActionChoiceBlueprintId, "lastActionChoiceBlueprintId");
        failedSearchActionTexts = sortedCopy(failedSearchActionTexts, "failedSearchActionTexts");
        failedSearchCardIds = sortedCopy(failedSearchCardIds, "failedSearchCardIds");
        failedSearchBlueprintIds = sortedCopy(failedSearchBlueprintIds, "failedSearchBlueprintIds");
        localBlockedResponses =
            canonicalListMap(localBlockedResponses, true, "localBlockedResponses");
        recentDecisionResponses =
            canonicalListMap(recentDecisionResponses, false, "recentDecisionResponses");
        recentReassignmentTurns = canonicalIntMap(recentReassignmentTurns, "recentReassignmentTurns");
        reassignmentCounts = canonicalIntMap(reassignmentCounts, "reassignmentCounts");
    }

    private static List<String> sortedCopy(List<String> values, String name) {
        List<String> copy = new ArrayList<>(Objects.requireNonNull(values, name));
        for (String value : copy) {
            Objects.requireNonNull(value, name + " element");
        }
        Collections.sort(copy);
        return Collections.unmodifiableList(copy);
    }

    /** Sorted outer keys; values sorted when set-backed, verbatim order when deque-backed. */
    private static Map<String, List<String>> canonicalListMap(Map<String, List<String>> map,
                                                              boolean sortValues, String name) {
        Objects.requireNonNull(map, name);
        TreeMap<String, List<String>> canonical = new TreeMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = Objects.requireNonNull(entry.getKey(), name + " key");
            List<String> values = new ArrayList<>(
                Objects.requireNonNull(entry.getValue(), name + " value for " + key));
            for (String value : values) {
                Objects.requireNonNull(value, name + " element for " + key);
            }
            if (sortValues) {
                Collections.sort(values);
            }
            canonical.put(key, Collections.unmodifiableList(values));
        }
        return Collections.unmodifiableMap(canonical);
    }

    private static Map<String, Integer> canonicalIntMap(Map<String, Integer> map, String name) {
        Objects.requireNonNull(map, name);
        TreeMap<String, Integer> canonical = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = Objects.requireNonNull(entry.getKey(), name + " key");
            canonical.put(key, Objects.requireNonNull(entry.getValue(), name + " value for " + key));
        }
        return Collections.unmodifiableMap(canonical);
    }
}
