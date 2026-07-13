package com.gempukku.swccgo.ai.models.common.trace;

import java.util.Collections;
import java.util.List;

/**
 * TRACE HOOK (2026-07-13): the ONE complete, immutable record a TraceSink receives for a
 * decision, after finalization. Contains the frozen first-seen candidate order and the
 * full append-only operation list.
 */
public final class DecisionTrace {

    private final String decisionId;
    private final String decisionType;
    private final String decisionText;
    private final List<String> candidateOrder;
    private final List<TraceOperation> operations;

    public DecisionTrace(String decisionId, String decisionType, String decisionText,
                         List<String> candidateOrder, List<TraceOperation> operations) {
        this.decisionId = decisionId;
        this.decisionType = decisionType;
        this.decisionText = decisionText;
        this.candidateOrder = Collections.unmodifiableList(candidateOrder);
        this.operations = Collections.unmodifiableList(operations);
    }

    public String getDecisionId() { return decisionId; }
    public String getDecisionType() { return decisionType; }
    public String getDecisionText() { return decisionText; }

    /** Action ids in frozen first-seen order (merge-map insertion order). Index = ordinal. */
    public List<String> getCandidateOrder() { return candidateOrder; }

    /** Append-only operation list, in recording order (seq ascending). */
    public List<TraceOperation> getOperations() { return operations; }
}
