package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.decision.DecisionFacts;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md):
 * mutable per-decision staging area. One instance per decision, per thread (managed by
 * TraceSession); never shared across threads.
 *
 * Candidate ordinals bind to the COMPLETE RAW decision arrays supplied at open time
 * (the action-id-to-first-ordinal index is built once from the raw order, first
 * occurrence wins for duplicates). Evaluator merge insertion order is kept as a
 * SEPARATE list so reordering/collapsing against the raw order stays visible.
 * Synthetic actions are marked by object identity, which lets a synthetic pass share
 * the "" action id with a real offered pass without stealing its ordinal.
 *
 * Every swallowed capture error lands here as a typed failure; finish() then stamps the
 * envelope INCOMPLETE. Silent truncation is structurally impossible: the status/failure
 * consistency check lives in the DecisionTrace constructor itself.
 *
 * TRACE-V2 GATE P0-2/P0-3/P1-5 (Handoffs/CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md):
 * finish() now enforces the route-specific completeness matrix (pass/cancel facts on
 * every route, pre-safety winner + operations on the evaluator route, explicit
 * not-applicable markers on routes that legitimately skip a fact) and cross-validates
 * the selected runtime route against the snapshot's frozen decision shape. fallback()
 * is the typed failure envelope for a finish() that itself throws. Non-final ONLY so
 * the same-package lifecycle test can force that failure deterministically.
 */
class TraceCollector {

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
        final TraceRuleId ruleId;
        final TraceDomainId domainId;
        final TraceOutputKind outputKind;
        final String detail;

        Staged(int seq, Object handle, String actionId, TraceOp op,
               Integer beforeBits, Integer deltaBits, Integer afterBits,
               boolean vetoed, String vetoReason, String evaluatorId,
               TraceRuleId ruleId, TraceDomainId domainId, TraceOutputKind outputKind, String detail) {
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

    private static final class RouteObservation {
        final TraceRoute route;
        final String evidence;
        final String fallbackReason;

        RouteObservation(TraceRoute route, String evidence, String fallbackReason) {
            this.route = route;
            this.evidence = evidence;
            this.fallbackReason = fallbackReason;
        }
    }

    private final String botModel;
    private final String decisionId;
    private final String decisionType;
    private final String decisionText;
    private final DecisionSnapshot snapshot;
    private final boolean expectsFinalResponse;

    private final List<String> rawCandidateOrder = new ArrayList<>();
    private final Map<String, Integer> rawFirstOrdinalByActionId = new HashMap<>();
    private final List<String> mergeOrder = new ArrayList<>();
    private final Map<Object, String> syntheticHandles = new IdentityHashMap<>();
    private final List<Staged> staged = new ArrayList<>();
    private final List<RouteObservation> routeObservations = new ArrayList<>();
    private final List<TraceCaptureFailure> failures = new ArrayList<>();
    private final List<TraceIntendedStateEvent> stateEvents = new ArrayList<>();

    private String currentEvaluatorId;
    private int nextSeq;

    // finalization staging
    private String preSafetyWinnerActionId;
    private Integer preSafetyWinnerScoreBits;
    private boolean preSafetyWinnerVetoed;
    private String preSafetyWinnerVetoReason;
    private boolean preSafetyWinnerRecorded;              // P0-3: recorded-null != never-reached
    private String preSafetyWinnerNotApplicableReason;    // P0-3: explicit skip, never silent null
    private Boolean passEligible;
    private String passEligibilityFacts;
    private String passEligibilityNotApplicableReason;    // P0-3: explicit skip, never silent null
    private String multiSelectResponse;
    private String emergencyResponse;
    private String emergencyReason;
    private final List<TraceCorrection> corrections = new ArrayList<>();
    private String finalResponse;
    private boolean finalResponseRecorded;
    private boolean skippedCommonFinalizer;

    TraceCollector(String botModel, String decisionId, String decisionType, String decisionText,
                   List<String> rawCandidateIds, DecisionSnapshot snapshot,
                   List<String> snapshotIssues, boolean expectsFinalResponse) {
        this.botModel = botModel;
        this.decisionId = decisionId;
        this.decisionType = decisionType;
        this.decisionText = decisionText;
        this.snapshot = snapshot;
        this.expectsFinalResponse = expectsFinalResponse;
        if (rawCandidateIds != null) {
            for (String id : rawCandidateIds) {
                // verbatim raw order, duplicates preserved; index = FIRST ordinal per id
                if (id != null && !rawFirstOrdinalByActionId.containsKey(id)) {
                    rawFirstOrdinalByActionId.put(id, rawCandidateOrder.size());
                }
                rawCandidateOrder.add(id);
            }
        }
        if (snapshotIssues != null) {
            for (String issue : snapshotIssues) {
                failures.add(new TraceCaptureFailure(TraceCaptureFailure.Stage.SNAPSHOT,
                    "snapshot-construction", issue));
            }
        }
        if (snapshot == null && (snapshotIssues == null || snapshotIssues.isEmpty())) {
            failures.add(new TraceCaptureFailure(TraceCaptureFailure.Stage.SNAPSHOT,
                "snapshot-construction", "no snapshot supplied at open"));
        }
    }

    void beginEvaluator(String evaluatorId) {
        this.currentEvaluatorId = evaluatorId;
    }

    void endEvaluator() {
        this.currentEvaluatorId = null;
    }

    /** Record an action id's first-seen EVALUATOR-MERGE insertion (reorder detector only —
     *  ordinals always bind to the raw order supplied at open). */
    void registerCandidate(String actionId) {
        if (actionId != null && !mergeOrder.contains(actionId)) {
            mergeOrder.add(actionId);
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
                TraceRuleId ruleId, TraceDomainId domainId, TraceOutputKind outputKind, String detail) {
        // GATE P1-4: producer identity is mandatory. Outside an evaluator binding the
        // framework itself is performing the operation (merge/rank/select/synthetic
        // pass), so it carries the typed COMBINED_EVALUATOR producer — never null.
        String producer = (currentEvaluatorId != null)
            ? currentEvaluatorId : TraceOperation.PRODUCER_COMBINED_EVALUATOR;
        staged.add(new Staged(nextSeq++, handle, actionId, op,
            beforeBits, deltaBits, afterBits, vetoed, vetoReason,
            producer, ruleId, domainId, outputKind, detail));
    }

    /** Ordered route observation. The SELECTED route is the last lane observed (the one
     *  that actually produced the response); every earlier observation stays as evidence. */
    void recordRoute(TraceRoute route, String evidence, String fallbackReason) {
        routeObservations.add(new RouteObservation(route, evidence, fallbackReason));
    }

    void recordPassEligibility(boolean eligible, String facts) {
        this.passEligible = eligible;
        this.passEligibilityFacts = facts;
        // a recorded value supersedes any earlier not-applicable marker (P0-3)
        this.passEligibilityNotApplicableReason = null;
    }

    void recordPreSafetyWinner(String actionId, Integer scoreBits, boolean vetoed, String vetoReason) {
        this.preSafetyWinnerActionId = actionId;
        this.preSafetyWinnerScoreBits = scoreBits;
        this.preSafetyWinnerVetoed = vetoed;
        this.preSafetyWinnerVetoReason = vetoReason;
        this.preSafetyWinnerRecorded = true;
        // a recorded value supersedes any earlier not-applicable marker (P0-3)
        this.preSafetyWinnerNotApplicableReason = null;
    }

    /**
     * GATE P0-3: a route that legitimately never runs the evaluator lane (direct
     * interceptors, chaos, pure heuristic, raw-noPass emergency after a non-evaluator
     * lane) marks the lane's finalization facts EXPLICITLY not-applicable instead of
     * leaving them silently null. No-op per fact when a real value was already
     * recorded (e.g. heuristic fallback after the evaluator lane ran and declined).
     */
    void markEvaluatorLaneNotApplicable(String reason) {
        String marked = (reason != null && !reason.isBlank())
            ? reason : "evaluator lane not applicable on this route";
        if (passEligible == null) {
            this.passEligibilityNotApplicableReason = marked;
        }
        if (!preSafetyWinnerRecorded) {
            this.preSafetyWinnerNotApplicableReason = marked;
        }
    }

    void recordMultiSelectResponse(String response) {
        this.multiSelectResponse = response;
    }

    void recordEmergencyResponse(String response, String reason) {
        this.emergencyResponse = response;
        this.emergencyReason = reason;
    }

    void recordCorrection(TraceCorrection.Kind kind, String before, String after, String detail) {
        corrections.add(new TraceCorrection(kind, before, after, detail));
    }

    void recordFinalResponse(String response, boolean skippedCommonFinalizer) {
        this.finalResponse = response;
        this.finalResponseRecorded = true;
        this.skippedCommonFinalizer = skippedCommonFinalizer;
    }

    void recordIntendedStateEvent(TraceIntendedStateEvent.Kind kind, String detail) {
        stateEvents.add(new TraceIntendedStateEvent(kind, detail));
    }

    /** Typed capture failure: the emitted record will be INCOMPLETE, never plausibly truncated. */
    void markFailure(TraceCaptureFailure.Stage stage, String errorClass, String detail) {
        failures.add(new TraceCaptureFailure(stage, errorClass, detail));
    }

    /** Build the one complete immutable envelope, resolving raw candidate ordinals. */
    DecisionTrace finish() {
        List<TraceOperation> ops = new ArrayList<>(staged.size());
        for (Staged s : staged) {
            String syntheticSource = (s.handle != null) ? syntheticHandles.get(s.handle) : null;
            int ordinal;
            if (syntheticSource != null) {
                ordinal = TraceOperation.ORDINAL_SYNTHETIC;
            } else {
                Integer first = (s.actionId != null) ? rawFirstOrdinalByActionId.get(s.actionId) : null;
                ordinal = (first != null) ? first.intValue() : TraceOperation.ORDINAL_UNKNOWN;
            }
            ops.add(new TraceOperation(s.seq, s.op, ordinal, syntheticSource,
                s.actionId, s.evaluatorId, s.ruleId, s.domainId, s.outputKind,
                s.beforeBits, s.deltaBits, s.afterBits, s.vetoed, s.vetoReason, s.detail));
        }

        TraceRouteRecord routeRecord = null;
        if (!routeObservations.isEmpty()) {
            List<String> evidence = new ArrayList<>(routeObservations.size());
            for (RouteObservation obs : routeObservations) {
                evidence.add(obs.route + ": " + (obs.evidence != null ? obs.evidence : "(no evidence)"));
            }
            RouteObservation selected = routeObservations.get(routeObservations.size() - 1);
            String fallbackReason = selected.fallbackReason;
            if (fallbackReason == null && routeObservations.size() > 1) {
                fallbackReason = "fell through from "
                    + routeObservations.get(routeObservations.size() - 2).route;
            }
            routeRecord = new TraceRouteRecord(selected.route, evidence, fallbackReason);
        } else {
            markFailure(TraceCaptureFailure.Stage.ROUTE, "route-record",
                "no route observation recorded for this decision");
        }

        if (expectsFinalResponse && !finalResponseRecorded) {
            markFailure(TraceCaptureFailure.Stage.FINALIZATION, "final-response",
                "bot-boundary session closed without a recorded final response");
        }

        // ── GATE P0-3: route-specific completeness matrix. COMPLETE requires every
        //    fact the selected route produces — recorded or EXPLICITLY not-applicable.
        //    (Final-response completeness stays owned by the expectsFinalResponse check
        //    above: only the bot decide() boundary reaches that fact at all.) ──
        if (routeRecord != null) {
            TraceRoute selectedRoute = routeRecord.selected();
            if (passEligible == null && passEligibilityNotApplicableReason == null) {
                markFailure(TraceCaptureFailure.Stage.FINALIZATION, "route-completeness",
                    "route " + selectedRoute + " closed without pass/cancel eligibility facts"
                        + " (record a value or an explicit not-applicable)");
            }
            if (selectedRoute == TraceRoute.COMBINED_EVALUATOR) {
                // the evaluator route PRODUCES a pre-safety winner and operations —
                // not-applicable is not an option here (gate: "evaluator routes do not
                // legitimately lack ops").
                if (!preSafetyWinnerRecorded) {
                    markFailure(TraceCaptureFailure.Stage.FINALIZATION, "route-completeness",
                        "COMBINED_EVALUATOR route closed without a recorded pre-safety winner");
                }
                if (staged.isEmpty()) {
                    markFailure(TraceCaptureFailure.Stage.OPERATION, "route-completeness",
                        "COMBINED_EVALUATOR route closed with zero recorded operations");
                }
            } else if (!preSafetyWinnerRecorded && preSafetyWinnerNotApplicableReason == null) {
                markFailure(TraceCaptureFailure.Stage.FINALIZATION, "route-completeness",
                    "route " + selectedRoute + " closed without a pre-safety winner"
                        + " (record a value or an explicit not-applicable)");
            }

            // ── GATE P1-5: frozen-fact cross-validation. The runtime-selected route must
            //    be compatible with the snapshot's frozen decision SHAPE (wire shape only —
            //    phase is a window, never a route key; see TraceRoute.isCompatibleWithFrozenShape).
            //    Disagreement is preserved as a typed ROUTE failure, not thrown away. ──
            if (snapshot != null) {
                DecisionFacts.DecisionRoute frozenShape = snapshot.decisionFacts().selectedRoute();
                if (!selectedRoute.isCompatibleWithFrozenShape(frozenShape)) {
                    markFailure(TraceCaptureFailure.Stage.ROUTE, "route-evidence-mismatch",
                        "selected runtime route " + selectedRoute
                            + " is incompatible with the frozen decision shape " + frozenShape);
                }
            }
        }

        TraceFinalization finalization = new TraceFinalization(
            preSafetyWinnerActionId, preSafetyWinnerScoreBits, preSafetyWinnerVetoed,
            preSafetyWinnerVetoReason, preSafetyWinnerRecorded, preSafetyWinnerNotApplicableReason,
            passEligible, passEligibilityFacts, passEligibilityNotApplicableReason,
            multiSelectResponse, emergencyResponse, emergencyReason,
            corrections, finalResponse, finalResponseRecorded, skippedCommonFinalizer);

        TraceStatus status = failures.isEmpty() ? TraceStatus.COMPLETE : TraceStatus.INCOMPLETE;

        return new DecisionTrace(DecisionTrace.SCHEMA_VERSION, botModel,
            decisionId, decisionType, decisionText,
            snapshot, status, failures, routeRecord,
            rawCandidateOrder, mergeOrder, ops, finalization, stateEvents);
    }

    /**
     * TRACE-V2 GATE P0-2 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md "record-construction
     * and sink failures still disappear"): the typed failure envelope for a finish() that
     * threw. Built from primitives and defensive copies only, so it survives whatever
     * broke the full construction; carries every already-collected typed failure plus a
     * CLOSE-stage failure naming the error class. TraceSession.close() returns this
     * instead of null, so construction failure reaches the sink as inspectable evidence.
     */
    DecisionTrace fallback(Throwable cause) {
        List<TraceCaptureFailure> fallbackFailures = new ArrayList<>();
        try {
            fallbackFailures.addAll(failures);
        } catch (Throwable ignored) {
            // the collected list itself is broken; the CLOSE failure below still lands
        }
        fallbackFailures.add(new TraceCaptureFailure(TraceCaptureFailure.Stage.CLOSE,
            cause.getClass().getName(),
            "record construction failed in finish(): " + cause.getMessage()));
        List<String> rawOrder;
        try {
            rawOrder = new ArrayList<>(rawCandidateOrder);
        } catch (Throwable ignored) {
            rawOrder = new ArrayList<>();
        }
        List<String> mergeOrderCopy;
        try {
            mergeOrderCopy = new ArrayList<>(mergeOrder);
        } catch (Throwable ignored) {
            mergeOrderCopy = new ArrayList<>();
        }
        TraceFinalization emptyFinalization = new TraceFinalization(
            null, null, false, null, false, null,
            null, null, null,
            null, null, null, List.of(), null, false, false);
        return new DecisionTrace(DecisionTrace.SCHEMA_VERSION, botModel,
            decisionId, decisionType, decisionText,
            null, TraceStatus.INCOMPLETE, fallbackFailures, null,
            rawOrder, mergeOrderCopy, List.of(), emptyFinalization, List.of());
    }
}
