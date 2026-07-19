package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeployCardValuePolicyTest {

    @Test
    public void ratioBandsRetainExactBoundaryScores() {
        assertBase(4, 0, 2, 0.0f,
                "deploy-value-excellent", 40.0f);
        assertBase(3, 0, 2, 0.0f,
                "deploy-value-good", 20.0f);
        assertBase(2, 0, 2, 0.0f,
                "deploy-value-average", 0.0f);
        assertBase(1, 0, 2, 0.0f,
                "V40-deploy-value-below-average", 0.0f);
        assertBase(2, 0, 0, 0.0f,
                "deploy-value-excellent", 40.0f);
    }

    @Test
    public void destinyAndEliteStackAfterRatioInLegacyOrder() {
        PolicyResult result = DeployCardValuePolicy.scoreBase(
                new DeployCardValueFacts.BaseValue(
                        "value", 3, 3, 3, 5.0f));
        assertRules(result.operations(),
                "deploy-value-excellent", "deploy-high-destiny");
        assertDeltas(result.operations(), 40.0f, 15.0f);
        assertDeltas(DeployCardValuePolicy.scoreElite(
                new DeployCardValueFacts.EliteValue("value", true))
                .operations(), 100.0f);
        assertTrue(DeployCardValuePolicy.scoreElite(
                new DeployCardValueFacts.EliteValue("value", false))
                .operations().isEmpty());

        PolicyResult belowDestiny = DeployCardValuePolicy.scoreBase(
                new DeployCardValueFacts.BaseValue(
                        "value", 3, 3, 3, 4.9f));
        assertEquals(1, belowDestiny.operations().size());
    }

    @Test
    public void typeValueRetainsCharacterAbilityBoundary() {
        assertDeltas(DeployCardValuePolicy.scoreType(
                new DeployCardValueFacts.TypeValue("type", true))
                .operations(), 25.0f);
        assertTrue(DeployCardValuePolicy.scoreType(
                new DeployCardValueFacts.TypeValue("type", false))
                .operations().isEmpty());
    }

    @Test
    public void strategicBonusesRemainIndependentAndOrdered() {
        assertDeltas(DeployCardValuePolicy.scoreStrategic(
                new DeployCardValueFacts.Strategic(
                        "strategic", true, true)).operations(),
                20.0f, 30.0f);
        assertDeltas(DeployCardValuePolicy.scoreStrategic(
                new DeployCardValueFacts.Strategic(
                        "strategic", true, false)).operations(),
                20.0f);
        assertDeltas(DeployCardValuePolicy.scoreStrategic(
                new DeployCardValueFacts.Strategic(
                        "strategic", false, true)).operations(),
                30.0f);
        assertTrue(DeployCardValuePolicy.scoreStrategic(
                new DeployCardValueFacts.Strategic(
                        "strategic", false, false)).operations().isEmpty());
    }

    @Test
    public void destinationAbilityPreservesEveryLegacyBoundary() {
        assertDestinationAbility(6.0f, "V29.7-destination-ability-high", 50.0f,
                "V29.7 HIGH ABILITY: Ability 6 — strong location control!");
        assertDestinationAbility(Math.nextDown(6.0f),
                "V29.7-destination-ability", 25.0f,
                "V29.7 ABILITY: Ability 5 — good for control");
        assertDestinationAbility(5.0f, "V29.7-destination-ability", 25.0f,
                "V29.7 ABILITY: Ability 5 — good for control");
        assertDestinationAbility(Math.nextDown(5.0f),
                "V29.7-destination-ability", 5.0f,
                "V29.7 ABILITY: Ability 4");
        assertDestinationAbility(3.0f, "V29.7-destination-ability", 5.0f,
                "V29.7 ABILITY: Ability 3");
        assertTrue(DeployCardValuePolicy.scoreDestinationAbility(
                new DeployCardValueFacts.DestinationAbility(
                        "ability", 2.99f)).operations().isEmpty());
        assertTrue(DeployCardValuePolicy.scoreDestinationAbility(
                new DeployCardValueFacts.DestinationAbility(
                        "ability", 1.0f)).operations().isEmpty());
        assertDestinationAbility(0.99f, "V29.7-destination-ability-low", -30.0f,
                "V29.7 LOW ABILITY: Ability 0 — weak presence");
        assertDestinationAbility(0.0f, "V29.7-destination-ability-low", -30.0f,
                "V29.7 LOW ABILITY: Ability 0 — weak presence");
    }

    private static void assertBase(
            int power,
            int ability,
            int cost,
            float destiny,
            String ruleId,
            float delta) {
        PolicyOperation operation = DeployCardValuePolicy.scoreBase(
                new DeployCardValueFacts.BaseValue(
                        "value", power, ability, cost, destiny))
                .operations().get(0);
        assertEquals(ruleId, operation.ruleArmId().id());
        assertRaw(delta, operation.delta());
    }

    private static void assertDestinationAbility(
            float ability, String ruleId, float delta, String reason) {
        PolicyOperation operation = DeployCardValuePolicy.scoreDestinationAbility(
                new DeployCardValueFacts.DestinationAbility(
                        "ability", ability)).operations().get(0);
        assertEquals(ruleId, operation.ruleArmId().id());
        assertRaw(delta, operation.delta());
        assertEquals(reason, operation.reason());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(TraceDomainId.DEPLOY_SITING, operation.domainId());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
    }

    private static void assertRules(
            List<PolicyOperation> operations, String... expected) {
        assertEquals(expected.length, operations.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], operations.get(i).ruleArmId().id());
        }
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
