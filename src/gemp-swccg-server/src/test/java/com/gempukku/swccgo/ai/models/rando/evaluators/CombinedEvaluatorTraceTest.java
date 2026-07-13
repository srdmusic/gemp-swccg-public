package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.NoOpTraceSink;
import com.gempukku.swccgo.ai.models.common.trace.TraceCaptureFailure;
import com.gempukku.swccgo.ai.models.common.trace.TraceOp;
import com.gempukku.swccgo.ai.models.common.trace.TraceOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md):
 * pure JUnit proof of the contract's minimum gate corpus at the CombinedEvaluator seam.
 * Scripted fake evaluators + a capture sink through the package-visible seam; no server,
 * no log parsing, no replay files. Candidate ordinals bind to the COMPLETE RAW decision
 * arrays supplied on the DecisionContext, never to evaluator merge output.
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

    /** Passable context carrying the COMPLETE raw candidate id array. */
    private static DecisionContext passableContext(String... rawActionIds) {
        DecisionContext ctx = new DecisionContext(null, "tester", "CARD_ACTION_CHOICE",
            "Choose action to take", "d1", Phase.DEPLOY);
        ctx.setNoPass(false);
        ctx.setMin(0);
        ctx.setSide(Side.DARK);
        ctx.setActionIds(new ArrayList<>(Arrays.asList(rawActionIds)));
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
    // Gate (a): no-op sink preserves winner/scores; traced envelope is COMPLETE
    // =========================================================================

    @Test
    public void noOpSinkPreservesWinnerAndScores() {
        EvaluatedAction plain = new CombinedEvaluator(richScenario(), NoOpTraceSink.INSTANCE)
            .evaluateDecision(passableContext("A", "B", "C"));

        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        EvaluatedAction traced = new CombinedEvaluator(richScenario(), sink)
            .evaluateDecision(passableContext("A", "B", "C"));

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
        // V2 envelope: versioned, COMPLETE, routed, snapshot present
        assertEquals(DecisionTrace.SCHEMA_VERSION, trace.getSchemaVersion());
        assertEquals(TraceStatus.COMPLETE, trace.getStatus());
        assertNotNull(trace.getSnapshot());
        assertEquals(TraceRoute.COMBINED_EVALUATOR, trace.getRoute().selected());
        assertTrue("seam session records the bot package as botModel",
            trace.getBotModel().contains("evaluators"));

        // candidate order comes from the COMPLETE RAW decision arrays...
        assertEquals(Arrays.asList("A", "B", "C"), trace.getRawCandidateOrder());
        // ...and the evaluator merge order is kept separately (identical here)
        assertEquals(Arrays.asList("A", "B", "C"), trace.getMergeOrder());
        // snapshot rows sit at their original ordinals
        assertEquals(3, trace.getSnapshot().actionFacts().size());
        assertEquals("B", trace.getSnapshot().actionFacts().get(1).actionId());

        // INITIAL bound to its evaluator, typed LEGACY_UNTAGGED, raw ordinal 0
        TraceOperation initial = findFirst(trace, TraceOp.INITIAL);
        assertEquals("A", initial.getActionId());
        assertEquals("E1", initial.getEvaluatorId());
        assertEquals(TraceRuleId.LEGACY_UNTAGGED, initial.getRuleId());
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
        assertEquals(2, veto.getCandidateOrdinal());

        // finalization: PRE-SAFETY winner (explicitly not the final response) + pass eligibility
        assertEquals("A", trace.getFinalization().preSafetyWinnerActionId());
        assertEquals(Float.floatToRawIntBits(136.25f),
            trace.getFinalization().preSafetyWinnerScoreBits().intValue());
        assertEquals(Boolean.TRUE, trace.getFinalization().passEligible());
        assertFalse("seam session never reaches the bot's final-response boundary",
            trace.getFinalization().finalResponseRecorded());

        // last op is the winner SELECT
        TraceOperation winner = lastOp(trace);
        assertEquals(TraceOp.SELECT, winner.getOp());
        assertEquals("A", winner.getActionId());
        assertEquals("winner", winner.getDetail());
    }

    // =========================================================================
    // Gate (b): candidate reordering fails comparison; ordinals stay raw-bound
    // =========================================================================

    private static List<ActionEvaluator> pairScenario(String firstId, float firstScore,
                                                      String secondId, float secondScore) {
        return Arrays.asList((ActionEvaluator) new ScriptedEvaluator("E1", ctx -> Arrays.asList(
            action(firstId, firstScore, "Deploy " + firstId),
            action(secondId, secondScore, "Deploy " + secondId))));
    }

    @Test
    public void candidateReorderingFailsComparison() {
        // SAME raw candidate arrays; the evaluator merely returns candidates reordered.
        DecisionTrace ab = capture(pairScenario("A", 100.0f, "B", 50.0f), passableContext("A", "B"));
        DecisionTrace ba = capture(pairScenario("B", 50.0f, "A", 100.0f), passableContext("A", "B"));

        // raw order is identical (it is the frozen input)...
        assertEquals(ab.getRawCandidateOrder(), ba.getRawCandidateOrder());
        // ...the reorder is visible in mergeOrder and op sequence, so comparison fails
        assertEquals(Arrays.asList("B", "A"), ba.getMergeOrder());
        TraceTestSupport.assertTracesDiffer(ab, ba);

        // ordinals bind to the RAW arrays even for the reordered run
        TraceOperation firstInitial = findFirst(ba, TraceOp.INITIAL);
        assertEquals("B", firstInitial.getActionId());
        assertEquals("B keeps raw ordinal 1 despite being returned first",
            1, firstInitial.getCandidateOrdinal());
    }

    // =========================================================================
    // Gate corpus: offered candidate omitted by every evaluator stays visible
    // =========================================================================

    @Test
    public void unreturnedCandidateVisibleAtRawOrdinal() {
        // Raw decision offered X first; no evaluator ever returns it.
        DecisionTrace trace = capture(pairScenario("A", 100.0f, "B", 50.0f),
            passableContext("X", "A", "B"));

        assertEquals(Arrays.asList("X", "A", "B"), trace.getRawCandidateOrder());
        assertEquals("X sits in the snapshot at its raw ordinal",
            "X", trace.getSnapshot().actionFacts().get(0).actionId());
        assertEquals(Arrays.asList("A", "B"), trace.getMergeOrder());
        for (TraceOperation op : trace.getOperations()) {
            assertFalse("no evaluator returned X, so no op may claim it",
                "X".equals(op.getActionId()));
        }
        // A and B keep their RAW ordinals (1 and 2), not merge positions (0 and 1)
        TraceOperation initial = findFirst(trace, TraceOp.INITIAL);
        assertEquals("A", initial.getActionId());
        assertEquals(1, initial.getCandidateOrdinal());
    }

    // =========================================================================
    // Gate corpus: duplicate id — one raw candidate identity, separate operations
    // =========================================================================

    @Test
    public void duplicateActionIdKeepsOneRawIdentityWithSeparateOperations() {
        // duplicate id in the RAW arrays: first ordinal wins for the id index
        List<ActionEvaluator> evaluators = Arrays.asList(
            new ScriptedEvaluator("E1", ctx -> Arrays.asList(action("A", 60.0f, "Deploy A"))),
            new ScriptedEvaluator("E2", ctx -> Arrays.asList(action("A", 40.0f, "Deploy A"))));
        DecisionTrace trace = capture(evaluators, passableContext("A", "A", "B"));

        // raw duplicates preserved verbatim; merge collapsed to one candidate identity
        assertEquals(Arrays.asList("A", "A", "B"), trace.getRawCandidateOrder());
        assertEquals(Arrays.asList("A"), trace.getMergeOrder());

        // separate operations: two INITIALs (one per evaluator) + the MERGE boundary,
        // all bound to A's FIRST raw ordinal
        int initials = 0;
        for (TraceOperation op : trace.getOperations()) {
            if (op.getOp() == TraceOp.INITIAL) {
                initials++;
                assertEquals("A", op.getActionId());
                assertEquals(0, op.getCandidateOrdinal());
            }
        }
        assertEquals(2, initials);
        TraceOperation merge = findFirst(trace, TraceOp.MERGE);
        assertEquals("E2", merge.getEvaluatorId());
        assertEquals(0, merge.getCandidateOrdinal());
    }

    // =========================================================================
    // Gate corpus: an evaluator-invented id (never offered) is visible drift
    // =========================================================================

    @Test
    public void evaluatorInventedIdGetsOrdinalUnknown() {
        DecisionTrace trace = capture(pairScenario("A", 100.0f, "Z", 50.0f), passableContext("A"));

        TraceOperation zInitial = null;
        for (TraceOperation op : trace.getOperations()) {
            if (op.getOp() == TraceOp.INITIAL && "Z".equals(op.getActionId())) {
                zInitial = op;
            }
        }
        assertNotNull(zInitial);
        assertEquals("id never offered by the raw decision must be ORDINAL_UNKNOWN",
            TraceOperation.ORDINAL_UNKNOWN, zInitial.getCandidateOrdinal());
    }

    // =========================================================================
    // Gate (c): a one-bit float change fails comparison
    // =========================================================================

    @Test
    public void oneBitFloatChangeFailsComparison() {
        float base = 100.0f;
        float oneBitOff = Float.intBitsToFloat(Float.floatToRawIntBits(base) + 1);

        DecisionTrace t1 = capture(pairScenario("A", base, "B", 50.0f), passableContext("A", "B"));
        DecisionTrace t2 = capture(pairScenario("A", oneBitOff, "B", 50.0f), passableContext("A", "B"));

        // same winner either way; only the exact-bits trace catches the drift
        assertEquals(t1.getFinalization().preSafetyWinnerActionId(),
            t2.getFinalization().preSafetyWinnerActionId());
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
        DecisionTrace t1 = capture(vetoScenario("solo charge"), passableContext("A", "B"));
        DecisionTrace t2 = capture(vetoScenario("naked deploy"), passableContext("A", "B"));

        // same winner (A) both times; the veto reason itself is what must not drift
        assertEquals(t1.getFinalization().preSafetyWinnerActionId(),
            t2.getFinalization().preSafetyWinnerActionId());
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
            })), passableContext("A"));

        DecisionTrace setTrace = capture(Arrays.asList(
            (ActionEvaluator) new ScriptedEvaluator("E1", ctx -> {
                EvaluatedAction a = action("A", 50.0f, "Deploy A");
                a.setScore(100.0f);
                return Arrays.asList(a);
            })), passableContext("A"));

        // identical final score bits...
        assertEquals(addTrace.getFinalization().preSafetyWinnerScoreBits(),
            setTrace.getFinalization().preSafetyWinnerScoreBits());
        // ...but the traces are not interchangeable
        TraceTestSupport.assertTracesDiffer(addTrace, setTrace);

        TraceOperation add = findFirst(addTrace, TraceOp.ADD);
        assertEquals(Float.floatToRawIntBits(50.0f), add.getBeforeBits().intValue());
        assertEquals(Float.floatToRawIntBits(50.0f), add.getDeltaBits().intValue());
        assertEquals(Float.floatToRawIntBits(100.0f), add.getAfterBits().intValue());
        assertEquals(TraceRuleId.LEGACY_UNTAGGED, add.getRuleId());
        assertNull("scripted ADD run must contain no SET", findFirstOrNull(addTrace, TraceOp.SET));

        TraceOperation set = findFirst(setTrace, TraceOp.SET);
        assertEquals(Float.floatToRawIntBits(50.0f), set.getBeforeBits().intValue());
        assertNull("SET must not fake an additive delta", set.getDeltaBits());
        assertEquals(Float.floatToRawIntBits(100.0f), set.getAfterBits().intValue());
        assertNull("scripted SET run must contain no ADD", findFirstOrNull(setTrace, TraceOp.ADD));
    }

    // =========================================================================
    // Gate (f): a finalization change fails comparison; synthetic pass never
    //           steals an offered empty-id ordinal
    // =========================================================================

    @Test
    public void finalizeChangeFailsComparison() {
        List<ActionEvaluator> allBad = Arrays.asList((ActionEvaluator) new ScriptedEvaluator("E1",
            ctx -> Arrays.asList(action("A", -150.0f, "Deploy A"))));

        // run 1: pass is legal, V148 pass wins finalization. The raw decision ALSO
        // offers an empty-id candidate at ordinal 1 — the synthetic pass must not
        // reuse or replace that ordinal.
        DecisionContext passable = passableContext("A", "");
        TraceTestSupport.CaptureSink passSink = new TraceTestSupport.CaptureSink();
        EvaluatedAction passResult = new CombinedEvaluator(allBad, passSink).evaluateDecision(passable);
        assertEquals(ActionType.PASS, passResult.getActionType());

        // run 2: identical candidates and scoring, but forced to choose
        List<ActionEvaluator> allBadAgain = Arrays.asList((ActionEvaluator) new ScriptedEvaluator("E1",
            ctx -> Arrays.asList(action("A", -150.0f, "Deploy A"))));
        DecisionContext forced = passableContext("A", "");
        forced.setNoPass(true);
        forced.setMin(1);
        TraceTestSupport.CaptureSink forcedSink = new TraceTestSupport.CaptureSink();
        EvaluatedAction forcedResult = new CombinedEvaluator(allBadAgain, forcedSink).evaluateDecision(forced);
        assertEquals("A", forcedResult.getActionId());

        DecisionTrace passTrace = passSink.single();
        DecisionTrace forcedTrace = forcedSink.single();

        // identical frozen raw candidates, different finalization: comparison must fail
        assertEquals(passTrace.getRawCandidateOrder(), forcedTrace.getRawCandidateOrder());
        TraceTestSupport.assertTracesDiffer(passTrace, forcedTrace);

        // pre-safety winner recorded separately per run
        assertEquals("", passTrace.getFinalization().preSafetyWinnerActionId());
        assertEquals("A", forcedTrace.getFinalization().preSafetyWinnerActionId());
        // pass eligibility captured with opposite outcomes
        assertEquals(Boolean.TRUE, passTrace.getFinalization().passEligible());
        assertEquals(Boolean.FALSE, forcedTrace.getFinalization().passEligible());

        // synthetic Pass carries the explicit synthetic ordinal + source marker and
        // does NOT steal the offered ""-id candidate's raw ordinal (1)
        TraceOperation passSelect = findFirst(passTrace, TraceOp.SELECT);
        assertEquals(TraceOperation.ORDINAL_SYNTHETIC, passSelect.getCandidateOrdinal());
        assertEquals("V148_ALL_BAD_PASS", passSelect.getSyntheticSource());
        assertEquals("offered empty-id candidate keeps its raw ordinal slot",
            "", passTrace.getRawCandidateOrder().get(1));
    }

    // =========================================================================
    // Gate corpus: evaluator exception marks INCOMPLETE, propagates, leaves no leak
    // =========================================================================

    @Test
    public void evaluatorExceptionMarksIncompleteAndLeavesNoLeak() {
        List<ActionEvaluator> throwing = Arrays.asList(
            new ScriptedEvaluator("E1", ctx -> Arrays.asList(action("A", 10.0f, "Deploy A"))),
            new ScriptedEvaluator("BOOM", ctx -> {
                throw new IllegalStateException("scripted evaluator failure");
            }));
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        try {
            new CombinedEvaluator(throwing, sink).evaluateDecision(passableContext("A"));
            fail("scripted evaluator exception must propagate (legacy behavior unchanged)");
        } catch (IllegalStateException expected) {
            assertEquals("scripted evaluator failure", expected.getMessage());
        }

        // the truncated record was still emitted, explicitly INCOMPLETE — no silent loss
        DecisionTrace trace = sink.single();
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        boolean evaluatorFailureFlagged = false;
        for (TraceCaptureFailure failure : trace.getCaptureFailures()) {
            if (failure.stage() == TraceCaptureFailure.Stage.EVALUATOR) {
                evaluatorFailureFlagged = true;
            }
        }
        assertTrue("EVALUATOR-stage capture failure must be recorded", evaluatorFailureFlagged);
        // the evaluator binding was released by the finally pairing; no session leaks
        assertFalse(TraceSession.isActive());

        // a strict fixture sink refuses this trace as evidence
        TraceTestSupport.StrictFixtureSink strict = new TraceTestSupport.StrictFixtureSink();
        try {
            strict.accept(trace);
            fail("strict fixture sink must reject an INCOMPLETE trace");
        } catch (AssertionError expected) {
            // required
        }
    }

    // =========================================================================
    // Gate corpus: sink exception never harms the decision, never leaks a session
    // =========================================================================

    @Test
    public void throwingSinkLeavesWinnerAndThreadStateIntact() {
        TraceTestSupport.CaptureSink throwingSink = new TraceTestSupport.CaptureSink() {
            @Override
            public void accept(DecisionTrace trace) {
                throw new RuntimeException("sink exploded");
            }
        };
        EvaluatedAction winner = new CombinedEvaluator(
            pairScenario("A", 100.0f, "B", 50.0f), throwingSink)
            .evaluateDecision(passableContext("A", "B"));
        assertNotNull(winner);
        assertEquals("A", winner.getActionId());
        assertFalse("sink exception must not leak the session", TraceSession.isActive());
    }

    // =========================================================================
    // Gate corpus: top-level decision field divergence is detected (not winner-only)
    // =========================================================================

    @Test
    public void topLevelDecisionFieldDivergenceDetected() {
        DecisionContext ctx1 = passableContext("A", "B");
        DecisionContext ctx2 = new DecisionContext(null, "tester", "CARD_ACTION_CHOICE",
            "Choose action to take", "d2", Phase.DEPLOY);  // decisionId differs ONLY
        ctx2.setNoPass(false);
        ctx2.setMin(0);
        ctx2.setSide(Side.DARK);
        ctx2.setActionIds(new ArrayList<>(Arrays.asList("A", "B")));

        DecisionTrace t1 = capture(pairScenario("A", 100.0f, "B", 50.0f), ctx1);
        DecisionTrace t2 = capture(pairScenario("A", 100.0f, "B", 50.0f), ctx2);

        // identical candidates, operations, and winner — only the decision id differs
        assertEquals(t1.getMergeOrder(), t2.getMergeOrder());
        assertEquals(t1.getOperations().size(), t2.getOperations().size());
        assertNotNull("comparator must flag top-level decision field divergence",
            TraceTestSupport.firstMismatch(t1, t2));
    }

    // =========================================================================
    // GAP P1-4 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): mandatory identity —
    // framework rank/select ops carry the typed COMBINED_EVALUATOR producer, and no
    // operation dimension is ever null
    // =========================================================================

    @Test
    public void frameworkOperationsCarryCombinedEvaluatorProducerIdentity() {
        DecisionTrace trace = capture(richScenario(), passableContext("A", "B", "C"));
        boolean sawFrameworkOp = false;
        for (TraceOperation op : trace.getOperations()) {
            assertNotNull("producer is mandatory on every op", op.getEvaluatorId());
            assertNotNull("ruleId is mandatory on every op", op.getRuleId());
            assertNotNull("domainId is mandatory on every op", op.getDomainId());
            assertNotNull("outputKind is mandatory on every op", op.getOutputKind());
            if (op.getOp() == TraceOp.RANK || op.getOp() == TraceOp.SELECT) {
                sawFrameworkOp = true;
                assertEquals("framework ranks/selects, so it is the producer",
                    TraceOperation.PRODUCER_COMBINED_EVALUATOR, op.getEvaluatorId());
            }
        }
        assertTrue("scenario must produce at least one rank/select", sawFrameworkOp);
    }
}
