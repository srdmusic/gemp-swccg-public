package com.gempukku.swccgo.ai.models.common.trace;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TRACE HOOK (2026-07-13): mutable per-decision staging area. One instance per decision,
 * per thread (managed by TraceSession); never shared across threads.
 *
 * Operations are staged append-only with a monotonic seq. Candidate ordinals are resolved
 * at finish() time from the frozen first-seen index, so operations recorded during an
 * evaluator's evaluate() call (before the merge loop registers the action id) still get
 * the correct ordinal. Synthetic actions are marked by object identity, which lets a
 * synthetic pass share the "" action id with a real offered pass without stealing its
 * ordinal.
 */
final class TraceCollector {

    private static final class Staged {
        final int seq;
        final Object handle;
        final String actionId;
        final TraceOp op;
        final Integer beforeBits;
        final Integer deltaBits;
        final Integer afterBits;
        final boolean vetoed;
        final String vetoReason;
        final String evaluatorId;
        final String ruleId;
        final String domainId;
        final String outputKind;
        final String detail;

        Staged(int seq, Object handle, String actionId, TraceOp op,
               Integer beforeBits, Integer deltaBits, Integer afterBits,
               boolean vetoed, String vetoReason, String evaluatorId,
               String ruleId, String domainId, String outputKind, String detail) {
            this.seq = seq;
            this.handle = handle;
            this.actionId = actionId;
            this.op = op;
            this.beforeBits = beforeBits;
            this.deltaBits = deltaBits;
            this.afterBits = afterBits;
            this.vetoed = vetoed;
            this.vetoReason = vetoReason;
            this.evaluatorId = evaluatorId;
            this.ruleId = ruleId;
            this.domainId = domainId;
            this.outputKind = outputKind;
            this.detail = detail;
        }
    }

    private final String decisionId;
    private final String decisionType;
    private final String decisionText;

    private final List<Staged> staged = new ArrayList<>();
    private final Map<String, Integer> firstOrdinalByActionId = new LinkedHashMap<>();
    private final Map<Object, String> syntheticHandles = new IdentityHashMap<>();
    private String currentEvaluatorId;
    private int nextSeq;

    TraceCollector(String decisionId, String decisionType, String decisionText) {
        this.decisionId = decisionId;
        this.decisionType = decisionType;
        this.decisionText = decisionText;
    }

    void beginEvaluator(String evaluatorId) {
        this.currentEvaluatorId = evaluatorId;
    }

    void endEvaluator() {
        this.currentEvaluatorId = null;
    }

    /** Freeze the first-seen ordinal of an action id (merge-map insertion order). */
    void registerCandidate(String actionId) {
        if (actionId != null && !firstOrdinalByActionId.containsKey(actionId)) {
            firstOrdinalByActionId.put(actionId, firstOrdinalByActionId.size());
        }
    }

    /** Mark an action instance as synthetic (by identity), with an explicit source marker. */
    void markSynthetic(Object handle, String sourceMarker) {
        if (handle != null) {
            syntheticHandles.put(handle, sourceMarker != null ? sourceMarker : "SYNTHETIC");
        }
    }

    void record(Object handle, String actionId, TraceOp op,
                Integer beforeBits, Integer deltaBits, Integer afterBits,
                boolean vetoed, String vetoReason,
                String ruleId, String domainId, String outputKind, String detail) {
        staged.add(new Staged(nextSeq++, handle, actionId, op,
            beforeBits, deltaBits, afterBits, vetoed, vetoReason,
            currentEvaluatorId, ruleId, domainId, outputKind, detail));
    }

    /** Build the one complete immutable record, resolving candidate ordinals. */
    DecisionTrace finish() {
        List<TraceOperation> ops = new ArrayList<>(staged.size());
        for (Staged s : staged) {
            String syntheticSource = (s.handle != null) ? syntheticHandles.get(s.handle) : null;
            int ordinal;
            if (syntheticSource != null) {
                ordinal = TraceOperation.ORDINAL_SYNTHETIC;
            } else {
                Integer first = (s.actionId != null) ? firstOrdinalByActionId.get(s.actionId) : null;
                ordinal = (first != null) ? first.intValue() : TraceOperation.ORDINAL_UNKNOWN;
            }
            ops.add(new TraceOperation(s.seq, s.op, ordinal, syntheticSource,
                s.actionId, s.evaluatorId, s.ruleId, s.domainId, s.outputKind,
                s.beforeBits, s.deltaBits, s.afterBits, s.vetoed, s.vetoReason, s.detail));
        }
        return new DecisionTrace(decisionId, decisionType, decisionText,
            new ArrayList<>(firstOrdinalByActionId.keySet()), ops);
    }
}
