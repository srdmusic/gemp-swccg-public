package com.gempukku.swccgo.game;

import com.gempukku.swccgo.ai.AiRegistry;
import com.gempukku.swccgo.ai.SwccgAiController;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
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

/**
 * F2 MEDIATOR RETRY FIXTURES (2026-07-13).
 *
 * Packet: Handoffs/CODEX_FINALIZER_FIXTURE_RETRY_PACKET_2026-07-13.md, F2.
 * Audit: Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md P0 #1 (a rejected AI
 * answer can strand the game; MAX_AI_CHAIN overflow returns silently; AI success
 * never credits the decision clock).
 *
 * Seam per packet: the package-private SwccgGameMediator test constructor, the
 * package-private maybeLetAiPlay for terminal re-entry assertions, and public
 * startGame() for normal scheduling. A REAL DefaultUserFeedback, a REAL anonymous
 * IntegerAwaitingDecision constrained to 1..1, a real AiRegistry registration with
 * unregisterGame cleanup, and a scripted SwccgAiController. SwccgGame is a
 * reflective interface proxy and GameState a recording subclass (the pinned
 * mockito-core 4.7.0 cannot instrument Java 21 class files, same reason as
 * EngineDecisionFixtures). No clock abstraction: assertions are timer-map
 * MEMBERSHIP and lifecycle, never elapsed milliseconds (packet F2).
 *
 * Declared residual (packet F2): participantDecided updates feedback history
 * before validation, so an invalid attempt appears in feedback history. Frozen
 * here as observable behavior, not redesigned.
 */
public class SwccgGameMediatorAiRetryTest {

    private static final String GAME_ID = "ai-retry-test-game";
    private static final String BOT = "bot";
    private static final String HUMAN = "human";

    private RecordingGameState _gameState;
    private SwccgGame _game;
    private DefaultUserFeedback _feedback;
    private Map<String, Integer> _playerClocks;
    private SwccgGameMediator _mediator;
    private ScriptedAi _ai;
    private int _carryOutCalls;

    @Before
    public void setUp() {
        _gameState = new RecordingGameState();
        _game = swccgGameStub();
        _feedback = new DefaultUserFeedback();
        _feedback.setGame(_game);
        _playerClocks = new HashMap<String, Integer>();
        _playerClocks.put(BOT, 0);
        _playerClocks.put(HUMAN, 0);
        _mediator = new SwccgGameMediator(GAME_ID, _game, _feedback, _playerClocks);
        _ai = new ScriptedAi();
        _carryOutCalls = 0;
        AiRegistry.register(GAME_ID, BOT, _ai);
    }

    @After
    public void tearDown() {
        AiRegistry.unregisterGame(GAME_ID);
    }

    // ── 1. invalidOnceRetriesOnceThenAccepts ─────────────────────────────────
    // Answers "0" (below the 1..1 minimum: checked rejection), then "1" (valid).
    // Two AI calls, one accepted callback, one pending-action continuation, no
    // pending decision, AI timer removed (the acceptance credited the clock).
    @Test
    public void invalidOnceRetriesOnceThenAccepts() throws Exception {
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);
        _ai.answers.add("0");
        _ai.answers.add("1");

        _mediator.startGame();

        assertEquals("initial attempt plus exactly one retry", 2, _ai.decideCalls);
        assertEquals("one accepted callback", 1, decision.acceptedCallbacks);
        assertEquals("one pending-action continuation", 1, _carryOutCalls);
        assertNull("no pending decision after acceptance", _feedback.getAwaitingDecision(BOT));
        assertFalse("AI decision timer removed on acceptance (clock credited)",
                decisionTimers().containsKey(BOT));
        assertTrue("no terminal message on a recovered retry", _gameState.messages.isEmpty());
    }

    // ── 2. invalidTwiceReportsTerminalAndPreservesDecision ───────────────────
    // Answers "0", then "2" (both outside 1..1). Two AI calls, no continuation,
    // the SAME decision object still pending, AI timer retained, exactly one
    // visible game-state message.
    @Test
    public void invalidTwiceReportsTerminalAndPreservesDecision() throws Exception {
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);
        _ai.answers.add("0");
        _ai.answers.add("2");

        _mediator.startGame();

        assertEquals("exactly two AI calls, then exhaustion", 2, _ai.decideCalls);
        assertEquals("no accepted callback", 0, decision.acceptedCallbacks);
        assertEquals("no pending-action continuation on terminal exhaustion", 0, _carryOutCalls);
        assertSame("the SAME decision object remains pending for diagnosis",
                decision, _feedback.getAwaitingDecision(BOT));
        assertTrue("original AI decision timer retained (no clock credit)",
                decisionTimers().containsKey(BOT));
        assertEquals("exactly one visible terminal message", 1, _gameState.messages.size());
        assertTrue("terminal message names the AI player",
                _gameState.messages.get(0).contains(BOT));
    }

    // ── 3. terminal re-entry is suppressed ───────────────────────────────────
    // Invoking maybeLetAiPlay again after terminal exhaustion: no third AI call
    // and no duplicate message (ownership keyed by player plus decision OBJECT).
    @Test
    public void reentryAfterTerminalExhaustionIsSuppressed() throws Exception {
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);
        _ai.answers.add("0");
        _ai.answers.add("2");
        _mediator.startGame();
        assertEquals(2, _ai.decideCalls);
        assertEquals(1, _gameState.messages.size());

        _mediator.maybeLetAiPlay(BOT);

        assertEquals("no third AI call for the same terminal decision object", 2, _ai.decideCalls);
        assertEquals("no duplicate terminal message", 1, _gameState.messages.size());
        assertSame(decision, _feedback.getAwaitingDecision(BOT));
        assertTrue(decisionTimers().containsKey(BOT));
    }

    // ── 4. chainLimitReportsTerminalAndPreservesDecision ─────────────────────
    // With the chain counter already at MAX_AI_CHAIN, the AI is not called, the
    // pending decision and its timer remain, and the failure is VISIBLE (the old
    // code returned silently). Re-entry is suppressed by the same ownership record.
    @Test
    public void chainLimitReportsTerminalAndPreservesDecision() throws Exception {
        RecordingIntegerDecision decision = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, decision);
        _ai.answers.add("1"); // must never be consumed
        setAiChainCounter(maxAiChain());

        _mediator.startGame();

        assertEquals("AI is not called past the chain limit", 0, _ai.decideCalls);
        assertSame("pending decision remains", decision, _feedback.getAwaitingDecision(BOT));
        assertTrue("decision timer remains", decisionTimers().containsKey(BOT));
        assertEquals("chain exhaustion is visible through the same terminal reporter",
                1, _gameState.messages.size());

        _mediator.maybeLetAiPlay(BOT);
        assertEquals("re-entry suppressed: still no AI call", 0, _ai.decideCalls);
        assertEquals("re-entry suppressed: no duplicate message", 1, _gameState.messages.size());
    }

    // ── 5. a NEW decision object gets a fresh budget ─────────────────────────
    // IntegerAwaitingDecision hardcodes numeric id 1, so the replacement decision
    // has the SAME numeric id as the exhausted one. Ownership is object identity:
    // the new object must be answered normally, with no duplicate terminal message.
    @Test
    public void newDecisionObjectWithSameNumericIdGetsFreshBudget() throws Exception {
        RecordingIntegerDecision first = new RecordingIntegerDecision();
        _feedback.sendAwaitingDecision(BOT, first);
        _ai.answers.add("0");
        _ai.answers.add("2");
        _mediator.startGame(); // terminal exhaustion on `first`
        assertEquals(2, _ai.decideCalls);
        assertEquals(1, _gameState.messages.size());

        RecordingIntegerDecision second = new RecordingIntegerDecision();
        assertEquals("both decision objects carry the hardcoded numeric id",
                first.getAwaitingDecisionId(), second.getAwaitingDecisionId());
        _feedback.sendAwaitingDecision(BOT, second);
        _ai.answers.add("1");

        _mediator.maybeLetAiPlay(BOT);

        assertEquals("fresh budget: the AI is asked again", 3, _ai.decideCalls);
        assertEquals("the new decision is accepted", 1, second.acceptedCallbacks);
        assertEquals("first decision never received a callback", 0, first.acceptedCallbacks);
        assertNull("no pending decision after acceptance", _feedback.getAwaitingDecision(BOT));
        assertFalse("timer cleared by the acceptance clock credit",
                decisionTimers().containsKey(BOT));
        assertEquals("no additional terminal message", 1, _gameState.messages.size());
    }

    // ═══ Test doubles ═══

    /** REAL engine validator constrained to 1..1 (packet F2): "0" and "2" are
     *  checked rejections, "1" is the only acceptable answer. Numeric id is the
     *  engine-hardcoded 1 (IntegerAwaitingDecision super(1, ...)). */
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

    /** Scripted AI: consumes pre-queued answers, counts decide calls, fails loudly
     *  if asked more times than scripted. */
    private static class ScriptedAi implements SwccgAiController {
        final Deque<String> answers = new ArrayDeque<String>();
        int decideCalls;

        @Override
        public String decide(String playerId, AwaitingDecision decision, GameState gameState) {
            decideCalls++;
            if (answers.isEmpty()) {
                throw new AssertionError("AI asked more times than scripted (call " + decideCalls + ")");
            }
            return answers.removeFirst();
        }
    }

    /** Records sendMessage (the F2 visible terminal channel); decision start/finish
     *  notifications are no-ops so the bare GameState needs no game wiring. */
    private static class RecordingGameState extends GameState {
        final List<String> messages = new ArrayList<String>();

        @Override
        public void sendMessage(String message) {
            messages.add(message);
        }

        @Override
        public void playerDecisionStarted(String playerId, AwaitingDecision awaitingDecision) {
            // no listeners in this seam
        }

        @Override
        public void playerDecisionFinished(String playerId) {
            // no listeners in this seam
        }
    }

    /** Minimal reflective SwccgGame proxy (interface): answers exactly what the
     *  mediator and DefaultUserFeedback touch on these paths. */
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

    // ═══ Timer-map membership access (packet F2: assert membership and lifecycle,
    // not elapsed milliseconds; reflection keeps the production seam to exactly the
    // packet-named constructor and maybeLetAiPlay visibility) ═══

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
