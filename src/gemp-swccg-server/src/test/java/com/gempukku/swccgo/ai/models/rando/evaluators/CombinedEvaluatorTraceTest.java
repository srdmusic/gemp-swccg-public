package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.NoOpTraceSink;
import com.gempukku.swccgo.ai.models.common.trace.TraceOp;
import com.gempukku.swccgo.ai.models.common.trace.TraceOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * TRACE HOOK (2026-07-13, CODEX_MINIMAL_DECISION_TRACE_HOOK): pure JUnit proof of the
 * spec's Gate section. Scripted fake evaluators + a capture sink through the
 * package-visible CombinedEvaluator seam; no server, no log parsing, no replay files.
 */
public class CombinedEvaluatorTraceTest {

    // =========================================================================
    // Scripted harness
    // =========================================================================

    private static final class ScriptedEvaluator extends ActionEvaluator {
        private final Function<DecisionContext, List<EvaluatedAction>> script;

        ScriptedEvaluator(String name, Function<DecisionContext, List<EvaluatedAction>> script) {
            super(name);
            this.script = script;
        }

        @Override
        public boolean canEvaluate(DecisionContext context) {
            return true;
        }

        @Override
        public List<EvaluatedAction> evaluate(DecisionContext context) {
            return script.apply(context);
        }
    }

    private static DecisionContext passableContext() {
        DecisionContext ctx = new DecisionContext(null, "tester", "CARD_ACTION_CHOICE",
            "Choose action to take", "d1", Phase.DEPLOY);
        ctx.setNoPass(false);
        ctx.setMin(0);
        return ctx;
    }

    private static EvaluatedAction action(String id, float score, String text) {
        return new EvaluatedAction(id, ActionType.UNKNOWN, score, text);
    }

    private static DecisionTrace capture(List<ActionEvaluator> evaluators, DecisionContext ctx) {
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        new CombinedEvaluator(evaluators, sink).evaluateDecision(ctx);
        return sink.single();
    }

    private static TraceOperation findFirst(DecisionTrace trace, TraceOp op) {
        TraceOperation found = findFirstOrNull(trace, op);
        if (found == null) {
            fail("no " + op + " operation in trace");
        }
        return found;
    }

    private static TraceOperation findFirstOrNull(DecisionTrace trace, TraceOp op) {
        for (TraceOperation operation : trace.getOperations()) {
            if (operation.getOp() == op) {
                return operation;
            }
        }
        return null;
    }

    private static TraceOperation lastOp(DecisionTrace trace) {
        List<TraceOperation> ops = trace.getOperations();
        return ops.get(ops.size() - 1);
    }

    /** Two evaluators, a merge, and a veto: enough structure to exercise every legacy hook. */
    private static List<ActionEvaluator> richScenario() {
        ScriptedEvaluator e1 = new ScriptedEvaluator("E1", ctx -> {
            EvaluatedAction a = action("A", 120.5f, "Deploy A");
            a.addReasoning("baseline bonus", 10.25f);
            EvaluatedAction b = action("B", 80.0f, "Deploy B");
            return Arrays.asList(a, b);
        });
        ScriptedEvaluator e2 = new ScriptedEvaluator("E2", ctx -> {
            EvaluatedAction a2 = action("A", 5.5f, "Deploy A");
            EvaluatedAction c = action("C", -20.0f, "Deploy C");
            c.hardVeto("solo charge");
            return Arrays.asList(a2, c);
        });
        return Arrays.asList(e1, e2);
    }

    // =========================================================================
    // Gate (a): no-op sink preserves winner/scores on a scripted scenario
    // =========================================================================

    @Test
    public void noOpSinkPreservesWinnerAndScores() {
        EvaluatedAction plain = new CombinedEvaluator(richScenario(), NoOpTraceSink.INSTANCE)
            .evaluateDecision(passableContext());

        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        EvaluatedAction traced = new CombinedEvaluator(richScenario(), sink)
            .evaluateDecision(passableContext());

        assertNotNull(plain);
        assertNotNull(traced);
        assertEquals(plain.getActionId(), traced.getActionId());
        assertEquals("score bits must match exactly",
            Float.floatToRawIntBits(plain.getScore()), Float.floatToRawIntBits(traced.getScore()));
        assertEquals(plain.isHardVetoed(), traced.isHardVetoed());
        assertEquals(plain.getReasoningString(), traced.getReasoningString());
        assertEquals("A", plain.getActionId());
        assertEquals(Float.floatToRawIntBits(136.25f), Float.floatToRawIntBits(plain.getScore()));

        // neither run leaks a trace session onto the thread
        assertFalse(TraceSession.isActive());

        DecisionTrace trace = sink.single();
        // candidate order frozen from first-seen merge order
        assertEquals(Arrays.asList("A", "B", "C"), trace.getCandidateOrder());

        // INITIAL bound to its evaluator, tagged LEGACY_UNTAGGED
        TraceOperation initial = findFirst(trace, TraceOp.INITIAL);
        assertEquals("A", initial.getActionId());
        assertEquals("E1", initial.getEvaluatorId());
        assertEquals(TraceOperation.RULE_LEGACY_UNTAGGED, initial.getRuleId());
        assertEquals(0, initial.getCandidateOrdinal());

        // MERGE records the boundary only: no synthetic delta, bound to the merging evaluator
        TraceOperation merge = findFirst(trace, TraceOp.MERGE);
        assertEquals("A", merge.getActionId());
        assertEquals("E2", merge.getEvaluatorId());
        assertNull("MERGE must not fake an additive delta", merge.getDeltaBits());
        assertEquals(Float.floatToRawIntBits(130.75f), merge.getBeforeBits().intValue());
        assertEquals(Float.floatToRawIntBits(136.25f), merge.getAfterBits().intValue());

        // veto state/reason captured
        TraceOperation veto = findFirst(trace, TraceOp.HARD_VETO);
        assertEquals("C", veto.getActionId());
        assertEquals(true, veto.isVetoed());
        assertEquals("solo charge", veto.getVetoReason());

        // the one complete record ends with FINALIZE carrying the winner
        TraceOperation finalize = lastOp(trace);
        assertEquals(TraceOp.FINALIZE, finalize.getOp());
        assertEquals("A", finalize.getActionId());
        assertEquals(Float.floatToRawIntBits(136.25f), finalize.getAfterBits().intValue());
    }

    // =========================================================================
    // Gate (b): candidate reordering fails comparison
    // =========================================================================

    private static List<ActionEvaluator> pairScenario(String firstId, float firstScore,
                                                      String secondId, float secondScore) {
        return Arrays.asList((ActionEvaluator) new ScriptedEvaluator("E1", ctx -> Arrays.asList(
            action(firstId, firstScore, "Deploy " + firstId),
            action(secondId, secondScore, "Deploy " + secondId))));
    }

    @Test
    public void candidateReorderingFailsComparison() {
        DecisionTrace ab = capture(pairScenario("A", 100.0f, "B", 50.0f), passableContext());
        DecisionTrace ba = capture(pairScenario("B", 50.0f, "A", 100.0f), passableContext());
        TraceTestSupport.assertTracesDiffer(ab, ba);
    }

    // =========================================================================
    // Gate (c): a one-bit float change fails comparison
    // =========================================================================

    @Test
    public void oneBitFloatChangeFailsComparison() {
        float base = 100.0f;
        float oneBitOff = Float.intBitsToFloat(Float.floatToRawIntBits(base) + 1);

        DecisionTrace t1 = capture(pairScenario("A", base, "B", 50.0f), passableContext());
        DecisionTrace t2 = capture(pairScenario("A", oneBitOff, "B", 50.0f), passableContext());

        // same winner either way; only the exact-bits trace catches the drift
        assertEquals(lastOp(t1).getActionId(), lastOp(t2).getActionId());
        TraceTestSupport.assertTracesDiffer(t1, t2);
    }

    // =========================================================================
    // Gate (d): a veto-reason change fails comparison
    // =========================================================================

    private static List<ActionEvaluator> vetoScenario(String vetoReason) {
        return Arrays.asList((ActionEvaluator) new ScriptedEvaluator("E1", ctx -> {
            EvaluatedAction a = action("A", 100.0f, "Deploy A");
            EvaluatedAction b = action("B", 50.0f, "Deploy B");
            b.hardVeto(vetoReason);
            return Arrays.asList(a, b);
        }));
    }

    @Test
    public void vetoReasonChangeFailsComparison() {
        DecisionTrace t1 = capture(vetoScenario("solo charge"), passableContext());
        DecisionTrace t2 = capture(vetoScenario("naked deploy"), passableContext());

        // same winner (A) both times; the veto reason itself is what must not drift
        assertEquals(lastOp(t1).getActionId(), lastOp(t2).getActionId());
        TraceTestSupport.assertTracesDiffer(t1, t2);
    }

    // =========================================================================
    // Gate (e): SET-versus-ADD distinction is visible
    // =========================================================================

    @Test
    public void setVersusAddDistinctionVisible() {
        DecisionTrace addTrace = capture(Arrays.asList(
            (ActionEvaluator) new ScriptedEvaluator("E1", ctx -> {
                EvaluatedAction a = action("A", 50.0f, "Deploy A");
                a.addReasoning("boost", 50.0f);
                return Arrays.asList(a);
            })), passableContext());

        DecisionTrace setTrace = capture(Arrays.asList(
            (ActionEvaluator) new ScriptedEvaluator("E1", ctx -> {
                EvaluatedAction a = action("A", 50.0f, "Deploy A");
                a.setScore(100.0f);
                return Arrays.asList(a);
            })), passableContext());

        // identical final score bits...
        assertEquals(lastOp(addTrace).getAfterBits(), lastOp(setTrace).getAfterBits());
        // ...but the traces are not interchangeable
        TraceTestSupport.assertTracesDiffer(addTrace, setTrace);

        TraceOperation add = findFirst(addTrace, TraceOp.ADD);
        assertEquals(Float.floatToRawIntBits(50.0f), add.getBeforeBits().intValue());
        assertEquals(Float.floatToRawIntBits(50.0f), add.getDeltaBits().intValue());
        assertEquals(Float.floatToRawIntBits(100.0f), add.getAfterBits().intValue());
        assertNull("scripted ADD run must contain no SET", findFirstOrNull(addTrace, TraceOp.SET));

        TraceOperation set = findFirst(setTrace, TraceOp.SET);
        assertEquals(Float.floatToRawIntBits(50.0f), set.getBeforeBits().intValue());
        assertNull("SET must not fake an additive delta", set.getDeltaBits());
        assertEquals(Float.floatToRawIntBits(100.0f), set.getAfterBits().intValue());
        assertNull("scripted SET run must contain no ADD", findFirstOrNull(setTrace, TraceOp.ADD));
    }

    // =========================================================================
    // Gate (f): a finalization change fails comparison
    // =========================================================================

    @Test
    public void finalizeChangeFailsComparison() {
        List<ActionEvaluator> allBad = Arrays.asList((ActionEvaluator) new ScriptedEvaluator("E1",
            ctx -> Arrays.asList(action("A", -150.0f, "Deploy A"))));

        // run 1: pass is legal, V148 pass wins finalization
        DecisionContext passable = passableContext();
        TraceTestSupport.CaptureSink passSink = new TraceTestSupport.CaptureSink();
        EvaluatedAction passResult = new CombinedEvaluator(allBad, passSink).evaluateDecision(passable);
        assertEquals(ActionType.PASS, passResult.getActionType());

        // run 2: identical candidates and scoring, but forced to choose
        List<ActionEvaluator> allBadAgain = Arrays.asList((ActionEvaluator) new ScriptedEvaluator("E1",
            ctx -> Arrays.asList(action("A", -150.0f, "Deploy A"))));
        DecisionContext forced = passableContext();
        forced.setNoPass(true);
        forced.setMin(1);
        TraceTestSupport.CaptureSink forcedSink = new TraceTestSupport.CaptureSink();
        EvaluatedAction forcedResult = new CombinedEvaluator(allBadAgain, forcedSink).evaluateDecision(forced);
        assertEquals("A", forcedResult.getActionId());

        DecisionTrace passTrace = passSink.single();
        DecisionTrace forcedTrace = forcedSink.single();

        // identical frozen candidates, different finalization: comparison must fail
        assertEquals(passTrace.getCandidateOrder(), forcedTrace.getCandidateOrder());
        TraceTestSupport.assertTracesDiffer(passTrace, forcedTrace);

        TraceOperation passFinalize = lastOp(passTrace);
        TraceOperation forcedFinalize = lastOp(forcedTrace);
        assertEquals(TraceOp.FINALIZE, passFinalize.getOp());
        assertEquals(TraceOp.FINALIZE, forcedFinalize.getOp());
        assertEquals("", passFinalize.getActionId());
        assertEquals("A", forcedFinalize.getActionId());

        // synthetic Pass carries the explicit synthetic ordinal + source marker
        TraceOperation passSelect = findFirst(passTrace, TraceOp.SELECT);
        assertEquals(TraceOperation.ORDINAL_SYNTHETIC, passSelect.getCandidateOrdinal());
        assertEquals("V148_ALL_BAD_PASS", passSelect.getSyntheticSource());
        assertEquals(TraceOperation.ORDINAL_SYNTHETIC, passFinalize.getCandidateOrdinal());
        assertEquals("V148_ALL_BAD_PASS", passFinalize.getSyntheticSource());
    }
}
