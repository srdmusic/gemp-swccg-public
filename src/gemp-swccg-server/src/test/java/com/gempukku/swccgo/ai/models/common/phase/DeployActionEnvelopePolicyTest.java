package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeployActionEnvelopePolicyTest {

    @Test
    public void parentClassificationPreservesInitialAndAdditiveScores() {
        DeployActionEnvelopePolicy.Evaluation blocked =
                DeployActionEnvelopePolicy.evaluateParent(
                        new DeployActionEnvelopeFacts.ParentAction(
                                "blocked", true, false));
        assertRaw(-9999.0f, blocked.initialScore());
        assertDeltas(blocked.result().operations(), -9999.0f);
        assertEquals(TraceDomainId.LOOP_SAFETY,
                blocked.result().operations().get(0).domainId());
        assertEquals(DeployActionEnvelopePolicy.AdapterStep.CONTINUE_ACTION,
                blocked.adapterStep());

        DeployActionEnvelopePolicy.Evaluation persona =
                DeployActionEnvelopePolicy.evaluateParent(
                        new DeployActionEnvelopeFacts.ParentAction(
                                "persona", false, true));
        assertRaw(-500.0f, persona.initialScore());
        assertDeltas(persona.result().operations(), -500.0f);
        assertEquals(DeployActionEnvelopePolicy.AdapterStep.CONTINUE_ACTION,
                persona.adapterStep());

        DeployActionEnvelopePolicy.Evaluation normal =
                DeployActionEnvelopePolicy.evaluateParent(
                        new DeployActionEnvelopeFacts.ParentAction(
                                "normal", false, false));
        assertRaw(50.0f, normal.initialScore());
        assertTrue(normal.result().operations().isEmpty());
        assertEquals(DeployActionEnvelopePolicy.AdapterStep.FALL_THROUGH,
                normal.adapterStep());
    }

    @Test
    public void resolvedEffectAndNonLocationRetainTerminalBoundary() {
        DeployActionEnvelopePolicy.Evaluation blocked =
                DeployActionEnvelopePolicy.evaluateTitleGate(
                        new DeployActionEnvelopeFacts.TitleGate(
                                "resolved", true));
        assertDeltas(blocked.result().operations(), -9999.0f);
        assertEquals("deploy-turn-one-effect-block",
                blocked.result().operations().get(0).ruleArmId().id());
        assertEquals(DeployActionEnvelopePolicy.AdapterStep.CONTINUE_ACTION,
                blocked.adapterStep());

        DeployActionEnvelopePolicy.Evaluation normal =
                DeployActionEnvelopePolicy.evaluateTitleGate(
                        new DeployActionEnvelopeFacts.TitleGate(
                                "resolved", false));
        assertTrue(normal.result().operations().isEmpty());
        assertEquals(DeployActionEnvelopePolicy.AdapterStep.FALL_THROUGH,
                normal.adapterStep());
    }

    @Test
    public void unknownFallbackRetainsAllNeutralAndTerminalBranches() {
        DeployActionEnvelopePolicy.Evaluation location =
                DeployActionEnvelopePolicy.evaluateUnknown(
                        new DeployActionEnvelopeFacts.UnknownAction(
                                "unknown", true, false, 1));
        assertDeltas(location.result().operations(), 200.0f, 0.0f);
        assertEquals("V29: Location deploy \u2014 always allowed!",
                location.result().operations().get(0).reason());
        assertEquals("V40: Unknown card (deploy from reserve?) \u2014 deploy freely",
                location.result().operations().get(1).reason());
        assertEquals(DeployActionEnvelopePolicy.AdapterStep.FALL_THROUGH,
                location.adapterStep());

        DeployActionEnvelopePolicy.Evaluation turnOnePlan =
                DeployActionEnvelopePolicy.evaluateUnknown(
                        new DeployActionEnvelopeFacts.UnknownAction(
                                "unknown", false, true, 1));
        assertDeltas(turnOnePlan.result().operations(), 0.0f);
        assertEquals(DeployActionEnvelopePolicy.AdapterStep.CONTINUE_ACTION,
                turnOnePlan.adapterStep());

        DeployActionEnvelopePolicy.Evaluation laterPlan =
                DeployActionEnvelopePolicy.evaluateUnknown(
                        new DeployActionEnvelopeFacts.UnknownAction(
                                "unknown", false, true, 2));
        assertDeltas(laterPlan.result().operations(), 0.0f, 0.0f);

        DeployActionEnvelopePolicy.Evaluation generic =
                DeployActionEnvelopePolicy.evaluateUnknown(
                        new DeployActionEnvelopeFacts.UnknownAction(
                                "unknown", false, false, 2));
        assertDeltas(generic.result().operations(), 0.0f);
        assertEquals("V40: Unknown card (deploy from reserve?) \u2014 deploy freely",
                generic.result().operations().get(0).reason());
    }

    private static void assertDeltas(
            List<PolicyOperation> operations, float... expected) {
        assertEquals(expected.length, operations.size());
        for (int i = 0; i < expected.length; i++) {
            assertRaw(expected[i], operations.get(i).delta());
        }
    }

    private static void assertRaw(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(actual));
    }
}
