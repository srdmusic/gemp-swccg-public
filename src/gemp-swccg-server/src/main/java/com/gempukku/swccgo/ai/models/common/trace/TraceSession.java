package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;

import java.util.List;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md):
 * thread-local access point for the per-decision trace session.
 *
 * OWNERSHIP: the bot entry point (RandoCalAi / TheChosenOneAi) opens the session at the
 * decide() boundary when its sink is enabled; the pure CombinedEvaluator test seam opens
 * one only when no session is already active. open() REFUSES a nested open (returns
 * false, outer session untouched) — only the opener closes and emits. All record*
 * statics are unconditional call sites guarded by a plain ThreadLocal null check: with
 * no session open (the production default, no-op sink) every call short-circuits —
 * zero behavior, zero allocation beyond that cheap guard.
 *
 * ERROR LAW: instrumentation must never throw into the decision path, and it must never
 * silently claim completion. Every swallowed Throwable after the null check marks the
 * current collector with a typed capture failure, so the emitted envelope is INCOMPLETE
 * rather than plausibly truncated.
 */
public final class TraceSession {

    private static final ThreadLocal<TraceCollector> CURRENT = new ThreadLocal<>();

    private TraceSession() {
        // static access only
    }

    /**
     * Open a session for one decision on this thread. Returns false on any failure AND
     * on a nested open (an active session is never replaced or corrupted; the would-be
     * opener simply does not own it).
     *
     * @param rawCandidateIds COMPLETE raw decision candidate ids, verbatim order
     * @param snapshot shadow DecisionSnapshot (null with snapshotIssues explaining why)
     * @param expectsFinalResponse true at the bot decide() boundary — closing without a
     *        recorded final response then marks the trace INCOMPLETE
     */
    public static boolean open(String botModel, String decisionId, String decisionType,
                               String decisionText, List<String> rawCandidateIds,
                               DecisionSnapshot snapshot, List<String> snapshotIssues,
                               boolean expectsFinalResponse) {
        try {
            if (CURRENT.get() != null) {
                return false;  // nested open refused; outer session stays intact
            }
            CURRENT.set(new TraceCollector(botModel, decisionId, decisionType, decisionText,
                rawCandidateIds, snapshot, snapshotIssues, expectsFinalResponse));
            return true;
        } catch (Throwable t) {
            // GATE P0-2 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): open() no longer
            // fails silently. A degraded evidence-only collector (no snapshot, no raw
            // ids — the failed inputs themselves may be what threw) is installed with a
            // typed OPEN failure, so the emitted envelope is INCOMPLETE with evidence
            // instead of the whole decision's trace disappearing.
            try {
                TraceCollector degraded = new TraceCollector(botModel, decisionId,
                    decisionType, decisionText, null, null,
                    List.of("session open failed before staging completed: "
                        + t.getClass().getName() + ": " + t.getMessage()),
                    expectsFinalResponse);
                degraded.markFailure(TraceCaptureFailure.Stage.OPEN, t.getClass().getName(),
                    "trace session construction failed; degraded evidence-only session installed");
                CURRENT.set(degraded);
                return true;
            } catch (Throwable second) {
                abandon();
                return false;  // structurally impossible to preserve evidence
            }
        }
    }

    /** TEST SEAM (package-private, GATE P0-2 lifecycle proof): install a prepared
     *  collector when none is active, so the finish()-failure fallback path can be
     *  driven deterministically from the same-package lifecycle test. Never used in
     *  production code. */
    static boolean openForTesting(TraceCollector collector) {
        if (collector == null || CURRENT.get() != null) {
            return false;
        }
        CURRENT.set(collector);
        return true;
    }

    /** Close the session and build the one complete envelope. Null when nothing was open.
     *  The thread-local is ALWAYS cleared, even when record construction fails.
     *  GATE P0-2: a finish() failure no longer returns null — the collector's typed
     *  fallback envelope (INCOMPLETE, CLOSE-stage failure) survives where structurally
     *  possible, so record-construction failure stays inspectable evidence. */
    public static DecisionTrace close() {
        TraceCollector collector = CURRENT.get();
        try {
            return (collector != null) ? collector.finish() : null;
        } catch (Throwable t) {
            try {
                return collector.fallback(t);
            } catch (Throwable second) {
                return null;  // even the primitive fallback failed; nothing sensible left
            }
        } finally {
            CURRENT.remove();
        }
    }

    /**
     * GATE P0-2: the one emission channel — close the session and deliver the envelope
     * to the sink, with every failure typed. A finish() failure emits the fallback
     * envelope (via close()); a sink accept() failure re-offers the SAME trace once,
     * derived INCOMPLETE with a typed SINK failure appended, so sink failures are
     * inspectable through the sink itself. Never throws into the decision path.
     */
    public static void closeAndEmit(TraceSink sink) {
        DecisionTrace trace = close();
        if (trace == null || sink == null) {
            return;
        }
        try {
            sink.accept(trace);
        } catch (Throwable sinkT) {
            try {
                sink.accept(trace.withAdditionalFailure(new TraceCaptureFailure(
                    TraceCaptureFailure.Stage.SINK, sinkT.getClass().getName(),
                    "sink accept failed for the finalized trace; re-offered once with this typed SINK failure: "
                        + sinkT.getMessage())));
            } catch (Throwable second) {
                // the sink refused the typed failure too; no further channel exists and
                // gameplay must never be harmed by capture
            }
        }
    }

    /** Drop any open session without producing a record. */
    public static void abandon() {
        try {
            CURRENT.remove();
        } catch (Throwable ignored) {
            // nothing sensible left to do
        }
    }

    /** True when a trace session is open on this thread. */
    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    /** Typed capture failure on the current session (no-op when none is open). */
    public static void markCaptureFailure(TraceCaptureFailure.Stage stage,
                                          String errorClass, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.markFailure(stage, errorClass, detail);
        } catch (Throwable ignored) {
            // the failure list itself failed; nothing sensible left to do
        }
    }

    private static void failQuietly(TraceCollector c, TraceCaptureFailure.Stage stage, Throwable t) {
        try {
            c.markFailure(stage, t.getClass().getName(), String.valueOf(t.getMessage()));
        } catch (Throwable ignored) {
            // best effort only
        }
    }

    /** Bind an evaluator id to every operation recorded until endEvaluator(). */
    public static void beginEvaluator(String evaluatorId) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.beginEvaluator(evaluatorId);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    public static void endEvaluator() {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.endEvaluator();
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    /** Record an id's first-seen evaluator-merge insertion (reorder detector; ordinals
     *  always bind to the raw candidate order supplied at open). */
    public static void registerCandidate(String actionId) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.registerCandidate(actionId);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    /** Mark an action instance synthetic (explicit ordinal + source marker in the record). */
    public static void markSynthetic(Object handle, String sourceMarker) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.markSynthetic(handle, sourceMarker);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    // ── GATE P1-4 sentinel filling: operation identity is mandatory on every
    //    dimension. A null from an unmigrated legacy arm becomes the explicit
    //    LEGACY_UNTAGGED sentinel at this choke point (visible debt, never null
    //    inference); framework merge/rank/select ops carry the COMBINED_EVALUATOR
    //    sentinels. TraceOperation's constructor rejects any null that slips past. ──

    private static TraceRuleId legacyOr(TraceRuleId ruleId) {
        return ruleId != null ? ruleId : TraceRuleId.LEGACY_UNTAGGED;
    }

    private static TraceDomainId legacyOr(TraceDomainId domainId) {
        return domainId != null ? domainId : TraceDomainId.LEGACY_UNTAGGED;
    }

    private static TraceOutputKind legacyOr(TraceOutputKind outputKind) {
        return outputKind != null ? outputKind : TraceOutputKind.LEGACY_UNTAGGED;
    }

    /** Constructor score. */
    public static void recordInitial(Object handle, String actionId, float score,
                                     TraceRuleId ruleId, TraceDomainId domainId,
                                     TraceOutputKind outputKind, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.INITIAL,
                null, null, Float.floatToRawIntBits(score),
                false, null, legacyOr(ruleId), legacyOr(domainId), legacyOr(outputKind), detail);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    /** Additive score change: before, delta, and after, all as raw float bits. */
    public static void recordAdd(Object handle, String actionId, float before, float delta, float after,
                                 TraceRuleId ruleId, TraceDomainId domainId,
                                 TraceOutputKind outputKind, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.ADD,
                Float.floatToRawIntBits(before), Float.floatToRawIntBits(delta), Float.floatToRawIntBits(after),
                false, null, legacyOr(ruleId), legacyOr(domainId), legacyOr(outputKind), detail);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    /** Overwrite: before and after only. A SET never fakes an additive delta. */
    public static void recordSet(Object handle, String actionId, float before, float after,
                                 TraceRuleId ruleId, TraceDomainId domainId,
                                 TraceOutputKind outputKind, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.SET,
                Float.floatToRawIntBits(before), null, Float.floatToRawIntBits(after),
                false, null, legacyOr(ruleId), legacyOr(domainId), legacyOr(outputKind), detail);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    /**
     * Hard veto. effectiveVetoReason is the action's resulting veto reason (first reason
     * wins on repeat vetoes); requestedReason is this call's argument, kept as detail.
     */
    public static void recordHardVeto(Object handle, String actionId,
                                      String effectiveVetoReason, String requestedReason,
                                      TraceRuleId ruleId, TraceDomainId domainId,
                                      TraceOutputKind outputKind) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.HARD_VETO,
                null, null, null,
                true, effectiveVetoReason,
                legacyOr(ruleId), legacyOr(domainId), legacyOr(outputKind), requestedReason);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    /** Merge boundary only: pre/post score bits and resulting veto state, no synthetic delta. */
    public static void recordMerge(Object handle, String actionId, float before, float after,
                                   boolean vetoed, String vetoReason, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.MERGE,
                Float.floatToRawIntBits(before), null, Float.floatToRawIntBits(after),
                vetoed, vetoReason, TraceRuleId.COMBINED_EVALUATOR,
                TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, detail);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    /** Ranking step (bucket best, pre-final best). Score is nullable for empty ranks. */
    public static void recordRank(Object handle, String actionId, Float score, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.RANK,
                null, null, (score != null) ? Float.floatToRawIntBits(score.floatValue()) : null,
                false, null, TraceRuleId.COMBINED_EVALUATOR,
                TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, detail);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    /** Selection of a winner (bucket winner, epilogue winner, pass, final best). */
    public static void recordSelect(Object handle, String actionId, Float score,
                                    boolean vetoed, String vetoReason, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.record(handle, actionId, TraceOp.SELECT,
                null, null, (score != null) ? Float.floatToRawIntBits(score.floatValue()) : null,
                vetoed, vetoReason, TraceRuleId.COMBINED_EVALUATOR,
                TraceDomainId.COMBINED_EVALUATOR, TraceOutputKind.COMBINED_EVALUATOR, detail);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.OPERATION, t);
        }
    }

    /** Ordered typed route observation (bot entry point / seam owner only). */
    public static void recordRoute(TraceRoute route, String evidence, String fallbackReason) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordRoute(route, evidence, fallbackReason);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.ROUTE, t);
        }
    }

    /**
     * GATE P0-3 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md "COMPLETE is not
     * route-complete"): routes that legitimately never run the evaluator lane (direct
     * interceptors, chaos fallback, pure heuristic, raw-noPass emergency) mark the
     * lane's finalization facts EXPLICITLY not-applicable — the route-completeness
     * matrix in finish() refuses COMPLETE for silently-null facts. Per-fact no-op when
     * a real value was already recorded.
     */
    public static void recordEvaluatorLaneNotApplicable(String reason) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.markEvaluatorLaneNotApplicable(reason);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.FINALIZATION, t);
        }
    }

    /** Semantic pass/cancel eligibility with the exact facts used (V148 semantics). */
    public static void recordPassEligibility(boolean eligible, String facts) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordPassEligibility(eligible, facts);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.FINALIZATION, t);
        }
    }

    /** CombinedEvaluator's selected action — explicitly NOT the AI's final answer. */
    public static void recordPreSafetyWinner(String actionId, Float score,
                                             boolean vetoed, String vetoReason) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordPreSafetyWinner(actionId,
                (score != null) ? Float.floatToRawIntBits(score.floatValue()) : null,
                vetoed, vetoReason);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.FINALIZATION, t);
        }
    }

    /** Multi-select formatting result (comma-joined card ids). */
    public static void recordMultiSelectResponse(String response) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordMultiSelectResponse(response);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.FINALIZATION, t);
        }
    }

    /** Raw-noPass emergency action and reason. */
    public static void recordEmergencyResponse(String response, String reason) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordEmergencyResponse(response, reason);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.FINALIZATION, t);
        }
    }

    /** One DecisionSafety correction with typed reason and before/after response. */
    public static void recordCorrection(TraceCorrection.Kind kind, String before, String after,
                                        String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordCorrection(kind, before, after, detail);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.FINALIZATION, t);
        }
    }

    /** Final response returned by the bot after safety (the AI's actual answer). */
    public static void recordFinalResponse(String response, boolean skippedCommonFinalizer) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordFinalResponse(response, skippedCommonFinalizer);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.FINALIZATION, t);
        }
    }

    /** Typed AI-side mutation event OBSERVED at an existing legacy choke point. */
    public static void recordIntendedStateEvent(TraceIntendedStateEvent.Kind kind, String detail) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordIntendedStateEvent(kind, detail);
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }
}
