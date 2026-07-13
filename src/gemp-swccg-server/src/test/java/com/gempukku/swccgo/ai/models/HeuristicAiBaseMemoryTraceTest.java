package com.gempukku.swccgo.ai.models;

import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceCaptureFailure;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStateEventFailureTestSupport;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicActionChoiceRememberEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicFailedSearchAddEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicReassignmentRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicRecentResponseAppendEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicSingleResponseRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicStateUpdateEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.MutationOutcome;
import com.gempukku.swccgo.ai.models.common.trace.state.TraceStateEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerPhaseChangeEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerUpdateStateEvent;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
 * "Required tests", focused owner fixtures): scripted-flow proof of the six real
 * HeuristicAiBase heuristic-memory owner boundaries through a minimal concrete owner
 * subclass, with the test managing the trace session directly (the DecisionSafety hook
 * pattern). Covers every changed path, every early-guard suppression path,
 * executed-write NO_OP paths, turn advance pruning, turn rollback, state-hash reset
 * scope, reserve verification mismatch/repeat/throw behavior, local-block folding, the
 * six-entry recent-response FIFO, reassignment key precedence with both map deltas, and
 * append-failure injection. No server; game state is the minimal stub the bot fixtures
 * already use.
 */
public class HeuristicAiBaseMemoryTraceTest {

    /** Minimal concrete heuristic owner: empty weights, no context scoring. */
    private static final class MemoryProbeAi extends HeuristicAiBase {
        @Override
        protected KeywordWeight[] getActionWeights() {
            return new KeywordWeight[0];
        }

        @Override
        protected KeywordWeight[] getActionPenalties() {
            return new KeywordWeight[0];
        }

        @Override
        protected KeywordWeight[] getChoiceWeights() {
            return new KeywordWeight[0];
        }

        @Override
        protected KeywordWeight[] getChoicePenalties() {
            return new KeywordWeight[0];
        }

        @Override
        protected String[] getCardHints() {
            return new String[0];
        }
    }

    /** Minimal scripted AwaitingDecision; decisionMade is never called by the AI. */
    private static AwaitingDecision decision(int id, AwaitingDecisionType type, String text,
                                             Map<String, String[]> params) {
        return new AwaitingDecision() {
            @Override
            public int getAwaitingDecisionId() {
                return id;
            }

            @Override
            public String getText() {
                return text;
            }

            @Override
            public AwaitingDecisionType getDecisionType() {
                return type;
            }

            @Override
            public Map<String, String[]> getDecisionParameters() {
                return params;
            }

            @Override
            public void decisionMade(String result) {
                throw new AssertionError("the trace must never call decisionMade");
            }
        };
    }

    /** Minimal real-GameState subclass overriding only the getters decide() reads. */
    private static class StubGameState extends GameState {
        private final int turn;
        private final int forcePile;
        private final List<PhysicalCard> reserve;

        StubGameState(int turn) {
            this(turn, 4, List.of());
        }

        StubGameState(int turn, int forcePile, List<PhysicalCard> reserve) {
            this.turn = turn;
            this.forcePile = forcePile;
            this.reserve = reserve;
        }

        @Override
        public int getPlayersLatestTurnNumber(String playerId) {
            return turn;
        }

        @Override
        public Phase getCurrentPhase() {
            return Phase.DEPLOY;
        }

        @Override
        public List<PhysicalCard> getHand(String playerId) {
            return List.of();
        }

        @Override
        public List<PhysicalCard> getAllPermanentCards() {
            return List.of();
        }

        @Override
        public int getForcePileSize(String playerId) {
            return forcePile;
        }

        @Override
        public int getReserveDeckSize(String playerId) {
            return 20;
        }

        @Override
        public List<PhysicalCard> getReserveDeck(String playerId) {
            return reserve;
        }
    }

    /** StubGameState whose hand read always throws: the packet's failed state read. */
    private static final class ThrowingHandGameState extends StubGameState {
        ThrowingHandGameState(int turn) {
            super(turn);
        }

        @Override
        public List<PhysicalCard> getHand(String playerId) {
            throw new RuntimeException("injected game-state read failure");
        }
    }

    /** StubGameState whose reserve-deck read always throws: the uncaught legacy path. */
    private static final class ThrowingReserveGameState extends StubGameState {
        ThrowingReserveGameState(int turn) {
            super(turn);
        }

        @Override
        public List<PhysicalCard> getReserveDeck(String playerId) {
            throw new RuntimeException("injected reserve-deck read failure");
        }
    }

    /** Reflective PhysicalCard stub for the failed-search verification path only. */
    private static PhysicalCard reserveCard(String blueprintId) {
        SwccgCardBlueprint blueprint = (SwccgCardBlueprint) Proxy.newProxyInstance(
            SwccgCardBlueprint.class.getClassLoader(),
            new Class<?>[]{SwccgCardBlueprint.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getCardCategory":
                        return CardCategory.CHARACTER;
                    case "toString":
                        return "stub-blueprint(" + blueprintId + ")";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        throw new UnsupportedOperationException(
                            "verification path grew: unexpected blueprint call " + method.getName());
                }
            });
        return (PhysicalCard) Proxy.newProxyInstance(
            PhysicalCard.class.getClassLoader(),
            new Class<?>[]{PhysicalCard.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getBlueprint":
                        return blueprint;
                    case "getBlueprintId":
                        return blueprintId;
                    case "toString":
                        return "stub-card(" + blueprintId + ")";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        throw new UnsupportedOperationException(
                            "verification path grew: unexpected card call " + method.getName());
                }
            });
    }

    private record TracedDecision(String response, DecisionTrace trace) {
    }

    private static TracedDecision decideTraced(HeuristicAiBase ai, AwaitingDecision decision,
                                               GameState gameState) {
        assertTrue(TraceSession.open("heuristic-4b1-fixture",
            String.valueOf(decision.getAwaitingDecisionId()),
            decision.getDecisionType() != null ? decision.getDecisionType().name() : "UNKNOWN",
            decision.getText(), List.of(), null,
            List.of("focused 4B1 owner fixture: snapshot deliberately absent"), false));
        String response;
        try {
            response = ai.decide("tester", decision, gameState);
        } catch (RuntimeException | Error t) {
            TraceSession.abandon();
            throw t;
        }
        DecisionTrace trace = TraceSession.close();
        assertNotNull(trace);
        assertFalse(TraceSession.isActive());
        return new TracedDecision(response, trace);
    }

    private static final List<Class<?>> HEURISTIC_MEMORY_EVENT_TYPES = List.of(
        HeuristicStateUpdateEvent.class, HeuristicActionChoiceRememberEvent.class,
        HeuristicFailedSearchAddEvent.class, HeuristicSingleResponseRecordEvent.class,
        HeuristicRecentResponseAppendEvent.class, HeuristicReassignmentRecordEvent.class);

    private static List<TraceStateEvent> heuristicMemoryEvents(DecisionTrace trace) {
        List<TraceStateEvent> events = new ArrayList<>();
        for (TraceStateEvent event : trace.getStateEvents()) {
            if (HEURISTIC_MEMORY_EVENT_TYPES.contains(event.getClass())) {
                events.add(event);
            }
        }
        return events;
    }

    @SuppressWarnings("unchecked")
    private static <T extends TraceStateEvent> T single(DecisionTrace trace, Class<T> type) {
        T found = null;
        for (TraceStateEvent event : trace.getStateEvents()) {
            if (type.isInstance(event)) {
                if (found != null) {
                    fail("expected exactly one " + type.getSimpleName() + ": " + trace.getStateEvents());
                }
                found = (T) event;
            }
        }
        if (found == null) {
            fail("expected exactly one " + type.getSimpleName() + ": " + trace.getStateEvents());
        }
        return found;
    }

    private static void assertNone(DecisionTrace trace, Class<? extends TraceStateEvent> type) {
        for (TraceStateEvent event : trace.getStateEvents()) {
            assertFalse("no " + type.getSimpleName() + " may exist here: " + event,
                type.isInstance(event));
        }
    }

    private static Map<String, String[]> transferParams(String cardId, String blueprintId,
                                                        String actionText) {
        Map<String, String[]> params = new HashMap<>();
        params.put("actionId", new String[]{"0"});
        params.put("actionText", new String[]{actionText});
        params.put("cardId", new String[]{cardId});
        params.put("blueprintId", new String[]{blueprintId});
        params.put("noPass", new String[]{"true"});
        return params;
    }

    private static Map<String, String[]> fireParams() {
        Map<String, String[]> params = new HashMap<>();
        params.put("actionId", new String[]{"0"});
        params.put("actionText", new String[]{"Fire blaster at stormtrooper"});
        params.put("cardId", new String[]{"7"});
        params.put("blueprintId", new String[]{"204_9"});
        params.put("noPass", new String[]{"true"});
        return params;
    }

    private static Map<String, String[]> multipleChoiceParams() {
        Map<String, String[]> params = new HashMap<>();
        params.put("results", new String[]{"Option A", "Option B"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        params.put("noPass", new String[]{"true"});
        return params;
    }

    private static Map<String, String[]> verificationParams(String... blueprintIds) {
        Map<String, String[]> params = new HashMap<>();
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"0"});
        params.put("blueprintId", blueprintIds);
        return params;
    }

    private static final String VERIFY_TEXT = "Verify: unsuccessful attempt to search Reserve Deck";

    // =========================================================================
    // Exact packet order on one fallback decision reaching five owner boundaries
    // =========================================================================

    @Test
    public void fallbackBoundariesEmitInExactPacketOrder() {
        MemoryProbeAi ai = new MemoryProbeAi();
        TracedDecision traced = decideTraced(ai,
            decision(1, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
                transferParams("12", "200_5", "Transfer stolen blaster to Vader")),
            new StubGameState(1));
        assertEquals("0", traced.response());

        List<TraceStateEvent> events = traced.trace().getStateEvents();
        assertEquals("expected the exact packet order: " + events, 8, events.size());
        assertTrue(events.get(0) instanceof TrackerPhaseChangeEvent);
        assertTrue(events.get(1) instanceof TrackerUpdateStateEvent);

        // heuristic STATE_UPDATE at helper exit, AFTER the nested shared UPDATE_STATE
        HeuristicStateUpdateEvent stateUpdate = (HeuristicStateUpdateEvent) events.get(2);
        assertEquals(0, stateUpdate.handSize());
        assertEquals(4, stateUpdate.forcePile());
        assertEquals(20, stateUpdate.reserveDeck());
        assertEquals(1, stateUpdate.turn());
        assertEquals(0, stateUpdate.cardsInPlay());
        assertEquals("", stateUpdate.before().currentStateHash());
        assertEquals("0:4:20:1:0", stateUpdate.after().currentStateHash());
        assertEquals(MutationOutcome.CHANGED, stateUpdate.outcome());
        assertTrue(stateUpdate.prunedReassignmentTurns().isEmpty());

        // then the five post-selection helpers in exact source order
        HeuristicActionChoiceRememberEvent remember =
            (HeuristicActionChoiceRememberEvent) events.get(3);
        assertEquals("CARD_ACTION_CHOICE", remember.decisionType());
        assertEquals("0", remember.result());
        assertEquals(0, remember.index());
        assertEquals("", remember.before().lastActionChoiceText());
        assertEquals("transfer stolen blaster to vader", remember.after().lastActionChoiceText());
        assertEquals("12", remember.after().lastActionChoiceCardId());
        assertEquals("200_5", remember.after().lastActionChoiceBlueprintId());
        assertEquals(MutationOutcome.CHANGED, remember.outcome());

        HeuristicSingleResponseRecordEvent single =
            (HeuristicSingleResponseRecordEvent) events.get(4);
        assertEquals("CARD_ACTION_CHOICE", single.decisionType());
        assertEquals("Choose action", single.decisionText());
        assertEquals("0", single.rawResponse());
        assertEquals("transfer stolen blaster to vader", single.trackingResponse());
        assertEquals("CARD_ACTION_CHOICE:Choose action", single.after().lastDecisionKey());
        assertEquals(1, single.after().lastDecisionRepeatCount());
        assertEquals(MutationOutcome.CHANGED, single.outcome());

        HeuristicRecentResponseAppendEvent recent =
            (HeuristicRecentResponseAppendEvent) events.get(5);
        assertEquals("CARD_ACTION_CHOICE:Choose action", recent.decisionKey());
        assertEquals("transfer stolen blaster to vader", recent.appendedResponse());
        assertTrue(recent.dequeBefore().isEmpty());
        assertEquals(List.of("transfer stolen blaster to vader"), recent.dequeAfter());
        assertTrue(recent.evictedRows().isEmpty());
        assertEquals(MutationOutcome.CHANGED, recent.outcome());

        HeuristicReassignmentRecordEvent reassignment =
            (HeuristicReassignmentRecordEvent) events.get(6);
        assertEquals(HeuristicReassignmentRecordEvent.Variant.CARD, reassignment.variant());
        assertEquals("card:12", reassignment.key());
        assertEquals(1, reassignment.turn());
        assertNull(reassignment.turnBefore());
        assertEquals(1, reassignment.turnAfter());
        assertNull(reassignment.countBefore());
        assertEquals(1, reassignment.countAfter());
        assertEquals(MutationOutcome.CHANGED, reassignment.outcome());

        // shared RECORD_RESPONSE stays last, separate from every 4B1 event
        assertTrue(events.get(7) instanceof TrackerRecordResponseEvent);
    }

    // =========================================================================
    // Early-guard suppression paths
    // =========================================================================

    @Test
    public void nullGameStateSuppressesEveryHeuristicMemoryBoundary() {
        MemoryProbeAi ai = new MemoryProbeAi();
        TracedDecision traced = decideTraced(ai,
            decision(2, AwaitingDecisionType.MULTIPLE_CHOICE, "Pick one", multipleChoiceParams()),
            null);
        // null game/player suppresses STATE_UPDATE; the empty state hash suppresses the
        // single-response and recent-response helpers; the type guards suppress the rest
        assertTrue("no heuristic-memory event without game state: " + traced.trace().getStateEvents(),
            heuristicMemoryEvents(traced.trace()).isEmpty());
        assertEquals("only the shared RECORD_RESPONSE runs on this route: "
            + traced.trace().getStateEvents(), 1, traced.trace().getStateEvents().size());
        assertTrue(traced.trace().getStateEvents().get(0) instanceof TrackerRecordResponseEvent);
    }

    @Test
    public void failedGameStateReadSuppressesStateUpdateAndStateHashGatedBoundaries() {
        MemoryProbeAi ai = new MemoryProbeAi();
        TracedDecision traced = decideTraced(ai,
            decision(3, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action", fireParams()),
            new ThrowingHandGameState(1));
        // the caught getter throw returns before any owned write: no STATE_UPDATE; the
        // still-empty state hash suppresses SINGLE_RESPONSE_RECORD and
        // RECENT_RESPONSE_APPEND; the zero turn suppresses REASSIGNMENT_RECORD; only
        // the hash-independent ACTION_CHOICE_REMEMBER write executes
        List<TraceStateEvent> heuristic = heuristicMemoryEvents(traced.trace());
        assertEquals("only ACTION_CHOICE_REMEMBER may fire: " + traced.trace().getStateEvents(),
            1, heuristic.size());
        HeuristicActionChoiceRememberEvent remember =
            (HeuristicActionChoiceRememberEvent) heuristic.get(0);
        assertEquals("fire blaster at stormtrooper", remember.after().lastActionChoiceText());
        assertEquals("", remember.after().currentStateHash());
        assertNone(traced.trace(), HeuristicStateUpdateEvent.class);
        assertNone(traced.trace(), TrackerUpdateStateEvent.class);
    }

    @Test
    public void multipleChoiceTypeGuardsLimitTheBoundariesToStateUpdateAndSingleResponse() {
        MemoryProbeAi ai = new MemoryProbeAi();
        TracedDecision traced = decideTraced(ai,
            decision(4, AwaitingDecisionType.MULTIPLE_CHOICE, "Pick one", multipleChoiceParams()),
            new StubGameState(1));
        List<TraceStateEvent> heuristic = heuristicMemoryEvents(traced.trace());
        assertEquals("MULTIPLE_CHOICE reaches exactly two owner boundaries: "
            + traced.trace().getStateEvents(), 2, heuristic.size());
        assertTrue(heuristic.get(0) instanceof HeuristicStateUpdateEvent);
        HeuristicSingleResponseRecordEvent single =
            (HeuristicSingleResponseRecordEvent) heuristic.get(1);
        assertEquals("MULTIPLE_CHOICE", single.decisionType());
        assertEquals(MutationOutcome.CHANGED, single.outcome());
    }

    @Test
    public void passPickResetsTheLoopAndSuppressesRecentAndReassignmentBoundaries() {
        MemoryProbeAi ai = new MemoryProbeAi();
        Map<String, String[]> params = new HashMap<>();
        params.put("actionId", new String[]{"0", "1"});
        params.put("actionText", new String[]{"Transfer stolen blaster to Vader", "Pass"});
        params.put("cardId", new String[]{"12", ""});
        params.put("blueprintId", new String[]{"200_5", ""});
        params.put("noPass", new String[]{"false"});
        TracedDecision traced = decideTraced(ai,
            decision(5, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action", params),
            new StubGameState(1));
        // the empty weights make pass (-200) beat the reassignment action (-300)
        assertEquals("1", traced.response());

        // the pass pick still executes the tuple write (legacy behavior preserved)
        HeuristicActionChoiceRememberEvent remember =
            single(traced.trace(), HeuristicActionChoiceRememberEvent.class);
        assertEquals("pass", remember.after().lastActionChoiceText());

        // the pass-like tracking response takes the empty-key reset exit: an executed
        // write on a fresh owner, so a real NO_OP event, never suppression
        HeuristicSingleResponseRecordEvent singleEvent =
            single(traced.trace(), HeuristicSingleResponseRecordEvent.class);
        assertEquals("", singleEvent.trackingResponse());
        assertEquals(MutationOutcome.NO_OP, singleEvent.outcome());

        // the empty tracking response and the pass response suppress the last two
        assertNone(traced.trace(), HeuristicRecentResponseAppendEvent.class);
        assertNone(traced.trace(), HeuristicReassignmentRecordEvent.class);
    }

    // =========================================================================
    // Empty-key executed writes on the single-response reset exit
    // =========================================================================

    @Test
    public void emptyCardSelectionExecutesTheResetWritesOnBothOutcomes() {
        MemoryProbeAi ai = new MemoryProbeAi();
        StubGameState state = new StubGameState(1);
        decideTraced(ai, decision(6, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
            fireParams()), state);

        Map<String, String[]> emptySelection = new HashMap<>();
        emptySelection.put("min", new String[]{"0"});
        TracedDecision reset = decideTraced(ai,
            decision(7, AwaitingDecisionType.CARD_SELECTION, "Choose device to steal",
                emptySelection), state);
        assertEquals("", reset.response());
        HeuristicSingleResponseRecordEvent changed =
            single(reset.trace(), HeuristicSingleResponseRecordEvent.class);
        assertEquals("", changed.rawResponse());
        assertEquals("", changed.trackingResponse());
        assertEquals("CARD_ACTION_CHOICE:Choose action", changed.before().lastDecisionKey());
        assertEquals("", changed.after().lastDecisionKey());
        assertEquals(0, changed.after().lastDecisionRepeatCount());
        assertEquals(MutationOutcome.CHANGED, changed.outcome());

        // the same reset re-executed on already-reset fields: a real NO_OP event
        TracedDecision repeat = decideTraced(ai,
            decision(8, AwaitingDecisionType.CARD_SELECTION, "Choose device to steal",
                emptySelection), state);
        HeuristicSingleResponseRecordEvent noOp =
            single(repeat.trace(), HeuristicSingleResponseRecordEvent.class);
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());
        assertNone(repeat.trace(), HeuristicRecentResponseAppendEvent.class);
        assertNone(repeat.trace(), HeuristicActionChoiceRememberEvent.class);
    }

    // =========================================================================
    // Local-block folding, repeat law, and the identical-rewrite NO_OP
    // =========================================================================

    @Test
    public void repeatedDecisionFoldsTheLocalBlockIntoSingleResponseRecord() {
        MemoryProbeAi ai = new MemoryProbeAi();
        StubGameState state = new StubGameState(1);
        TracedDecision first = decideTraced(ai,
            decision(9, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action", fireParams()),
            state);
        HeuristicSingleResponseRecordEvent firstSingle =
            single(first.trace(), HeuristicSingleResponseRecordEvent.class);
        assertEquals(1, firstSingle.after().lastDecisionRepeatCount());
        assertTrue(firstSingle.after().localBlockedResponses().isEmpty());

        TracedDecision second = decideTraced(ai,
            decision(10, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action", fireParams()),
            state);
        assertEquals(first.response(), second.response());

        // the identical tuple rewrite is an executed-write NO_OP
        HeuristicActionChoiceRememberEvent rememberRepeat =
            single(second.trace(), HeuristicActionChoiceRememberEvent.class);
        assertEquals(MutationOutcome.NO_OP, rememberRepeat.outcome());

        // the repeat reaches the loop threshold; the internally created local block is
        // FOLDED into this one event via the localBlockedResponses delta, and no
        // seventh event family exists anywhere in the stream
        HeuristicSingleResponseRecordEvent secondSingle =
            single(second.trace(), HeuristicSingleResponseRecordEvent.class);
        assertEquals(1, secondSingle.before().lastDecisionRepeatCount());
        assertEquals(2, secondSingle.after().lastDecisionRepeatCount());
        assertTrue(secondSingle.before().localBlockedResponses().isEmpty());
        assertEquals(List.of("0", "fire blaster at stormtrooper"),
            secondSingle.after().localBlockedResponses().get("CARD_ACTION_CHOICE:Choose action"));
        assertEquals(MutationOutcome.CHANGED, secondSingle.outcome());
        assertEquals("exactly four heuristic events, no separate local-block event: "
            + second.trace().getStateEvents(), 4, heuristicMemoryEvents(second.trace()).size());

        // the second STATE_UPDATE saw identical state: an executed-write NO_OP
        HeuristicStateUpdateEvent secondState =
            single(second.trace(), HeuristicStateUpdateEvent.class);
        assertEquals(MutationOutcome.NO_OP, secondState.outcome());
    }

    // =========================================================================
    // Six-entry recent-response FIFO proven with seven appends
    // =========================================================================

    @Test
    public void sevenAppendsProveTheSixEntryRecentResponseFifo() {
        MemoryProbeAi ai = new MemoryProbeAi();
        StubGameState state = new StubGameState(1);
        String expected = "fire blaster at stormtrooper";
        HeuristicRecentResponseAppendEvent last = null;
        for (int i = 1; i <= 7; i++) {
            TracedDecision traced = decideTraced(ai,
                decision(10 + i, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
                    fireParams()), state);
            last = single(traced.trace(), HeuristicRecentResponseAppendEvent.class);
            assertEquals(expected, last.appendedResponse());
            if (i <= 6) {
                assertEquals(i - 1, last.dequeBefore().size());
                assertEquals(i, last.dequeAfter().size());
                assertTrue(last.evictedRows().isEmpty());
                assertEquals(MutationOutcome.CHANGED, last.outcome());
            }
        }
        // the seventh append evicts the oldest row and keeps exactly six entries; with
        // identical entries the deque is unchanged: a real executed-write NO_OP
        assertNotNull(last);
        assertEquals(6, last.dequeBefore().size());
        assertEquals(6, last.dequeAfter().size());
        assertEquals(List.of(expected), last.evictedRows());
        assertEquals(List.of(expected, expected, expected, expected, expected, expected),
            last.dequeAfter());
        assertEquals(MutationOutcome.NO_OP, last.outcome());
    }

    // =========================================================================
    // Turn advance pruning and turn rollback
    // =========================================================================

    @Test
    public void turnAdvancePrunesOnlyExpiredRowsWhileCountsPersist() {
        MemoryProbeAi ai = new MemoryProbeAi();
        decideTraced(ai, decision(21, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
            transferParams("12", "200_5", "Transfer stolen blaster to Vader")),
            new StubGameState(1));

        // turn 2: the turn-1 row is within the one-turn memory and survives
        TracedDecision secondTurn = decideTraced(ai,
            decision(22, AwaitingDecisionType.MULTIPLE_CHOICE, "Pick one", multipleChoiceParams()),
            new StubGameState(2));
        HeuristicStateUpdateEvent kept = single(secondTurn.trace(), HeuristicStateUpdateEvent.class);
        assertTrue(kept.prunedReassignmentTurns().isEmpty());
        assertEquals(Integer.valueOf(1), kept.after().recentReassignmentTurns().get("card:12"));

        // turn 3: the row expires; ONLY recentReassignmentTurns is pruned and
        // reassignmentCounts persists until rollback (4A0 matrix correction)
        TracedDecision thirdTurn = decideTraced(ai,
            decision(23, AwaitingDecisionType.MULTIPLE_CHOICE, "Pick one", multipleChoiceParams()),
            new StubGameState(3));
        HeuristicStateUpdateEvent pruned = single(thirdTurn.trace(), HeuristicStateUpdateEvent.class);
        assertEquals(Map.of("card:12", 1), pruned.prunedReassignmentTurns());
        assertTrue(pruned.after().recentReassignmentTurns().isEmpty());
        assertEquals(Integer.valueOf(1), pruned.after().reassignmentCounts().get("card:12"));
        assertEquals(MutationOutcome.CHANGED, pruned.outcome());
    }

    @Test
    public void turnRollbackClearsBothReassignmentMapsWithNoPrunedRows() {
        MemoryProbeAi ai = new MemoryProbeAi();
        decideTraced(ai, decision(24, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
            transferParams("12", "200_5", "Transfer stolen blaster to Vader")),
            new StubGameState(2));

        TracedDecision rollback = decideTraced(ai,
            decision(25, AwaitingDecisionType.MULTIPLE_CHOICE, "Pick one", multipleChoiceParams()),
            new StubGameState(1));
        HeuristicStateUpdateEvent event = single(rollback.trace(), HeuristicStateUpdateEvent.class);
        assertEquals(Integer.valueOf(2), event.before().recentReassignmentTurns().get("card:12"));
        assertEquals(Integer.valueOf(1), event.before().reassignmentCounts().get("card:12"));
        assertTrue(event.after().recentReassignmentTurns().isEmpty());
        assertTrue(event.after().reassignmentCounts().isEmpty());
        assertTrue("rollback prunes nothing: the maps were cleared first",
            event.prunedReassignmentTurns().isEmpty());
        assertEquals(MutationOutcome.CHANGED, event.outcome());
    }

    // =========================================================================
    // Reassignment key precedence: card over blueprint over text, both map deltas
    // =========================================================================

    @Test
    public void reassignmentKeyPrecedenceIsCardThenBlueprintThenText() {
        MemoryProbeAi ai = new MemoryProbeAi();
        StubGameState state = new StubGameState(1);

        // card id present: card wins over the non-sentinel blueprint and the text
        TracedDecision card = decideTraced(ai,
            decision(26, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose transfer action",
                transferParams("12", "200_5", "Transfer stolen blaster to Vader")), state);
        HeuristicReassignmentRecordEvent cardEvent =
            single(card.trace(), HeuristicReassignmentRecordEvent.class);
        assertEquals(HeuristicReassignmentRecordEvent.Variant.CARD, cardEvent.variant());
        assertEquals("card:12", cardEvent.key());

        // sentinel blueprint falls through to the extracted text subject
        TracedDecision text = decideTraced(ai,
            decision(27, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose second transfer",
                transferParams("", "inplay", "Transfer stolen blaster to Vader")), state);
        HeuristicReassignmentRecordEvent textEvent =
            single(text.trace(), HeuristicReassignmentRecordEvent.class);
        assertEquals(HeuristicReassignmentRecordEvent.Variant.TEXT, textEvent.variant());
        assertEquals("text:stolen blaster", textEvent.key());

        // non-sentinel blueprint beats the text when the card id is empty
        TracedDecision blueprint = decideTraced(ai,
            decision(28, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose relocate action",
                transferParams("", "200_7", "Relocate probe droid to Hoth")), state);
        HeuristicReassignmentRecordEvent blueprintEvent =
            single(blueprint.trace(), HeuristicReassignmentRecordEvent.class);
        assertEquals(HeuristicReassignmentRecordEvent.Variant.BLUEPRINT, blueprintEvent.variant());
        assertEquals("blueprint:200_7", blueprintEvent.key());

        // each fresh key carries null prior rows and a folded count of one
        for (HeuristicReassignmentRecordEvent event
                : List.of(cardEvent, textEvent, blueprintEvent)) {
            assertNull(event.turnBefore());
            assertEquals(1, event.turnAfter());
            assertNull(event.countBefore());
            assertEquals(1, event.countAfter());
            assertEquals(MutationOutcome.CHANGED, event.outcome());
        }
    }

    @Test
    public void repeatedReassignmentRewritesTheTurnAndIncrementsTheFoldedCount() {
        MemoryProbeAi ai = new MemoryProbeAi();
        decideTraced(ai, decision(29, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
            transferParams("12", "200_5", "Transfer stolen blaster to Vader")),
            new StubGameState(1));
        TracedDecision repeat = decideTraced(ai,
            decision(30, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
                transferParams("12", "200_5", "Transfer stolen blaster to Vader")),
            new StubGameState(2));
        HeuristicReassignmentRecordEvent event =
            single(repeat.trace(), HeuristicReassignmentRecordEvent.class);
        assertEquals("card:12", event.key());
        assertEquals(2, event.turn());
        assertEquals(Integer.valueOf(1), event.turnBefore());
        assertEquals(2, event.turnAfter());
        assertEquals(Integer.valueOf(1), event.countBefore());
        assertEquals(2, event.countAfter());
        assertEquals(MutationOutcome.CHANGED, event.outcome());
    }

    // =========================================================================
    // Reserve verification: exact match adds, repeat NO_OP, mismatch nothing,
    // throwing getter propagates uncaught with no event and no state change
    // =========================================================================

    @Test
    public void reserveVerificationMatchRepeatMismatchAndThrowBehaveExactly() {
        MemoryProbeAi ai = new MemoryProbeAi();
        StubGameState armingState = new StubGameState(1);
        decideTraced(ai, decision(31, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
            fireParams()), armingState);

        // exact unsuccessful-search verification: all three armed identities added
        StubGameState matching = new StubGameState(1, 4, List.of(reserveCard("200_1")));
        TracedDecision match = decideTraced(ai,
            decision(32, AwaitingDecisionType.ARBITRARY_CARDS, VERIFY_TEXT,
                verificationParams("200_1")), matching);
        HeuristicFailedSearchAddEvent added =
            single(match.trace(), HeuristicFailedSearchAddEvent.class);
        assertEquals("fire blaster at stormtrooper", added.priorActionText());
        assertEquals("7", added.priorCardId());
        assertEquals("204_9", added.priorBlueprintId());
        assertEquals(List.of("fire blaster at stormtrooper"), added.addedActionTexts());
        assertEquals(List.of("7"), added.addedCardIds());
        assertEquals(List.of("204_9"), added.addedBlueprintIds());
        assertEquals(MutationOutcome.CHANGED, added.outcome());

        // repeated verification: adds executed, memberships unchanged, real NO_OP
        TracedDecision repeat = decideTraced(ai,
            decision(33, AwaitingDecisionType.ARBITRARY_CARDS, VERIFY_TEXT,
                verificationParams("200_1")), matching);
        HeuristicFailedSearchAddEvent noOp =
            single(repeat.trace(), HeuristicFailedSearchAddEvent.class);
        assertEquals(MutationOutcome.NO_OP, noOp.outcome());
        assertTrue(noOp.addedActionTexts().isEmpty());
        assertTrue(noOp.addedCardIds().isEmpty());
        assertTrue(noOp.addedBlueprintIds().isEmpty());

        // mismatched reserve order/content: the guard fails and nothing is emitted
        StubGameState mismatched = new StubGameState(1, 4, List.of(reserveCard("999_9")));
        TracedDecision mismatch = decideTraced(ai,
            decision(34, AwaitingDecisionType.ARBITRARY_CARDS, VERIFY_TEXT,
                verificationParams("200_1")), mismatched);
        assertNone(mismatch.trace(), HeuristicFailedSearchAddEvent.class);

        // throwing reserve-deck getter: uncaught legacy propagation, no event, no
        // failed-search state change
        assertTrue(TraceSession.open("heuristic-4b1-fixture", "35", "ARBITRARY_CARDS",
            VERIFY_TEXT, List.of(), null,
            List.of("focused 4B1 owner fixture: snapshot deliberately absent"), false));
        RuntimeException thrown = null;
        try {
            ai.decide("tester", decision(35, AwaitingDecisionType.ARBITRARY_CARDS, VERIFY_TEXT,
                verificationParams("200_1")), new ThrowingReserveGameState(1));
        } catch (RuntimeException e) {
            thrown = e;
        }
        DecisionTrace throwTrace = TraceSession.close();
        assertFalse(TraceSession.isActive());
        assertNotNull("the reserve-deck getter throw must propagate uncaught", thrown);
        assertEquals("injected reserve-deck read failure", thrown.getMessage());
        assertNotNull(throwTrace);
        assertNone(throwTrace, HeuristicFailedSearchAddEvent.class);

        // the sets kept exactly the earlier memberships: proven from the next
        // decision's own before snapshot, and retained with no reset invented
        TracedDecision after = decideTraced(ai,
            decision(36, AwaitingDecisionType.MULTIPLE_CHOICE, "Pick one",
                multipleChoiceParams()), armingState);
        HeuristicStateUpdateEvent stateAfter =
            single(after.trace(), HeuristicStateUpdateEvent.class);
        assertEquals(List.of("fire blaster at stormtrooper"),
            stateAfter.before().failedSearchActionTexts());
        assertEquals(List.of("7"), stateAfter.before().failedSearchCardIds());
        assertEquals(List.of("204_9"), stateAfter.before().failedSearchBlueprintIds());
    }

    // =========================================================================
    // State-hash reset scope: clears local/recent memory and the repeat count,
    // retains failed-search sets (the packet's retention rule, no reset invented)
    // =========================================================================

    @Test
    public void stateHashChangeClearsOnlyLocalAndRecentMemoryAndTheRepeatCount() {
        MemoryProbeAi ai = new MemoryProbeAi();
        StubGameState state = new StubGameState(1);
        decideTraced(ai, decision(41, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
            fireParams()), state);
        decideTraced(ai, decision(42, AwaitingDecisionType.CARD_ACTION_CHOICE, "Choose action",
            fireParams()), state);
        StubGameState verifying = new StubGameState(1, 4, List.of(reserveCard("200_1")));
        decideTraced(ai, decision(43, AwaitingDecisionType.ARBITRARY_CARDS, VERIFY_TEXT,
            verificationParams("200_1")), verifying);

        // a different force pile changes the state hash
        TracedDecision changed = decideTraced(ai,
            decision(44, AwaitingDecisionType.MULTIPLE_CHOICE, "Pick one",
                multipleChoiceParams()), new StubGameState(1, 9, List.of()));
        HeuristicStateUpdateEvent event = single(changed.trace(), HeuristicStateUpdateEvent.class);
        assertFalse(event.before().localBlockedResponses().isEmpty());
        assertFalse(event.before().recentDecisionResponses().isEmpty());
        assertTrue(event.after().localBlockedResponses().isEmpty());
        assertTrue(event.after().recentDecisionResponses().isEmpty());
        assertEquals(0, event.after().lastDecisionRepeatCount());
        assertEquals("0:9:20:1:0", event.after().currentStateHash());
        assertEquals("0:9:20:1:0", event.after().blockStateHash());
        // retention: failed-search sets and the action tuple survive the hash change
        assertEquals(event.before().failedSearchActionTexts(),
            event.after().failedSearchActionTexts());
        assertFalse(event.after().failedSearchActionTexts().isEmpty());
        assertEquals(event.before().lastActionChoiceText(),
            event.after().lastActionChoiceText());
        assertEquals(MutationOutcome.CHANGED, event.outcome());
    }

    // =========================================================================
    // Failure injection at the owner level: every append throws, legacy mutators
    // still run exactly once, response identical, envelope INCOMPLETE/STATE_EVENT
    // =========================================================================

    @Test
    public void injectedAppendFailureCoversAllHeuristicMemoryBoundariesExactlyOnce() {
        AwaitingDecision first = decision(51, AwaitingDecisionType.CARD_ACTION_CHOICE,
            "Choose action", transferParams("12", "200_5", "Transfer stolen blaster to Vader"));
        AwaitingDecision second = decision(52, AwaitingDecisionType.CARD_ACTION_CHOICE,
            "Choose action", transferParams("12", "200_5", "Transfer stolen blaster to Vader"));
        StubGameState state = new StubGameState(1);

        // untraced twin: the same two decisions on a fresh owner
        MemoryProbeAi untraced = new MemoryProbeAi();
        String untraced1 = untraced.decide("tester", first, state);
        String untraced2 = untraced.decide("tester", second, state);
        assertFalse(TraceSession.isActive());

        // run 1: every state-event append throws
        MemoryProbeAi probed = new MemoryProbeAi();
        TraceStateEventFailureTestSupport.openThrowingStateEventSession();
        String run1;
        DecisionTrace failureTrace;
        try {
            run1 = probed.decide("tester", first, state);
            assertTrue("the injected session must survive the decide call",
                TraceSession.isActive());
        } finally {
            failureTrace = TraceStateEventFailureTestSupport.close();
        }
        assertFalse(TraceSession.isActive());
        assertEquals(untraced1, run1);

        assertNotNull(failureTrace);
        assertEquals(TraceStatus.INCOMPLETE, failureTrace.getStatus());
        assertTrue("no event may survive an append failure",
            failureTrace.getStateEvents().isEmpty());
        long injected = failureTrace.getCaptureFailures().stream().filter(failure ->
            failure.stage() == TraceCaptureFailure.Stage.STATE_EVENT
                && failure.detail().contains("injected state-event append failure")).count();
        // shared PHASE_CHANGE, shared UPDATE_STATE, heuristic STATE_UPDATE,
        // ACTION_CHOICE_REMEMBER, SINGLE_RESPONSE_RECORD, RECENT_RESPONSE_APPEND,
        // REASSIGNMENT_RECORD, shared RECORD_RESPONSE = 8 attempted appends
        assertEquals("one typed failure per attempted append: "
            + failureTrace.getCaptureFailures(), 8, injected);

        // run 2 with normal capture: the owners' own before snapshots prove every
        // run-1 legacy mutator ran exactly once
        TracedDecision run2 = decideTraced(probed, second, state);
        assertEquals(untraced2, run2.response());

        HeuristicStateUpdateEvent stateUpdate =
            single(run2.trace(), HeuristicStateUpdateEvent.class);
        assertEquals("0:4:20:1:0", stateUpdate.before().currentStateHash());
        assertEquals(MutationOutcome.NO_OP, stateUpdate.outcome());

        HeuristicActionChoiceRememberEvent remember =
            single(run2.trace(), HeuristicActionChoiceRememberEvent.class);
        assertEquals("transfer stolen blaster to vader",
            remember.before().lastActionChoiceText());
        assertEquals(MutationOutcome.NO_OP, remember.outcome());

        HeuristicSingleResponseRecordEvent singleResponse =
            single(run2.trace(), HeuristicSingleResponseRecordEvent.class);
        assertEquals(1, singleResponse.before().lastDecisionRepeatCount());
        assertEquals(2, singleResponse.after().lastDecisionRepeatCount());

        HeuristicRecentResponseAppendEvent recent =
            single(run2.trace(), HeuristicRecentResponseAppendEvent.class);
        assertEquals(List.of("transfer stolen blaster to vader"), recent.dequeBefore());
        assertEquals(2, recent.dequeAfter().size());

        HeuristicReassignmentRecordEvent reassignment =
            single(run2.trace(), HeuristicReassignmentRecordEvent.class);
        assertEquals(Integer.valueOf(1), reassignment.countBefore());
        assertEquals(2, reassignment.countAfter());
        assertEquals(Integer.valueOf(1), reassignment.turnBefore());
        assertEquals(1, reassignment.turnAfter());
    }
}
