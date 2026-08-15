package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DeploySequencingPolicyTest {
    @Test
    public void phaseEnvelopePreservesUrgencyAndObligationOrder() {
        DeploySequencingPolicy.Evaluation evaluation =
                DeploySequencingPolicy.phaseEnvelope(
                        "a", 12, 10, 2,
                        new DeploySequencingFacts.PowerGap("Endor", 4, 8),
                        new DeploySequencingFacts.PowerGap("Hoth", 9, 5));

        assertOperations(evaluation.result().operations(),
                new String[]{"V38.4", "V169-deploy-umbrella", "V176"},
                new float[]{300.0f, 500.0f, -800.0f});
        assertEquals(DeploySequencingPolicy.AdapterStep.FALL_THROUGH,
                evaluation.adapterStep());
        assertNull(evaluation.scoreOverride());
    }

    @Test
    public void urgencyPreservesEveryHandAndForceBoundary() {
        assertFirstDelta(DeploySequencingPolicy.phaseEnvelope("a", 0, 20, 20, null, null), null);
        assertFirstDelta(DeploySequencingPolicy.phaseEnvelope("a", 1, 5, 20, null, null), 50.0f);
        assertFirstDelta(DeploySequencingPolicy.phaseEnvelope("a", 5, 5, 20, null, null), 80.0f);
        assertFirstDelta(DeploySequencingPolicy.phaseEnvelope("a", 7, 6, 20, null, null), 160.0f);
        assertFirstDelta(DeploySequencingPolicy.phaseEnvelope("a", 9, 9, 20, null, null), 100.0f);
        assertFirstDelta(DeploySequencingPolicy.phaseEnvelope("a", 11, 10, 20, null, null), 260.0f);
        assertFirstDelta(DeploySequencingPolicy.phaseEnvelope("a", 13, 10, 20, null, null), 350.0f);
    }

    @Test
    public void winnableBattleOnlyReservesAtTwoOrLessForce() {
        DeploySequencingFacts.PowerGap battle =
                new DeploySequencingFacts.PowerGap("Cloud City", 8, 3);
        assertEquals(0, DeploySequencingPolicy.phaseEnvelope(
                "a", 0, 0, 3, null, battle).result().operations().size());
        assertEquals(-800.0f, DeploySequencingPolicy.phaseEnvelope(
                "a", 0, 0, 2, null, battle).result().operations().get(0).delta(), 0.0f);
    }

    @Test
    public void locationOrderPreservesHealthyAndLowLifeScores() {
        DeploySequencingPolicy.Evaluation healthy =
                DeploySequencingPolicy.locationFromHand("a", true, 11, "Bespin");
        assertOperations(healthy.result().operations(),
                new String[]{"V162", "V67ai"},
                new float[]{500.0f, 1400.0f});

        DeploySequencingPolicy.Evaluation low =
                DeploySequencingPolicy.locationFromHand("a", true, 10, "Bespin");
        assertOperations(low.result().operations(),
                new String[]{"V162"}, new float[]{-200.0f});
        assertEquals(0, DeploySequencingPolicy.locationFromHand(
                "a", false, 99, "Bespin").result().operations().size());
    }

    @Test
    public void tailScriptsStayAdditiveAndOrdered() {
        DeploySequencingPolicy.TailFacts facts = new DeploySequencingPolicy.TailFacts(
                "a", 1, 3, true, false, true,
                "Luke", "luke", "deploy a jedi survivor",
                true, 6.0f);
        List<PolicyOperation> operations =
                DeploySequencingPolicy.tailScripts(facts).result().operations();
        assertOperations(operations,
                new String[]{"V52-momentum", "V55", "V52b"},
                new float[]{200.0f, 500.0f, 300.0f});
    }

    @Test
    public void tdigwattAndSkywalkerScriptsPreserveElseIfSelection() {
        DeploySequencingPolicy.TailFacts tdigwatt = new DeploySequencingPolicy.TailFacts(
                "a", 1, 0, true, false, false,
                "Bespin", "bespin", "deploy system", false, 0.0f);
        assertOperations(DeploySequencingPolicy.tailScripts(tdigwatt).result().operations(),
                new String[]{"V52-TDIGWATT-T1"}, new float[]{300.0f});

        DeploySequencingPolicy.TailFacts skywalker = new DeploySequencingPolicy.TailFacts(
                "a", 2, 0, false, true, false,
                "Tatooine: Cantina", "tatooine: cantina", "deploy",
                false, 0.0f);
        assertOperations(DeploySequencingPolicy.tailScripts(skywalker).result().operations(),
                new String[]{"V54"}, new float[]{1275.0f});
    }

    @Test
    public void actionTextPoliciesPreserveV53cDoubleBlockContract() {
        DeploySequencingPolicy.Evaluation wokling =
                DeploySequencingPolicy.woklingEarlySearch("a", true, true, 3);
        assertEquals(DeploySequencingPolicy.AdapterStep.CONTINUE_ACTION,
                wokling.adapterStep());
        assertEquals(-9999.0f, wokling.scoreOverride(), 0.0f);
        assertEquals(-9999.0f, wokling.result().operations().get(0).delta(), 0.0f);

        assertEquals(-800.0f, DeploySequencingPolicy.locationsFirstNonDeploy(
                "a", true, false).result().operations().get(0).delta(), 0.0f);
        assertEquals(0, DeploySequencingPolicy.locationsFirstNonDeploy(
                "a", true, true).result().operations().size());
    }

    private static void assertFirstDelta(DeploySequencingPolicy.Evaluation evaluation,
                                         Float expected) {
        if (expected == null) {
            assertEquals(0, evaluation.result().operations().size());
        } else {
            assertEquals(expected, evaluation.result().operations().get(0).delta(), 0.0f);
        }
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
