package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure owner of generic DEPLOY card-value and board-urgency scoring. */
public final class DeployCardValuePolicy {

    private static final String PRODUCER = "DEPLOY_CARD_VALUE_POLICY";

    private DeployCardValuePolicy() {
    }

    public static PolicyResult scoreBase(DeployCardValueFacts.BaseValue facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        int cardValue = facts.power() + facts.ability();
        float ratio = facts.deployCost() > 0
                ? (float) cardValue / facts.deployCost() : cardValue;

        if (ratio >= 2.0f) {
            operations.add(operation(facts.actionId(), "deploy-value-excellent",
                    TraceOutputKind.ORDERING, 40.0f,
                    String.format("Excellent value (%.1f)", ratio)));
        } else if (ratio >= 1.5f) {
            operations.add(operation(facts.actionId(), "deploy-value-good",
                    TraceOutputKind.ORDERING, 20.0f,
                    String.format("Good value (%.1f)", ratio)));
        } else if (ratio >= 1.0f) {
            operations.add(operation(facts.actionId(), "deploy-value-average",
                    TraceOutputKind.BANDED, 0.0f,
                    String.format("Average value (%.1f)", ratio)));
        } else {
            operations.add(operation(facts.actionId(), "V40-deploy-value-below-average",
                    TraceOutputKind.BANDED, 0.0f,
                    String.format("V40: Below average value (%.1f) \u2014 deploy anyway", ratio)));
        }

        if (facts.destiny() >= 5.0f) {
            operations.add(operation(facts.actionId(), "deploy-high-destiny",
                    TraceOutputKind.ORDERING, 15.0f,
                    String.format("High destiny (%.0f)", facts.destiny())));
        }
        return new PolicyResult(PRODUCER, operations);
    }

    public static PolicyResult scoreElite(
            DeployCardValueFacts.EliteValue facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.eliteCharacter()) {
            return new PolicyResult(PRODUCER, List.of());
        }
        return new PolicyResult(PRODUCER, List.of(operation(
                facts.actionId(), "V40-elite-character",
                TraceOutputKind.ORDERING, 100.0f,
                "V40 ELITE: Vader/Emperor deploy bonus!")));
    }

    public static PolicyResult scoreStrategic(
            DeployCardValueFacts.Strategic facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.needsReinforcement()) {
            operations.add(operation(facts.actionId(), "deploy-needs-reinforcement",
                    TraceOutputKind.ORDERING, 20.0f,
                    "Need to reinforce board"));
        }
        if (facts.criticalLifeForce()) {
            operations.add(operation(facts.actionId(), "deploy-critical-life-force",
                    TraceOutputKind.ORDERING, 30.0f,
                    "Critical life force - must deploy!"));
        }
        return new PolicyResult(PRODUCER, operations);
    }

    public static PolicyResult scoreType(DeployCardValueFacts.TypeValue facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.highAbilityCharacter()) {
            return new PolicyResult(PRODUCER, List.of());
        }
        return new PolicyResult(PRODUCER, List.of(operation(
                facts.actionId(), "deploy-high-ability-character",
                TraceOutputKind.ORDERING, 25.0f,
                "High-ability character")));
    }

    private static PolicyOperation operation(
            String actionId,
            String ruleId,
            TraceOutputKind outputKind,
            float delta,
            String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SEQUENCING, outputKind, delta, reason);
    }
}
