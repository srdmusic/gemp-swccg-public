package com.gempukku.swccgo.ai.models.common.trace.state;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Required tests", common state): constructor, closed-identity, defensive-immutability,
 * canonical-equality, outcome-derivation, and impossible-input tests for
 * HeuristicMemorySnapshot and the six heuristic-memory event families. No server, no
 * bots, no game state.
 */
public class HeuristicMemoryEventTest {

    /** Mutable builder for readable snapshot fixtures; defaults model a live turn-1 state. */
    private static final class Memory {
        String currentStateHash = "3:4:20:1:7";
        String blockStateHash = "3:4:20:1:7";
        String lastDecisionStateHash = "";
        String lastDecisionKey = "";
        String lastDecisionResponse = "";
        int lastDecisionRepeatCount = 0;
        int currentTurnNumber = 1;
        String lastActionChoiceText = "";
        String lastActionChoiceCardId = "";
        String lastActionChoiceBlueprintId = "";
        List<String> failedSearchActionTexts = List.of();
        List<String> failedSearchCardIds = List.of();
        List<String> failedSearchBlueprintIds = List.of();
        Map<String, List<String>> localBlockedResponses = Map.of();
        Map<String, List<String>> recentDecisionResponses = Map.of();
        Map<String, Integer> recentReassignmentTurns = Map.of();
        Map<String, Integer> reassignmentCounts = Map.of();

        HeuristicMemorySnapshot build() {
            return new HeuristicMemorySnapshot(currentStateHash, blockStateHash,
                lastDecisionStateHash, lastDecisionKey, lastDecisionResponse,
                lastDecisionRepeatCount, currentTurnNumber,
                lastActionChoiceText, lastActionChoiceCardId, lastActionChoiceBlueprintId,
                failedSearchActionTexts, failedSearchCardIds, failedSearchBlueprintIds,
                localBlockedResponses, recentDecisionResponses,
                recentReassignmentTurns, reassignmentCounts);
        }
    }

    private static void expectIllegalArgument(Runnable construction, String what) {
        try {
            construction.run();
            fail(what + " must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }

    private static void expectNullPointer(Runnable construction, String what) {
        try {
            construction.run();
            fail(what + " must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
    }

    // =========================================================================
    // HeuristicMemorySnapshot: nulls, impossible ints, defensive immutability
    // =========================================================================

    @Test
    public void snapshotRejectsNullsAndImpossibleInputs() {
        expectNullPointer(() -> {
            Memory m = new Memory();
            m.currentStateHash = null;
            m.build();
        }, "null currentStateHash");
        expectNullPointer(() -> {
            Memory m = new Memory();
            m.lastActionChoiceBlueprintId = null;
            m.build();
        }, "null lastActionChoiceBlueprintId");
        expectNullPointer(() -> {
            Memory m = new Memory();
            m.failedSearchCardIds = null;
            m.build();
        }, "null failedSearchCardIds");
        expectNullPointer(() -> {
            Memory m = new Memory();
            List<String> withNull = new ArrayList<>();
            withNull.add(null);
            m.failedSearchActionTexts = withNull;
            m.build();
        }, "null failed-search element");
        expectNullPointer(() -> {
            Memory m = new Memory();
            Map<String, List<String>> withNull = new HashMap<>();
            withNull.put("key", null);
            m.localBlockedResponses = withNull;
            m.build();
        }, "null localBlockedResponses value");
        expectNullPointer(() -> {
            Memory m = new Memory();
            Map<String, Integer> withNull = new HashMap<>();
            withNull.put("card:12", null);
            m.recentReassignmentTurns = withNull;
            m.build();
        }, "null recentReassignmentTurns value");
        expectIllegalArgument(() -> {
            Memory m = new Memory();
            m.lastDecisionRepeatCount = -1;
            m.build();
        }, "negative lastDecisionRepeatCount");
        expectIllegalArgument(() -> {
            Memory m = new Memory();
            m.currentTurnNumber = -1;
            m.build();
        }, "negative currentTurnNumber");
    }

    @Test
    public void snapshotFreezesEveryCollectionDefensively() {
        List<String> callerList = new ArrayList<>(List.of("b", "a"));
        Map<String, List<String>> callerMap = new HashMap<>();
        callerMap.put("k", new ArrayList<>(List.of("z", "y")));
        Map<String, Integer> callerInts = new HashMap<>();
        callerInts.put("card:12", 1);

        Memory m = new Memory();
        m.failedSearchActionTexts = callerList;
        m.localBlockedResponses = callerMap;
        m.recentReassignmentTurns = callerInts;
        HeuristicMemorySnapshot snapshot = m.build();

        // caller-owned collections mutated after construction; the snapshot must not move
        callerList.clear();
        callerMap.get("k").clear();
        callerMap.clear();
        callerInts.clear();
        assertEquals(List.of("a", "b"), snapshot.failedSearchActionTexts());
        assertEquals(List.of("y", "z"), snapshot.localBlockedResponses().get("k"));
        assertEquals(Integer.valueOf(1), snapshot.recentReassignmentTurns().get("card:12"));

        try {
            snapshot.failedSearchActionTexts().add("x");
            fail("failedSearchActionTexts must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
        try {
            snapshot.localBlockedResponses().put("x", List.of());
            fail("localBlockedResponses must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
        try {
            snapshot.localBlockedResponses().get("k").add("x");
            fail("localBlockedResponses values must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
        try {
            snapshot.reassignmentCounts().put("x", 1);
            fail("reassignmentCounts must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
    }

    @Test
    public void snapshotCanonicalizesShuffledHashInputsDeterministically() {
        // deliberately different HashMap/HashSet insertion orders on both sides
        Memory left = new Memory();
        left.failedSearchActionTexts = new ArrayList<>(new HashSet<>(List.of("zeta", "alpha", "mid")));
        Map<String, List<String>> leftBlocked = new HashMap<>();
        leftBlocked.put("k2", new ArrayList<>(new HashSet<>(List.of("r2", "r1"))));
        leftBlocked.put("k1", new ArrayList<>(new HashSet<>(List.of("r9", "r3"))));
        left.localBlockedResponses = leftBlocked;
        Map<String, Integer> leftTurns = new LinkedHashMap<>();
        leftTurns.put("text:b", 2);
        leftTurns.put("card:a", 1);
        left.recentReassignmentTurns = leftTurns;

        Memory right = new Memory();
        right.failedSearchActionTexts = new ArrayList<>(List.of("mid", "zeta", "alpha"));
        Map<String, List<String>> rightBlocked = new LinkedHashMap<>();
        rightBlocked.put("k1", new ArrayList<>(List.of("r3", "r9")));
        rightBlocked.put("k2", new ArrayList<>(List.of("r1", "r2")));
        right.localBlockedResponses = rightBlocked;
        Map<String, Integer> rightTurns = new HashMap<>();
        rightTurns.put("card:a", 1);
        rightTurns.put("text:b", 2);
        right.recentReassignmentTurns = rightTurns;

        HeuristicMemorySnapshot a = left.build();
        HeuristicMemorySnapshot b = right.build();
        assertEquals(a, b);
        // deterministic iteration order, independent of hash insertion order
        assertEquals(List.of("alpha", "mid", "zeta"), a.failedSearchActionTexts());
        assertEquals(List.of("k1", "k2"), new ArrayList<>(a.localBlockedResponses().keySet()));
        assertEquals(new ArrayList<>(a.localBlockedResponses().keySet()),
            new ArrayList<>(b.localBlockedResponses().keySet()));
        assertEquals(List.of("r3", "r9"), a.localBlockedResponses().get("k1"));
        assertEquals(List.of("card:a", "text:b"), new ArrayList<>(a.recentReassignmentTurns().keySet()));
        assertEquals(new ArrayList<>(a.recentReassignmentTurns().keySet()),
            new ArrayList<>(b.recentReassignmentTurns().keySet()));

        // deque-backed values keep insertion order and are NOT sorted
        Memory dequeOwner = new Memory();
        dequeOwner.recentDecisionResponses = Map.of("key", List.of("z", "a", "m"));
        assertEquals(List.of("z", "a", "m"),
            dequeOwner.build().recentDecisionResponses().get("key"));
    }

    // =========================================================================
    // STATE_UPDATE: pruned-row derivation, rollback law, frozen remainder
    // =========================================================================

    @Test
    public void stateUpdateEventDerivesPrunedRowsAndOutcome() {
        // normal advance turn 2 -> 3 pruning the expired row only; counts persist
        Memory before = new Memory();
        before.currentTurnNumber = 2;
        before.currentStateHash = "3:4:20:2:7";
        before.blockStateHash = "3:4:20:2:7";
        before.recentReassignmentTurns = Map.of("card:x", 1, "card:y", 2);
        before.reassignmentCounts = Map.of("card:x", 1, "card:y", 1);
        Memory after = new Memory();
        after.currentTurnNumber = 3;
        after.currentStateHash = "3:4:20:3:7";
        after.blockStateHash = "3:4:20:3:7";
        after.recentReassignmentTurns = Map.of("card:y", 2);
        after.reassignmentCounts = Map.of("card:x", 1, "card:y", 1);
        HeuristicStateUpdateEvent pruning = HeuristicStateUpdateEvent.of(
            3, 4, 20, 3, 7, before.build(), after.build());
        assertEquals(MutationOutcome.CHANGED, pruning.outcome());
        assertEquals(Map.of("card:x", 1), pruning.prunedReassignmentTurns());

        // identical snapshots: an executed write boundary with a real NO_OP outcome
        HeuristicStateUpdateEvent noOp = HeuristicStateUpdateEvent.of(
            3, 4, 20, 1, 7, new Memory().build(), new Memory().build());
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());
        assertTrue(noOp.prunedReassignmentTurns().isEmpty());

        // rollback: both maps cleared, pruning had nothing to do on the emptied map
        Memory rollbackBefore = new Memory();
        rollbackBefore.currentTurnNumber = 3;
        rollbackBefore.currentStateHash = "3:4:20:3:7";
        rollbackBefore.blockStateHash = "3:4:20:3:7";
        rollbackBefore.recentReassignmentTurns = Map.of("card:x", 3);
        rollbackBefore.reassignmentCounts = Map.of("card:x", 2);
        HeuristicStateUpdateEvent rollback = HeuristicStateUpdateEvent.of(
            3, 4, 20, 1, 7, rollbackBefore.build(), new Memory().build());
        assertEquals(MutationOutcome.CHANGED, rollback.outcome());
        assertTrue(rollback.prunedReassignmentTurns().isEmpty());
    }

    @Test
    public void stateUpdateEventRejectsImpossibleInputs() {
        HeuristicMemorySnapshot base = new Memory().build();
        expectIllegalArgument(() -> HeuristicStateUpdateEvent.of(-1, 4, 20, 1, 7, base, base),
            "negative state-read value");
        expectIllegalArgument(() -> HeuristicStateUpdateEvent.of(3, 4, 20, 2, 7, base, base),
            "after.currentTurnNumber differing from the turn argument");
        expectIllegalArgument(() -> HeuristicStateUpdateEvent.of(9, 9, 9, 1, 9, base, base),
            "after.currentStateHash differing from the joined state reads");
        expectIllegalArgument(() -> new HeuristicStateUpdateEvent(3, 4, 20, 1, 7,
            Map.of(), base, base, MutationOutcome.CHANGED),
            "CHANGED with equal snapshots");
        expectNullPointer(() -> new HeuristicStateUpdateEvent(3, 4, 20, 1, 7,
            Map.of(), null, base, MutationOutcome.NO_OP), "null before snapshot");

        // blockStateHash must always leave equal to currentStateHash
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.blockStateHash = "stale";
            HeuristicStateUpdateEvent.of(3, 4, 20, 1, 7, base, after.build());
        }, "after.blockStateHash differing from after.currentStateHash");

        // rollback with surviving reassignment rows
        expectIllegalArgument(() -> {
            Memory before = new Memory();
            before.currentTurnNumber = 3;
            before.currentStateHash = "3:4:20:3:7";
            before.blockStateHash = "3:4:20:3:7";
            before.recentReassignmentTurns = Map.of("card:x", 3);
            before.reassignmentCounts = Map.of("card:x", 1);
            Memory after = new Memory();
            after.reassignmentCounts = Map.of("card:x", 1);
            HeuristicStateUpdateEvent.of(3, 4, 20, 1, 7, before.build(), after.build());
        }, "rollback leaving reassignmentCounts populated");

        // claimed pruned rows inconsistent with the map delta
        expectIllegalArgument(() -> new HeuristicStateUpdateEvent(3, 4, 20, 1, 7,
            Map.of("card:ghost", 1), base, base, MutationOutcome.NO_OP),
            "pruned rows not derived from the delta");

        // normal advance may never add reassignment rows or touch the counts
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.recentReassignmentTurns = Map.of("card:new", 1);
            HeuristicStateUpdateEvent.of(3, 4, 20, 1, 7, base, after.build());
        }, "a row added at the STATE_UPDATE boundary");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.reassignmentCounts = Map.of("card:x", 5);
            HeuristicStateUpdateEvent.of(3, 4, 20, 1, 7, base, after.build());
        }, "counts mutated on a normal advance");

        // frozen remainder: the helper never writes the action tuple or failed sets
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.lastActionChoiceText = "sneaky";
            HeuristicStateUpdateEvent.of(3, 4, 20, 1, 7, base, after.build());
        }, "an action-tuple change claimed by STATE_UPDATE");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.failedSearchCardIds = List.of("12");
            HeuristicStateUpdateEvent.of(3, 4, 20, 1, 7, base, after.build());
        }, "a failed-search change claimed by STATE_UPDATE");
    }

    // =========================================================================
    // ACTION_CHOICE_REMEMBER: closed types, sentinel law, tuple-only frame
    // =========================================================================

    @Test
    public void actionChoiceRememberEventDerivesOutcomeAndEnforcesTupleLaws() {
        Memory before = new Memory();
        Memory after = new Memory();
        after.lastActionChoiceText = "transfer stolen blaster to vader";
        after.lastActionChoiceCardId = "12";
        after.lastActionChoiceBlueprintId = "200_5";
        HeuristicActionChoiceRememberEvent changed = HeuristicActionChoiceRememberEvent.of(
            "CARD_ACTION_CHOICE", "0", 0, before.build(), after.build());
        assertEquals(MutationOutcome.CHANGED, changed.outcome());

        // identical rewrite: executed writes, real NO_OP
        HeuristicActionChoiceRememberEvent noOp = HeuristicActionChoiceRememberEvent.of(
            "ACTION_CHOICE", "0", 0, after.build(), after.build());
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());

        HeuristicMemorySnapshot base = new Memory().build();
        expectIllegalArgument(() -> HeuristicActionChoiceRememberEvent.of(
            "MULTIPLE_CHOICE", "0", 0, base, base), "a decision type outside the legacy guard");
        expectIllegalArgument(() -> HeuristicActionChoiceRememberEvent.of(
            "ACTION_CHOICE", "", 0, base, base), "an empty result");
        expectIllegalArgument(() -> HeuristicActionChoiceRememberEvent.of(
            "ACTION_CHOICE", "abc", 0, base, base), "an unparseable result");
        expectIllegalArgument(() -> HeuristicActionChoiceRememberEvent.of(
            "ACTION_CHOICE", "1", 0, base, base), "an index differing from the parsed result");
        expectIllegalArgument(() -> HeuristicActionChoiceRememberEvent.of(
            "ACTION_CHOICE", "-1", -1, base, base), "a negative index");
        expectIllegalArgument(() -> {
            Memory sentinel = new Memory();
            sentinel.lastActionChoiceBlueprintId = "inplay";
            HeuristicActionChoiceRememberEvent.of("ACTION_CHOICE", "0", 0, base, sentinel.build());
        }, "a sentinel blueprint id surviving into the after tuple");
        expectIllegalArgument(() -> {
            Memory outside = new Memory();
            outside.lastDecisionRepeatCount = 3;
            HeuristicActionChoiceRememberEvent.of("ACTION_CHOICE", "0", 0, base, outside.build());
        }, "a change outside the lastActionChoice tuple");
        expectIllegalArgument(() -> new HeuristicActionChoiceRememberEvent(
            "ACTION_CHOICE", "0", 0, base, base, MutationOutcome.CHANGED),
            "CHANGED with equal snapshots");
    }

    // =========================================================================
    // FAILED_SEARCH_ADD: membership-only deltas, repeat NO_OP, growth law
    // =========================================================================

    @Test
    public void failedSearchAddEventDerivesSortedDeltasAndOutcome() {
        Memory before = new Memory();
        before.lastActionChoiceText = "download bunker from reserve deck";
        before.lastActionChoiceCardId = "12";
        Memory after = new Memory();
        after.lastActionChoiceText = "download bunker from reserve deck";
        after.lastActionChoiceCardId = "12";
        after.failedSearchActionTexts = List.of("download bunker from reserve deck");
        after.failedSearchCardIds = List.of("12");
        HeuristicFailedSearchAddEvent changed = HeuristicFailedSearchAddEvent.of(
            "download bunker from reserve deck", "12", "", before.build(), after.build());
        assertEquals(MutationOutcome.CHANGED, changed.outcome());
        assertEquals(List.of("download bunker from reserve deck"), changed.addedActionTexts());
        assertEquals(List.of("12"), changed.addedCardIds());
        assertTrue("an empty prior identity executes no add", changed.addedBlueprintIds().isEmpty());

        // repeated addition: adds executed, sets unchanged, real NO_OP with empty deltas
        HeuristicFailedSearchAddEvent repeat = HeuristicFailedSearchAddEvent.of(
            "download bunker from reserve deck", "12", "", after.build(), after.build());
        assertEquals(MutationOutcome.NO_OP, repeat.outcome());
        assertTrue(repeat.addedActionTexts().isEmpty());
        assertTrue(repeat.addedCardIds().isEmpty());
    }

    @Test
    public void failedSearchAddEventRejectsImpossibleInputs() {
        HeuristicMemorySnapshot base = new Memory().build();
        expectIllegalArgument(() -> HeuristicFailedSearchAddEvent.of("", "", "", base, base),
            "an all-empty prior action tuple");
        expectIllegalArgument(() -> {
            Memory before = new Memory();
            before.failedSearchCardIds = List.of("12");
            HeuristicFailedSearchAddEvent.of("text", "", "", before.build(), base);
        }, "a shrinking failed-search set");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.failedSearchCardIds = List.of("99");
            HeuristicFailedSearchAddEvent.of("", "12", "", base, after.build());
        }, "a delta not matching the prior identity");
        expectIllegalArgument(() -> HeuristicFailedSearchAddEvent.of("text", "", "", base, base),
            "a non-empty prior identity absent from the after set");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.failedSearchActionTexts = List.of("text");
            after.lastDecisionRepeatCount = 4;
            HeuristicFailedSearchAddEvent.of("text", "", "", base, after.build());
        }, "a change outside the three failed-search sets");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.failedSearchActionTexts = List.of("text");
            new HeuristicFailedSearchAddEvent("text", "", "", List.of(), List.of(), List.of(),
                base, after.build(), MutationOutcome.CHANGED);
        }, "a claimed delta inconsistent with the snapshots");
    }

    // =========================================================================
    // SINGLE_RESPONSE_RECORD: both exits, repeat law, folded local block
    // =========================================================================

    @Test
    public void singleResponseRecordEventValidatesBothExitsAndTheRepeatLaw() {
        // main path, fresh decision: repeat resets to 1
        Memory before = new Memory();
        Memory after = new Memory();
        after.lastDecisionKey = "CARD_ACTION_CHOICE:Choose action";
        after.lastDecisionResponse = "fire blaster";
        after.lastDecisionStateHash = "3:4:20:1:7";
        after.lastDecisionRepeatCount = 1;
        HeuristicSingleResponseRecordEvent fresh = HeuristicSingleResponseRecordEvent.of(
            "CARD_ACTION_CHOICE", "Choose action", "0", "fire blaster",
            before.build(), after.build());
        assertEquals(MutationOutcome.CHANGED, fresh.outcome());

        // repeat with the folded local block: repeat 1 -> 2 plus the block rows
        Memory repeatBefore = new Memory();
        repeatBefore.lastDecisionKey = "CARD_ACTION_CHOICE:Choose action";
        repeatBefore.lastDecisionResponse = "fire blaster";
        repeatBefore.lastDecisionStateHash = "3:4:20:1:7";
        repeatBefore.lastDecisionRepeatCount = 1;
        Memory repeatAfter = new Memory();
        repeatAfter.lastDecisionKey = "CARD_ACTION_CHOICE:Choose action";
        repeatAfter.lastDecisionResponse = "fire blaster";
        repeatAfter.lastDecisionStateHash = "3:4:20:1:7";
        repeatAfter.lastDecisionRepeatCount = 2;
        repeatAfter.localBlockedResponses = Map.of(
            "CARD_ACTION_CHOICE:Choose action", List.of("0", "fire blaster"));
        HeuristicSingleResponseRecordEvent folded = HeuristicSingleResponseRecordEvent.of(
            "CARD_ACTION_CHOICE", "Choose action", "0", "fire blaster",
            repeatBefore.build(), repeatAfter.build());
        assertEquals(MutationOutcome.CHANGED, folded.outcome());

        // empty response key: the reset writes executed; NO_OP when already reset
        HeuristicSingleResponseRecordEvent emptyNoOp = HeuristicSingleResponseRecordEvent.of(
            "CARD_SELECTION", "Choose cards", "", "", before.build(), before.build());
        assertEquals(MutationOutcome.NO_OP, emptyNoOp.outcome());
        HeuristicSingleResponseRecordEvent emptyChanged = HeuristicSingleResponseRecordEvent.of(
            "CARD_SELECTION", "Choose cards", "", "", repeatBefore.build(), before.build());
        assertEquals(MutationOutcome.CHANGED, emptyChanged.outcome());

        // a null tracking response is passed verbatim; the fold keys on the raw response
        Memory rawKeyed = new Memory();
        rawKeyed.lastDecisionKey = "INTEGER:Choose amount";
        rawKeyed.lastDecisionResponse = "3";
        rawKeyed.lastDecisionStateHash = "3:4:20:1:7";
        rawKeyed.lastDecisionRepeatCount = 1;
        HeuristicSingleResponseRecordEvent nullTracking = HeuristicSingleResponseRecordEvent.of(
            "INTEGER", "Choose amount", "3", null, before.build(), rawKeyed.build());
        assertEquals(MutationOutcome.CHANGED, nullTracking.outcome());
        assertNull(nullTracking.trackingResponse());

        // decision text over 60 characters is truncated into the key exactly
        String longText = "X".repeat(70);
        Memory truncated = new Memory();
        truncated.lastDecisionKey = "CARD_ACTION_CHOICE:" + "X".repeat(60);
        truncated.lastDecisionResponse = "fire blaster";
        truncated.lastDecisionStateHash = "3:4:20:1:7";
        truncated.lastDecisionRepeatCount = 1;
        HeuristicSingleResponseRecordEvent longKey = HeuristicSingleResponseRecordEvent.of(
            "CARD_ACTION_CHOICE", longText, "0", "fire blaster", before.build(), truncated.build());
        assertEquals(MutationOutcome.CHANGED, longKey.outcome());
    }

    @Test
    public void singleResponseRecordEventRejectsImpossibleInputs() {
        HeuristicMemorySnapshot base = new Memory().build();
        expectIllegalArgument(() -> {
            Memory unhashed = new Memory();
            unhashed.currentStateHash = "";
            unhashed.blockStateHash = "";
            HeuristicSingleResponseRecordEvent.of("CARD_SELECTION", "Choose", "", "",
                unhashed.build(), unhashed.build());
        }, "an empty before.currentStateHash (guard suppression, not an event)");
        expectNullPointer(() -> HeuristicSingleResponseRecordEvent.of(
            "CARD_SELECTION", "Choose", null, "", base, base), "a null raw response");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.lastDecisionKey = "leftover";
            HeuristicSingleResponseRecordEvent.of("CARD_SELECTION", "Choose", "", "",
                base, after.build());
        }, "an empty response key not resetting the last-decision fields");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.localBlockedResponses = Map.of("k", List.of("v"));
            HeuristicSingleResponseRecordEvent.of("CARD_SELECTION", "Choose", "", "",
                base, after.build());
        }, "the empty-response-key path touching local blocks");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.lastDecisionKey = "CARD_ACTION_CHOICE:Choose action";
            after.lastDecisionResponse = "fire blaster";
            after.lastDecisionStateHash = "3:4:20:1:7";
            after.lastDecisionRepeatCount = 2;
            HeuristicSingleResponseRecordEvent.of("CARD_ACTION_CHOICE", "Choose action",
                "0", "fire blaster", base, after.build());
        }, "a repeat count not following the same-decision law");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.lastDecisionKey = "WRONG:key";
            after.lastDecisionResponse = "fire blaster";
            after.lastDecisionStateHash = "3:4:20:1:7";
            after.lastDecisionRepeatCount = 1;
            HeuristicSingleResponseRecordEvent.of("CARD_ACTION_CHOICE", "Choose action",
                "0", "fire blaster", base, after.build());
        }, "an after key not built from this decision");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.lastDecisionKey = "CARD_ACTION_CHOICE:Choose action";
            after.lastDecisionResponse = "fire blaster";
            after.lastDecisionStateHash = "3:4:20:1:7";
            after.lastDecisionRepeatCount = 1;
            after.recentDecisionResponses = Map.of("k", List.of("v"));
            HeuristicSingleResponseRecordEvent.of("CARD_ACTION_CHOICE", "Choose action",
                "0", "fire blaster", base, after.build());
        }, "a change outside the last-decision fields and local blocks");
    }

    // =========================================================================
    // RECENT_RESPONSE_APPEND: derivation, six-entry FIFO, identical-entry NO_OP
    // =========================================================================

    @Test
    public void recentResponseAppendEventDerivesDequesEvictionsAndOutcome() {
        String key = "CARD_ACTION_CHOICE:Choose action";

        // fresh key: map-entry creation folded in
        Memory after = new Memory();
        after.recentDecisionResponses = Map.of(key, List.of("fire blaster"));
        HeuristicRecentResponseAppendEvent fresh = HeuristicRecentResponseAppendEvent.of(
            key, "fire blaster", new Memory().build(), after.build());
        assertEquals(MutationOutcome.CHANGED, fresh.outcome());
        assertTrue(fresh.dequeBefore().isEmpty());
        assertEquals(List.of("fire blaster"), fresh.dequeAfter());
        assertTrue(fresh.evictedRows().isEmpty());

        // FIFO eviction past six entries: the oldest row leaves, order preserved
        Memory sixBefore = new Memory();
        sixBefore.recentDecisionResponses = Map.of(key, List.of("r1", "r2", "r3", "r4", "r5", "r6"));
        Memory sixAfter = new Memory();
        sixAfter.recentDecisionResponses = Map.of(key, List.of("r2", "r3", "r4", "r5", "r6", "r7"));
        HeuristicRecentResponseAppendEvent evicting = HeuristicRecentResponseAppendEvent.of(
            key, "r7", sixBefore.build(), sixAfter.build());
        assertEquals(MutationOutcome.CHANGED, evicting.outcome());
        assertEquals(List.of("r1"), evicting.evictedRows());
        assertEquals(List.of("r2", "r3", "r4", "r5", "r6", "r7"), evicting.dequeAfter());

        // six identical entries appending an identical entry: a real executed-write
        // NO_OP with a real evicted row, never event suppression
        Memory identical = new Memory();
        identical.recentDecisionResponses = Map.of(key, List.of("x", "x", "x", "x", "x", "x"));
        HeuristicRecentResponseAppendEvent noOp = HeuristicRecentResponseAppendEvent.of(
            key, "x", identical.build(), identical.build());
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());
        assertEquals(List.of("x"), noOp.evictedRows());
    }

    @Test
    public void recentResponseAppendEventRejectsImpossibleInputs() {
        String key = "CARD_ACTION_CHOICE:Choose action";
        HeuristicMemorySnapshot base = new Memory().build();
        Memory oneAfter = new Memory();
        oneAfter.recentDecisionResponses = Map.of(key, List.of("r1"));
        expectIllegalArgument(() -> HeuristicRecentResponseAppendEvent.of(
            key, "", base, oneAfter.build()), "an empty appended response");
        expectIllegalArgument(() -> HeuristicRecentResponseAppendEvent.of(
            key, "r1", base, base), "an after snapshot missing the appended deque");
        expectIllegalArgument(() -> HeuristicRecentResponseAppendEvent.of(
            key, "other", base, oneAfter.build()), "an appended response not last in the deque");
        expectIllegalArgument(() -> {
            Memory unhashed = new Memory();
            unhashed.currentStateHash = "";
            unhashed.blockStateHash = "";
            Memory unhashedAfter = new Memory();
            unhashedAfter.currentStateHash = "";
            unhashedAfter.blockStateHash = "";
            unhashedAfter.recentDecisionResponses = Map.of(key, List.of("r1"));
            HeuristicRecentResponseAppendEvent.of(key, "r1",
                unhashed.build(), unhashedAfter.build());
        }, "an empty before.currentStateHash (guard suppression, not an event)");
        expectIllegalArgument(() -> {
            Memory foreign = new Memory();
            foreign.recentDecisionResponses = Map.of(key, List.of("r1"), "other", List.of("z"));
            HeuristicRecentResponseAppendEvent.of(key, "r1", base, foreign.build());
        }, "a foreign decision key changed at this boundary");
        expectIllegalArgument(() -> {
            Memory sixBefore = new Memory();
            sixBefore.recentDecisionResponses = Map.of(key, List.of("r1", "r2", "r3", "r4", "r5", "r6"));
            Memory badAfter = new Memory();
            badAfter.recentDecisionResponses = Map.of(key, List.of("r1", "r3", "r4", "r5", "r6", "r7"));
            HeuristicRecentResponseAppendEvent.of(key, "r7", sixBefore.build(), badAfter.build());
        }, "evicted rows not forming the exact FIFO prefix");
        expectIllegalArgument(() -> {
            Memory after = new Memory();
            after.recentDecisionResponses = Map.of(key, List.of("r1"));
            after.lastDecisionRepeatCount = 5;
            HeuristicRecentResponseAppendEvent.of(key, "r1", base, after.build());
        }, "a change outside recentDecisionResponses");
    }

    // =========================================================================
    // REASSIGNMENT_RECORD: closed variants, key precedence prefixes, map deltas
    // =========================================================================

    @Test
    public void reassignmentRecordEventDerivesVariantsAndBothMapDeltas() {
        // closed identities: exactly the three legacy precedence variants
        assertEquals(List.of(
                HeuristicReassignmentRecordEvent.Variant.CARD,
                HeuristicReassignmentRecordEvent.Variant.BLUEPRINT,
                HeuristicReassignmentRecordEvent.Variant.TEXT),
            List.of(HeuristicReassignmentRecordEvent.Variant.values()));
        assertEquals("card:", HeuristicReassignmentRecordEvent.Variant.CARD.prefix());
        assertEquals("blueprint:", HeuristicReassignmentRecordEvent.Variant.BLUEPRINT.prefix());
        assertEquals("text:", HeuristicReassignmentRecordEvent.Variant.TEXT.prefix());

        // fresh card key: null prior rows, count folded to 1
        Memory after = new Memory();
        after.recentReassignmentTurns = Map.of("card:12", 1);
        after.reassignmentCounts = Map.of("card:12", 1);
        HeuristicReassignmentRecordEvent fresh = HeuristicReassignmentRecordEvent.of(
            "card:12", 1, new Memory().build(), after.build());
        assertEquals(HeuristicReassignmentRecordEvent.Variant.CARD, fresh.variant());
        assertEquals(MutationOutcome.CHANGED, fresh.outcome());
        assertNull(fresh.turnBefore());
        assertEquals(1, fresh.turnAfter());
        assertNull(fresh.countBefore());
        assertEquals(1, fresh.countAfter());

        // repeat text key: turn rewritten, count folded to 2
        Memory textBefore = new Memory();
        textBefore.recentReassignmentTurns = Map.of("text:stolen blaster", 1);
        textBefore.reassignmentCounts = Map.of("text:stolen blaster", 1);
        Memory textAfter = new Memory();
        textAfter.recentReassignmentTurns = Map.of("text:stolen blaster", 1);
        textAfter.reassignmentCounts = Map.of("text:stolen blaster", 2);
        HeuristicReassignmentRecordEvent repeat = HeuristicReassignmentRecordEvent.of(
            "text:stolen blaster", 1, textBefore.build(), textAfter.build());
        assertEquals(HeuristicReassignmentRecordEvent.Variant.TEXT, repeat.variant());
        assertEquals(Integer.valueOf(1), repeat.turnBefore());
        assertEquals(Integer.valueOf(1), repeat.countBefore());
        assertEquals(2, repeat.countAfter());
        assertEquals(MutationOutcome.CHANGED, repeat.outcome());

        // blueprint variant
        Memory bpAfter = new Memory();
        bpAfter.recentReassignmentTurns = Map.of("blueprint:200_7", 1);
        bpAfter.reassignmentCounts = Map.of("blueprint:200_7", 1);
        assertEquals(HeuristicReassignmentRecordEvent.Variant.BLUEPRINT,
            HeuristicReassignmentRecordEvent.of("blueprint:200_7", 1,
                new Memory().build(), bpAfter.build()).variant());
    }

    @Test
    public void reassignmentRecordEventRejectsImpossibleInputs() {
        HeuristicMemorySnapshot base = new Memory().build();
        Memory after = new Memory();
        after.recentReassignmentTurns = Map.of("card:12", 1);
        after.reassignmentCounts = Map.of("card:12", 1);
        HeuristicMemorySnapshot goodAfter = after.build();

        expectIllegalArgument(() -> HeuristicReassignmentRecordEvent.of(
            "bogus:12", 1, base, goodAfter), "a key without a closed variant prefix");
        expectIllegalArgument(() -> HeuristicReassignmentRecordEvent.of(
            "card:", 1, base, goodAfter), "a bare prefix with an empty value");
        expectIllegalArgument(() -> HeuristicReassignmentRecordEvent.of(
            "card:12", 1, base, base), "an after snapshot missing both map rows");
        expectIllegalArgument(() -> {
            Memory zeroTurn = new Memory();
            zeroTurn.currentTurnNumber = 0;
            Memory zeroAfter = new Memory();
            zeroAfter.currentTurnNumber = 0;
            zeroAfter.recentReassignmentTurns = Map.of("card:12", 0);
            zeroAfter.reassignmentCounts = Map.of("card:12", 1);
            HeuristicReassignmentRecordEvent.of("card:12", 0, zeroTurn.build(), zeroAfter.build());
        }, "a non-positive turn (legacy guard, not an event)");
        expectIllegalArgument(() -> {
            Memory staleTurn = new Memory();
            staleTurn.recentReassignmentTurns = Map.of("card:12", 9);
            staleTurn.reassignmentCounts = Map.of("card:12", 1);
            HeuristicReassignmentRecordEvent.of("card:12", 1, base, staleTurn.build());
        }, "a put not recording the current turn");
        expectIllegalArgument(() -> {
            Memory badCount = new Memory();
            badCount.recentReassignmentTurns = Map.of("card:12", 1);
            badCount.reassignmentCounts = Map.of("card:12", 5);
            HeuristicReassignmentRecordEvent.of("card:12", 1, base, badCount.build());
        }, "a count not following the folded increment");
        expectIllegalArgument(() -> {
            Memory foreign = new Memory();
            foreign.recentReassignmentTurns = Map.of("card:12", 1, "card:99", 1);
            foreign.reassignmentCounts = Map.of("card:12", 1);
            HeuristicReassignmentRecordEvent.of("card:12", 1, base, foreign.build());
        }, "a foreign key changed at this boundary");
        expectIllegalArgument(() -> {
            Memory outside = new Memory();
            outside.recentReassignmentTurns = Map.of("card:12", 1);
            outside.reassignmentCounts = Map.of("card:12", 1);
            outside.lastActionChoiceText = "sneaky";
            HeuristicReassignmentRecordEvent.of("card:12", 1, base, outside.build());
        }, "a change outside the two reassignment maps");
        expectIllegalArgument(() -> new HeuristicReassignmentRecordEvent(
            HeuristicReassignmentRecordEvent.Variant.TEXT, "card:12", 1,
            null, 1, null, 1, base, goodAfter, MutationOutcome.CHANGED),
            "a variant inconsistent with the key prefix");
    }
}
