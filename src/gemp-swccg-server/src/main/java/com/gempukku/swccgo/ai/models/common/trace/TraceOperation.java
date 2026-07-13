package com.gempukku.swccgo.ai.models.common.trace;

import java.util.Objects;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Operation record"): one immutable operation in the append-only decision trace.
 *
 * Score values are stored as RAW FLOAT BITS (Float.floatToRawIntBits) so comparison is
 * exact, never decimal-tolerant. A field is null when not applicable to the operation:
 * a SET records before and after but NO delta (it must never masquerade as an additive
 * contribution), and a MERGE records the boundary only (the merged source operations
 * already explain the value, so no synthetic delta either).
 *
 * candidateOrdinal binds to the COMPLETE RAW decision candidate order (the id's first
 * ordinal in DecisionTrace.rawCandidateOrder) — never to evaluator output or the merge
 * map. Synthetic actions (e.g. the pass actions CombinedEvaluator manufactures) carry
 * ORDINAL_SYNTHETIC plus an explicit syntheticSource marker; a synthetic Pass with
 * action id "" can never reuse or replace an offered candidate's ordinal (identity-based
 * marking, not id-based). An id that never appeared in the raw arrays carries
 * ORDINAL_UNKNOWN — visible drift, not silent adoption.
 *
 * Rule/domain/kind identity is TYPED (TraceRuleId / TraceDomainId / TraceOutputKind):
 * free-form identity strings are rejected at construction, and unmigrated arms carry
 * the one explicit TraceRuleId.LEGACY_UNTAGGED value. Reason text stays diagnostic
 * evidence; it never defines identity.
 *
 * TRACE-V2 GATE P1-4 (Handoffs/CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md "operation
 * identity remains nullable"): identity is now MANDATORY on every dimension — op,
 * producer (evaluatorId), ruleId, domainId, and outputKind are rejected as null at
 * construction. Framework-produced merge/rank/select/synthetic-pass operations carry
 * the typed {@link #PRODUCER_COMBINED_EVALUATOR} producer plus the COMBINED_EVALUATOR
 * sentinel on each identity dimension; unmigrated legacy arms carry the explicit
 * LEGACY_UNTAGGED sentinel on each dimension. Null never means anything.
 */
public final class TraceOperation {

    /** candidateOrdinal for synthetic actions (see syntheticSource for the origin marker). */
    public static final int ORDINAL_SYNTHETIC = -1;

    /** candidateOrdinal when the action id never appeared in the raw candidate arrays. */
    public static final int ORDINAL_UNKNOWN = -2;

    /** Typed producer id for operations the CombinedEvaluator FRAMEWORK itself performs
     *  (merge boundaries recorded outside a binding, rank/select steps, synthetic Pass
     *  construction) — the framework ranks and selects, so it is the producer (gate P1-4). */
    public static final String PRODUCER_COMBINED_EVALUATOR = "COMBINED_EVALUATOR";

    private final int seq;
    private final TraceOp op;
    private final int candidateOrdinal;
    private final String syntheticSource;
    private final String actionId;
    private final String evaluatorId;
    private final TraceRuleId ruleId;
    private final TraceDomainId domainId;
    private final TraceOutputKind outputKind;
    private final Integer beforeBits;
    private final Integer deltaBits;
    private final Integer afterBits;
    private final boolean vetoed;
    private final String vetoReason;
    private final String detail;

    public TraceOperation(int seq, TraceOp op, int candidateOrdinal, String syntheticSource,
                          String actionId, String evaluatorId, TraceRuleId ruleId,
                          TraceDomainId domainId, TraceOutputKind outputKind,
                          Integer beforeBits, Integer deltaBits, Integer afterBits,
                          boolean vetoed, String vetoReason, String detail) {
        // GATE P1-4: incomplete operation identity is rejected at construction —
        // explicit sentinels exist for every dimension, so null carries no meaning.
        Objects.requireNonNull(op, "op");
        Objects.requireNonNull(evaluatorId, "evaluatorId (producer) — use PRODUCER_COMBINED_EVALUATOR for framework ops");
        Objects.requireNonNull(ruleId, "ruleId — use TraceRuleId.LEGACY_UNTAGGED/COMBINED_EVALUATOR sentinels");
        Objects.requireNonNull(domainId, "domainId — use TraceDomainId.LEGACY_UNTAGGED/COMBINED_EVALUATOR sentinels");
        Objects.requireNonNull(outputKind, "outputKind — use TraceOutputKind.LEGACY_UNTAGGED/COMBINED_EVALUATOR sentinels");
        if (evaluatorId.isBlank()) {
            throw new IllegalArgumentException("evaluatorId (producer) must be nonblank");
        }
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
    public TraceRuleId getRuleId() { return ruleId; }
    public TraceDomainId getDomainId() { return domainId; }
    public TraceOutputKind getOutputKind() { return outputKind; }
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
            + (ruleId != null ? ", rule=" + ruleId.id() : "")
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
