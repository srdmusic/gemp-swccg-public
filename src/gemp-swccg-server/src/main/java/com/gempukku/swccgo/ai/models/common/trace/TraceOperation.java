package com.gempukku.swccgo.ai.models.common.trace;

/**
 * TRACE HOOK (2026-07-13): one immutable operation in the append-only decision trace.
 *
 * Score values are stored as RAW FLOAT BITS (Float.floatToRawIntBits) so comparison is
 * exact, never decimal-tolerant. A field is null when not applicable to the operation:
 * a SET records before and after but NO delta (it must never masquerade as an additive
 * contribution), and a MERGE records the boundary only (the merged source operations
 * already explain the value, so no synthetic delta either).
 *
 * candidateOrdinal is the action id's FIRST-SEEN ordinal in the frozen candidate order
 * (CombinedEvaluator's LinkedHashMap insertion order). Synthetic actions (e.g. the
 * pass actions CombinedEvaluator manufactures) carry ORDINAL_SYNTHETIC plus an explicit
 * syntheticSource marker instead.
 */
public final class TraceOperation {

    /** Rule id recorded by the un-migrated legacy choke points. Visible debt, never guessed. */
    public static final String RULE_LEGACY_UNTAGGED = "LEGACY_UNTAGGED";

    /** candidateOrdinal for synthetic actions (see syntheticSource for the origin marker). */
    public static final int ORDINAL_SYNTHETIC = -1;

    /** candidateOrdinal when the action id never entered the frozen candidate order. */
    public static final int ORDINAL_UNKNOWN = -2;

    private final int seq;
    private final TraceOp op;
    private final int candidateOrdinal;
    private final String syntheticSource;
    private final String actionId;
    private final String evaluatorId;
    private final String ruleId;
    private final String domainId;
    private final String outputKind;
    private final Integer beforeBits;
    private final Integer deltaBits;
    private final Integer afterBits;
    private final boolean vetoed;
    private final String vetoReason;
    private final String detail;

    public TraceOperation(int seq, TraceOp op, int candidateOrdinal, String syntheticSource,
                          String actionId, String evaluatorId, String ruleId, String domainId,
                          String outputKind, Integer beforeBits, Integer deltaBits, Integer afterBits,
                          boolean vetoed, String vetoReason, String detail) {
        this.seq = seq;
        this.op = op;
        this.candidateOrdinal = candidateOrdinal;
        this.syntheticSource = syntheticSource;
        this.actionId = actionId;
        this.evaluatorId = evaluatorId;
        this.ruleId = ruleId;
        this.domainId = domainId;
        this.outputKind = outputKind;
        this.beforeBits = beforeBits;
        this.deltaBits = deltaBits;
        this.afterBits = afterBits;
        this.vetoed = vetoed;
        this.vetoReason = vetoReason;
        this.detail = detail;
    }

    public int getSeq() { return seq; }
    public TraceOp getOp() { return op; }
    public int getCandidateOrdinal() { return candidateOrdinal; }
    public String getSyntheticSource() { return syntheticSource; }
    public String getActionId() { return actionId; }
    public String getEvaluatorId() { return evaluatorId; }
    public String getRuleId() { return ruleId; }
    public String getDomainId() { return domainId; }
    public String getOutputKind() { return outputKind; }
    public Integer getBeforeBits() { return beforeBits; }
    public Integer getDeltaBits() { return deltaBits; }
    public Integer getAfterBits() { return afterBits; }
    public boolean isVetoed() { return vetoed; }
    public String getVetoReason() { return vetoReason; }
    public String getDetail() { return detail; }

    @Override
    public String toString() {
        return "TraceOperation(seq=" + seq + ", op=" + op
            + ", ordinal=" + candidateOrdinal
            + (syntheticSource != null ? ", synthetic=" + syntheticSource : "")
            + ", actionId=" + actionId
            + (evaluatorId != null ? ", evaluator=" + evaluatorId : "")
            + (ruleId != null ? ", rule=" + ruleId : "")
            + (domainId != null ? ", domain=" + domainId : "")
            + (outputKind != null ? ", kind=" + outputKind : "")
            + (beforeBits != null ? ", beforeBits=" + beforeBits : "")
            + (deltaBits != null ? ", deltaBits=" + deltaBits : "")
            + (afterBits != null ? ", afterBits=" + afterBits : "")
            + ", vetoed=" + vetoed
            + (vetoReason != null ? ", vetoReason=" + vetoReason : "")
            + (detail != null ? ", detail=" + detail : "")
            + ")";
    }
}
