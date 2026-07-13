package com.gempukku.swccgo.ai.models.common.trace;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.TraceStateEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "One immutable envelope"): the ONE versioned, deeply-immutable record a TraceSink
 * receives for a decision, after finalization.
 *
 * Envelope fields per the contract:
 *  1. schemaVersion — exact supported trace schema version.
 *  2. botModel — Rando or ChosenOne source path identifier (bot package name).
 *  3. snapshot — the shared immutable DecisionSnapshot, built once in shadow from the
 *     complete raw input (null ONLY with a SNAPSHOT capture failure + INCOMPLETE status).
 *  4. status — COMPLETE or INCOMPLETE, with ordered typed capture failures.
 *  5. route — one selected typed route id + ordered evidence + bypass/fallback reason
 *     (null ONLY with a ROUTE capture failure + INCOMPLETE status).
 *  6. operations — append-only typed score/veto/merge/rank/select events, raw float bits.
 *  7. finalization — pre-safety winner, pass eligibility, corrections, final response.
 *  8. stateEvents — ordered typed sealed TraceStateEvent records: completed legacy state
 *     mutations observed at their owner sites (never applied by the trace). Stage 4A1
 *     (Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md).
 *
 * Candidate order: rawCandidateOrder is the COMPLETE raw decision id array, verbatim
 * (duplicates preserved, unreturned candidates included); every operation's ordinal binds
 * to it. mergeOrder is the separate first-seen evaluator-merge insertion order, kept so
 * reordered/collapsed candidates are directly visible against the raw order.
 *
 * DEEP IMMUTABILITY (gate blocker m00268 #2): every list is defensively COPIED here —
 * later mutation of any caller-owned collection cannot reach an emitted trace. Wrapping
 * a caller-owned list with Collections.unmodifiableList alone is insufficient and is
 * deliberately not done anywhere in this class.
 */
public final class DecisionTrace {

    /** V3 envelope schema: typed sealed stateEvents replaced the prose intended-event
     *  list (Stage 4A1). V2 was the prose TraceIntendedStateEvent shape; V1 was the
     *  55c22fdde merge-order shape, never captured. This is the ENVELOPE version;
     *  DecisionSnapshot.CURRENT_VERSION tracks the snapshot separately. */
    public static final int SCHEMA_VERSION = 3;

    private final int schemaVersion;
    private final String botModel;
    private final String decisionId;
    private final String decisionType;
    private final String decisionText;
    private final DecisionSnapshot snapshot;
    private final TraceStatus status;
    private final List<TraceCaptureFailure> captureFailures;
    private final TraceRouteRecord route;
    private final List<String> rawCandidateOrder;
    private final List<String> mergeOrder;
    private final List<TraceOperation> operations;
    private final TraceFinalization finalization;
    private final List<TraceStateEvent> stateEvents;

    public DecisionTrace(int schemaVersion, String botModel,
                         String decisionId, String decisionType, String decisionText,
                         DecisionSnapshot snapshot,
                         TraceStatus status, List<TraceCaptureFailure> captureFailures,
                         TraceRouteRecord route,
                         List<String> rawCandidateOrder, List<String> mergeOrder,
                         List<TraceOperation> operations,
                         TraceFinalization finalization,
                         List<TraceStateEvent> stateEvents) {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1, was " + schemaVersion);
        }
        this.schemaVersion = schemaVersion;
        this.botModel = Objects.requireNonNull(botModel, "botModel");
        this.decisionId = decisionId;
        this.decisionType = decisionType;
        this.decisionText = decisionText;
        this.snapshot = snapshot;
        this.status = Objects.requireNonNull(status, "status");
        this.captureFailures = List.copyOf(captureFailures);
        this.route = route;
        this.rawCandidateOrder = deepCopyNullTolerant(rawCandidateOrder);
        this.mergeOrder = deepCopyNullTolerant(mergeOrder);
        this.operations = List.copyOf(operations);
        this.finalization = Objects.requireNonNull(finalization, "finalization");
        this.stateEvents = List.copyOf(stateEvents);
        // Status/failure consistency: INCOMPLETE iff there are typed failures. A trace
        // can never silently claim completion, and can never carry failures as COMPLETE.
        if ((status == TraceStatus.INCOMPLETE) != !this.captureFailures.isEmpty()) {
            throw new IllegalArgumentException("status " + status + " inconsistent with "
                + this.captureFailures.size() + " capture failures");
        }
        if (snapshot == null && status == TraceStatus.COMPLETE) {
            throw new IllegalArgumentException("snapshot absent but status COMPLETE");
        }
        if (route == null && status == TraceStatus.COMPLETE) {
            throw new IllegalArgumentException("route absent but status COMPLETE");
        }
    }

    /** Defensive copy that tolerates null ELEMENTS (raw arrays may contain nulls verbatim). */
    private static List<String> deepCopyNullTolerant(List<String> src) {
        return Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(src, "list")));
    }

    /**
     * TRACE-V2 GATE P0-2 (Handoffs/CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md
     * "record-construction and sink failures still disappear"): derive an INCOMPLETE
     * copy of this (already-immutable) trace with one more typed failure appended.
     * This is the typed failure channel for post-finalization failures — e.g. a sink
     * that throws on accept: the emitter re-offers the derived trace once, so the sink
     * failure is inspectable evidence instead of a silent abandon.
     */
    public DecisionTrace withAdditionalFailure(TraceCaptureFailure failure) {
        Objects.requireNonNull(failure, "failure");
        List<TraceCaptureFailure> augmented = new ArrayList<>(captureFailures);
        augmented.add(failure);
        return new DecisionTrace(schemaVersion, botModel, decisionId, decisionType,
            decisionText, snapshot, TraceStatus.INCOMPLETE, augmented, route,
            rawCandidateOrder, mergeOrder, operations, finalization, stateEvents);
    }

    public int getSchemaVersion() { return schemaVersion; }
    public String getBotModel() { return botModel; }
    public String getDecisionId() { return decisionId; }
    public String getDecisionType() { return decisionType; }
    public String getDecisionText() { return decisionText; }

    /** Shared immutable snapshot of the complete raw input; null only when INCOMPLETE. */
    public DecisionSnapshot getSnapshot() { return snapshot; }

    public TraceStatus getStatus() { return status; }
    public List<TraceCaptureFailure> getCaptureFailures() { return captureFailures; }

    /** Selected typed route + ordered evidence; null only when INCOMPLETE. */
    public TraceRouteRecord getRoute() { return route; }

    /** COMPLETE raw decision candidate ids, verbatim order, duplicates preserved. */
    public List<String> getRawCandidateOrder() { return rawCandidateOrder; }

    /** First-seen evaluator merge order (LinkedHashMap insertion) — reorder detector. */
    public List<String> getMergeOrder() { return mergeOrder; }

    /** Append-only operation list, in recording order (seq ascending). */
    public List<TraceOperation> getOperations() { return operations; }

    public TraceFinalization getFinalization() { return finalization; }

    /** Ordered typed state events: completed legacy mutations observed during the one
     *  legacy run (list position is the authoritative event order). */
    public List<TraceStateEvent> getStateEvents() { return stateEvents; }
}
