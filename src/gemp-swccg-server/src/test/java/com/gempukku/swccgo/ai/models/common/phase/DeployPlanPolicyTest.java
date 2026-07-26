package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
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
    public void turnOneNonTdigwattLocationPlanFallsThrough() {
        DeployPlanPolicy.Evaluation evaluation = DeployPlanPolicy.evaluate(
                new DeployPlanPolicy.Facts(
                        "a", true, true, false, false, 99,
                        false, false, true, false,
                        false, 1, 6, false, 0,
                        false, "locations"));
        assertEquals(DeployPlanPolicy.AdapterStep.FALL_THROUGH,
                evaluation.adapterStep());
        assertOperations(evaluation.result().operations(),
                new String[]{"V40-plan-non-tdigwatt"}, new float[]{0.0f});
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

    @Test
    public void destinationTargetPreservesMatchAndMismatchScores() {
        PolicyOperation match = DeployPlanPolicy.evaluateDestinationTarget(
                new DeployPlanPolicy.DestinationTargetFacts(
                        "a", true, true, "Bespin")).operations().get(0);
        assertEquals("deploy-plan-target-match", match.ruleArmId().id());
        assertEquals(200.0f, match.delta(), 0.0f);
        assertEquals("PLANNED TARGET: Bespin", match.reason());
        assertEquals(PolicyOperationKind.ADD, match.kind());
        assertEquals(TraceDomainId.DEPLOY_SEQUENCING, match.domainId());
        assertEquals(TraceOutputKind.ORDERING, match.outputKind());

        List<PolicyOperation> offeredOther = DeployPlanPolicy.evaluateDestinationTarget(
                new DeployPlanPolicy.DestinationTargetFacts(
                        "a", false, true, null)).operations();
        PolicyOperation other = offeredOther.get(0);
        assertEquals("deploy-plan-target-other", other.ruleArmId().id());
        assertEquals(-100.0f, other.delta(), 0.0f);
        assertEquals("Not planned target (want null)", other.reason());
        assertEquals(2, offeredOther.size());
        assertEquals("deploy-plan-target-defer", offeredOther.get(1).ruleArmId().id());
        assertEquals(PolicyOperationKind.DEFER, offeredOther.get(1).kind());

        List<PolicyOperation> unavailableOther = DeployPlanPolicy.evaluateDestinationTarget(
                new DeployPlanPolicy.DestinationTargetFacts(
                        "a", false, false, "Bespin")).operations();
        assertEquals(1, unavailableOther.size());
        assertEquals(PolicyOperationKind.ADD, unavailableOther.get(0).kind());
    }

    @Test
    public void objectiveFormationTieBreakAddsOnlyTwentyFiveToPlannedCard() {
        DeployPlanPolicy.Evaluation offPlan = DeployPlanPolicy.evaluate(
                new DeployPlanPolicy.Facts(
                        "maul", true, true, false, true,
                        Integer.MAX_VALUE, false, false, false, false,
                        false, 5, 8, false, 0, false, "reinforce"));
        assertEquals(DeployPlanPolicy.AdapterStep.FALL_THROUGH,
                offPlan.adapterStep());
        assertEquals(1, offPlan.result().operations().size());
        assertEquals("V40-plan-off-plan",
                offPlan.result().operations().get(0).ruleArmId().id());
        assertEquals(PolicyOperationKind.ADD,
                offPlan.result().operations().get(0).kind());

        DeployPlanPolicy.Evaluation buddy = DeployPlanPolicy.evaluate(
                new DeployPlanPolicy.Facts(
                        "buddy", true, true, true, true,
                        2, false, false, false, false,
                        false, 5, 8, false, 0, false, "reinforce"));
        assertEquals(DeployPlanPolicy.AdapterStep.FALL_THROUGH,
                buddy.adapterStep());
        assertOperations(buddy.result().operations(),
                new String[]{"deploy-plan-membership", "deploy-plan-priority",
                        "DEPLOY.FORMATION.OBJECTIVE_TIE_BREAK"},
                new float[]{100.0f, 25.0f, 25.0f});
    }

    @Test
    public void eopBunkerGarrisonDominatesObservedShipComboGap() {
        DeployPlanPolicy.Evaluation evaluation =
                DeployPlanPolicy.evaluate(
                        new DeployPlanPolicy.Facts(
                                "ozzel", true, true, true,
                                false, true, 1,
                                false, false, false, false,
                                false, 3, 8, false, 0,
                                false, "establish"));
        assertOperations(evaluation.result().operations(),
                new String[]{
                        "deploy-plan-membership",
                        "deploy-plan-priority",
                        "DEPLOY.EOP.BUNKER_GARRISON"},
                new float[]{100.0f, 50.0f, 2500.0f});
    }

    private static DeployPlanPolicy.Facts facts(
            boolean hasPlan, boolean pending, boolean planned, int priority,
            boolean allowExtras, boolean waiting, boolean locationStrategy,
            boolean locationCard, int turn, int force, boolean complete,
            int extraBudget, boolean holdBack) {
        return new DeployPlanPolicy.Facts(
                "a", hasPlan, pending, planned, false,
                priority, allowExtras, waiting,
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
