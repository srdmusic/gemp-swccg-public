package com.gempukku.swccgo.ai.models.common.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * TRACE HOOK (2026-07-13): trace-comparison assertion helper for the minimal decision
 * trace hook tests. Ordered events, raw float bits exact, complete veto reasons; never
 * decimal-tolerant. The full comparator tool is a later increment; this helper only has
 * to prove the Gate cases (reordering, one-bit drift, veto-reason change, SET-vs-ADD,
 * finalize change) all fail comparison.
 */
public final class TraceTestSupport {

    private TraceTestSupport() {
    }

    /** Capture sink: enabled, stores every finalized DecisionTrace it receives. */
    public static final class CaptureSink implements TraceSink {
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

    /** @return null when the traces compare equal, otherwise the FIRST mismatch found. */
    public static String firstMismatch(DecisionTrace a, DecisionTrace b) {
        if (!a.getCandidateOrder().equals(b.getCandidateOrder())) {
            return "candidateOrder: " + a.getCandidateOrder() + " != " + b.getCandidateOrder();
        }
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
