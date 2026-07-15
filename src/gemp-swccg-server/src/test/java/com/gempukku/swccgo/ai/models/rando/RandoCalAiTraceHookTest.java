package com.gempukku.swccgo.ai.models.rando;

import com.gempukku.swccgo.ai.models.common.phase.ActivateDecisionRouting;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceCaptureFailure;
import com.gempukku.swccgo.ai.models.common.trace.TraceCorrection;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStateEventFailureTestSupport;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicActionChoiceRememberEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicFailedSearchAddEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicReassignmentRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicRecentResponseAppendEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicSingleResponseRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicStateUpdateEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.MutationOutcome;
import com.gempukku.swccgo.ai.models.common.trace.state.PendingConcedeEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.PendingDeployEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TraceStateEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerBlockResponseEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerClearEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerOwner;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerPhaseChangeEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerUpdateStateEvent;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Route record" + "Finalization record"): scripted-flow proof of the BOT-BOUNDARY
 * hooks: a real decide() call records a direct-interceptor route with its final
 * response and skipped-finalizer flag, and DecisionSafety records typed corrections.
 * No server, no game state (null GameState exercises the null-safe interceptor path).
 *
 * NOTE (increment scope): with no GameState the bot's Side is unknown, so the shadow
 * DecisionSnapshot cannot be constructed and the envelope is honestly INCOMPLETE with a
 * SNAPSHOT-stage failure. Full COMPLETE bot-entry fixtures over a real game state are
 * the contract's landing increment 5.
 */
public class RandoCalAiTraceHookTest {

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

    /** TRACE 4A2a: minimal real-GameState subclass overriding only the getters the
     *  decide() path reads (GameState's public no-arg constructor exists for
     *  snapshots). Mockito cannot instrument Java 21 class files under this build (see
     *  EngineDecisionFixtures) and reflective Proxy needs interfaces, so a plain
     *  subclass is the minimum stand-in. */
    private static class StubGameState extends GameState {
        private final int turn;
        private final Phase phase;

        StubGameState(int turn) {
            this(turn, Phase.DEPLOY);
        }

        StubGameState(int turn, Phase phase) {
            this.turn = turn;
            this.phase = phase;
        }

        @Override
        public String getOpponent(String playerId) {
            return "opponent";
        }

        @Override
        public Side getSide(String playerId) {
            return Side.DARK;
        }

        @Override
        public int getPlayersLatestTurnNumber(String playerId) {
            return turn;
        }

        @Override
        public Phase getCurrentPhase() {
            return phase;
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
            return 4;
        }

        @Override
        public int getReserveDeckSize(String playerId) {
            return 20;
        }

        @Override
        public int getPlayerLifeForce(String playerId) {
            return 40;
        }

        @Override
        public List<PhysicalCard> getUsedPile(String playerId) {
            return List.of();
        }

        @Override
        public PhysicalCard getBattleLocation() {
            return null;
        }
    }

    /** TRACE 4A2b: no HEURISTIC_SHARED event may exist on ANY route that never reached
     *  super.decide(...); asserted on the direct-interceptor fixtures AND on the
     *  packet-required primary-evaluator fixture (m00447). */
    private static void assertNoSharedOwnerEvents(List<TraceStateEvent> events) {
        for (TraceStateEvent e : events) {
            assertFalse("no shared PHASE_CHANGE without super.decide: " + e,
                e instanceof TrackerPhaseChangeEvent);
            assertFalse("no shared BLOCK_RESPONSE without super.decide: " + e,
                e instanceof TrackerBlockResponseEvent);
            if (e instanceof TrackerUpdateStateEvent update) {
                assertFalse("no shared UPDATE_STATE without super.decide: " + e,
                    update.owner() == TrackerOwner.HEURISTIC_SHARED);
            }
            if (e instanceof TrackerRecordResponseEvent record) {
                assertFalse("no shared RECORD_RESPONSE without super.decide: " + e,
                    record.owner() == TrackerOwner.HEURISTIC_SHARED);
            }
        }
    }

    /** TRACE 4B1 (packet scope law): the heuristic-memory owner boundaries live inside
     *  super.decide(...), so primary evaluator and direct interceptor routes must show
     *  ZERO heuristic-memory events; suppressed guards on fallback routes emit nothing. */
    private static void assertNoHeuristicMemoryEvents(List<TraceStateEvent> events) {
        for (TraceStateEvent e : events) {
            assertFalse("no heuristic-memory event may exist here: " + e,
                e instanceof HeuristicStateUpdateEvent
                    || e instanceof HeuristicActionChoiceRememberEvent
                    || e instanceof HeuristicFailedSearchAddEvent
                    || e instanceof HeuristicSingleResponseRecordEvent
                    || e instanceof HeuristicRecentResponseAppendEvent
                    || e instanceof HeuristicReassignmentRecordEvent);
        }
    }

    // =========================================================================
    // Direct interceptor (V45): distinct route + final response, skipped finalizer
    // =========================================================================

    @Test
    public void v45InterceptorRecordsRouteAndFinalResponse() {
        RandoCalAi ai = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);

        Map<String, String[]> params = new HashMap<>();
        params.put("cardId", new String[]{"temp1", "temp2"});
        params.put("selectable", new String[]{"true", "true"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        params.put("noPass", new String[]{"false"});

        String result = ai.decide("tester",
            decision(42, AwaitingDecisionType.CARD_SELECTION,
                "Choose cards to forfeit, if desired", params),
            null);

        // legacy behavior unchanged: V45 passes on the optional forfeit
        assertEquals("", result);
        assertFalse("decide() must not leak a trace session", TraceSession.isActive());

        DecisionTrace trace = sink.single();
        assertNotNull(trace.getRoute());
        assertEquals(TraceRoute.V45_OPTIONAL_FORFEIT, trace.getRoute().selected());
        assertTrue("the direct interceptor skips the common finalizer — recorded, not hidden",
            trace.getFinalization().skippedCommonFinalizer());
        assertTrue(trace.getFinalization().finalResponseRecorded());
        assertEquals("", trace.getFinalization().finalResponse());
        // GATE P0-3: the fields the direct route skips are EXPLICITLY not-applicable
        assertNotNull("pass eligibility must be explicit n/a on a direct route",
            trace.getFinalization().passEligibilityNotApplicableReason());
        assertNotNull("pre-safety winner must be explicit n/a on a direct route",
            trace.getFinalization().preSafetyWinnerNotApplicableReason());
        // full frozen raw input: CARD_SELECTION candidates come from the raw cardId array
        assertEquals(Arrays.asList("temp1", "temp2"), trace.getRawCandidateOrder());
        assertEquals("42", trace.getDecisionId());
        // TRACE 4B1: the direct interceptor never reaches super.decide
        assertNoHeuristicMemoryEvents(trace.getStateEvents());
        assertTrue("bot model is the bot source-package identifier",
            trace.getBotModel().contains("models"));

        // no GameState => Side unknown => snapshot honestly INCOMPLETE (never fabricated)
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        boolean snapshotFailure = false;
        for (TraceCaptureFailure f : trace.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.SNAPSHOT) {
                snapshotFailure = true;
            }
        }
        assertTrue("snapshot construction failure must be typed, not silent", snapshotFailure);
    }

    // =========================================================================
    // DecisionSafety hook: typed correction with before/after (scripted flow)
    // =========================================================================

    @Test
    public void decisionSafetyRecordsTypedCorrection() {
        Map<String, String[]> params = new HashMap<>();
        params.put("noPass", new String[]{"true"});
        params.put("min", new String[]{"1"});
        AwaitingDecision mustChoose = decision(7, AwaitingDecisionType.CARD_ACTION_CHOICE,
            "Choose blast destiny target", params);

        assertTrue(TraceSession.open("test-bot", "7", "CARD_ACTION_CHOICE",
            "Choose blast destiny target", List.of("5", "7"), null,
            List.of("test: snapshot deliberately absent"), false));
        try {
            TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "scripted", null);
            String[] corrected = DecisionSafety.ensureValidResponse(mustChoose, "",
                new String[]{"5", "7"});
            // legacy behavior unchanged: empty response force-corrected to a valid option
            assertTrue(corrected[0].equals("5") || corrected[0].equals("7"));
            assertFalse(corrected[1].isEmpty());

            TraceSession.recordFinalResponse(corrected[0], false);
            DecisionTrace trace = TraceSession.close();
            assertNotNull(trace);
            assertEquals(1, trace.getFinalization().corrections().size());
            TraceCorrection correction = trace.getFinalization().corrections().get(0);
            assertEquals(TraceCorrection.Kind.SAFETY_FORCED_CHOICE, correction.kind());
            assertEquals("", correction.beforeResponse());
            assertEquals(corrected[0], correction.afterResponse());
            assertEquals(corrected[0], trace.getFinalization().finalResponse());
        } finally {
            TraceSession.abandon();
        }
        assertFalse(TraceSession.isActive());
    }

    // =========================================================================
    // TRACE 4A1 (matrix "prove one legacy call with or without tracing") + 4A2b: the
    // same scripted decision through a NoOp sink and a capture sink returns the
    // identical decision result, and the capture sink sees the typed state events in
    // source order: the SHARED tracker RECORD_RESPONSE from super.decide (4A2b), the
    // outer tracker RECORD_RESPONSE, then the pending-deploy SET from the actual
    // direct write in trackStrategicEvents. Null game state exercises the packet's
    // suppression law for the other shared mutators.
    // =========================================================================

    @Test
    public void legacyDecisionIsIdenticalWithOrWithoutTracingAndTypedEventsAreOrdered() {
        Map<String, String[]> params = new HashMap<>();
        params.put("results", new String[]{"Option A", "Option B"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        // noPass=true keeps this deterministic end-to-end: the evaluator lane cannot
        // synthesize a winning pass, so the decision falls to the keyword heuristic's
        // deterministic index pick (no emergency randomness: the pick is non-empty).
        params.put("noPass", new String[]{"true"});

        // run 1: production default NoOp sink — no session opens
        RandoCalAi untraced = new RandoCalAi();
        String untracedResult = untraced.decide("tester",
            decision(77, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            null);
        assertFalse(TraceSession.isActive());

        // run 2: identical fresh bot + identical decision with a capture sink
        RandoCalAi traced = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        traced.setDecisionTraceSinkForTesting(sink);
        String tracedResult = traced.decide("tester",
            decision(77, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            null);
        assertFalse(TraceSession.isActive());

        // identical legacy decision result with or without tracing
        assertNotNull(untracedResult);
        assertEquals(untracedResult, tracedResult);
        assertFalse("scripted choice must pick an option, not pass", tracedResult.isEmpty());

        // the capture sink sees the typed events in list order.
        // TRACE 4A2b: the fallback route ran super.decide, so the SHARED tracker's
        // RECORD_RESPONSE now appears FIRST (source order: shared recordDecision inside
        // super.decide, then the outer recordDecision, then the pending-deploy write).
        // With a null GameState the packet's suppression law holds for the rest of the
        // shared family: phase == null suppresses PHASE_CHANGE, the early
        // updateDecisionTrackerState return suppresses the shared UPDATE_STATE, and the
        // non-empty result suppresses BLOCK_RESPONSE; only the executed mutator emits.
        DecisionTrace trace = sink.single();
        List<TraceStateEvent> events = trace.getStateEvents();
        assertEquals("expected shared RECORD_RESPONSE + outer RECORD_RESPONSE + PENDING_DEPLOY SET: "
            + events, 3, events.size());
        TrackerRecordResponseEvent shared = (TrackerRecordResponseEvent) events.get(0);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, shared.owner());
        assertEquals("MULTIPLE_CHOICE", shared.decisionType());
        assertEquals("77", shared.decisionId());
        // the SHARED call records the heuristic trackingResponse (the lowercased choice
        // text), while the outer call records the final result (the index); the
        // packet's overlap law: both records are real, never coalesced
        String expectedTracking = "0".equals(tracedResult) ? "option a" : "option b";
        assertEquals(expectedTracking, shared.response());
        assertEquals(0, shared.before().sequenceRows().size());
        assertEquals(1, shared.after().sequenceRows().size());
        assertEquals(MutationOutcome.CHANGED, shared.outcome());
        TrackerRecordResponseEvent tracker = (TrackerRecordResponseEvent) events.get(1);
        assertEquals(TrackerOwner.OUTER_RANDO, tracker.owner());
        assertEquals("MULTIPLE_CHOICE", tracker.decisionType());
        assertEquals("77", tracker.decisionId());
        assertEquals(tracedResult, tracker.response());
        // captured AFTER the legacy call: the after snapshot gained the sequence row
        assertEquals(0, tracker.before().sequenceRows().size());
        assertEquals(1, tracker.after().sequenceRows().size());
        assertEquals(tracedResult, tracker.after().sequenceRows().get(0).response());
        assertEquals(MutationOutcome.CHANGED, tracker.outcome());
        assertFalse("shared and outer response records legitimately differ on this decision",
            shared.response().equals(tracker.response()));
        for (TraceStateEvent e : events) {
            assertFalse("null phase must suppress the shared PHASE_CHANGE: " + e,
                e instanceof TrackerPhaseChangeEvent);
            assertFalse("null game state must suppress the shared UPDATE_STATE: " + e,
                e instanceof TrackerUpdateStateEvent);
            assertFalse("non-empty result must suppress the shared BLOCK_RESPONSE: " + e,
                e instanceof TrackerBlockResponseEvent);
        }
        // TRACE 4B1: the null game state suppresses the heuristic STATE_UPDATE, the
        // still-empty state hash suppresses the single-response and recent-response
        // owners, and the type guards suppress the rest: zero heuristic-memory events
        assertNoHeuristicMemoryEvents(events);
        PendingDeployEvent deploySet = (PendingDeployEvent) events.get(2);
        assertEquals(PendingDeployEvent.Operation.SET, deploySet.operation());
        assertNull(deploySet.typeBefore());
        assertEquals("character", deploySet.typeAfter());
        assertEquals(MutationOutcome.CHANGED, deploySet.outcome());
    }

    // =========================================================================
    // TRACE 4A2a (packet "Bot-boundary fixtures"): outer tracker lifecycle events.
    // One method proves the batch: traced and untraced responses identical; the first
    // non-null-game decision records the outer tracker CLEAR before the outer tracker
    // UPDATE_STATE, exactly one event per legacy call; the direct-return interceptor
    // (V45) records no RECORD_RESPONSE; a same-game repeat records no second clear and
    // an update whose outcome follows lifecycle-snapshot equality; the production
    // default (first run below) opens no session and records no lifecycle event.
    // =========================================================================

    @Test
    public void outerTrackerLifecycleEventsFollowTheLegacyCallsExactly() {
        Map<String, String[]> params = new HashMap<>();
        params.put("cardId", new String[]{"temp1", "temp2"});
        params.put("selectable", new String[]{"true", "true"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        params.put("noPass", new String[]{"false"});

        // production default: NoOp sink — no session opens, no lifecycle event exists
        RandoCalAi untraced = new RandoCalAi();
        String untracedResult = untraced.decide("tester",
            decision(51, AwaitingDecisionType.CARD_SELECTION,
                "Choose cards to forfeit, if desired", params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());

        // traced run 1: identical fresh bot + identical decision + identical stub state
        RandoCalAi traced = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        traced.setDecisionTraceSinkForTesting(sink);
        String first = traced.decide("tester",
            decision(51, AwaitingDecisionType.CARD_SELECTION,
                "Choose cards to forfeit, if desired", params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());

        // legacy behavior identical with or without tracing (V45 passes either way)
        assertEquals("", untracedResult);
        assertEquals(untracedResult, first);

        DecisionTrace trace1 = sink.getTraces().get(0);
        assertEquals(TraceRoute.V45_OPTIONAL_FORFEIT, trace1.getRoute().selected());
        List<TraceStateEvent> events1 = trace1.getStateEvents();
        // exact source order, exactly ONE event per legacy call: pending-concede
        // new-game clear, then tracker CLEAR, then tracker UPDATE_STATE
        assertEquals("expected pending-concede clear + tracker CLEAR + UPDATE_STATE: " + events1,
            3, events1.size());
        assertTrue(events1.get(0) instanceof PendingConcedeEvent);

        TrackerClearEvent clear = (TrackerClearEvent) events1.get(1);
        assertEquals(TrackerOwner.OUTER_RANDO, clear.owner());
        assertEquals(TrackerClearEvent.ClearCause.NEW_GAME_RESET, clear.cause());
        // fresh tracker: the unconditional new-game clear had nothing to erase
        assertEquals(MutationOutcome.NO_OP, clear.outcome());

        TrackerUpdateStateEvent update = (TrackerUpdateStateEvent) events1.get(2);
        assertEquals(TrackerOwner.OUTER_RANDO, update.owner());
        // the EXACT legacy call arguments from the stub state
        assertEquals(0, update.handSize());
        assertEquals(4, update.forcePile());
        assertEquals(20, update.reserveDeck());
        assertEquals(1, update.turn());
        assertEquals(0, update.cardsInPlay());
        assertEquals(MutationOutcome.CHANGED, update.outcome());
        assertEquals(0, update.before().lastTurn());
        assertEquals("", update.before().lastStateHash());
        assertEquals(1, update.after().lastTurn());
        assertEquals("0:4:20:1:0", update.after().lastStateHash());

        // the V45 direct return records no RECORD_RESPONSE (control flow preserved)
        for (TraceStateEvent e : events1) {
            assertFalse("V45 direct return must record no RECORD_RESPONSE: " + e,
                e instanceof TrackerRecordResponseEvent);
        }
        // TRACE 4A2b: the V45 direct-interceptor route never reaches super.decide, so
        // ZERO HEURISTIC_SHARED events may exist. This covers the non-super interceptor
        // routes only; the packet's primary-evaluator proof is the dedicated
        // primaryEvaluatorRouteProducesZeroSharedOwnerEvents fixture (m00447).
        assertNoSharedOwnerEvents(events1);
        assertNoHeuristicMemoryEvents(events1);

        // traced run 2: same game (same opponent/side) — no second clear, exactly one
        // update whose outcome follows lifecycle-snapshot equality (nothing moved)
        String second = traced.decide("tester",
            decision(52, AwaitingDecisionType.CARD_SELECTION,
                "Choose cards to forfeit, if desired", params),
            new StubGameState(1));
        assertEquals("", second);
        assertEquals(2, sink.getTraces().size());
        List<TraceStateEvent> events2 = sink.getTraces().get(1).getStateEvents();
        assertEquals("same-game repeat: exactly one UPDATE_STATE, no clear: " + events2,
            1, events2.size());
        TrackerUpdateStateEvent repeat = (TrackerUpdateStateEvent) events2.get(0);
        assertEquals(TrackerOwner.OUTER_RANDO, repeat.owner());
        assertEquals(repeat.before(), repeat.after());
        assertEquals(MutationOutcome.NO_OP, repeat.outcome());
        assertNoSharedOwnerEvents(events2);
        assertNoHeuristicMemoryEvents(events2);
    }

    // =========================================================================
    // TRACE 4A2b (packet "Bot boundary" + "Highest overlap risk" fixtures) + 4B1: on
    // a fallback route the shared events appear in EXACT source order between the
    // outer events: outer new-game clear + outer UPDATE_STATE first, then inside
    // super.decide the shared PHASE_CHANGE, shared UPDATE_STATE (identical call
    // arguments to the outer one, DISTINCT owner, never coalesced), the heuristic
    // STATE_UPDATE summary immediately after it (4B1), SINGLE_RESPONSE_RECORD
    // immediately before the shared RECORD_RESPONSE (trackingResponse), then the
    // outer RECORD_RESPONSE (final result) and the pending-deploy write. Untraced
    // twin proves behavior parity.
    // =========================================================================

    @Test
    public void sharedTrackerEventsFollowTheFallbackRouteInExactSourceOrder() {
        Map<String, String[]> params = new HashMap<>();
        params.put("results", new String[]{"Option A", "Option B"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        params.put("noPass", new String[]{"true"});

        // production default twin: identical decision, NoOp sink, no session
        RandoCalAi untraced = new RandoCalAi();
        String untracedResult = untraced.decide("tester",
            decision(77, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());

        RandoCalAi traced = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        traced.setDecisionTraceSinkForTesting(sink);
        String tracedResult = traced.decide("tester",
            decision(77, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());

        // legacy behavior identical with or without tracing
        assertNotNull(untracedResult);
        assertEquals(untracedResult, tracedResult);
        assertFalse(tracedResult.isEmpty());

        DecisionTrace trace = sink.single();
        assertEquals(TraceRoute.HEURISTIC_FALLBACK, trace.getRoute().selected());
        List<TraceStateEvent> events = trace.getStateEvents();
        assertEquals("expected the full outer+shared+heuristic fallback stream: " + events,
            10, events.size());

        // outer events first, exactly as 4A1/4A2a landed them
        assertTrue(events.get(0) instanceof PendingConcedeEvent);
        TrackerClearEvent clear = (TrackerClearEvent) events.get(1);
        assertEquals(TrackerOwner.OUTER_RANDO, clear.owner());
        TrackerUpdateStateEvent outerUpdate = (TrackerUpdateStateEvent) events.get(2);
        assertEquals(TrackerOwner.OUTER_RANDO, outerUpdate.owner());
        assertEquals(MutationOutcome.CHANGED, outerUpdate.outcome());

        // shared events inside super.decide, in the packet's exact order
        TrackerPhaseChangeEvent phase = (TrackerPhaseChangeEvent) events.get(3);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, phase.owner());
        assertEquals("DEPLOY", phase.phase());
        assertEquals("", phase.before().lastPhase());
        assertEquals("DEPLOY", phase.after().lastPhase());
        assertEquals(MutationOutcome.CHANGED, phase.outcome());

        TrackerUpdateStateEvent sharedUpdate = (TrackerUpdateStateEvent) events.get(4);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedUpdate.owner());
        assertEquals(MutationOutcome.CHANGED, sharedUpdate.outcome());
        // the packet's HIGHEST OVERLAP RISK: identical call arguments, distinct owners,
        // both events present and ordered; the trace never coalesces equal payloads
        assertEquals(outerUpdate.handSize(), sharedUpdate.handSize());
        assertEquals(outerUpdate.forcePile(), sharedUpdate.forcePile());
        assertEquals(outerUpdate.reserveDeck(), sharedUpdate.reserveDeck());
        assertEquals(outerUpdate.turn(), sharedUpdate.turn());
        assertEquals(outerUpdate.cardsInPlay(), sharedUpdate.cardsInPlay());
        assertFalse("owners must stay distinct on identical arguments",
            outerUpdate.owner() == sharedUpdate.owner());

        // TRACE 4B1: the heuristic STATE_UPDATE summary lands immediately after the
        // nested shared UPDATE_STATE, exactly as the packet orders the two summaries
        HeuristicStateUpdateEvent heuristicState = (HeuristicStateUpdateEvent) events.get(5);
        assertEquals(sharedUpdate.handSize(), heuristicState.handSize());
        assertEquals(sharedUpdate.turn(), heuristicState.turn());
        assertEquals("", heuristicState.before().currentStateHash());
        assertEquals("0:4:20:1:0", heuristicState.after().currentStateHash());
        assertEquals(MutationOutcome.CHANGED, heuristicState.outcome());
        assertTrue(heuristicState.prunedReassignmentTurns().isEmpty());

        // TRACE 4B1: SINGLE_RESPONSE_RECORD lands immediately before the shared
        // RECORD_RESPONSE; the other heuristic owners stay guard-suppressed here
        HeuristicSingleResponseRecordEvent singleResponse =
            (HeuristicSingleResponseRecordEvent) events.get(6);
        assertEquals("MULTIPLE_CHOICE", singleResponse.decisionType());
        assertEquals(tracedResult, singleResponse.rawResponse());
        assertEquals(1, singleResponse.after().lastDecisionRepeatCount());
        assertEquals(MutationOutcome.CHANGED, singleResponse.outcome());

        TrackerRecordResponseEvent sharedRecord = (TrackerRecordResponseEvent) events.get(7);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedRecord.owner());
        assertEquals("77", sharedRecord.decisionId());
        assertEquals(MutationOutcome.CHANGED, sharedRecord.outcome());
        // the shared call records the same heuristic tracking response 4B1 observed
        assertEquals(singleResponse.trackingResponse(), sharedRecord.response());
        TrackerRecordResponseEvent outerRecord = (TrackerRecordResponseEvent) events.get(8);
        assertEquals(TrackerOwner.OUTER_RANDO, outerRecord.owner());
        assertEquals(tracedResult, outerRecord.response());
        // shared trackingResponse (choice text) vs outer final result (index)
        assertFalse("the two owners' recorded responses legitimately differ",
            sharedRecord.response().equals(outerRecord.response()));

        PendingDeployEvent deploySet = (PendingDeployEvent) events.get(9);
        assertEquals(PendingDeployEvent.Operation.SET, deploySet.operation());
        assertEquals("character", deploySet.typeAfter());
    }

    // =========================================================================
    // TRACE 4A2b (packet source-order law, optional BLOCK_RESPONSE position): an
    // empty CARD_SELECTION result on the fallback route fires the one direct
    // blockLastActionOnCancel call between the shared UPDATE_STATE and the shared
    // RECORD_RESPONSE; with no armed last action the legacy call returns false and
    // the event is an honest NO_OP.
    // =========================================================================

    @Test
    public void emptyCardSelectionFallbackRecordsTheSharedBlockResponseInOrder() {
        // no cardId array: the evaluator lane produces nothing, the heuristic returns
        // empty, and the decision then hits the DecisionSafety critical-no-options arm
        Map<String, String[]> params = new HashMap<>();
        params.put("min", new String[]{"1"});
        params.put("noPass", new String[]{"false"});

        RandoCalAi untraced = new RandoCalAi();
        String untracedResult = untraced.decide("tester",
            decision(61, AwaitingDecisionType.CARD_SELECTION,
                "Choose device to steal, or click Done to cancel", params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());

        RandoCalAi traced = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        traced.setDecisionTraceSinkForTesting(sink);
        String tracedResult = traced.decide("tester",
            decision(61, AwaitingDecisionType.CARD_SELECTION,
                "Choose device to steal, or click Done to cancel", params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());
        assertEquals(untracedResult, tracedResult);

        DecisionTrace trace = sink.single();
        List<TraceStateEvent> events = trace.getStateEvents();
        assertEquals("expected the fallback stream with the shared BLOCK_RESPONSE: " + events,
            10, events.size());
        assertTrue(events.get(0) instanceof PendingConcedeEvent);
        assertEquals(TrackerOwner.OUTER_RANDO, ((TrackerClearEvent) events.get(1)).owner());
        assertEquals(TrackerOwner.OUTER_RANDO, ((TrackerUpdateStateEvent) events.get(2)).owner());
        assertEquals(TrackerOwner.HEURISTIC_SHARED, ((TrackerPhaseChangeEvent) events.get(3)).owner());
        assertEquals(TrackerOwner.HEURISTIC_SHARED, ((TrackerUpdateStateEvent) events.get(4)).owner());

        // TRACE 4B1: heuristic STATE_UPDATE immediately after the shared UPDATE_STATE
        HeuristicStateUpdateEvent heuristicState = (HeuristicStateUpdateEvent) events.get(5);
        assertEquals(MutationOutcome.CHANGED, heuristicState.outcome());
        assertEquals("0:4:20:1:0", heuristicState.after().currentStateHash());

        TrackerBlockResponseEvent block = (TrackerBlockResponseEvent) events.get(6);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, block.owner());
        assertEquals("CARD_SELECTION", block.decisionType());
        assertEquals("Choose device to steal, or click Done to cancel", block.decisionText());
        // absent last action: the legacy call declined: false return, honest NO_OP
        assertFalse(block.blocked());
        assertEquals(MutationOutcome.NO_OP, block.outcome());

        // TRACE 4B1: the empty response takes the reset exit of the single-response
        // owner; the writes executed on already-reset fields, a real NO_OP event
        HeuristicSingleResponseRecordEvent singleResponse =
            (HeuristicSingleResponseRecordEvent) events.get(7);
        assertEquals("", singleResponse.rawResponse());
        assertEquals("", singleResponse.trackingResponse());
        assertEquals(MutationOutcome.NO_OP, singleResponse.outcome());

        TrackerRecordResponseEvent sharedRecord = (TrackerRecordResponseEvent) events.get(8);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedRecord.owner());
        TrackerRecordResponseEvent outerRecord = (TrackerRecordResponseEvent) events.get(9);
        assertEquals(TrackerOwner.OUTER_RANDO, outerRecord.owner());
    }

    // =========================================================================
    // TRACE 4A2b (packet "Gate additions" + the 4A2a gate's pinned fault-injection
    // debt, m00434): with Codex's prepared throwing collector installed through the
    // TraceSession.openForTesting seam, every state-event append fails, and the
    // legacy mutators still run EXACTLY ONCE. Run 1 proves the typed STATE_EVENT
    // evidence and unchanged behavior; run 2 (normal capture, same bot) proves the
    // exactly-once claim from the owners' own before-snapshots: every tracker carries
    // precisely one decision's worth of run-1 state.
    // =========================================================================

    @Test
    public void injectedAppendFailureNeverSkipsOrRepeatsTheLegacyMutators() {
        Map<String, String[]> params = new HashMap<>();
        params.put("results", new String[]{"Option A", "Option B"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        params.put("noPass", new String[]{"true"});

        // untraced twin: the same two decisions on a fresh bot
        RandoCalAi untraced = new RandoCalAi();
        String untraced1 = untraced.decide("tester",
            decision(77, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            new StubGameState(1));
        String untraced2 = untraced.decide("tester",
            decision(78, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            new StubGameState(1));

        RandoCalAi traced = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        traced.setDecisionTraceSinkForTesting(sink);

        // run 1: the throwing collector owns the thread: the bot's own open is the
        // refused nested open, every hook records into the prepared collector, and
        // every append throws
        TraceStateEventFailureTestSupport.openThrowingStateEventSession();
        String run1 = null;
        DecisionTrace failureTrace;
        try {
            run1 = traced.decide("tester",
                decision(77, AwaitingDecisionType.MULTIPLE_CHOICE,
                    "Deploy which character to your site?", params),
                new StubGameState(1));
            // the bot never owned the session, so it must not have closed or emitted it
            assertTrue("the injected session must survive the decide call",
                TraceSession.isActive());
            assertEquals(0, sink.getTraces().size());
        } finally {
            // close (not abandon): the evidence envelope stays inspectable and the
            // thread-local is always cleared, even if the decide call itself failed
            failureTrace = TraceStateEventFailureTestSupport.close();
        }
        assertFalse(TraceSession.isActive());

        // behavior parity: the injected failures never changed the legacy decision
        assertEquals(untraced1, run1);

        // typed evidence: INCOMPLETE, zero events, one STATE_EVENT failure per append
        assertNotNull(failureTrace);
        assertEquals(TraceStatus.INCOMPLETE, failureTrace.getStatus());
        assertTrue("no event may survive an append failure",
            failureTrace.getStateEvents().isEmpty());
        long injected = failureTrace.getCaptureFailures().stream().filter(failure ->
            failure.stage() == TraceCaptureFailure.Stage.STATE_EVENT
                && failure.detail().contains("injected state-event append failure")).count();
        // pending-concede clear, outer CLEAR, outer UPDATE_STATE, shared PHASE_CHANGE,
        // shared UPDATE_STATE, heuristic STATE_UPDATE, heuristic SINGLE_RESPONSE_RECORD,
        // shared RECORD_RESPONSE, pending-deploy SET = 9 appends (TRACE 4B1 adds the
        // two heuristic-memory boundaries this MULTIPLE_CHOICE route reaches; the
        // outer RECORD_RESPONSE hook is traceOpened-guarded and the bot never owned
        // this session, so its append was never attempted)
        assertEquals("one typed failure per attempted append: " + failureTrace.getCaptureFailures(),
            9, injected);

        // run 2: normal capture on the SAME bot; the owners' before-snapshots prove
        // every run-1 legacy mutator ran exactly once
        String run2 = traced.decide("tester",
            decision(78, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());
        assertEquals(untraced2, run2);

        DecisionTrace trace2 = sink.single();
        List<TraceStateEvent> events2 = trace2.getStateEvents();
        assertEquals("same game: no concede/clear events, full fallback stream: " + events2,
            8, events2.size());

        // outer updateState ran once in run 1: run 2's before already carries turn 1
        TrackerUpdateStateEvent outerUpdate = (TrackerUpdateStateEvent) events2.get(0);
        assertEquals(TrackerOwner.OUTER_RANDO, outerUpdate.owner());
        assertEquals(1, outerUpdate.before().lastTurn());
        assertEquals("0:4:20:1:0", outerUpdate.before().lastStateHash());
        assertEquals(MutationOutcome.NO_OP, outerUpdate.outcome());
        // exactly ONE outer sequence row from run 1's outer recordDecision
        assertEquals(1, outerUpdate.before().decisionState().sequenceRows().size());

        // shared onPhaseChange ran once in run 1: run 2's repeat is an honest NO_OP
        TrackerPhaseChangeEvent phase = (TrackerPhaseChangeEvent) events2.get(1);
        assertEquals("DEPLOY", phase.before().lastPhase());
        assertEquals(MutationOutcome.NO_OP, phase.outcome());

        // shared updateState ran once in run 1: lifecycle state already present
        TrackerUpdateStateEvent sharedUpdate = (TrackerUpdateStateEvent) events2.get(2);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedUpdate.owner());
        assertEquals(1, sharedUpdate.before().lastTurn());
        assertEquals(MutationOutcome.NO_OP, sharedUpdate.outcome());

        // TRACE 4B1: the heuristic state writes ran once in run 1 despite the failed
        // appends: run 2's before already carries the run-1 hash, an executed NO_OP
        HeuristicStateUpdateEvent heuristicState = (HeuristicStateUpdateEvent) events2.get(3);
        assertEquals("0:4:20:1:0", heuristicState.before().currentStateHash());
        assertEquals(MutationOutcome.NO_OP, heuristicState.outcome());

        // TRACE 4B1: the single-response owner ran EXACTLY ONCE in run 1: run 2's
        // repeat count moves 1 to 2 and folds the threshold local block into this event
        HeuristicSingleResponseRecordEvent singleResponse =
            (HeuristicSingleResponseRecordEvent) events2.get(4);
        assertEquals(1, singleResponse.before().lastDecisionRepeatCount());
        assertEquals(2, singleResponse.after().lastDecisionRepeatCount());
        assertTrue(singleResponse.before().localBlockedResponses().isEmpty());
        assertEquals(1, singleResponse.after().localBlockedResponses().size());
        assertEquals(MutationOutcome.CHANGED, singleResponse.outcome());

        // shared recordDecision ran EXACTLY ONCE in run 1: one row before, two after
        TrackerRecordResponseEvent sharedRecord = (TrackerRecordResponseEvent) events2.get(5);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedRecord.owner());
        assertEquals(1, sharedRecord.before().sequenceRows().size());
        assertEquals(2, sharedRecord.after().sequenceRows().size());

        // outer recordDecision ran EXACTLY ONCE in run 1: one row before, two after
        TrackerRecordResponseEvent outerRecord = (TrackerRecordResponseEvent) events2.get(6);
        assertEquals(TrackerOwner.OUTER_RANDO, outerRecord.owner());
        assertEquals(1, outerRecord.before().sequenceRows().size());
        assertEquals(2, outerRecord.after().sequenceRows().size());

        // the run-1 pending-deploy write happened: run 2's rewrite is a NO_OP SET
        PendingDeployEvent deploySet = (PendingDeployEvent) events2.get(7);
        assertEquals(PendingDeployEvent.Operation.SET, deploySet.operation());
        assertEquals("character", deploySet.typeBefore());
        assertEquals("character", deploySet.typeAfter());
        assertEquals(MutationOutcome.NO_OP, deploySet.outcome());
    }

    // =========================================================================
    // TRACE 4A2b (reviewer m00446): the injected-append-failure route above never
    // reaches the direct BLOCK_RESPONSE boundary, so the four-mutator gate needs an
    // injected EMPTY CANCELABLE CARD_SELECTION path too. The exact append count is
    // the not-skipped/not-repeated evidence for the block call (the injected failure
    // details are family-blind): 9 appends (with the two 4B1 heuristic-memory
    // boundaries) means every boundary including the block attempted exactly one
    // append; 8 would be a skip, 10 a repeat. The armed TRUE
    // block is NOT reachable at this boundary: arming the shared last-action pair
    // needs a fallback CARD_ACTION_CHOICE with a non-empty heuristic pick, but any
    // CARD_ACTION_CHOICE with candidates is consumed by the ActionTextEvaluator
    // primary lane, so the armed mutation's exactly-once proof lives at the real
    // tracker level in DecisionTrackerSharedTraceTest.
    // =========================================================================

    @Test
    public void injectedAppendFailureCoversTheDirectBlockResponseBoundary() {
        Map<String, String[]> params = new HashMap<>();
        params.put("min", new String[]{"1"});
        params.put("noPass", new String[]{"false"});

        RandoCalAi untraced = new RandoCalAi();
        String untraced1 = untraced.decide("tester",
            decision(61, AwaitingDecisionType.CARD_SELECTION,
                "Choose device to steal, or click Done to cancel", params),
            new StubGameState(1));
        String untraced2 = untraced.decide("tester",
            decision(62, AwaitingDecisionType.CARD_SELECTION,
                "Choose device to steal, or click Done to cancel", params),
            new StubGameState(1));

        RandoCalAi traced = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        traced.setDecisionTraceSinkForTesting(sink);

        // run 1: every append throws, including the append attempted at the direct
        // BLOCK_RESPONSE boundary
        TraceStateEventFailureTestSupport.openThrowingStateEventSession();
        String run1 = null;
        DecisionTrace failureTrace;
        try {
            run1 = traced.decide("tester",
                decision(61, AwaitingDecisionType.CARD_SELECTION,
                    "Choose device to steal, or click Done to cancel", params),
                new StubGameState(1));
            assertTrue("the injected session must survive the decide call",
                TraceSession.isActive());
            assertEquals(0, sink.getTraces().size());
        } finally {
            failureTrace = TraceStateEventFailureTestSupport.close();
        }
        assertFalse(TraceSession.isActive());
        assertEquals(untraced1, run1);

        assertNotNull(failureTrace);
        assertEquals(TraceStatus.INCOMPLETE, failureTrace.getStatus());
        assertTrue(failureTrace.getStateEvents().isEmpty());
        long injected = failureTrace.getCaptureFailures().stream().filter(failure ->
            failure.stage() == TraceCaptureFailure.Stage.STATE_EVENT
                && failure.detail().contains("injected state-event append failure")).count();
        // pending-concede clear, outer CLEAR, outer UPDATE_STATE, shared PHASE_CHANGE,
        // shared UPDATE_STATE, heuristic STATE_UPDATE, shared BLOCK_RESPONSE, heuristic
        // SINGLE_RESPONSE_RECORD, shared RECORD_RESPONSE = 9 (TRACE 4B1 adds the two
        // heuristic-memory boundaries this empty CARD_SELECTION route reaches; no
        // pending-deploy write: the decision text has no deploy subject; the outer
        // RECORD_RESPONSE hook is traceOpened-guarded on this refused nested open)
        assertEquals("exactly one append per boundary including the direct block: "
            + failureTrace.getCaptureFailures(), 9, injected);

        // run 2: normal capture on the SAME bot. The empty cancel response never
        // enters sequenceRows (the owner tracks non-pass responses only), so the
        // exactly-once proof for run 1's recordDecision is the CANCEL pair (m00449):
        // run 1 started the streak at count 1 under the cancelable decision key, and
        // run 2's legacy call advances exactly that streak to 2, on BOTH owners.
        String run2 = traced.decide("tester",
            decision(62, AwaitingDecisionType.CARD_SELECTION,
                "Choose device to steal, or click Done to cancel", params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());
        assertEquals(untraced2, run2);

        String cancelKey = "CARD_SELECTION:Choose device to steal, or click Done to cancel";
        DecisionTrace trace2 = sink.single();
        List<TraceStateEvent> events2 = trace2.getStateEvents();
        assertEquals("same game: full fallback stream with the block boundary: " + events2,
            8, events2.size());
        assertEquals(TrackerOwner.OUTER_RANDO, ((TrackerUpdateStateEvent) events2.get(0)).owner());
        assertEquals(TrackerOwner.HEURISTIC_SHARED, ((TrackerPhaseChangeEvent) events2.get(1)).owner());
        assertEquals(TrackerOwner.HEURISTIC_SHARED, ((TrackerUpdateStateEvent) events2.get(2)).owner());

        // TRACE 4B1: run 1's heuristic state writes happened despite the failed
        // appends: run 2's before carries them and the repeat is an executed NO_OP
        HeuristicStateUpdateEvent heuristicState = (HeuristicStateUpdateEvent) events2.get(3);
        assertEquals("0:4:20:1:0", heuristicState.before().currentStateHash());
        assertEquals(MutationOutcome.NO_OP, heuristicState.outcome());

        TrackerBlockResponseEvent block = (TrackerBlockResponseEvent) events2.get(4);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, block.owner());
        assertFalse(block.blocked());
        assertEquals(MutationOutcome.NO_OP, block.outcome());

        // TRACE 4B1: run 1 already executed the empty-key reset, so run 2's reset
        // rewrite is an executed-write NO_OP folded boundary event
        HeuristicSingleResponseRecordEvent singleResponse =
            (HeuristicSingleResponseRecordEvent) events2.get(5);
        assertEquals("", singleResponse.rawResponse());
        assertEquals("", singleResponse.trackingResponse());
        assertEquals(MutationOutcome.NO_OP, singleResponse.outcome());

        TrackerRecordResponseEvent sharedRecord = (TrackerRecordResponseEvent) events2.get(6);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedRecord.owner());
        assertEquals(cancelKey, sharedRecord.before().consecutiveCancelKey());
        assertEquals(1, sharedRecord.before().consecutiveCancelCount());
        assertEquals(cancelKey, sharedRecord.after().consecutiveCancelKey());
        assertEquals(2, sharedRecord.after().consecutiveCancelCount());
        assertEquals(0, sharedRecord.before().sequenceRows().size());
        assertEquals(0, sharedRecord.after().sequenceRows().size());
        assertEquals(MutationOutcome.CHANGED, sharedRecord.outcome());
        TrackerRecordResponseEvent outerRecord = (TrackerRecordResponseEvent) events2.get(7);
        assertEquals(TrackerOwner.OUTER_RANDO, outerRecord.owner());
        assertEquals(cancelKey, outerRecord.before().consecutiveCancelKey());
        assertEquals(1, outerRecord.before().consecutiveCancelCount());
        assertEquals(cancelKey, outerRecord.after().consecutiveCancelKey());
        assertEquals(2, outerRecord.after().consecutiveCancelCount());
        assertEquals(0, outerRecord.before().sequenceRows().size());
        assertEquals(0, outerRecord.after().sequenceRows().size());
        assertEquals(MutationOutcome.CHANGED, outerRecord.outcome());
    }

    // =========================================================================
    // TRACE 4A2b (packet suppression law, m00452): a FAILED game-state read
    // suppresses ONLY its unexecuted shared UPDATE_STATE. HeuristicAiBase's helper
    // catches the RuntimeException and RETURNS before its tracker call, so no shared
    // UPDATE_STATE event may exist; the shared PHASE_CHANGE (phase was non-null) and
    // the shared RECORD_RESPONSE still appear in source order. The OUTER helper
    // catches the same exception and CONTINUES with zero defaults, so the outer
    // UPDATE_STATE still appears with all-zero legacy call arguments, identified by
    // its owner. Behavior itself is unchanged legacy code, proven by the untraced twin.
    // =========================================================================

    /** StubGameState whose hand read always throws, poisoning both bots' tracker
     *  state helpers at the exact getter the packet names. */
    private static final class ThrowingHandGameState extends StubGameState {
        ThrowingHandGameState(int turn) {
            super(turn);
        }

        @Override
        public List<PhysicalCard> getHand(String playerId) {
            throw new RuntimeException("injected game-state read failure");
        }
    }

    @Test
    public void failedGameStateReadSuppressesOnlyTheSharedUpdateStateEvent() {
        Map<String, String[]> params = new HashMap<>();
        params.put("results", new String[]{"Option A", "Option B"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        params.put("noPass", new String[]{"true"});

        RandoCalAi untraced = new RandoCalAi();
        String untracedResult = untraced.decide("tester",
            decision(83, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            new ThrowingHandGameState(1));
        assertFalse(TraceSession.isActive());

        RandoCalAi traced = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        traced.setDecisionTraceSinkForTesting(sink);
        String tracedResult = traced.decide("tester",
            decision(83, AwaitingDecisionType.MULTIPLE_CHOICE,
                "Deploy which character to your site?", params),
            new ThrowingHandGameState(1));
        assertFalse(TraceSession.isActive());
        assertEquals(untracedResult, tracedResult);
        assertFalse(tracedResult.isEmpty());

        DecisionTrace trace = sink.single();
        assertEquals(TraceRoute.HEURISTIC_FALLBACK, trace.getRoute().selected());
        List<TraceStateEvent> events = trace.getStateEvents();
        assertEquals("expected the fallback stream WITHOUT the shared UPDATE_STATE: " + events,
            7, events.size());
        assertTrue(events.get(0) instanceof PendingConcedeEvent);
        assertEquals(TrackerOwner.OUTER_RANDO, ((TrackerClearEvent) events.get(1)).owner());

        // the OUTER helper survived the throw and updated with zero defaults
        TrackerUpdateStateEvent outerUpdate = (TrackerUpdateStateEvent) events.get(2);
        assertEquals(TrackerOwner.OUTER_RANDO, outerUpdate.owner());
        assertEquals(0, outerUpdate.handSize());
        assertEquals(0, outerUpdate.forcePile());
        assertEquals(0, outerUpdate.reserveDeck());
        assertEquals(0, outerUpdate.turn());
        assertEquals(0, outerUpdate.cardsInPlay());

        // the shared PHASE_CHANGE still fired (phase was non-null)
        TrackerPhaseChangeEvent phase = (TrackerPhaseChangeEvent) events.get(3);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, phase.owner());
        assertEquals("DEPLOY", phase.phase());

        // the suppressed mutator is the ONLY missing one: no shared UPDATE_STATE
        for (TraceStateEvent e : events) {
            if (e instanceof TrackerUpdateStateEvent update) {
                assertFalse("the failed read must suppress the shared UPDATE_STATE: " + e,
                    update.owner() == TrackerOwner.HEURISTIC_SHARED);
            }
        }
        // TRACE 4B1: the same failed read returns before any heuristic-memory write
        // (no STATE_UPDATE), and the still-empty state hash makes the single-response
        // helper return before any owned write, so the stream stays at seven
        assertNoHeuristicMemoryEvents(events);

        // the shared RECORD_RESPONSE still appears, in source order, before the outer's
        TrackerRecordResponseEvent sharedRecord = (TrackerRecordResponseEvent) events.get(4);
        assertEquals(TrackerOwner.HEURISTIC_SHARED, sharedRecord.owner());
        TrackerRecordResponseEvent outerRecord = (TrackerRecordResponseEvent) events.get(5);
        assertEquals(TrackerOwner.OUTER_RANDO, outerRecord.owner());
        assertTrue(events.get(6) instanceof PendingDeployEvent);
    }

    // =========================================================================
    // TRACE 4A2b (packet "Primary evaluator route" fixture, m00447/m00451): a REAL
    // decide() whose selected route IS the evaluator lane produces ZERO
    // HEURISTIC_SHARED events because super.decide(...) was never called. The
    // deterministic carrier is the exact opponent-allowance INTEGER decision. Generic
    // INTEGER decisions are deliberately left to the inherited heuristic route.
    // =========================================================================

    @Test
    public void primaryEvaluatorRouteProducesZeroSharedOwnerEvents() {
        Map<String, String[]> params = new HashMap<>();
        params.put("min", new String[]{"1"});
        params.put("max", new String[]{"3"});

        // untraced twin proves behavior parity on the evaluator lane too
        RandoCalAi untraced = new RandoCalAi();
        String untracedResult = untraced.decide("tester",
            decision(91, AwaitingDecisionType.INTEGER,
                ActivateDecisionRouting.OPPONENT_ALLOWANCE_PROMPT, params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());

        RandoCalAi traced = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        traced.setDecisionTraceSinkForTesting(sink);
        String tracedResult = traced.decide("tester",
            decision(91, AwaitingDecisionType.INTEGER,
                ActivateDecisionRouting.OPPONENT_ALLOWANCE_PROMPT, params),
            new StubGameState(1));
        assertFalse(TraceSession.isActive());
        assertEquals(untracedResult, tracedResult);

        DecisionTrace trace = sink.single();
        // the packet's premise, asserted rather than assumed: this IS the evaluator lane
        assertEquals(TraceRoute.COMBINED_EVALUATOR, trace.getRoute().selected());
        List<TraceStateEvent> events = trace.getStateEvents();
        // primary evaluator routes do not mutate the inherited tracker: zero shared events
        assertNoSharedOwnerEvents(events);
        // TRACE 4B1: nor do they reach any heuristic-memory owner boundary
        assertNoHeuristicMemoryEvents(events);
        // the outer owner still observed ITS decision normally on this route
        boolean outerRecordSeen = false;
        for (TraceStateEvent e : events) {
            if (e instanceof TrackerRecordResponseEvent record) {
                assertEquals(TrackerOwner.OUTER_RANDO, record.owner());
                outerRecordSeen = true;
            }
        }
        assertTrue("the outer tracker must still record on the evaluator lane", outerRecordSeen);
    }

    @Test
    public void activateAmountRoutingIsOneShotAndZeroConfirmUsesResultOrdinals() {
        RandoCalAi ai = new RandoCalAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        StubGameState state = new StubGameState(3, Phase.ACTIVATE);

        Map<String, String[]> actions = new HashMap<>();
        actions.put("actionId", new String[]{"pass", "activate"});
        actions.put("actionText", new String[]{"Pass", "Activate Force"});
        assertEquals("activate", ai.decide("tester",
            decision(92, AwaitingDecisionType.CARD_ACTION_CHOICE,
                "Choose an action", actions), state));

        Map<String, String[]> amount = new HashMap<>();
        amount.put("min", new String[]{"0"});
        amount.put("max", new String[]{"3"});
        ai.decide("tester", decision(93, AwaitingDecisionType.INTEGER,
            "Choose amount of Force to activate", amount), state);
        assertEquals("3", ai.decide("tester", decision(94, AwaitingDecisionType.INTEGER,
            "Choose how many cards to draw", amount), state));

        Map<String, String[]> confirm = new HashMap<>();
        confirm.put("results", new String[]{"Yes", "No"});
        assertEquals("1", ai.decide("tester",
            decision(95, AwaitingDecisionType.MULTIPLE_CHOICE,
                ActivateDecisionRouting.ZERO_CONFIRMATION_PROMPT, confirm), state));

        List<DecisionTrace> traces = sink.getTraces();
        assertEquals(4, traces.size());
        assertEquals(TraceRoute.COMBINED_EVALUATOR, traces.get(0).getRoute().selected());
        assertEquals(TraceRoute.COMBINED_EVALUATOR, traces.get(1).getRoute().selected());
        assertEquals(TraceRoute.HEURISTIC_FALLBACK, traces.get(2).getRoute().selected());
        assertEquals(TraceRoute.COMBINED_EVALUATOR, traces.get(3).getRoute().selected());
        assertEquals(List.of("0", "1"), traces.get(3).getRawCandidateOrder());
    }

    // =========================================================================
    // Production default: no sink, no session, no trace — decide() unaffected
    // =========================================================================

    @Test
    public void productionDefaultOpensNoSession() {
        RandoCalAi ai = new RandoCalAi();  // default NoOpTraceSink, nothing set
        Map<String, String[]> params = new HashMap<>();
        params.put("cardId", new String[]{"temp1"});
        params.put("min", new String[]{"0"});

        String result = ai.decide("tester",
            decision(43, AwaitingDecisionType.CARD_SELECTION,
                "Choose cards to forfeit, if desired", params),
            null);

        assertEquals("", result);
        assertFalse(TraceSession.isActive());
    }
}
