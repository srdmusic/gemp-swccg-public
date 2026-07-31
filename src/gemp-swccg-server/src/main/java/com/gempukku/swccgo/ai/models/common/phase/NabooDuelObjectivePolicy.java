package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/** Exact action scoring for the mirrored Reflections III Naboo duels. */
public final class NabooDuelObjectivePolicy {
    public static final String FRONT_TARGET_LOSS_RULE_ID =
            "OBJECTIVE.NABOO_DUEL.FRONT_TARGET_LOSS";
    public static final String LIGHTSABER_COMBAT_RULE_ID =
            "OBJECTIVE.NABOO_DUEL.INITIATE_LIGHTSABER_COMBAT";
    public static final String FRONT_DEPLOY_ROUTE_RULE_ID =
            "DEPLOY.OBJECTIVE.NABOO_DUEL_FRONT_ROUTE";

    private static final String PRODUCER =
            "NABOO_DUEL_OBJECTIVE_POLICY";

    public enum ActionKind {
        FRONT_TARGET_LOSS,
        INITIATE_LIGHTSABER_COMBAT
    }

    private NabooDuelObjectivePolicy() {
    }

    public static PolicyResult score(
            String actionId, ActionKind kind) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(kind, "kind");
        boolean front = kind == ActionKind.FRONT_TARGET_LOSS;
        return new PolicyResult(
                PRODUCER,
                List.of(PolicyOperation.add(
                    actionId,
                    TraceRuleId.of(front
                        ? FRONT_TARGET_LOSS_RULE_ID
                        : LIGHTSABER_COMBAT_RULE_ID),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    front ? 400.0f : 300.0f,
                    front
                        ? "NABOO DUEL FRONT: use the exact objective action to make the legal opposing character lost"
                        : "NABOO DUEL BACK: initiate the objective's two-destiny lightsaber combat")));
    }

    public static PolicyResult scoreFrontDeployDestination(
            String actionId, boolean advancesFrontTargetRoute) {
        Objects.requireNonNull(actionId, "actionId");
        if (!advancesFrontTargetRoute) {
            return new PolicyResult(PRODUCER, List.of());
        }
        return new PolicyResult(
                PRODUCER,
                List.of(PolicyOperation.add(
                    actionId,
                    TraceRuleId.of(FRONT_DEPLOY_ROUTE_RULE_ID),
                    TraceDomainId.DEPLOY_SITING,
                    TraceOutputKind.BANDED,
                    1200.0f,
                    "NABOO DUEL FRONT ROUTE: deploy the typed duelist with the legal interior Theed Palace target")));
    }
}
