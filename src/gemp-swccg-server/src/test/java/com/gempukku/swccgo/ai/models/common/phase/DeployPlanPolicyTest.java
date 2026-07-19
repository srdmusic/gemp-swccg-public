package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeployPlanPolicyTest {
    @Test
    public void plannedPriorityCardKeepsMembershipThenPriorityOrder() {
        DeployPlanPolicy.Evaluation evaluation = DeployPlanPolicy.evaluate(
                facts(true, true, true, 1, false, false,
                        false, false, 1, 6, false, 0, false));
        assertOperations(evaluation.result().operations(),
                new String[]{"deploy-plan-membership", "deploy-plan-priority"},
                new float[]{100.0f, 50.0f});
    }

    @Test
    public void turnOneTdigwattOffPlanCandidateStops() {
        DeployPlanPolicy.Evaluation evaluation = DeployPlanPolicy.evaluate(
                facts(true, true, false, 99, false, false,
                        true, false, 1, 6, false, 0, true));
        assertEquals(DeployPlanPolicy.AdapterStep.CONTINUE_ACTION,
                evaluation.adapterStep());
        assertOperations(evaluation.result().operations(),
                new String[]{"V40-plan-location-first"}, new float[]{-1000.0f});
    }

    @Test
    public void lowForceWaitStopsButSurplusFallsThrough() {
        DeployPlanPolicy.Evaluation low = DeployPlanPolicy.evaluate(
                facts(true, true, false, 99, false, true,
                        false, false, 3, 7, false, 0, false));
        assertEquals(DeployPlanPolicy.AdapterStep.CONTINUE_ACTION, low.adapterStep());
        assertOperations(low.result().operations(),
                new String[]{"V40-plan-save-force"}, new float[]{0.0f});

        DeployPlanPolicy.Evaluation high = DeployPlanPolicy.evaluate(
                facts(true, true, false, 99, false, true,
                        false, false, 3, 8, false, 0, false));
        assertEquals(DeployPlanPolicy.AdapterStep.FALL_THROUGH, high.adapterStep());
        assertOperations(high.result().operations(),
                new String[]{"V40-plan-force-surplus"}, new float[]{0.0f});
    }

    @Test
    public void completeLocationPlanPreservesTwoIndependentBonuses() {
        DeployPlanPolicy.Evaluation evaluation = DeployPlanPolicy.evaluate(
                facts(true, false, false, 99, false, false,
                        true, false, 4, 10, true, 5, true));
        assertOperations(evaluation.result().operations(),
                new String[]{"deploy-plan-locations-complete",
                        "deploy-plan-extra-budget", "V40-plan-hold-back"},
                new float[]{25.0f, 25.0f, 0.0f});
    }

    private static DeployPlanPolicy.Facts facts(
            boolean hasPlan, boolean pending, boolean planned, int priority,
            boolean allowExtras, boolean waiting, boolean locationStrategy,
            boolean locationCard, int turn, int force, boolean complete,
            int extraBudget, boolean holdBack) {
        return new DeployPlanPolicy.Facts(
                "a", hasPlan, pending, planned, priority, allowExtras, waiting,
                locationStrategy, locationCard, locationStrategy,
                turn, force, complete, extraBudget, holdBack, "locations");
    }

    private static void assertOperations(List<PolicyOperation> operations,
                                         String[] rules, float[] deltas) {
        assertEquals(rules.length, operations.size());
        for (int i = 0; i < rules.length; i++) {
            assertEquals(rules[i], operations.get(i).ruleArmId().id());
            assertEquals(deltas[i], operations.get(i).delta(), 0.0f);
        }
    }
}
