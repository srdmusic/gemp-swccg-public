package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Exact engine-offered front objective flip scoring. */
public final class ObjectiveFlipActionPolicy {
    public static final String MWYHL_FLIP_RULE_ID =
            "OBJECTIVE.MWYHL.FLIP";

    private static final String PRODUCER =
            "OBJECTIVE_FLIP_ACTION_POLICY";
    private static final String MWYHL_FRONT_BLUEPRINT = "225_53";
    private static final String DEPLOY_EFFECT =
            "Deploy Effect from Reserve Deck";
    private static final String DEPLOY_DAGOBAH_LOCATION =
            "Deploy Dagobah location from Reserve Deck";

    public record Facts(
            String sourceBlueprintId,
            boolean sourceOwnedByPlayer,
            boolean sourceInPlay,
            boolean sourceFlipped,
            String actionText,
            boolean usefulPriorityFrontSetupActionOffered) {
    }

    public enum FrontSetupKind {
        NONE,
        EFFECT,
        DAGOBAH_LOCATION
    }

    private ObjectiveFlipActionPolicy() {
    }

    public static PolicyResult score(String actionId, Facts facts) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(facts, "facts");
        if (!MWYHL_FRONT_BLUEPRINT.equals(facts.sourceBlueprintId())
                || !facts.sourceOwnedByPlayer()
                || !facts.sourceInPlay()
                || facts.sourceFlipped()
                || facts.actionText() == null
                || !"Flip".equals(facts.actionText().trim())
                || facts.usefulPriorityFrontSetupActionOffered()) {
            return empty();
        }
        return new PolicyResult(
                PRODUCER,
                List.of(PolicyOperation.add(
                        actionId,
                        TraceRuleId.of(MWYHL_FLIP_RULE_ID),
                        TraceDomainId.OBJECTIVE_INTENT,
                        TraceOutputKind.BANDED,
                        600.0f,
                        "OBJECTIVE.MWYHL.FLIP: use the exact engine-offered front objective payoff")));
    }

    public static FrontSetupKind classifyPriorityFrontSetupAction(
            String actionText) {
        if (actionText == null) {
            return FrontSetupKind.NONE;
        }
        return switch (actionText.trim()) {
            case DEPLOY_EFFECT -> FrontSetupKind.EFFECT;
            case DEPLOY_DAGOBAH_LOCATION ->
                    FrontSetupKind.DAGOBAH_LOCATION;
            default -> FrontSetupKind.NONE;
        };
    }

    private static PolicyResult empty() {
        return new PolicyResult(PRODUCER, List.of());
    }
}
