package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure owner of DEPLOY parent-action, early terminal, and unknown-card routing. */
public final class DeployActionEnvelopePolicy {

    private static final String PRODUCER = "DEPLOY_ACTION_ENVELOPE_POLICY";

    public enum AdapterStep {
        FALL_THROUGH,
        CONTINUE_ACTION
    }

    public record Evaluation(
            float initialScore,
            PolicyResult result,
            AdapterStep adapterStep) {

        public Evaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(adapterStep, "adapterStep");
        }
    }

    private DeployActionEnvelopePolicy() {
    }

    public static Evaluation evaluateParent(
            DeployActionEnvelopeFacts.ParentAction facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.blockedResponse()) {
            return evaluation(-9999.0f, List.of(operation(
                            facts.actionId(), "deploy-cancel-loop-block",
                            TraceDomainId.LOOP_SAFETY, TraceOutputKind.VETO,
                            -9999.0f,
                            "CANCEL-LOOP BLOCK: this action led to repeated Done-cancels \u2014 try something else")),
                    AdapterStep.CONTINUE_ACTION);
        }
        if (facts.personaReplace()) {
            return evaluation(-500.0f, List.of(operation(
                            facts.actionId(), "V38.4-persona-replace",
                            TraceDomainId.DEPLOY_SEQUENCING, TraceOutputKind.VETO,
                            -500.0f,
                            "V38.4 PERSONA REPLACE: Loses armed character \u2014 blocked!")),
                    AdapterStep.CONTINUE_ACTION);
        }
        return evaluation(50.0f, List.of(), AdapterStep.FALL_THROUGH);
    }

    public static Evaluation evaluateTitleGate(
            DeployActionEnvelopeFacts.TitleGate facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.blockTurnOneEffect()) {
            return evaluation(0.0f, List.of(operation(
                            facts.actionId(), "deploy-turn-one-effect-block",
                            TraceDomainId.DEPLOY_SEQUENCING, TraceOutputKind.VETO,
                            -9999.0f,
                            "BLOCKED: Do not deploy this Effect on turn 1")),
                    AdapterStep.CONTINUE_ACTION);
        }
        return evaluation(0.0f, List.of(), AdapterStep.FALL_THROUGH);
    }

    public static Evaluation evaluateUnknown(
            DeployActionEnvelopeFacts.UnknownAction facts) {
        Objects.requireNonNull(facts, "facts");
        List<PolicyOperation> operations = new ArrayList<>();
        if (facts.earlyCardIsLocation()) {
            operations.add(operation(facts.actionId(), "V29-unknown-location",
                    TraceDomainId.DEPLOY_SEQUENCING, TraceOutputKind.ORDERING,
                    200.0f, "V29: Location deploy \u2014 always allowed!"));
        } else if (facts.deployLocationsPlanActive()) {
            if (facts.turnNumber() <= 1) {
                operations.add(operation(facts.actionId(), "V40-unknown-location-plan-turn-one",
                        TraceDomainId.DEPLOY_SEQUENCING, TraceOutputKind.BANDED,
                        0.0f, "V40: Unknown card during DEPLOY_LOCATIONS (neutral)"));
                return evaluation(0.0f, operations, AdapterStep.CONTINUE_ACTION);
            }
            operations.add(operation(facts.actionId(), "V40-unknown-location-plan-later",
                    TraceDomainId.DEPLOY_SEQUENCING, TraceOutputKind.BANDED,
                    0.0f,
                    "V40: DEPLOY_LOCATIONS incomplete turn " + facts.turnNumber()
                            + " \u2014 deploy freely"));
        }
        operations.add(operation(facts.actionId(), "V40-unknown-card",
                TraceDomainId.DEPLOY_SEQUENCING, TraceOutputKind.BANDED,
                0.0f, "V40: Unknown card (deploy from reserve?) \u2014 deploy freely"));
        return evaluation(0.0f, operations, AdapterStep.FALL_THROUGH);
    }

    private static PolicyOperation operation(
            String actionId,
            String ruleId,
            TraceDomainId domain,
            TraceOutputKind outputKind,
            float delta,
            String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId), domain,
                outputKind, delta, reason);
    }

    private static Evaluation evaluation(
            float initialScore,
            List<PolicyOperation> operations,
            AdapterStep step) {
        return new Evaluation(initialScore,
                new PolicyResult(PRODUCER, operations), step);
    }
}
