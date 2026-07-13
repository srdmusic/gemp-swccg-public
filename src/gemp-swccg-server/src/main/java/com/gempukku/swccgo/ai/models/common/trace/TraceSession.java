package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerLifecycleSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerPhaseSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.EngineCallOutcome;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicActionChoiceRememberEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicFailedSearchAddEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicMemorySnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicReassignmentRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicRecentResponseAppendEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicSingleResponseRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.HeuristicStateUpdateEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.EnginePlayerLostEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.PendingConcedeEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.PendingDeployEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerBlockResponseEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerClearEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerOwner;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerPhaseChangeEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerUpdateStateEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyBattleOrderRefreshEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyBattleResultRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyControllerOwner;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyControllerSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyFocusDeployRecordEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyResetEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategySideSetEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyStartTurnEvent;
import com.gempukku.swccgo.common.GameEndReason;
import com.gempukku.swccgo.common.Side;

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

    // ── TRACE STAGE 4A1 (Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md
    //    "First Java slice after review"): one typed recording method per state-event
    //    family. Each checks CURRENT first, then constructs and appends the record
    //    inside the try/catch; no event is ever constructed without an open session,
    //    and construction/append failure marks the envelope INCOMPLETE/STATE_EVENT.
    //    Hooks pass only already-computed values read after the legacy write or call. ──

    /** Outer decision-tracker RECORD_RESPONSE (m00372 Option A, accepted m00373):
     *  the complete decision-affecting owner snapshots before and after the legacy
     *  recordDecision(...) call, captured AFTER the call per the matrix correction.
     *  The hook must build snapshots ONLY under its session/enabled guard: disabled
     *  capture calls neither pure accessor. */
    public static void recordTrackerRecordResponse(TrackerOwner owner, String decisionType,
                                                   String decisionId, String decisionKey,
                                                   String response,
                                                   DecisionTrackerSnapshot before,
                                                   DecisionTrackerSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(TrackerRecordResponseEvent.of(owner, decisionType, decisionId,
                decisionKey, response, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** TRACE STAGE 4A2a (Handoffs/CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md
     *  "Recording API"): outer decision-tracker UPDATE_STATE lifecycle call, observed
     *  AFTER the legacy updateState(...) ran, with the EXACT legacy call arguments and
     *  the complete lifecycle before/after snapshots from the tracker's pure
     *  traceLifecycleSnapshot() seam. Exactly one event per legacy call; the hook
     *  builds snapshots ONLY under its active-session guard. */
    public static void recordTrackerUpdateState(TrackerOwner owner, int handSize, int forcePile,
                                                int reserveDeck, int turn, int cardsInPlay,
                                                DecisionTrackerLifecycleSnapshot before,
                                                DecisionTrackerLifecycleSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(TrackerUpdateStateEvent.of(owner, handSize, forcePile,
                reserveDeck, turn, cardsInPlay, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** TRACE STAGE 4A2a: outer decision-tracker new-game CLEAR lifecycle call, observed
     *  AFTER the legacy clear() ran at its unchanged source position; ClearCause is
     *  closed to NEW_GAME_RESET. Exactly one event per legacy call. */
    public static void recordTrackerClear(TrackerOwner owner, TrackerClearEvent.ClearCause cause,
                                          DecisionTrackerLifecycleSnapshot before,
                                          DecisionTrackerLifecycleSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(TrackerClearEvent.of(owner, cause, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** TRACE STAGE 4A2b (Handoffs/CODEX_TRACE_STAGE4_4A2B_SHARED_TRACKER_PREFLIGHT_2026-07-13.md
     *  "Authorized implementation shape"): the inherited shared-tracker
     *  onPhaseChange(...) call at the HeuristicAiBase.decide(...) boundary, observed
     *  AFTER the legacy call ran, with the exact phase argument and the complete
     *  phase-owner before/after snapshots (decision state + exact lastPhase) from the
     *  public read-only DecisionTrackerTraceAccess bridge. Exactly one event per direct
     *  legacy call; no event when phase == null (the call did not run). */
    public static void recordTrackerPhaseChange(TrackerOwner owner, String phase,
                                                DecisionTrackerPhaseSnapshot before,
                                                DecisionTrackerPhaseSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(TrackerPhaseChangeEvent.of(owner, phase, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** TRACE STAGE 4A2b: the one DIRECT inherited shared-tracker
     *  blockLastActionOnCancel(...) call at the HeuristicAiBase.decide(...) empty
     *  CARD_SELECTION/ARBITRARY_CARDS boundary, observed AFTER the legacy call ran,
     *  with the exact call subject, the EXACT legacy boolean return, and the complete
     *  decision-affecting before/after snapshots. The INTERNAL cancel-block call inside
     *  DecisionTracker.recordDecision(...) stays FOLDED into that call's single
     *  RECORD_RESPONSE event and never reaches this method. */
    public static void recordTrackerBlockResponse(TrackerOwner owner, String decisionType,
                                                  String decisionText, boolean blocked,
                                                  DecisionTrackerSnapshot before,
                                                  DecisionTrackerSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(TrackerBlockResponseEvent.of(owner, decisionType, decisionText,
                blocked, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Pending-concede SET_PENDING/CLEAR_PENDING, observed after the legacy field
     *  writes; the outcome is derived from the exact before/after pair. */
    public static void recordPendingConcede(PendingConcedeEvent.Operation operation,
                                            PendingConcedeEvent.Cause cause, String playerId,
                                            Integer myLostPileSize, Integer opponentLostPileSize,
                                            Integer lostPileDeficit,
                                            boolean pendingBefore, String reasonBefore,
                                            boolean pendingAfter, String reasonAfter) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(PendingConcedeEvent.of(operation, cause, playerId,
                myLostPileSize, opponentLostPileSize, lostPileDeficit,
                pendingBefore, reasonBefore, pendingAfter, reasonAfter));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Engine playerLost(...) call attempt, recorded around the actual legacy call with
     *  the DISTINCT EngineCallOutcome (SUCCESS on normal return, THREW on a caught
     *  Exception); ordered before its CLEAR_PENDING exactly as source runs. */
    public static void recordEnginePlayerLost(String playerId, GameEndReason reason,
                                              EngineCallOutcome outcome) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(new EnginePlayerLostEvent(playerId, reason, outcome));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Pending-deploy SET/CLEAR at the lastPendingDeployType direct-write sites, with
     *  the exact legacy value before/after; the outcome is derived from the pair. */
    public static void recordPendingDeploy(PendingDeployEvent.Operation operation,
                                           String typeBefore, String typeAfter) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(PendingDeployEvent.of(operation, typeBefore, typeAfter));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    // ── TRACE STAGE 4B1 (Handoffs/CODEX_TRACE_STAGE4_4B1_HEURISTIC_MEMORY_PREFLIGHT_2026-07-13.md
    //    "Source-Complete Owner Table"): one typed recording method per heuristic-memory
    //    family, following the 4A2b error law. With no active session every call
    //    immediately returns. Each hook passes only already-computed values plus the
    //    before/after snapshots captured under its own active-session guard; event
    //    construction or append failure marks the envelope INCOMPLETE/STATE_EVENT and
    //    never alters or throws into the legacy decision path. ──

    /** Heuristic STATE_UPDATE at updateDecisionTrackerState(...) exit: the exact five
     *  state-read values plus complete before/after snapshots; pruned rows are derived
     *  inside the event factory from the two snapshots. Recorded AFTER the nested
     *  4A2b-owned shared UPDATE_STATE event, which stays separate. */
    public static void recordHeuristicStateUpdate(int handSize, int forcePile, int reserveDeck,
                                                  int turn, int cardsInPlay,
                                                  HeuristicMemorySnapshot before,
                                                  HeuristicMemorySnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(HeuristicStateUpdateEvent.of(handSize, forcePile, reserveDeck,
                turn, cardsInPlay, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Heuristic ACTION_CHOICE_REMEMBER at the updateLastActionChoiceText(...) in-range
     *  write branch: exact decision type, result, and parsed index; the prior/new tuple
     *  rides the complete before/after snapshots. */
    public static void recordHeuristicActionChoiceRemember(String decisionType, String result,
                                                           int index,
                                                           HeuristicMemorySnapshot before,
                                                           HeuristicMemorySnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(HeuristicActionChoiceRememberEvent.of(decisionType, result, index,
                before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Heuristic FAILED_SEARCH_ADD after the handleFailedSearchVerification(...) adds:
     *  the exact prior action tuple; the sorted per-set deltas are derived inside the
     *  event factory from the two snapshots. Never called when all three identities are
     *  empty (no owned write executed) or when the verification match failed. */
    public static void recordHeuristicFailedSearchAdd(String priorActionText, String priorCardId,
                                                      String priorBlueprintId,
                                                      HeuristicMemorySnapshot before,
                                                      HeuristicMemorySnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(HeuristicFailedSearchAddEvent.of(priorActionText, priorCardId,
                priorBlueprintId, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Heuristic SINGLE_RESPONSE_RECORD on either updateSingleDecisionLoop(...) exit:
     *  the exact decision, raw response, and tracking response (null passed verbatim);
     *  the internally created local-response block stays FOLDED into this one event via
     *  the snapshots' localBlockedResponses delta. */
    public static void recordHeuristicSingleResponseRecord(String decisionType,
                                                           String decisionText,
                                                           String rawResponse,
                                                           String trackingResponse,
                                                           HeuristicMemorySnapshot before,
                                                           HeuristicMemorySnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(HeuristicSingleResponseRecordEvent.of(decisionType, decisionText,
                rawResponse, trackingResponse, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Heuristic RECENT_RESPONSE_APPEND after the recordRecentDecisionResponse(...)
     *  deque append: the legacy helper's own decision key and appended response; the
     *  ordered deque before/after and the evicted FIFO rows are derived inside the
     *  event factory from the two snapshots. */
    public static void recordHeuristicRecentResponseAppend(String decisionKey,
                                                           String appendedResponse,
                                                           HeuristicMemorySnapshot before,
                                                           HeuristicMemorySnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(HeuristicRecentResponseAppendEvent.of(decisionKey,
                appendedResponse, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Heuristic REASSIGNMENT_RECORD after the recordRecentReassignment(...) writes:
     *  the legacy helper's own map key (closed variant derived from its prefix) and the
     *  recorded turn; both map deltas are derived inside the event factory from the two
     *  snapshots, with the count increment FOLDED in. */
    public static void recordHeuristicReassignmentRecord(String key, int turn,
                                                         HeuristicMemorySnapshot before,
                                                         HeuristicMemorySnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(HeuristicReassignmentRecordEvent.of(key, turn, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    // ── TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
    //    "Source-Complete Owner Table"): one typed recording method per StrategyController
    //    family, following the same STATE_EVENT error law. With no active session every
    //    call immediately returns. Each hook passes only already-computed values plus the
    //    before/after snapshots captured under its own active-session guard; event
    //    construction or append failure marks the envelope INCOMPLETE/STATE_EVENT and
    //    never alters or throws into the legacy decision path. The controller mutators
    //    themselves stay byte-for-byte unchanged; hooks land at the outer call sites. ──

    /** Strategy SIDE_SET at the new-game setSide(...) call: the exact nullable side
     *  argument plus the complete before/after controller snapshots. */
    public static void recordStrategySideSet(StrategyControllerOwner owner, Side side,
                                             StrategyControllerSnapshot before,
                                             StrategyControllerSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(StrategySideSetEvent.of(owner, side, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Strategy RESET at the new-game reset() call immediately after setSide: no call
     *  argument, complete before/after controller snapshots. */
    public static void recordStrategyReset(StrategyControllerOwner owner,
                                           StrategyControllerSnapshot before,
                                           StrategyControllerSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(StrategyResetEvent.of(owner, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Strategy START_TURN at the turn-changed startNewTurn(...) call: the exact int turn
     *  argument plus the complete before/after controller snapshots. */
    public static void recordStrategyStartTurn(StrategyControllerOwner owner, int turnNumber,
                                               StrategyControllerSnapshot before,
                                               StrategyControllerSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(StrategyStartTurnEvent.of(owner, turnNumber, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Strategy FOCUS_DEPLOY_RECORD at the optional onSuccessfulDeploy(...) call, recorded
     *  before the outer pending-deploy CLEAR: the exact non-null card-type argument plus
     *  the complete before/after controller snapshots. */
    public static void recordStrategyFocusDeployRecord(StrategyControllerOwner owner, String cardType,
                                                       StrategyControllerSnapshot before,
                                                       StrategyControllerSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(StrategyFocusDeployRecordEvent.of(owner, cardType, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Strategy BATTLE_ORDER_REFRESH at the once-per-decision
     *  updateBattleOrderFromGameState(...) call: no GameState or service reference, only
     *  the complete before/after controller snapshots (the internal
     *  setUnderBattleOrderRules write stays folded in). */
    public static void recordStrategyBattleOrderRefresh(StrategyControllerOwner owner,
                                                        StrategyControllerSnapshot before,
                                                        StrategyControllerSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(StrategyBattleOrderRefreshEvent.of(owner, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }

    /** Strategy BATTLE_RESULT_RECORD at either the win or the loss onBattleResult(...)
     *  lexical hook (two hooks, one operation kind): the exact boolean won argument plus
     *  the complete before/after controller snapshots. */
    public static void recordStrategyBattleResultRecord(StrategyControllerOwner owner, boolean won,
                                                        StrategyControllerSnapshot before,
                                                        StrategyControllerSnapshot after) {
        TraceCollector c = CURRENT.get();
        if (c == null) return;
        try {
            c.recordStateEvent(StrategyBattleResultRecordEvent.of(owner, won, before, after));
        } catch (Throwable t) {
            failQuietly(c, TraceCaptureFailure.Stage.STATE_EVENT, t);
        }
    }
}
