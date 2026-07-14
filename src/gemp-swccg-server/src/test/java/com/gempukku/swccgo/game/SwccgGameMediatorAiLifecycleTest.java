package com.gempukku.swccgo.game;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.AiRegistry;
import com.gempukku.swccgo.ai.DecisionRejectionKind;
import com.gempukku.swccgo.ai.SwccgAiController;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.decisions.IntegerAwaitingDecision;
import com.gempukku.swccgo.logic.timing.DefaultUserFeedback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * FINALIZER RUNTIME (2026-07-13, Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md
 * §9 "Controller And Mediator"): the mediator's typed decideForEngine + synchronous
 * engine-disposition callbacks + loop-local immutable RejectionHistory. Same seam as
 * SwccgGameMediatorAiRetryTest: the package-private test constructor, package-private
 * maybeLetAiPlay, a REAL DefaultUserFeedback subclass counting participantDecided, a REAL
 * IntegerAwaitingDecision constrained to 1..1, a reflective SwccgGame proxy, and a
 * lifecycle-aware scripted controller. No clock abstraction; assertions are timer-map
 * membership and lifecycle, never elapsed milliseconds.
 */
public class SwccgGameMediatorAiLifecycleTest {

    private static final String GAME_ID = "ai-lifecycle-test-game";
    private static final String BOT = "bot";
    private static final String HUMAN = "human";

    private RecordingGameState _gameState;
    private SwccgGame _game;
    private CountingUserFeedback _feedback;
    private Map<String, Integer> _playerClocks;
    private SwccgGameMediator _mediator;
    private int _carryOutCalls;

    @Before
    public void setUp() {
        _gameState = new RecordingGameState();
        _game = swccgGameStub();
        _feedback = new CountingUserFeedback();
        _feedback.setGame(_game);
        _playerClocks = new HashMap<String, Integer>();
        _playerClocks.put(BOT, 0);
        _playerClocks.put(HUMAN, 0);
        _mediator = new SwccgGameMediator(GAME_ID, _game, _feedback, _playerClocks);
        _carryOutCalls = 0;
    }

    @After
    public void tearDown() {
        AiRegistry.unregisterGame(GAME_ID);
    }

    // ── 1. legacy default controller: one wire response, no callback mutation ─────
    @Test
    public void legacyDefaultControllerSubmitsOneWireAndReceivesNoCallbackMutation() {
        LegacyAi ai = new LegacyAi("1");
        AiRegistry.register(GAME_ID, BOT, ai);
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);

        _mediator.startGame();

        assertEquals("legacy decide() called once via default decideForEngine", 1, ai.decideCalls);
        assertEquals("engine accepted the wire once", 1, decision.acceptedCallbacks);
        assertEquals("pending actions continued once", 1, _carryOutCalls);
        assertNull("no pending decision after acceptance", _feedback.getAwaitingDecision(BOT));
    }

    // ── 2. valid typed response ──────────────────────────────────────────────────
    @Test
    public void validTypedResponseAcceptsWithOneAcceptedCallback() throws Exception {
        LifecycleAi ai = new LifecycleAi();
        ai.steps.add(h -> AiDecisionResult.wire("1", AiDecisionResult.MutationMode.OUTER_COMMON, "1"));
        AiRegistry.register(GAME_ID, BOT, ai);
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);

        _mediator.startGame();

        assertEquals("one AI call", 1, ai.decideForEngineCalls);
        assertEquals("one decisionMade acceptance", 1, decision.acceptedCallbacks);
        assertEquals("one accepted callback", 1, ai.acceptedCalls);
        assertEquals("zero rejected callbacks", 0, ai.rejectedKinds.size());
        assertEquals("participantDecided once", 1, _feedback.participantDecidedCalls);
        assertEquals("pending actions continue once", 1, _carryOutCalls);
        assertFalse("AI timer removed on acceptance", decisionTimers().containsKey(BOT));
    }

    // ── 3. checked invalid then valid: counts 0 then 1, exact history ────────────
    @Test
    public void checkedInvalidThenValidCarriesExactHistory() {
        LifecycleAi ai = new LifecycleAi();
        ai.steps.add(h -> AiDecisionResult.wire("0", AiDecisionResult.MutationMode.NONE, "1"));
        ai.steps.add(h -> AiDecisionResult.wire("1", AiDecisionResult.MutationMode.NONE, "1"));
        AiRegistry.register(GAME_ID, BOT, ai);
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);

        _mediator.startGame();

        assertEquals("two AI calls", 2, ai.decideForEngineCalls);
        assertEquals("prior-rejection counts 0 then 1", List.of(0, 1), ai.historyCounts);
        assertEquals("one rejected callback", 1, ai.rejectedKinds.size());
        assertEquals("rejection kind ENGINE_REJECTED", DecisionRejectionKind.ENGINE_REJECTED,
                ai.rejectedKinds.get(0));
        assertEquals("one accepted callback", 1, ai.acceptedCalls);
        assertEquals("one final accepted mutation (decisionMade)", 1, decision.acceptedCallbacks);
        assertEquals("pending actions continue once", 1, _carryOutCalls);

        RejectionHistory retryHistory = ai.historiesSeen.get(1);
        assertEquals("retry history has exactly one attempt", 1, retryHistory.size());
        assertEquals("retry history carries the exact first wire", "0",
                retryHistory.attempts().get(0).wireResponse());
        assertEquals("retry history reason is ENGINE_DECISION_INVALID",
                FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID,
                retryHistory.attempts().get(0).reason());
    }

    @Test
    public void checkedRequeueDispatchFailureStillFiresEngineRejectedOnceAfterRequeue() throws Exception {
        LifecycleAi ai = new LifecycleAi();
        ai.steps.add(h -> AiDecisionResult.wire("0", AiDecisionResult.MutationMode.NONE, "1"));
        List<String> order = new ArrayList<>();
        ai.lifecycleOrder = order;
        _feedback.lifecycleOrder = order;
        AiRegistry.register(GAME_ID, BOT, ai);
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);
        _feedback.failNextSendAwaitingDecision = true;

        try {
            _mediator.maybeLetAiPlay(BOT);
            fail("the requeue dispatch fault must propagate");
        } catch (RuntimeException expected) {
            assertEquals("requeue-dispatch-fault", expected.getMessage());
        }

        assertEquals("one AI attempt, no retry", 1, ai.decideForEngineCalls);
        assertEquals("initial history only", List.of(0), ai.historyCounts);
        assertEquals("one ENGINE_REJECTED callback", List.of(DecisionRejectionKind.ENGINE_REJECTED),
                ai.rejectedKinds);
        assertEquals("requeue occurs before callback", List.of("requeue", "callback"), order);
        assertEquals("zero accepted callbacks", 0, ai.acceptedCalls);
        assertEquals("no pending-action continuation", 0, _carryOutCalls);
        assertSame("same decision object remains pending", decision, _feedback.getAwaitingDecision(BOT));
    }

    // ── 4. checked invalid twice: terminal exhaustion ───────────────────────────
    @Test
    public void checkedInvalidTwiceReportsTerminalNoAcceptedCallback() throws Exception {
        LifecycleAi ai = new LifecycleAi();
        ai.steps.add(h -> AiDecisionResult.wire("0", AiDecisionResult.MutationMode.NONE, "1"));
        ai.steps.add(h -> AiDecisionResult.wire("2", AiDecisionResult.MutationMode.NONE, "1"));
        AiRegistry.register(GAME_ID, BOT, ai);
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);

        _mediator.startGame();

        assertEquals("exactly two AI calls", 2, ai.decideForEngineCalls);
        assertEquals("two rejected callbacks", 2, ai.rejectedKinds.size());
        assertEquals("zero accepted callbacks", 0, ai.acceptedCalls);
        assertEquals("no pending-action continuation", 0, _carryOutCalls);
        assertSame("same decision object remains pending", decision, _feedback.getAwaitingDecision(BOT));
        assertTrue("original AI decision timer retained", decisionTimers().containsKey(BOT));
        assertEquals("exactly one visible terminal message", 1, _gameState.messages.size());
    }

    // ── 5. typed pre-engine rejection ───────────────────────────────────────────
    @Test
    public void typedPreEngineRejectionIsTerminalWithoutDecisionMade() throws Exception {
        LifecycleAi ai = new LifecycleAi();
        ai.steps.add(h -> AiDecisionResult.typedRejection(
                FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK, "nothing legal", "1"));
        AiRegistry.register(GAME_ID, BOT, ai);
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);

        _mediator.startGame();

        assertEquals("one AI call, no retry", 1, ai.decideForEngineCalls);
        assertEquals("zero participantDecided", 0, _feedback.participantDecidedCalls);
        assertEquals("zero decisionMade", 0, decision.acceptedCallbacks);
        assertEquals("one rejected callback", 1, ai.rejectedKinds.size());
        assertEquals("rejection kind TYPED_REJECTION", DecisionRejectionKind.TYPED_REJECTION,
                ai.rejectedKinds.get(0));
        assertEquals("no pending-action continuation", 0, _carryOutCalls);
        assertSame("same decision object remains pending", decision, _feedback.getAwaitingDecision(BOT));
        assertTrue("AI decision timer retained", decisionTimers().containsKey(BOT));
        assertEquals("exactly one visible terminal message", 1, _gameState.messages.size());
    }

    // ── 6. decisionMade runtime fault: ATTEMPT_FAILED, no retry ─────────────────
    @Test
    public void decisionMadeRuntimeFaultIsAttemptFailedNoRetry() {
        LifecycleAi ai = new LifecycleAi();
        ai.steps.add(h -> AiDecisionResult.wire("anything", AiDecisionResult.MutationMode.NONE, "7"));
        AiRegistry.register(GAME_ID, BOT, ai);
        ThrowingDecision decision = new ThrowingDecision();
        _feedback.sendAwaitingDecision(BOT, decision);

        try {
            _mediator.maybeLetAiPlay(BOT);
            fail("the unchecked decisionMade fault must propagate");
        } catch (RuntimeException expected) {
            assertEquals("decisionMade-fault", expected.getMessage());
        }

        assertEquals("one AI call, no retry", 1, ai.decideForEngineCalls);
        assertEquals("one rejected callback", 1, ai.rejectedKinds.size());
        assertEquals("rejection kind ATTEMPT_FAILED", DecisionRejectionKind.ATTEMPT_FAILED,
                ai.rejectedKinds.get(0));
        assertEquals("zero accepted callbacks", 0, ai.acceptedCalls);
        assertEquals("no pending-action continuation", 0, _carryOutCalls);
    }

    // ── 7. accepted-callback fault: latched, no rejection, no retry ─────────────
    @Test
    public void acceptedCallbackFaultDoesNotRejectOrRetry() {
        LifecycleAi ai = new LifecycleAi();
        ai.throwOnAccepted = true;
        ai.steps.add(h -> AiDecisionResult.wire("1", AiDecisionResult.MutationMode.OUTER_COMMON, "1"));
        AiRegistry.register(GAME_ID, BOT, ai);
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);

        _mediator.startGame();  // must NOT throw: acceptance latched, mediator logs the fault

        assertEquals("acceptance latched: engine accepted once", 1, decision.acceptedCallbacks);
        assertEquals("one accepted callback attempted", 1, ai.acceptedCalls);
        assertEquals("zero rejected callbacks", 0, ai.rejectedKinds.size());
        assertEquals("no retry", 1, ai.decideForEngineCalls);
        assertEquals("accepted engine continuation occurs once", 1, _carryOutCalls);
    }

    // ── 8. failure before a result: attempt-failed callback, no retry ───────────
    @Test
    public void computationFaultBeforeResultIsAttemptFailedNoRetry() {
        LifecycleAi ai = new LifecycleAi();
        ai.steps.add(h -> { throw new RuntimeException("compute-boom"); });
        AiRegistry.register(GAME_ID, BOT, ai);
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);

        try {
            _mediator.maybeLetAiPlay(BOT);
            fail("the computation fault must propagate");
        } catch (RuntimeException expected) {
            assertEquals("compute-boom", expected.getMessage());
        }

        assertEquals("one attempt-failed callback", 1, ai.attemptFailedCalls);
        assertEquals("zero accepted callbacks", 0, ai.acceptedCalls);
        assertEquals("zero rejected callbacks", 0, ai.rejectedKinds.size());
        assertEquals("no retry", 1, ai.decideForEngineCalls);
        assertEquals("no pending-action continuation", 0, _carryOutCalls);
    }

    // ── 9. chain-limit terminal path emits no fabricated callback ───────────────
    @Test
    public void chainLimitTerminalEmitsNoFabricatedCallback() throws Exception {
        LifecycleAi ai = new LifecycleAi();
        ai.steps.add(h -> AiDecisionResult.wire("1", AiDecisionResult.MutationMode.NONE, "1"));
        AiRegistry.register(GAME_ID, BOT, ai);
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);
        setAiChainCounter(maxAiChain());

        _mediator.startGame();

        assertEquals("AI not called past the chain limit", 0, ai.decideForEngineCalls);
        assertEquals("no accepted callback", 0, ai.acceptedCalls);
        assertEquals("no rejected callback", 0, ai.rejectedKinds.size());
        assertEquals("no attempt-failed callback", 0, ai.attemptFailedCalls);
        assertEquals("chain exhaustion is visible", 1, _gameState.messages.size());
        assertSame("pending decision remains", decision, _feedback.getAwaitingDecision(BOT));
        assertTrue("decision timer remains", decisionTimers().containsKey(BOT));
    }

    // ═══ Test doubles ═══

    private static class RecordingIntegerDecision extends IntegerAwaitingDecision {
        int acceptedCallbacks;

        RecordingIntegerDecision() {
            super("Choose a number", 1, 1, 1);
        }

        @Override
        public void decisionMade(int result) {
            acceptedCallbacks++;
        }
    }

    /** A decision whose decisionMade throws an UNCHECKED exception (a runtime fault). */
    private static class ThrowingDecision implements AwaitingDecision {
        @Override public int getAwaitingDecisionId() { return 7; }
        @Override public String getText() { return "throwing decision"; }
        @Override public AwaitingDecisionType getDecisionType() { return AwaitingDecisionType.INTEGER; }
        @Override public Map<String, String[]> getDecisionParameters() { return new HashMap<>(); }
        @Override public void decisionMade(String result) throws DecisionResultInvalidException {
            throw new RuntimeException("decisionMade-fault");
        }
    }

    /** Legacy controller: implements only decide(); uses the interface default lifecycle. */
    private static class LegacyAi implements SwccgAiController {
        final String answer;
        int decideCalls;
        LegacyAi(String answer) { this.answer = answer; }
        @Override public String decide(String playerId, AwaitingDecision decision, GameState gameState) {
            decideCalls++;
            return answer;
        }
    }

    /** Lifecycle-aware scripted controller: scripts one AiDecisionResult per attempt and
     *  counts every disposition callback + the history counts it observed. */
    private static class LifecycleAi implements SwccgAiController {
        interface Step { AiDecisionResult run(RejectionHistory history); }
        final Deque<Step> steps = new ArrayDeque<>();
        final List<Integer> historyCounts = new ArrayList<>();
        final List<RejectionHistory> historiesSeen = new ArrayList<>();
        final List<DecisionRejectionKind> rejectedKinds = new ArrayList<>();
        int decideForEngineCalls;
        int acceptedCalls;
        int attemptFailedCalls;
        boolean throwOnAccepted;
        List<String> lifecycleOrder;

        @Override
        public String decide(String playerId, AwaitingDecision decision, GameState gameState) {
            throw new AssertionError("mediator must call decideForEngine, not decide");
        }

        @Override
        public AiDecisionResult decideForEngine(String playerId, AwaitingDecision decision,
                                                GameState gameState, RejectionHistory history) {
            decideForEngineCalls++;
            historyCounts.add(history.size());
            historiesSeen.add(history);
            if (steps.isEmpty()) {
                throw new AssertionError("AI asked more times than scripted");
            }
            return steps.removeFirst().run(history);
        }

        @Override
        public void onDecisionAccepted(String playerId, AwaitingDecision decision,
                                       GameState gameState, AiDecisionResult result) {
            acceptedCalls++;
            if (throwOnAccepted) {
                throw new RuntimeException("accepted-callback boom");
            }
        }

        @Override
        public void onDecisionRejected(String playerId, AwaitingDecision decision,
                                       GameState gameState, AiDecisionResult result,
                                       DecisionRejectionKind kind, String detail) {
            if (lifecycleOrder != null) {
                lifecycleOrder.add("callback");
            }
            rejectedKinds.add(kind);
        }

        @Override
        public void onDecisionAttemptFailed(String playerId, AwaitingDecision decision,
                                            GameState gameState, String detail) {
            attemptFailedCalls++;
        }
    }

    /** Real DefaultUserFeedback that counts participantDecided (packet §9: zero on a typed
     *  pre-engine rejection). */
    private static class CountingUserFeedback extends DefaultUserFeedback {
        int participantDecidedCalls;
        boolean failNextSendAwaitingDecision;
        List<String> lifecycleOrder;

        @Override
        public void sendAwaitingDecision(String playerId, AwaitingDecision decision) {
            super.sendAwaitingDecision(playerId, decision);
            if (failNextSendAwaitingDecision) {
                failNextSendAwaitingDecision = false;
                if (lifecycleOrder != null) {
                    lifecycleOrder.add("requeue");
                }
                throw new RuntimeException("requeue-dispatch-fault");
            }
        }

        @Override
        public void participantDecided(String playerId) {
            participantDecidedCalls++;
            super.participantDecided(playerId);
        }
    }

    private static class RecordingGameState extends GameState {
        final List<String> messages = new ArrayList<String>();
        @Override public void sendMessage(String message) { messages.add(message); }
        @Override public void playerDecisionStarted(String playerId, AwaitingDecision awaitingDecision) { }
        @Override public void playerDecisionFinished(String playerId) { }
    }

    private SwccgGame swccgGameStub() {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getGameState":
                    return _gameState;
                case "getDarkPlayer":
                    return BOT;
                case "getLightPlayer":
                    return HUMAN;
                case "carryOutPendingActionsUntilDecisionNeeded":
                    _carryOutCalls++;
                    return null;
                case "isFinished":
                    return false;
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "stub-SwccgGame";
                default:
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    if (rt == long.class) return 0L;
                    if (rt == float.class) return 0f;
                    if (rt == double.class) return 0d;
                    return null;
            }
        };
        return (SwccgGame) Proxy.newProxyInstance(
                SwccgGame.class.getClassLoader(), new Class<?>[]{SwccgGame.class}, handler);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> decisionTimers() throws Exception {
        Field field = SwccgGameMediator.class.getDeclaredField("_decisionQuerySentTimes");
        field.setAccessible(true);
        return (Map<String, Long>) field.get(_mediator);
    }

    private void setAiChainCounter(int value) throws Exception {
        Field field = SwccgGameMediator.class.getDeclaredField("aiChainCounter");
        field.setAccessible(true);
        field.setInt(_mediator, value);
    }

    private int maxAiChain() throws Exception {
        Field field = SwccgGameMediator.class.getDeclaredField("MAX_AI_CHAIN");
        field.setAccessible(true);
        return field.getInt(null);
    }
}
