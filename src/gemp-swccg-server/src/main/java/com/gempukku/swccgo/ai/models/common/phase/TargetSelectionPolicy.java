package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure ordered scoring policy for shared target-selection value arithmetic. */
public final class TargetSelectionPolicy {

    private static final String PRODUCER = "TARGET_SELECTION_POLICY";
    private static final TraceDomainId DOMAIN = TraceDomainId.BATTLE_WEAPONS;

    public record InitialScore(String actionId,
                               float score,
                               String displayText,
                               TraceRuleId ruleArmId,
                               TraceDomainId domainId,
                               TraceOutputKind outputKind) {
        public InitialScore {
            Objects.requireNonNull(actionId, "actionId");
            if (actionId.isBlank()) {
                throw new IllegalArgumentException("actionId must be nonblank");
            }
            Objects.requireNonNull(displayText, "displayText");
            Objects.requireNonNull(ruleArmId, "ruleArmId");
            Objects.requireNonNull(domainId, "domainId");
            Objects.requireNonNull(outputKind, "outputKind");
        }
    }

    private TargetSelectionPolicy() {
    }

    public static InitialScore initialScore(String actionId) {
        Objects.requireNonNull(actionId, "actionId");
        if (actionId.isBlank()) {
            throw new IllegalArgumentException("actionId must be nonblank");
        }
        return new InitialScore(actionId, 50.0f, "Target " + actionId,
                TraceRuleId.of("TARGET-base"), DOMAIN, TraceOutputKind.BANDED);
    }

    public static PolicyResult scoreOwnership(
            TargetSelectionFacts.OwnershipFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.intent() == TargetSelectionFacts.Intent.BENEFICIAL) {
            if (facts.ownership() == TargetSelectionFacts.Ownership.OWN) {
                add(operations, facts.actionId(), "TARGET-beneficial-own",
                        TraceOutputKind.BANDED, 50.0f,
                        "Beneficial effect on our card");
            } else {
                add(operations, facts.actionId(), "TARGET-beneficial-opponent",
                        TraceOutputKind.VETO, -200.0f,
                        "Don't buff opponent's card!");
            }
        } else if (facts.ownership() == TargetSelectionFacts.Ownership.OPPONENT) {
            add(operations, facts.actionId(), "TARGET-harmful-opponent",
                    TraceOutputKind.BANDED, 50.0f,
                    "Target opponent's card");
        }

        return result(operations);
    }

    public static PolicyResult scoreUndercover(
            TargetSelectionFacts.UndercoverFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.intent() == TargetSelectionFacts.Intent.HARMFUL
                && facts.ownership() == TargetSelectionFacts.Ownership.OPPONENT
                && facts.undercover()) {
            add(operations, facts.actionId(), "V51-kill-spy",
                    TraceOutputKind.ORDERING, 500.0f,
                    "V51 KILL SPY: Target is an undercover spy — eliminate it!");
        }

        return result(operations);
    }

    public static PolicyResult scoreValue(TargetSelectionFacts.ValueFacts facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();

        if (facts.intent() == TargetSelectionFacts.Intent.BENEFICIAL
                && facts.ownership() == TargetSelectionFacts.Ownership.OWN) {
            if (facts.highPower()) {
                add(operations, facts.actionId(), "TARGET-beneficial-high-power",
                        TraceOutputKind.ORDERING, 30.0f,
                        "High-power target for buff");
            }
            if (facts.unique()) {
                add(operations, facts.actionId(), "TARGET-beneficial-unique",
                        TraceOutputKind.ORDERING, 20.0f,
                        "Unique target for buff");
            }
        } else if (facts.intent() == TargetSelectionFacts.Intent.HARMFUL
                && facts.ownership() == TargetSelectionFacts.Ownership.OPPONENT) {
            if (!facts.battleMode() && facts.highPower()) {
                add(operations, facts.actionId(), "TARGET-outside-battle-high-power",
                        TraceOutputKind.ORDERING, 30.0f,
                        "High-power target");
            }
            if (facts.unique()) {
                add(operations, facts.actionId(), "TARGET-harmful-unique",
                        TraceOutputKind.ORDERING, 20.0f,
                        "Unique target");
            }
        }

        return result(operations);
    }

    private static PolicyResult result(List<PolicyOperation> operations) {
        return new PolicyResult(PRODUCER, operations);
    }

    private static void add(List<PolicyOperation> operations,
                            String actionId,
                            String ruleId,
                            TraceOutputKind outputKind,
                            float delta,
                            String reason) {
        operations.add(PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                DOMAIN, outputKind, delta, reason));
    }
}
