package com.gempukku.swccgo.ai.models.common.trace;

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
    private Boolean passEligible;
    private String passEligibilityFacts;
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
        staged.add(new Staged(nextSeq++, handle, actionId, op,
            beforeBits, deltaBits, afterBits, vetoed, vetoReason,
            currentEvaluatorId, ruleId, domainId, outputKind, detail));
    }

    /** Ordered route observation. The SELECTED route is the last lane observed (the one
     *  that actually produced the response); every earlier observation stays as evidence. */
    void recordRoute(TraceRoute route, String evidence, String fallbackReason) {
        routeObservations.add(new RouteObservation(route, evidence, fallbackReason));
    }

    void recordPassEligibility(boolean eligible, String facts) {
        this.passEligible = eligible;
        this.passEligibilityFacts = facts;
    }

    void recordPreSafetyWinner(String actionId, Integer scoreBits, boolean vetoed, String vetoReason) {
        this.preSafetyWinnerActionId = actionId;
        this.preSafetyWinnerScoreBits = scoreBits;
        this.preSafetyWinnerVetoed = vetoed;
        this.preSafetyWinnerVetoReason = vetoReason;
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

        TraceFinalization finalization = new TraceFinalization(
            preSafetyWinnerActionId, preSafetyWinnerScoreBits, preSafetyWinnerVetoed,
            preSafetyWinnerVetoReason, passEligible, passEligibilityFacts,
            multiSelectResponse, emergencyResponse, emergencyReason,
            corrections, finalResponse, finalResponseRecorded, skippedCommonFinalizer);

        TraceStatus status = failures.isEmpty() ? TraceStatus.COMPLETE : TraceStatus.INCOMPLETE;

        return new DecisionTrace(DecisionTrace.SCHEMA_VERSION, botModel,
            decisionId, decisionType, decisionText,
            snapshot, status, failures, routeRecord,
            rawCandidateOrder, mergeOrder, ops, finalization, stateEvents);
    }
}
