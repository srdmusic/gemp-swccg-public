package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeployBudgetPolicyTest {
    @Test
    public void newMaintenancePreservesHardHolisticAndDrainThresholds() {
        assertSingle(newMaintenance(4, 5, 1, 0), "V59", -2000.0f);
        assertSingle(newMaintenance(8, 5, 1, 1), "V59", -1500.0f);
        assertSingle(newMaintenance(10, 5, 2, 0), "V64", -400.0f);
        assertEquals(0, newMaintenance(11, 5, 2, 0).size());
    }

    @Test
    public void existingMaintenancePreservesEqualityBoundary() {
        assertSingle(DeployBudgetPolicy.existingMaintenance(
                "a", 5, 3, 3).result().operations(), "V24.5", -50.0f);
        assertSingle(DeployBudgetPolicy.existingMaintenance(
                "a", 6, 3, 3).result().operations(), "V24.5", -50.0f);
        assertEquals(0, DeployBudgetPolicy.existingMaintenance(
                "a", 8, 3, 3).result().operations().size());
    }

    @Test
    public void affordabilityOwnsTerminalControl() {
        DeployBudgetPolicy.Evaluation blocked =
                DeployBudgetPolicy.affordability("a", 6, 5);
        assertEquals(DeployBudgetPolicy.AdapterStep.CONTINUE_ACTION,
                blocked.adapterStep());
        assertSingle(blocked.result().operations(), "deploy-affordability", -1000.0f);
        assertEquals(DeployBudgetPolicy.AdapterStep.FALL_THROUGH,
                DeployBudgetPolicy.affordability("a", 5, 5).adapterStep());
    }

    @Test
    public void futureObligationsStackInLegacyOrder() {
        DeployBudgetPolicy.FutureObligationFacts facts =
                new DeployBudgetPolicy.FutureObligationFacts(
                        "a", 3, 3, 1, 3, 1,
                        true, 1, true, true, 0);
        List<PolicyOperation> operations =
                DeployBudgetPolicy.futureObligations(facts).result().operations();
        assertEquals(5, operations.size());
        assertOperation(operations.get(0), "V48", -500.0f);
        assertOperation(operations.get(1), "V67z", -1500.0f);
        assertOperation(operations.get(2), "V79", -500.0f);
        assertOperation(operations.get(3), "V29.13-maintenance", -500.0f);
        assertOperation(operations.get(4), "V29.13-interrupt", -30.0f);
    }

    @Test
    public void freeDeployNeverConsumesFutureObligationReserve() {
        DeployBudgetPolicy.FutureObligationFacts facts =
                new DeployBudgetPolicy.FutureObligationFacts(
                        "a", 0, 0, 1, 3, 1,
                        true, 1, true, true, 3);
        assertEquals(0, DeployBudgetPolicy.futureObligations(facts)
                .result().operations().size());
    }

    @Test
    public void objectiveFormationReservePenalizesOnlyBudgetBreakingDeploys() {
        DeployBudgetPolicy.Evaluation breaking =
                DeployBudgetPolicy.futureObligations(
                        new DeployBudgetPolicy.FutureObligationFacts(
                                "maul", 5, 4, 0, 0, 0,
                                false, 0, false, false, 3));
        assertEquals(DeployBudgetPolicy.AdapterStep.FALL_THROUGH,
                breaking.adapterStep());
        assertSingle(breaking.result().operations(),
                "DEPLOY.BUDGET.OBJECTIVE_FORMATION_RESERVE", -500.0f);

        DeployBudgetPolicy.Evaluation affordable =
                DeployBudgetPolicy.futureObligations(
                        new DeployBudgetPolicy.FutureObligationFacts(
                                "cheap", 5, 2, 0, 0, 0,
                                false, 0, false, false, 3));
        assertEquals(DeployBudgetPolicy.AdapterStep.FALL_THROUGH,
                affordable.adapterStep());
        assertEquals(0, affordable.result().operations().size());
    }

    private static List<PolicyOperation> newMaintenance(int totalForce, int cost,
                                                        int maintenance,
                                                        int pending) {
        return DeployBudgetPolicy.newMaintenanceCard(
                new DeployBudgetPolicy.NewMaintenanceFacts(
                        "a", "Lando", true, totalForce, cost,
                        maintenance, pending, 2, 2))
                .result().operations();
    }

    private static void assertSingle(List<PolicyOperation> operations,
                                     String rule, float delta) {
        assertEquals(1, operations.size());
        assertOperation(operations.get(0), rule, delta);
    }

    private static void assertOperation(PolicyOperation operation,
                                        String rule, float delta) {
        assertEquals(rule, operation.ruleArmId().id());
        assertEquals(delta, operation.delta(), 0.0f);
    }
}
