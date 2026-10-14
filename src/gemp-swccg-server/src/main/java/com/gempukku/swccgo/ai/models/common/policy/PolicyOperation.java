package com.gempukku.swccgo.ai.models.common.policy;

import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.Objects;

/** One ordered, typed contribution from an AI policy to an offered stock action. */
public record PolicyOperation(
        String actionId,
        TraceRuleId ruleArmId,
        TraceDomainId domainId,
        TraceOutputKind outputKind,
        PolicyOperationKind kind,
        float delta,
        String reason,
        ObjectivePreferencePolicy.Strength objectiveStrength) {

    public PolicyOperation {
        // The existing CombinedEvaluator represents legal Pass with action id "".
        // Null is never identity; blank is intentionally reserved for that stock Pass.
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(ruleArmId, "ruleArmId");
        Objects.requireNonNull(domainId, "domainId");
        Objects.requireNonNull(outputKind, "outputKind");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(objectiveStrength, "objectiveStrength");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must be nonblank");
        }
        if (!Float.isFinite(delta)) {
            throw new IllegalArgumentException("delta must be finite");
        }
        if (ruleArmId.equals(TraceRuleId.LEGACY_UNTAGGED)
                || ruleArmId.equals(TraceRuleId.COMBINED_EVALUATOR)) {
            throw new IllegalArgumentException("a migrated policy operation requires a real rule-arm id");
        }
        if (domainId == TraceDomainId.LEGACY_UNTAGGED
                || domainId == TraceDomainId.COMBINED_EVALUATOR) {
            throw new IllegalArgumentException("a migrated policy operation requires a real domain");
        }
        if (outputKind == TraceOutputKind.LEGACY_UNTAGGED
                || outputKind == TraceOutputKind.COMBINED_EVALUATOR) {
            throw new IllegalArgumentException("a migrated policy operation requires a manifest output kind");
        }
        if (kind == PolicyOperationKind.HARD_VETO
                && Float.floatToRawIntBits(delta) != Float.floatToRawIntBits(0.0f)) {
            throw new IllegalArgumentException("HARD_VETO must not carry an additive delta");
        }
        if (!ObjectivePreferencePolicy.isObjective(domainId)
                && objectiveStrength
                != ObjectivePreferencePolicy.Strength.NONE) {
            throw new IllegalArgumentException(
                    "non-objective operation cannot carry objective strength");
        }
        if (kind == PolicyOperationKind.ADD
                && ObjectivePreferencePolicy.isObjective(domainId)
                && objectiveStrength
                == ObjectivePreferencePolicy.Strength.NONE) {
            throw new IllegalArgumentException(
                    "objective addition requires an explicit strength");
        }
        if (kind != PolicyOperationKind.ADD
                && objectiveStrength
                != ObjectivePreferencePolicy.Strength.NONE) {
            throw new IllegalArgumentException(
                    "non-add operation cannot carry objective strength");
        }
    }

    public static PolicyOperation add(String actionId, TraceRuleId ruleArmId,
                                      TraceDomainId domainId, TraceOutputKind outputKind,
                                      float delta, String reason) {
        ObjectivePreferencePolicy.Strength strength =
                ObjectivePreferencePolicy.defaultStrength(domainId);
        return new PolicyOperation(actionId, ruleArmId, domainId, outputKind,
                PolicyOperationKind.ADD,
                ObjectivePreferencePolicy.normalize(
                        domainId, strength, delta),
                reason, strength);
    }

    public static PolicyOperation exactObjectiveProgress(
            String actionId, TraceRuleId ruleArmId,
            TraceOutputKind outputKind, String reason) {
        return new PolicyOperation(
                actionId, ruleArmId, TraceDomainId.OBJECTIVE_INTENT,
                outputKind, PolicyOperationKind.ADD,
                ObjectivePreferencePolicy.EXACT_PROGRESS_SCORE,
                reason,
                ObjectivePreferencePolicy.Strength.EXACT_PROGRESS);
    }

    public static PolicyOperation hardVeto(String actionId, TraceRuleId ruleArmId,
                                           TraceDomainId domainId, TraceOutputKind outputKind,
                                           String reason) {
        return new PolicyOperation(actionId, ruleArmId, domainId, outputKind,
                PolicyOperationKind.HARD_VETO, 0.0f, reason,
                ObjectivePreferencePolicy.Strength.NONE);
    }

    public static PolicyOperation defer(String actionId, TraceRuleId ruleArmId,
                                        TraceDomainId domainId, TraceOutputKind outputKind,
                                        float mandatoryFallbackDelta, String reason) {
        return new PolicyOperation(actionId, ruleArmId, domainId, outputKind,
                PolicyOperationKind.DEFER, mandatoryFallbackDelta, reason,
                ObjectivePreferencePolicy.Strength.NONE);
    }

    public PolicyOperation withAppliedDelta(float appliedDelta) {
        return new PolicyOperation(
                actionId, ruleArmId, domainId, outputKind,
                kind, appliedDelta, reason, objectiveStrength);
    }
}
