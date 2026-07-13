package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * TRACE STAGE 4A1, m00372 Option A (accepted m00373): the pure, immutable
 * decision-affecting snapshot of one DecisionTracker owner, captured by the tracker's
 * package-local traceSnapshot() seam before and after the legacy recordDecision(...)
 * call. Owner-specific before/after snapshots define CHANGED and NO_OP.
 *
 * EXCLUDED by ruling: history (diagnostic only), blockedResponses (no active population
 * path), lastTurn and lastPhase (updateState/onPhaseChange-owned; they land with the
 * 4A2 UPDATE_STATE family), and standalone lastStateHash (recordDecision only READS it;
 * it is visible only inside the sequence rows it stamped).
 *
 * Canonical form is guaranteed by construction: sequence rows keep their live order
 * (ordered evidence for loop replay); turn-block rows are sorted by decision key with
 * each row's responses sorted; every list is defensively copied.
 */
public record DecisionTrackerSnapshot(
    List<TrackerSequenceRow> sequenceRows,
    int sequenceRepeatCount,
    int detectedLoopLength,
    String lastActionChoiceKey,
    String lastActionChoiceResponse,
    String consecutiveCancelKey,
    int consecutiveCancelCount,
    List<TrackerTurnBlockRow> turnBlockRows) {

    /** One live sequence entry, verbatim and in order: [decisionKey, response, stateHash]. */
    public record TrackerSequenceRow(String decisionKey, String response, String stateHash) {
        public TrackerSequenceRow {
            Objects.requireNonNull(decisionKey, "decisionKey");
            Objects.requireNonNull(response, "response");
            Objects.requireNonNull(stateHash, "stateHash");
        }
    }

    /** One turn-blocked-actions entry, canonicalized: responses sorted at construction. */
    public record TrackerTurnBlockRow(String decisionKey, List<String> sortedResponses) {
        public TrackerTurnBlockRow {
            Objects.requireNonNull(decisionKey, "decisionKey");
            List<String> canonical = new ArrayList<>(Objects.requireNonNull(sortedResponses, "sortedResponses"));
            for (String response : canonical) {
                Objects.requireNonNull(response, "response");
            }
            Collections.sort(canonical);
            sortedResponses = Collections.unmodifiableList(canonical);
        }
    }

    public DecisionTrackerSnapshot {
        List<TrackerSequenceRow> sequenceCopy =
            new ArrayList<>(Objects.requireNonNull(sequenceRows, "sequenceRows"));
        for (TrackerSequenceRow row : sequenceCopy) {
            Objects.requireNonNull(row, "sequence row");
        }
        sequenceRows = Collections.unmodifiableList(sequenceCopy);
        if (sequenceRepeatCount < 0) {
            throw new IllegalArgumentException("sequenceRepeatCount must be >= 0, was " + sequenceRepeatCount);
        }
        if (detectedLoopLength < 0) {
            throw new IllegalArgumentException("detectedLoopLength must be >= 0, was " + detectedLoopLength);
        }
        Objects.requireNonNull(lastActionChoiceKey, "lastActionChoiceKey");
        Objects.requireNonNull(lastActionChoiceResponse, "lastActionChoiceResponse");
        Objects.requireNonNull(consecutiveCancelKey, "consecutiveCancelKey");
        if (consecutiveCancelCount < 0) {
            throw new IllegalArgumentException("consecutiveCancelCount must be >= 0, was " + consecutiveCancelCount);
        }
        List<TrackerTurnBlockRow> blockCopy =
            new ArrayList<>(Objects.requireNonNull(turnBlockRows, "turnBlockRows"));
        for (TrackerTurnBlockRow row : blockCopy) {
            Objects.requireNonNull(row, "turn-block row");
        }
        blockCopy.sort(Comparator.comparing(TrackerTurnBlockRow::decisionKey));
        turnBlockRows = Collections.unmodifiableList(blockCopy);
    }
}
