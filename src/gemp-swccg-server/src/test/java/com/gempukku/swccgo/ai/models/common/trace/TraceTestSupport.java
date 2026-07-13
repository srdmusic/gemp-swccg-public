package com.gempukku.swccgo.ai.models.common.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Finalization record": "The comparator includes all top-level decision fields, the
 * snapshot, status/errors, route, operations, finalization, and intended state events.
 * Winner-only comparison is rejected."): trace-comparison assertion helper.
 *
 * Ordered events, raw float bits exact, complete veto reasons, full envelope coverage;
 * never decimal-tolerant. The standalone fixture comparator tool is a later increment;
 * this helper proves the gate corpus cases fail comparison.
 */
public final class TraceTestSupport {

    private TraceTestSupport() {
    }

    /** Capture sink: enabled, stores every finalized DecisionTrace it receives. */
    public static class CaptureSink implements TraceSink {
        private final List<DecisionTrace> traces = new ArrayList<>();

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void accept(DecisionTrace trace) {
            traces.add(trace);
        }

        public List<DecisionTrace> getTraces() {
            return traces;
        }

        public DecisionTrace single() {
            if (traces.size() != 1) {
                fail("expected exactly 1 finalized trace, got " + traces.size());
            }
            return traces.get(0);
        }
    }

    /**
     * Strict fixture sink (contract "Error and lifecycle rules": "A strict fixture sink
     * fails on any incomplete trace"): an INCOMPLETE trace must never become fixture
     * evidence, so accepting one is an immediate test failure.
     */
    public static final class StrictFixtureSink extends CaptureSink {
        @Override
        public void accept(DecisionTrace trace) {
            if (trace.getStatus() != TraceStatus.COMPLETE) {
                fail("strict fixture sink rejects INCOMPLETE trace: " + trace.getCaptureFailures());
            }
            super.accept(trace);
        }
    }

    /** @return null when the traces compare equal, otherwise the FIRST mismatch found. */
    public static String firstMismatch(DecisionTrace a, DecisionTrace b) {
        // ── top-level decision fields (winner-only comparison is rejected) ──
        if (a.getSchemaVersion() != b.getSchemaVersion()) {
            return "schemaVersion: " + a.getSchemaVersion() + " != " + b.getSchemaVersion();
        }
        if (!Objects.equals(a.getBotModel(), b.getBotModel())) {
            return "botModel: " + a.getBotModel() + " != " + b.getBotModel();
        }
        if (!Objects.equals(a.getDecisionId(), b.getDecisionId())) {
            return "decisionId: " + a.getDecisionId() + " != " + b.getDecisionId();
        }
        if (!Objects.equals(a.getDecisionType(), b.getDecisionType())) {
            return "decisionType: " + a.getDecisionType() + " != " + b.getDecisionType();
        }
        if (!Objects.equals(a.getDecisionText(), b.getDecisionText())) {
            return "decisionText: " + a.getDecisionText() + " != " + b.getDecisionText();
        }
        // ── status + ordered typed failures ──
        if (a.getStatus() != b.getStatus()) {
            return "status: " + a.getStatus() + " != " + b.getStatus();
        }
        if (!a.getCaptureFailures().equals(b.getCaptureFailures())) {
            return "captureFailures: " + a.getCaptureFailures() + " != " + b.getCaptureFailures();
        }
        // ── frozen input snapshot (records: deep equals via components) ──
        if (!Objects.equals(a.getSnapshot(), b.getSnapshot())) {
            return "snapshot: differs";
        }
        // ── route ──
        if (!Objects.equals(a.getRoute(), b.getRoute())) {
            return "route: " + a.getRoute() + " != " + b.getRoute();
        }
        // ── candidate orders: raw (ordinal authority) AND merge (reorder detector) ──
        if (!a.getRawCandidateOrder().equals(b.getRawCandidateOrder())) {
            return "rawCandidateOrder: " + a.getRawCandidateOrder() + " != " + b.getRawCandidateOrder();
        }
        if (!a.getMergeOrder().equals(b.getMergeOrder())) {
            return "mergeOrder: " + a.getMergeOrder() + " != " + b.getMergeOrder();
        }
        // ── finalization (pre-safety winner, pass eligibility, corrections, final response) ──
        if (!Objects.equals(a.getFinalization(), b.getFinalization())) {
            return "finalization: " + a.getFinalization() + " != " + b.getFinalization();
        }
        // ── typed state events (records: deep equals via components, ordered) ──
        if (!a.getStateEvents().equals(b.getStateEvents())) {
            return "stateEvents: " + a.getStateEvents()
                + " != " + b.getStateEvents();
        }
        // ── ordered operations, raw float bits exact ──
        List<TraceOperation> opsA = a.getOperations();
        List<TraceOperation> opsB = b.getOperations();
        int common = Math.min(opsA.size(), opsB.size());
        for (int i = 0; i < common; i++) {
            String mismatch = opMismatch(i, opsA.get(i), opsB.get(i));
            if (mismatch != null) {
                return mismatch;
            }
        }
        if (opsA.size() != opsB.size()) {
            return "operation count: " + opsA.size() + " != " + opsB.size();
        }
        return null;
    }

    private static String opMismatch(int index, TraceOperation x, TraceOperation y) {
        if (x.getSeq() != y.getSeq()) return diff(index, "seq", x.getSeq(), y.getSeq());
        if (x.getOp() != y.getOp()) return diff(index, "op", x.getOp(), y.getOp());
        if (x.getCandidateOrdinal() != y.getCandidateOrdinal())
            return diff(index, "candidateOrdinal", x.getCandidateOrdinal(), y.getCandidateOrdinal());
        if (!Objects.equals(x.getSyntheticSource(), y.getSyntheticSource()))
            return diff(index, "syntheticSource", x.getSyntheticSource(), y.getSyntheticSource());
        if (!Objects.equals(x.getActionId(), y.getActionId()))
            return diff(index, "actionId", x.getActionId(), y.getActionId());
        if (!Objects.equals(x.getEvaluatorId(), y.getEvaluatorId()))
            return diff(index, "evaluatorId", x.getEvaluatorId(), y.getEvaluatorId());
        if (!Objects.equals(x.getRuleId(), y.getRuleId()))
            return diff(index, "ruleId", x.getRuleId(), y.getRuleId());
        if (!Objects.equals(x.getDomainId(), y.getDomainId()))
            return diff(index, "domainId", x.getDomainId(), y.getDomainId());
        if (!Objects.equals(x.getOutputKind(), y.getOutputKind()))
            return diff(index, "outputKind", x.getOutputKind(), y.getOutputKind());
        // raw float bits, exact
        if (!Objects.equals(x.getBeforeBits(), y.getBeforeBits()))
            return diff(index, "beforeBits", x.getBeforeBits(), y.getBeforeBits());
        if (!Objects.equals(x.getDeltaBits(), y.getDeltaBits()))
            return diff(index, "deltaBits", x.getDeltaBits(), y.getDeltaBits());
        if (!Objects.equals(x.getAfterBits(), y.getAfterBits()))
            return diff(index, "afterBits", x.getAfterBits(), y.getAfterBits());
        if (x.isVetoed() != y.isVetoed())
            return diff(index, "vetoed", x.isVetoed(), y.isVetoed());
        if (!Objects.equals(x.getVetoReason(), y.getVetoReason()))
            return diff(index, "vetoReason", x.getVetoReason(), y.getVetoReason());
        if (!Objects.equals(x.getDetail(), y.getDetail()))
            return diff(index, "detail", x.getDetail(), y.getDetail());
        return null;
    }

    private static String diff(int index, String field, Object a, Object b) {
        return "op[" + index + "]." + field + ": " + a + " != " + b;
    }

    public static void assertTracesEqual(DecisionTrace a, DecisionTrace b) {
        String mismatch = firstMismatch(a, b);
        if (mismatch != null) {
            fail("traces differ: " + mismatch);
        }
    }

    public static void assertTracesDiffer(DecisionTrace a, DecisionTrace b) {
        assertNotNull("expected traces to differ, but comparison found them identical",
            firstMismatch(a, b));
    }
}
