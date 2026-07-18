package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;

import java.util.ArrayList;
import java.util.List;

/**
 * An action that has been scored by evaluators.
 *
 * Represents a possible decision with:
 * - The action to take (action_id or card_id)
 * - Score (higher = better)
 * - Reasoning (for debugging/logging)
 */
public class EvaluatedAction {
    private String actionId;
    private ActionType actionType;
    private float score;
    private List<String> reasoning;

    // Optional metadata
    private String displayText = "";
    private String cardName = "";
    private String blueprintId = "";
    private int deployCost = 0;
    private float expectedValue = 0.0f;
    // FORMATION SAFETY (2026-07-11c, Codex root-cause audit + Steve's four laws): a TRUE veto that
    // survives additive merging. ~20 prior fixes coded the basics (no solos, no destiny-less battles,
    // no buddy-less deploys, no solo charges) as -150..-500 penalties, which the R2 +6000 move band
    // and +600/+700 bonus stacks routinely outvoted. hardVeto is OR-merged and CombinedEvaluator
    // never selects a vetoed action regardless of score.
    private boolean hardVeto = false;
    private String vetoReason = null;
    // V201: non-additive middle tier for strategically unsupported actions. An
    // admissible action or legal Pass always beats DEFER, while a mandatory
    // choice may still take the best deferred action instead of a hard veto.
    private boolean deferred = false;
    private String deferReason = null;

    public EvaluatedAction(String actionId, ActionType actionType, float score, String displayText) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.score = score;
        this.displayText = displayText;
        this.reasoning = new ArrayList<>();
        // TRACE HOOK (2026-07-13, CODEX_MINIMAL_DECISION_TRACE_HOOK): INITIAL score op,
        // LEGACY_UNTAGGED until arms migrate to the tagged overloads. No-op (cheap
        // thread-local guard) unless a trace session is open.
        TraceSession.recordInitial(this, actionId, score,
            TraceRuleId.LEGACY_UNTAGGED, null, null, displayText);
    }

    /**
     * Add reasoning with optional score adjustment.
     *
     * @param reason the reasoning text
     * @param scoreDelta score adjustment (can be 0)
     */
    public void addReasoning(String reason, float scoreDelta) {
        // TRACE HOOK (2026-07-13): legacy arm, records ADD as LEGACY_UNTAGGED.
        addReasoning(reason, scoreDelta, TraceRuleId.LEGACY_UNTAGGED, null, null);
    }

    /**
     * TRACE HOOK (2026-07-13, ORACLE V2): tagged overload for migrated arms. Identical
     * score and reasoning behavior to addReasoning(reason, delta); additionally stamps
     * TYPED rule/domain/kind identity on the trace ADD op (validated stable-id value
     * types — free strings no longer compile). NEVER parse V-tags out of reason prose;
     * supply them here instead (reason text stays diagnostic).
     */
    public void addReasoning(String reason, float scoreDelta, TraceRuleId ruleId,
                             TraceDomainId domainId, TraceOutputKind outputKind) {
        float traceBefore = score;
        if (scoreDelta != 0) {
            reasoning.add(String.format("%s (%+.1f)", reason, scoreDelta));
            score += scoreDelta;
        } else {
            reasoning.add(reason);
        }
        TraceSession.recordAdd(this, actionId, traceBefore, scoreDelta, score,
            ruleId, domainId, outputKind, reason);
    }

    /**
     * Add reasoning without score adjustment.
     */
    public void addReasoning(String reason) {
        addReasoning(reason, 0.0f);
    }

    /**
     * Merge another action's score and reasoning into this one.
     * Used when multiple evaluators score the same action ID.
     *
     * @param other the other action to merge from
     */
    public void mergeFrom(EvaluatedAction other) {
        if (other == null) return;

        // TRACE HOOK (2026-07-13): capture the boundary's before-score (primitive, free).
        float traceBefore = this.score;

        // FORMATION SAFETY (2026-07-11c): vetoes are OR-merged — no bonus stack can wash one out.
        if (other.hardVeto) {
            this.hardVeto = true;
            if (this.vetoReason == null) this.vetoReason = other.vetoReason;
        }
        if (other.deferred) {
            this.deferred = true;
            if (this.deferReason == null) this.deferReason = other.deferReason;
        }

        // Add the other action's score to this one
        this.score += other.score;

        // Merge reasoning lists
        this.reasoning.addAll(other.reasoning);

        // Keep the more specific action type if this one is UNKNOWN
        if (this.actionType == ActionType.UNKNOWN && other.actionType != ActionType.UNKNOWN) {
            this.actionType = other.actionType;
        }

        // Use the more descriptive display text if this one is empty
        if ((this.displayText == null || this.displayText.isEmpty()) &&
            other.displayText != null && !other.displayText.isEmpty()) {
            this.displayText = other.displayText;
        }

        // TRACE HOOK (2026-07-13): MERGE records the boundary ONLY. The merged source's own
        // INITIAL/ADD/SET ops already explain the value, so no synthetic delta is recorded.
        // isActive() guard keeps the detail string from being built in production.
        if (TraceSession.isActive()) {
            TraceSession.recordMerge(this, actionId, traceBefore, this.score, this.hardVeto,
                this.vetoReason, "mergeFrom actionId=" + other.actionId + " score=" + other.score);
        }
    }

    /** FORMATION SAFETY (2026-07-11c): mark this action un-selectable regardless of score. */
    public void hardVeto(String reason) {
        // TRACE HOOK (2026-07-13): legacy arm, records HARD_VETO as LEGACY_UNTAGGED.
        hardVeto(reason, TraceRuleId.LEGACY_UNTAGGED, null, null);
    }

    /** TRACE HOOK (2026-07-13, ORACLE V2): typed tagged overload for migrated veto arms,
     *  identical behavior. */
    public void hardVeto(String reason, TraceRuleId ruleId, TraceDomainId domainId,
                         TraceOutputKind outputKind) {
        this.hardVeto = true;
        if (this.vetoReason == null) this.vetoReason = reason;
        this.reasoning.add("HARD VETO: " + reason);
        TraceSession.recordHardVeto(this, actionId, this.vetoReason, reason,
            ruleId, domainId, outputKind);
    }

    public boolean isHardVetoed() { return hardVeto; }
    public String getVetoReason() { return vetoReason; }

    /**
     * V201: mark an action deferred without making it impossible. The optional
     * ranking delta is compared only when no admissible action or legal Pass
     * exists, so later positive score stacks cannot revive the action.
     */
    public void defer(String reason, float mandatoryFallbackDelta) {
        this.deferred = true;
        if (this.deferReason == null) this.deferReason = reason;
        addReasoning("DEFER: " + reason, mandatoryFallbackDelta);
    }

    /** V202: typed policy overload. Selection behavior is identical to the legacy overload. */
    public void defer(String reason, float mandatoryFallbackDelta, TraceRuleId ruleId,
                      TraceDomainId domainId, TraceOutputKind outputKind) {
        this.deferred = true;
        if (this.deferReason == null) this.deferReason = reason;
        addReasoning("DEFER: " + reason, mandatoryFallbackDelta, ruleId, domainId, outputKind);
    }

    public void defer(String reason) {
        defer(reason, 0.0f);
    }

    public boolean isDeferred() { return deferred; }
    public String getDeferReason() { return deferReason; }

    // Getters and setters
    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        // TRACE HOOK (2026-07-13): legacy arm, records SET as LEGACY_UNTAGGED.
        setScore(score, TraceRuleId.LEGACY_UNTAGGED, null, null);
    }

    /**
     * TRACE HOOK (2026-07-13, ORACLE V2): typed tagged overload for migrated arms,
     * identical behavior. A SET records before and after; it never masquerades as an
     * additive delta.
     */
    public void setScore(float score, TraceRuleId ruleId, TraceDomainId domainId,
                         TraceOutputKind outputKind) {
        float traceBefore = this.score;
        this.score = score;
        TraceSession.recordSet(this, actionId, traceBefore, this.score,
            ruleId, domainId, outputKind, null);
    }

    public List<String> getReasoning() {
        return reasoning;
    }

    public String getReasoningString() {
        return String.join(" | ", reasoning);
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(String blueprintId) {
        this.blueprintId = blueprintId;
    }

    public int getDeployCost() {
        return deployCost;
    }

    public void setDeployCost(int deployCost) {
        this.deployCost = deployCost;
    }

    public float getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(float expectedValue) {
        this.expectedValue = expectedValue;
    }

    @Override
    public String toString() {
        return String.format("EvaluatedAction(id=%s, score=%.1f, %s)", actionId, score, displayText);
    }
}
