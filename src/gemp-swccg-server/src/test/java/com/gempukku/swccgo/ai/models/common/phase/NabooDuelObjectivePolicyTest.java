package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NabooDuelObjectivePolicyTest {

    @Test
    public void mirroredFrontActionsKeepTheExactTargetLossContribution() {
        assertOperation(
                NabooDuelObjectivePolicy.score(
                        "light-front",
                        NabooDuelObjectivePolicy.ActionKind
                                .FRONT_TARGET_LOSS),
                "light-front",
                NabooDuelObjectivePolicy.FRONT_TARGET_LOSS_RULE_ID,
                300.0f);
        assertOperation(
                NabooDuelObjectivePolicy.score(
                        "dark-front",
                        NabooDuelObjectivePolicy.ActionKind
                                .FRONT_TARGET_LOSS),
                "dark-front",
                NabooDuelObjectivePolicy.FRONT_TARGET_LOSS_RULE_ID,
                300.0f);
    }

    @Test
    public void mirroredBackActionsKeepTheExactLightsaberCombatContribution() {
        assertOperation(
                NabooDuelObjectivePolicy.score(
                        "light-back",
                        NabooDuelObjectivePolicy.ActionKind
                                .INITIATE_LIGHTSABER_COMBAT),
                "light-back",
                NabooDuelObjectivePolicy.LIGHTSABER_COMBAT_RULE_ID,
                300.0f);
        assertOperation(
                NabooDuelObjectivePolicy.score(
                        "dark-back",
                        NabooDuelObjectivePolicy.ActionKind
                                .INITIATE_LIGHTSABER_COMBAT),
                "dark-back",
                NabooDuelObjectivePolicy.LIGHTSABER_COMBAT_RULE_ID,
                300.0f);
    }

    @Test
    public void frontDeployRouteScoresOnlyAnExactAdvancingDestination() {
        assertOperation(
                NabooDuelObjectivePolicy
                    .scoreFrontDeployDestination(
                        "front-route", true),
                "front-route",
                NabooDuelObjectivePolicy.FRONT_DEPLOY_ROUTE_RULE_ID,
                TraceDomainId.OBJECTIVE_INTENT,
                300.0f);
        assertEquals(0, NabooDuelObjectivePolicy
                .scoreFrontDeployDestination(
                    "wrong-route", false)
                .operations().size());
    }

    private static void assertOperation(
            PolicyResult result, String actionId,
            String ruleId, float delta) {
        assertOperation(result, actionId, ruleId,
                TraceDomainId.OBJECTIVE_INTENT, delta);
    }

    private static void assertOperation(
            PolicyResult result, String actionId,
            String ruleId, TraceDomainId domainId,
            float delta) {
        assertEquals("NABOO_DUEL_OBJECTIVE_POLICY", result.producerId());
        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertEquals(actionId, operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(domainId, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(Float.floatToRawIntBits(delta),
                Float.floatToRawIntBits(operation.delta()));
    }
}
