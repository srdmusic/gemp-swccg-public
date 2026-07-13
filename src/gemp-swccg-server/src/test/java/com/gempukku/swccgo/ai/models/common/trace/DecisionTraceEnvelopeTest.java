package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md):
 * pure construction, deep-copy, typed-id, and session-lifecycle tests for the V2
 * envelope. No production consumer, no server, no bot classes — common package only.
 */
public class DecisionTraceEnvelopeTest {

    private static final TraceFinalization EMPTY_FINALIZATION = new TraceFinalization(
        null, null, false, null, false, null,
        null, null, null,
        null, null, null, List.of(), null, false, false);

    private static TraceCaptureFailure failure(String detail) {
        return new TraceCaptureFailure(TraceCaptureFailure.Stage.SNAPSHOT, "test-failure", detail);
    }

    // =========================================================================
    // Deep immutability: defensive copies, not unmodifiable wrappers
    // =========================================================================

    @Test
    public void envelopeDefensivelyCopiesEveryCallerOwnedList() {
        List<TraceCaptureFailure> failures = new ArrayList<>(List.of(failure("f1")));
        List<String> raw = new ArrayList<>(Arrays.asList("A", "B"));
        List<String> merge = new ArrayList<>(Arrays.asList("B", "A"));
        List<TraceOperation> ops = new ArrayList<>();
        ops.add(new TraceOperation(0, TraceOp.INITIAL, 0, null, "A", "E1",
            TraceRuleId.LEGACY_UNTAGGED, TraceDomainId.LEGACY_UNTAGGED,
            TraceOutputKind.LEGACY_UNTAGGED, null, null,
            Float.floatToRawIntBits(1.0f), false, null, "init"));
        List<TraceIntendedStateEvent> events = new ArrayList<>(List.of(
            new TraceIntendedStateEvent(TraceIntendedStateEvent.Kind.PENDING_CONCEDE, "test")));

        DecisionTrace trace = new DecisionTrace(DecisionTrace.SCHEMA_VERSION, "test-bot",
            "d1", "CARD_ACTION_CHOICE", "Choose action", null,
            TraceStatus.INCOMPLETE, failures, null, raw, merge, ops, EMPTY_FINALIZATION, events);

        // mutate every source list AFTER construction — the trace must not move
        failures.add(failure("f2"));
        raw.add("C");
        merge.clear();
        ops.clear();
        events.clear();

        assertEquals(1, trace.getCaptureFailures().size());
        assertEquals(Arrays.asList("A", "B"), trace.getRawCandidateOrder());
        assertEquals(Arrays.asList("B", "A"), trace.getMergeOrder());
        assertEquals(1, trace.getOperations().size());
        assertEquals(1, trace.getIntendedStateEvents().size());

        // and the exposed lists are unmodifiable
        try {
            trace.getRawCandidateOrder().add("Z");
            fail("rawCandidateOrder must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
        try {
            trace.getOperations().clear();
            fail("operations must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // required
        }
    }

    @Test
    public void routeRecordAndFinalizationDefensivelyCopy() {
        List<String> evidence = new ArrayList<>(List.of("COMBINED_EVALUATOR: seam"));
        TraceRouteRecord route = new TraceRouteRecord(TraceRoute.COMBINED_EVALUATOR, evidence, null);
        evidence.add("tampered");
        assertEquals(1, route.orderedEvidence().size());

        List<TraceCorrection> corrections = new ArrayList<>(List.of(new TraceCorrection(
            TraceCorrection.Kind.SELECTABLE_CLAMP, "x", "y", "clamped")));
        TraceFinalization finalization = new TraceFinalization(null, null, false, null,
            false, null, null, null, null,
            null, null, null, corrections, "y", true, false);
        corrections.clear();
        assertEquals(1, finalization.corrections().size());
    }

    // =========================================================================
    // Status/failure consistency: no silent truncation, no phantom failures
    // =========================================================================

    @Test
    public void statusAndFailuresMustAgree() {
        try {
            new DecisionTrace(DecisionTrace.SCHEMA_VERSION, "test-bot", "d1", "T", "text", null,
                TraceStatus.COMPLETE, List.of(failure("f")), null,
                List.of(), List.of(), List.of(), EMPTY_FINALIZATION, List.of());
            fail("COMPLETE with capture failures must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
        try {
            new DecisionTrace(DecisionTrace.SCHEMA_VERSION, "test-bot", "d1", "T", "text", null,
                TraceStatus.INCOMPLETE, List.of(), null,
                List.of(), List.of(), List.of(), EMPTY_FINALIZATION, List.of());
            fail("INCOMPLETE without capture failures must be rejected");
        } catch (IllegalArgumentException expected) {
            // required
        }
    }

    // =========================================================================
    // Typed identity: stable-id validation, prose rejected, LEGACY_UNTAGGED remains
    // =========================================================================

    @Test
    public void ruleIdValidatesStableRegistryForm() {
        assertEquals("V67bc", TraceRuleId.of("V67bc").id());
        assertEquals("V24.15-drain", TraceRuleId.of("V24.15-drain").id());
        assertEquals("FS-L1-abandon", TraceRuleId.of("FS-L1-abandon").id());
        assertEquals("vehicle-pilot+docking-bay", TraceRuleId.of("vehicle-pilot+docking-bay").id());
        assertEquals(TraceRuleId.LEGACY_UNTAGGED, TraceRuleId.of("LEGACY_UNTAGGED"));

        for (String prose : new String[]{
                "", "  ", "V148 (Steve, 2026-05-28): always offer Done",
                "reasoning text with spaces", "-leading-dash"}) {
            try {
                TraceRuleId.of(prose);
                fail("prose/blank must never validate as rule identity: \"" + prose + "\"");
            } catch (IllegalArgumentException expected) {
                // required
            }
        }
    }

    // =========================================================================
    // Session lifecycle: nested opens refused, no leaks, thread-local always cleared
    // =========================================================================

    private static TraceSnapshots.Result testSnapshot(String decisionId, List<String> actionIds) {
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.decisionId = decisionId;
        in.decisionTypeName = "CARD_ACTION_CHOICE";
        in.decisionText = "Choose action";
        in.phase = Phase.DEPLOY;
        in.turn = 1;
        in.currentPlayer = "tester";
        in.side = Side.DARK;
        in.noPassParam = false;
        in.minParam = 0;
        in.maxParam = 1;
        in.actionIds = actionIds;
        return TraceSnapshots.build(in);
    }

    @Test
    public void nestedOpenIsRefusedAndOuterSessionSurvives() {
        TraceSnapshots.Result snap = testSnapshot("outer", List.of("A"));
        assertTrue(TraceSession.open("outer-bot", "outer", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), false));
        try {
            assertFalse("nested open must be refused",
                TraceSession.open("inner-bot", "inner", "CARD_ACTION_CHOICE", "text",
                    List.of("B"), null, List.of("inner"), false));
            assertTrue(TraceSession.isActive());
            TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "outer evidence", null);
        } finally {
            DecisionTrace trace = TraceSession.close();
            assertNotNull(trace);
            assertEquals("outer-bot", trace.getBotModel());
            assertEquals("outer", trace.getDecisionId());
            assertEquals(List.of("A"), trace.getRawCandidateOrder());
        }
        assertFalse("close must always clear the thread-local", TraceSession.isActive());
    }

    @Test
    public void closeWithoutOpenReturnsNullAndRecordCallsNoOp() {
        assertFalse(TraceSession.isActive());
        // every record call must be a safe no-op with no session open
        TraceSession.recordRoute(TraceRoute.HEURISTIC_FALLBACK, "no session", null);
        TraceSession.recordFinalResponse("x", false);
        TraceSession.recordInitial(new Object(), "A", 1.0f, TraceRuleId.LEGACY_UNTAGGED, null, null, "d");
        assertNull(TraceSession.close());
        assertFalse(TraceSession.isActive());
    }

    // =========================================================================
    // Scripted bot-boundary flow: route fallback chain + finalization capture
    // =========================================================================

    @Test
    public void scriptedRouteAndFinalResponseFlowIsCaptured() {
        TraceSnapshots.Result snap = testSnapshot("d9", List.of("5", "7"));
        assertTrue(TraceSession.open("bot", "d9", "CARD_ACTION_CHOICE", "Choose action",
            List.of("5", "7"), snap.snapshot(), snap.issues(), true));

        TraceSession.recordRoute(TraceRoute.HEURISTIC_FALLBACK, "no evaluator handled", null);
        TraceSession.recordRoute(TraceRoute.RAW_NOPASS_EMERGENCY, "empty result with noPass", null);
        // GATE P0-3: COMPLETE now requires the route's pass/cancel facts and winner
        // facts — recorded or explicitly not-applicable, never silently null.
        TraceSession.recordPassEligibility(true, "min=0 noPass=false (scripted)");
        TraceSession.recordEvaluatorLaneNotApplicable(
            "scripted: emergency route, evaluator lane never produced a winner");
        TraceSession.recordEmergencyResponse("5", "Emergency: Choosing random action (5)");
        TraceSession.recordCorrection(TraceCorrection.Kind.SAFETY_FORCED_CHOICE, "", "5", "forced");
        TraceSession.recordIntendedStateEvent(
            TraceIntendedStateEvent.Kind.DECISION_TRACKER_RECORD, "recordDecision response='5'");
        TraceSession.recordFinalResponse("5", false);

        DecisionTrace trace = TraceSession.close();
        assertNotNull(trace);
        assertEquals(TraceStatus.COMPLETE, trace.getStatus());
        // P0-3: the recorded pass facts and the explicit winner not-applicable are visible
        assertEquals(Boolean.TRUE, trace.getFinalization().passEligible());
        assertFalse(trace.getFinalization().preSafetyWinnerRecorded());
        assertNotNull(trace.getFinalization().preSafetyWinnerNotApplicableReason());
        // selected route = the lane that produced the response; the earlier lane stays
        // in the ordered evidence and names the fall-through
        assertEquals(TraceRoute.RAW_NOPASS_EMERGENCY, trace.getRoute().selected());
        assertEquals(2, trace.getRoute().orderedEvidence().size());
        assertTrue(trace.getRoute().orderedEvidence().get(0).startsWith("HEURISTIC_FALLBACK"));
        assertEquals("fell through from HEURISTIC_FALLBACK", trace.getRoute().bypassOrFallbackReason());
        // finalization record complete
        assertEquals("5", trace.getFinalization().emergencyResponse());
        assertEquals(1, trace.getFinalization().corrections().size());
        assertEquals(TraceCorrection.Kind.SAFETY_FORCED_CHOICE,
            trace.getFinalization().corrections().get(0).kind());
        assertEquals("5", trace.getFinalization().finalResponse());
        assertTrue(trace.getFinalization().finalResponseRecorded());
        assertFalse(trace.getFinalization().skippedCommonFinalizer());
        // intended state events observed, never applied
        assertEquals(1, trace.getIntendedStateEvents().size());
        assertEquals(TraceIntendedStateEvent.Kind.DECISION_TRACKER_RECORD,
            trace.getIntendedStateEvents().get(0).kind());
    }

    @Test
    public void botBoundarySessionWithoutFinalResponseIsIncomplete() {
        TraceSnapshots.Result snap = testSnapshot("d10", List.of("A"));
        assertTrue(TraceSession.open("bot", "d10", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), true));
        TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "evidence", null);
        DecisionTrace trace = TraceSession.close();
        assertNotNull(trace);
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        boolean finalizationFailure = false;
        for (TraceCaptureFailure f : trace.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.FINALIZATION) {
                finalizationFailure = true;
            }
        }
        assertTrue("missing final response must be a FINALIZATION capture failure",
            finalizationFailure);
    }

    @Test
    public void missingRouteObservationIsIncomplete() {
        TraceSnapshots.Result snap = testSnapshot("d11", List.of("A"));
        assertTrue(TraceSession.open("bot", "d11", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), false));
        DecisionTrace trace = TraceSession.close();
        assertNotNull(trace);
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        assertNull(trace.getRoute());
        boolean routeFailure = false;
        for (TraceCaptureFailure f : trace.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.ROUTE) {
                routeFailure = true;
            }
        }
        assertTrue("missing route must be a ROUTE capture failure", routeFailure);
    }

    // =========================================================================
    // Frozen input: parallel raw arrays of different lengths mark INCOMPLETE
    // =========================================================================

    @Test
    public void mismatchedParallelArraysMarkTraceIncomplete() {
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.decisionId = "d12";
        in.decisionTypeName = "CARD_ACTION_CHOICE";
        in.decisionText = "Choose action";
        in.phase = Phase.DEPLOY;
        in.turn = 1;
        in.currentPlayer = "tester";
        in.side = Side.DARK;
        in.noPassParam = false;
        in.minParam = 0;
        in.maxParam = 1;
        in.actionIds = List.of("A", "B");
        in.actionTexts = List.of("Deploy A", "Deploy B", "Deploy GHOST");  // longer!
        TraceSnapshots.Result result = TraceSnapshots.build(in);

        assertNotNull("mismatch is retained in the snapshot, not rejected", result.snapshot());
        assertFalse("length mismatch must be reported", result.issues().isEmpty());
        // the mismatch is RETAINED: 3 rows, the ghost row has text but no id (no padding)
        assertEquals(3, result.snapshot().actionFacts().size());
        assertNull(result.snapshot().actionFacts().get(2).actionId());
        assertEquals("Deploy GHOST", result.snapshot().actionFacts().get(2).actionText());

        assertTrue(TraceSession.open("bot", "d12", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A", "B"), result.snapshot(), result.issues(), false));
        TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "evidence", null);
        DecisionTrace trace = TraceSession.close();
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        assertEquals(TraceCaptureFailure.Stage.SNAPSHOT,
            trace.getCaptureFailures().get(0).stage());
    }

    // =========================================================================
    // GAP P0-1 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): complete raw decision
    // =========================================================================

    /** Gate repair fixture 1: present-EMPTY versus present-nonempty parallel arrays
     *  becomes INCOMPLETE (previously indistinguishable from absent). */
    @Test
    public void presentEmptyParallelArrayMarksTraceIncomplete() {
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.decisionId = "d13";
        in.decisionTypeName = "CARD_ACTION_CHOICE";
        in.decisionText = "Choose action";
        in.phase = Phase.DEPLOY;
        in.turn = 1;
        in.currentPlayer = "tester";
        in.side = Side.DARK;
        in.noPassParam = false;
        in.minParam = 0;
        in.maxParam = 1;
        in.actionIds = List.of("A", "B");
        in.actionTexts = List.of();  // PRESENT and EMPTY — not absent
        TraceSnapshots.Result result = TraceSnapshots.build(in);

        assertNotNull(result.snapshot());
        assertFalse("present-empty vs present-nonempty must be reported as a mismatch",
            result.issues().isEmpty());

        assertTrue(TraceSession.open("bot", "d13", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A", "B"), result.snapshot(), result.issues(), false));
        TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "evidence", null);
        DecisionTrace trace = TraceSession.close();
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        assertEquals(TraceCaptureFailure.Stage.SNAPSHOT,
            trace.getCaptureFailures().get(0).stage());
    }

    /** Gate repair fixture 2: every engine decision parameter is preserved separately,
     *  verbatim, with the absent / present-empty distinction and blanks kept blank. */
    @Test
    public void everyEngineParameterPreservedSeparatelyAndVerbatim() {
        Map<String, String[]> engineParams = new LinkedHashMap<>();
        // the full engine key set (AbstractAwaitingDecision setParam call sites)
        engineParams.put("actionId", new String[]{"5", "", "7"});   // blank stays blank
        engineParams.put("actionText", new String[]{"Deploy A", "Pass", "Deploy B"});
        engineParams.put("cardId", new String[]{"temp1", "temp2", "temp3"});
        engineParams.put("blueprintId", new String[]{"1_1", "1_2", "1_3"});
        engineParams.put("testingText", new String[]{"A", "B", "C"});
        engineParams.put("backSideTestingText", new String[]{"", "", ""});
        engineParams.put("selectable", new String[]{"true", "false", "true"});
        engineParams.put("preselected", new String[]{});            // PRESENT-EMPTY
        engineParams.put("results", new String[]{"Yes", "No"});     // own array, never folded away
        engineParams.put("min", new String[]{"0"});
        engineParams.put("max", new String[]{"1"});
        engineParams.put("noPass", new String[]{"true"});
        engineParams.put("autoPassEligible", new String[]{"false"});
        engineParams.put("defaultIndex", new String[]{"2"});
        engineParams.put("defaultValue", new String[]{"1"});
        engineParams.put("horizontal", new String[]{"true"});
        engineParams.put("cardText", new String[]{"some card text"});
        engineParams.put("returnAnyChange", new String[]{"false"});
        engineParams.put("yourTurn", new String[]{"true"});
        engineParams.put("noLongDelay", new String[]{"true"});
        engineParams.put("revertEligible", new String[]{"false"});
        engineParams.put("timeoutValue", new String[]{"30"});

        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.decisionId = "d14";
        in.decisionTypeName = "CARD_ACTION_CHOICE";
        in.decisionText = "Choose action";
        in.phase = Phase.DEPLOY;
        in.turn = 1;
        in.currentPlayer = "tester";
        in.side = Side.DARK;
        in.noPassParam = true;
        in.minParam = 0;
        in.maxParam = 1;
        in.actionIds = Arrays.asList("5", "", "7");
        in.actionTexts = Arrays.asList("Deploy A", "Pass", "Deploy B");
        in.cardIds = Arrays.asList("temp1", "temp2", "temp3");
        in.blueprintIds = Arrays.asList("1_1", "1_2", "1_3");
        in.testingTexts = Arrays.asList("A", "B", "C");
        in.selectable = Arrays.asList(true, false, true);
        in.rawParameters = engineParams;
        TraceSnapshots.Result result = TraceSnapshots.build(in);

        assertNotNull(result.snapshot());
        assertTrue("all-parallel arrays agree; no issues expected: " + result.issues(),
            result.issues().isEmpty());
        DecisionSnapshot.RawDecision raw = result.snapshot().rawDecision();
        assertEquals(DecisionSnapshot.RawDecision.Source.ENGINE_PARAMETERS, raw.source());
        // every key present, separately, verbatim
        for (Map.Entry<String, String[]> entry : engineParams.entrySet()) {
            assertTrue("raw key must be present: " + entry.getKey(), raw.has(entry.getKey()));
            assertEquals("raw values must be verbatim for " + entry.getKey(),
                Arrays.asList(entry.getValue()), raw.values(entry.getKey()));
        }
        // blank id preserved as blank in the raw record even though the normalized
        // ActionFacts row maps it to absent
        assertEquals("", raw.values("actionId").get(1));
        assertNull(result.snapshot().actionFacts().get(1).actionId());
        // results captured as their OWN array (not just folded into row text)
        assertEquals(Arrays.asList("Yes", "No"), raw.values("results"));
        // present-empty vs absent
        assertTrue(raw.has("preselected"));
        assertEquals(List.of(), raw.values("preselected"));
        assertFalse("a key the engine never sent must be ABSENT", raw.has("neverSent"));
    }

    // =========================================================================
    // GAP P0-3: route-specific completeness matrix
    // =========================================================================

    /** Gate repair fixture 3: a CombinedEvaluator trace missing pass eligibility, the
     *  pre-safety winner, or any operation is rejected (INCOMPLETE, typed failures). */
    @Test
    public void combinedEvaluatorRouteRequiresPassEligibilityWinnerAndOperations() {
        // missing pass eligibility
        TraceSnapshots.Result snap = testSnapshot("d15", List.of("A"));
        assertTrue(TraceSession.open("bot", "d15", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), false));
        TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "evidence", null);
        TraceSession.recordSelect(new Object(), "A", 1.0f, false, null, "winner");
        TraceSession.recordPreSafetyWinner("A", 1.0f, false, null);
        DecisionTrace noPassFacts = TraceSession.close();
        assertEquals(TraceStatus.INCOMPLETE, noPassFacts.getStatus());
        assertTrue(hasFailureContaining(noPassFacts, "pass/cancel eligibility"));

        // missing pre-safety winner
        snap = testSnapshot("d16", List.of("A"));
        assertTrue(TraceSession.open("bot", "d16", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), false));
        TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "evidence", null);
        TraceSession.recordPassEligibility(true, "min=0 noPass=false");
        TraceSession.recordSelect(new Object(), "A", 1.0f, false, null, "winner");
        DecisionTrace noWinner = TraceSession.close();
        assertEquals(TraceStatus.INCOMPLETE, noWinner.getStatus());
        assertTrue(hasFailureContaining(noWinner, "pre-safety winner"));

        // zero operations — the evaluator route legitimately never lacks ops
        snap = testSnapshot("d17", List.of("A"));
        assertTrue(TraceSession.open("bot", "d17", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), false));
        TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR, "evidence", null);
        TraceSession.recordPassEligibility(true, "min=0 noPass=false");
        TraceSession.recordPreSafetyWinner("A", 1.0f, false, null);
        DecisionTrace noOps = TraceSession.close();
        assertEquals(TraceStatus.INCOMPLETE, noOps.getStatus());
        assertTrue(hasFailureContaining(noOps, "zero recorded operations"));
    }

    /** Gate repair fixture 4: a direct route records pass/winner facts as EXPLICIT
     *  not-applicable and is COMPLETE with them; nothing is silently null. */
    @Test
    public void directRouteWithExplicitNotApplicableFinalizationIsComplete() {
        TraceSnapshots.Result snap = testSnapshot("d18", List.of("temp1", "temp2"));
        assertTrue(TraceSession.open("bot", "d18", "CARD_ACTION_CHOICE",
            "Choose cards to forfeit, if desired",
            List.of("temp1", "temp2"), snap.snapshot(), snap.issues(), true));
        TraceSession.recordRoute(TraceRoute.V45_OPTIONAL_FORFEIT,
            "decision text contains 'forfeit' + 'if desired'", null);
        TraceSession.recordEvaluatorLaneNotApplicable(
            "direct interceptor V45: evaluator lane never runs on this route");
        TraceSession.recordFinalResponse("", true);

        DecisionTrace trace = TraceSession.close();
        assertEquals("explicit not-applicable satisfies the completeness matrix: "
            + trace.getCaptureFailures(), TraceStatus.COMPLETE, trace.getStatus());
        assertNull(trace.getFinalization().passEligible());
        assertNotNull(trace.getFinalization().passEligibilityNotApplicableReason());
        assertFalse(trace.getFinalization().preSafetyWinnerRecorded());
        assertNotNull(trace.getFinalization().preSafetyWinnerNotApplicableReason());
        assertTrue(trace.getFinalization().skippedCommonFinalizer());
    }

    // =========================================================================
    // GAP P0-2: construction/sink failures yield typed INCOMPLETE evidence
    // =========================================================================

    /** Gate repair fixture 5 (open path): session construction failure installs a
     *  degraded evidence-only session with a typed OPEN failure — never a silent false. */
    @Test
    public void openConstructionFailureInstallsDegradedTypedSession() {
        List<String> bomb = new ArrayList<>() {
            @Override
            public Iterator<String> iterator() {
                throw new IllegalStateException("scripted issues-list iterator bomb");
            }
        };
        assertTrue("open must preserve evidence via the degraded session",
            TraceSession.open("bot", "d19", "CARD_ACTION_CHOICE", "Choose action",
                List.of("A"), null, bomb, false));
        DecisionTrace trace = TraceSession.close();
        assertNotNull(trace);
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        boolean openFailure = false;
        for (TraceCaptureFailure f : trace.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.OPEN) {
                openFailure = true;
            }
        }
        assertTrue("open failure must be typed OPEN-stage evidence", openFailure);
        assertEquals("bot", trace.getBotModel());
        assertFalse(TraceSession.isActive());
    }

    /** Gate repair fixture 5 (finish path): record construction failure produces an
     *  inspectable typed fallback envelope instead of a null. */
    @Test
    public void finishFailureProducesTypedFallbackEnvelope() {
        TraceCollector broken = new TraceCollector("bot", "d20", "CARD_ACTION_CHOICE",
            "Choose action", List.of("A"), null, List.of("scripted: snapshot absent"), false) {
            @Override
            DecisionTrace finish() {
                throw new IllegalStateException("scripted finish failure");
            }
        };
        assertTrue(TraceSession.openForTesting(broken));
        DecisionTrace trace = TraceSession.close();
        assertNotNull("finish failure must yield the typed fallback envelope, not null", trace);
        assertEquals(TraceStatus.INCOMPLETE, trace.getStatus());
        boolean closeFailure = false;
        for (TraceCaptureFailure f : trace.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.CLOSE
                    && f.errorClass().equals(IllegalStateException.class.getName())) {
                closeFailure = true;
            }
        }
        assertTrue("CLOSE-stage failure naming the error class is required", closeFailure);
        assertEquals("bot", trace.getBotModel());
        assertEquals(List.of("A"), trace.getRawCandidateOrder());
        assertFalse("thread-local must be cleared even on finish failure", TraceSession.isActive());
    }

    /** Gate repair fixture 6: sink acceptance failure produces an inspectable typed
     *  SINK failure through the emission channel (re-offered once, evidence intact). */
    @Test
    public void sinkFailureIsReofferedOnceWithTypedSinkFailure() {
        TraceSnapshots.Result snap = testSnapshot("d21", List.of("A"));
        assertTrue(TraceSession.open("bot", "d21", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), true));
        TraceSession.recordRoute(TraceRoute.V45_OPTIONAL_FORFEIT, "scripted", null);
        TraceSession.recordEvaluatorLaneNotApplicable("scripted direct route");
        TraceSession.recordFinalResponse("", true);

        class OnceThrowingSink extends TraceTestSupport.CaptureSink {
            private boolean threwOnce;

            @Override
            public void accept(DecisionTrace trace) {
                if (!threwOnce) {
                    threwOnce = true;
                    throw new RuntimeException("first accept exploded");
                }
                super.accept(trace);
            }
        }
        OnceThrowingSink sink = new OnceThrowingSink();
        TraceSession.closeAndEmit(sink);

        DecisionTrace received = sink.single();
        assertEquals("the re-offered trace is INCOMPLETE with the sink failure typed",
            TraceStatus.INCOMPLETE, received.getStatus());
        boolean sinkFailure = false;
        for (TraceCaptureFailure f : received.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.SINK
                    && f.detail().contains("first accept exploded")) {
                sinkFailure = true;
            }
        }
        assertTrue("typed SINK failure must be inspectable through the sink", sinkFailure);
        // the original evidence survives on the derived trace
        assertEquals(TraceRoute.V45_OPTIONAL_FORFEIT, received.getRoute().selected());
        assertEquals("", received.getFinalization().finalResponse());
        assertFalse(TraceSession.isActive());
    }

    // =========================================================================
    // GAP P1-4: operation identity is mandatory (sentinels, never null)
    // =========================================================================

    /** Gate repair fixture 7: null producer/rule/domain/kind identity is rejected at
     *  construction; the recording choke points substitute explicit sentinels. */
    @Test
    public void operationIdentityIsMandatoryAndSentinelFilled() {
        // direct construction rejects every null identity dimension
        try {
            new TraceOperation(0, TraceOp.SELECT, 0, null, "A", null,
                TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR,
                TraceOutputKind.COMBINED_EVALUATOR, null, null, null, false, null, "d");
            fail("null producer must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            new TraceOperation(0, TraceOp.SELECT, 0, null, "A", "E1",
                null, TraceDomainId.COMBINED_EVALUATOR,
                TraceOutputKind.COMBINED_EVALUATOR, null, null, null, false, null, "d");
            fail("null ruleId must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            new TraceOperation(0, TraceOp.SELECT, 0, null, "A", "E1",
                TraceRuleId.COMBINED_EVALUATOR, null,
                TraceOutputKind.COMBINED_EVALUATOR, null, null, null, false, null, "d");
            fail("null domainId must be rejected");
        } catch (NullPointerException expected) {
            // required
        }
        try {
            new TraceOperation(0, TraceOp.SELECT, 0, null, "A", "E1",
                TraceRuleId.COMBINED_EVALUATOR, TraceDomainId.COMBINED_EVALUATOR,
                null, null, null, null, false, null, "d");
            fail("null outputKind must be rejected");
        } catch (NullPointerException expected) {
            // required
        }

        // the recording choke points substitute explicit sentinels
        TraceSnapshots.Result snap = testSnapshot("d22", List.of("A"));
        assertTrue(TraceSession.open("bot", "d22", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), snap.snapshot(), snap.issues(), false));
        try {
            // legacy arm outside any evaluator binding: LEGACY_UNTAGGED trio + framework producer
            TraceSession.recordInitial(new Object(), "A", 1.0f, null, null, null, "legacy init");
            // framework selection: COMBINED_EVALUATOR trio + framework producer
            TraceSession.recordSelect(new Object(), "A", 1.0f, false, null, "winner");
        } finally {
            DecisionTrace trace = TraceSession.close();
            assertEquals(2, trace.getOperations().size());
            TraceOperation initial = trace.getOperations().get(0);
            assertEquals(TraceOperation.PRODUCER_COMBINED_EVALUATOR, initial.getEvaluatorId());
            assertEquals(TraceRuleId.LEGACY_UNTAGGED, initial.getRuleId());
            assertEquals(TraceDomainId.LEGACY_UNTAGGED, initial.getDomainId());
            assertEquals(TraceOutputKind.LEGACY_UNTAGGED, initial.getOutputKind());
            TraceOperation select = trace.getOperations().get(1);
            assertEquals(TraceOperation.PRODUCER_COMBINED_EVALUATOR, select.getEvaluatorId());
            assertEquals(TraceRuleId.COMBINED_EVALUATOR, select.getRuleId());
            assertEquals(TraceDomainId.COMBINED_EVALUATOR, select.getDomainId());
            assertEquals(TraceOutputKind.COMBINED_EVALUATOR, select.getOutputKind());
        }
    }

    // =========================================================================
    // GAP P1-5: selected route cross-validated against the frozen decision shape
    // =========================================================================

    private static TraceSnapshots.Result multipleChoiceSnapshot(String decisionId,
                                                                List<String> results) {
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.decisionId = decisionId;
        in.decisionTypeName = "MULTIPLE_CHOICE";
        in.decisionText = "Choose an option";
        in.phase = Phase.DEPLOY;
        in.turn = 1;
        in.currentPlayer = "tester";
        in.side = Side.DARK;
        in.noPassParam = true;
        in.minParam = 0;
        in.maxParam = 0;
        in.multipleChoiceResults = results;
        return TraceSnapshots.build(in);
    }

    /** Gate repair fixture 8: a selected route inconsistent with the frozen decision
     *  shape is rejected as a typed ROUTE failure (wire shape only — phase is a window,
     *  never a route key, per the amended route map). */
    @Test
    public void routeIncompatibleWithFrozenShapeIsTypedRouteFailure() {
        // V44 revert approval claims MULTIPLE_CHOICE, but the frozen shape is
        // CARD_ACTION_CHOICE — the disagreement is preserved as typed evidence.
        TraceSnapshots.Result wrongShape = testSnapshot("d23", List.of("A"));
        assertTrue(TraceSession.open("bot", "d23", "CARD_ACTION_CHOICE", "Choose action",
            List.of("A"), wrongShape.snapshot(), wrongShape.issues(), true));
        TraceSession.recordRoute(TraceRoute.V44_V67J_REVERT_APPROVAL, "scripted mismatch", null);
        TraceSession.recordEvaluatorLaneNotApplicable("scripted direct route");
        TraceSession.recordFinalResponse("0", true);
        DecisionTrace mismatch = TraceSession.close();
        assertEquals(TraceStatus.INCOMPLETE, mismatch.getStatus());
        boolean routeMismatch = false;
        for (TraceCaptureFailure f : mismatch.getCaptureFailures()) {
            if (f.stage() == TraceCaptureFailure.Stage.ROUTE
                    && "route-evidence-mismatch".equals(f.errorClass())) {
                routeMismatch = true;
            }
        }
        assertTrue("frozen-shape disagreement must be a typed ROUTE failure", routeMismatch);

        // positive control: the same route over a genuine MULTIPLE_CHOICE snapshot
        // carries no mismatch and stays COMPLETE
        TraceSnapshots.Result rightShape = multipleChoiceSnapshot("d24", List.of("Yes", "No"));
        assertTrue("control snapshot must build cleanly: " + rightShape.issues(),
            rightShape.issues().isEmpty());
        assertTrue(TraceSession.open("bot", "d24", "MULTIPLE_CHOICE", "Choose an option",
            List.of("0", "1"), rightShape.snapshot(), rightShape.issues(), true));
        TraceSession.recordRoute(TraceRoute.V44_V67J_REVERT_APPROVAL,
            "MULTIPLE_CHOICE + decision text contains 'revert'", null);
        TraceSession.recordEvaluatorLaneNotApplicable("scripted direct route");
        TraceSession.recordFinalResponse("0", true);
        DecisionTrace compatible = TraceSession.close();
        assertEquals("compatible route/shape must not be flagged: "
            + compatible.getCaptureFailures(), TraceStatus.COMPLETE, compatible.getStatus());
    }

    private static boolean hasFailureContaining(DecisionTrace trace, String needle) {
        for (TraceCaptureFailure f : trace.getCaptureFailures()) {
            if (f.detail() != null && f.detail().contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
