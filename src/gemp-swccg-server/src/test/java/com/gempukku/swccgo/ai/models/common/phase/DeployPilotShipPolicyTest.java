package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeployPilotShipPolicyTest {
    @Test
    public void matchingPilotPreservesExclusiveStateOrderAndObjectiveStack() {
        assertRules(DeployPilotShipPolicy.evaluateMatchingPilot(
                        new DeployPilotShipPolicy.MatchingPilotFacts(
                                "a", "Piett", "Executor", true, true,
                                true, true, "Bespin")).operations(),
                new String[]{"V30-pilot-combo", "V30-pilot-objective"},
                new float[]{1000.0f, 1000.0f});
        assertRules(DeployPilotShipPolicy.evaluateMatchingPilot(
                        new DeployPilotShipPolicy.MatchingPilotFacts(
                                "a", "Piett", "Executor", false, true,
                                true, true, "Bespin")).operations(),
                new String[]{"V30-pilot-in-play"}, new float[]{300.0f});
        assertRules(DeployPilotShipPolicy.evaluateMatchingPilot(
                        new DeployPilotShipPolicy.MatchingPilotFacts(
                                "a", "Piett", "Executor", false, false,
                                true, true, "")).operations(),
                new String[]{"V30-pilot-amsd"}, new float[]{-500.0f});
    }

    @Test
    public void matchingShipStacksComboAndObjectiveOnlyWhenPilotIsInHand() {
        assertRules(DeployPilotShipPolicy.evaluateMatchingShip(
                        new DeployPilotShipPolicy.MatchingShipFacts(
                                "a", "Executor", "Piett", true, "Bespin")).operations(),
                new String[]{"V30-ship-combo", "V30-ship-objective"},
                new float[]{1000.0f, 1000.0f});
        assertTrue(DeployPilotShipPolicy.evaluateMatchingShip(
                new DeployPilotShipPolicy.MatchingShipFacts(
                        "a", "Executor", "Piett", false, "Bespin"))
                .operations().isEmpty());
    }

    @Test
    public void crewPolicyPreservesUnavailableAndUnaffordableReasons() {
        assertRules(DeployPilotShipPolicy.evaluateCrew(
                        new DeployPilotShipPolicy.CrewFacts(
                                "a", true, false, false, false,
                                5, 4, false, "")).operations(),
                new String[]{"V30-crew-required"}, new float[]{-1500.0f});
        List<PolicyOperation> unaffordable = DeployPilotShipPolicy.evaluateCrew(
                new DeployPilotShipPolicy.CrewFacts(
                        "a", true, true, false, false,
                        5, 4, false, "")).operations();
        assertRules(unaffordable,
                new String[]{"V30-crew-required"}, new float[]{-1500.0f});
        assertTrue(unaffordable.get(0).reason().contains("unaffordable"));
        assertTrue(DeployPilotShipPolicy.evaluateCrew(
                new DeployPilotShipPolicy.CrewFacts(
                        "a", true, true, true, false,
                        5, 8, false, "")).operations().isEmpty());
    }

    @Test
    public void crewPolicyBoostsLowPowerPilotForUnmannedAsset() {
        assertRules(DeployPilotShipPolicy.evaluateCrew(
                        new DeployPilotShipPolicy.CrewFacts(
                                "a", false, false, false, false,
                                0, 0, true, "Blizzard 2")).operations(),
                new String[]{"V30-crew-unmanned"}, new float[]{400.0f});
    }

    @Test
    public void shipAbilityPreservesNamedPilotThenWarningOrder() {
        assertRules(DeployPilotShipPolicy.evaluateShipAbility(
                        new DeployPilotShipPolicy.ShipAbilityFacts(
                                "a", "Executor", 2.0f, true,
                                "Piett", 2.0f, 4.0f, true)).operations(),
                new String[]{"V35.6-named-pilot", "V35.6-ability"},
                new float[]{300.0f, -50.0f});
        assertRules(DeployPilotShipPolicy.evaluateShipAbility(
                        new DeployPilotShipPolicy.ShipAbilityFacts(
                                "a", "First Light", 2.0f, false,
                                "", 0.0f, 2.0f, false)).operations(),
                new String[]{"V35.6-ability"}, new float[]{-50.0f});
        assertTrue(DeployPilotShipPolicy.evaluateShipAbility(
                new DeployPilotShipPolicy.ShipAbilityFacts(
                        "a", "Executor", 4.0f, false,
                        "", 0.0f, 4.0f, false)).operations().isEmpty());
    }

    @Test
    public void shipThreatPreservesStrictOnePointFiveBoundary() {
        assertTrue(DeployPilotShipPolicy.evaluateShipThreat(
                new DeployPilotShipPolicy.ShipThreatFacts(
                        "a", "Shuttle", "Bespin", 4.0f, 6.0f))
                .operations().isEmpty());
        assertRules(DeployPilotShipPolicy.evaluateShipThreat(
                        new DeployPilotShipPolicy.ShipThreatFacts(
                                "a", "Shuttle", "Bespin", 4.0f, 6.01f)).operations(),
                new String[]{"V35.5"}, new float[]{-100.0f});
    }

    @Test
    public void v121PreservesCorrectAndWrongDestinationScores() {
        assertRules(DeployPilotShipPolicy.evaluateObjectivePilotDestination(
                        new DeployPilotShipPolicy.ObjectivePilotDestinationFacts(
                                "a", true, "Trade Federation Droid Control Ship",
                                "Bridge", false)).operations(),
                new String[]{"V121"}, new float[]{-1500.0f});
        assertRules(DeployPilotShipPolicy.evaluateObjectivePilotDestination(
                        new DeployPilotShipPolicy.ObjectivePilotDestinationFacts(
                                "a", true, "Trade Federation Droid Control Ship",
                                "Trade Federation Droid Control Ship", true)).operations(),
                new String[]{"V121"}, new float[]{300.0f});
    }

    @Test
    public void assetTailPreservesExecutorAndBespinAdditiveOrder() {
        assertRules(DeployPilotShipPolicy.evaluateAssetTail(
                        new DeployPilotShipPolicy.AssetTailFacts(
                                "a", "Executor", true, true, true,
                                false, 1, false, true,
                                false, false, false, false)).operations(),
                new String[]{"asset-base", "V24.10", "V23"},
                new float[]{15.0f, -9999.0f, 300.0f});
        assertRules(DeployPilotShipPolicy.evaluateAssetTail(
                        new DeployPilotShipPolicy.AssetTailFacts(
                                "a", "Executor", true, true, true,
                                true, 2, false, false,
                                false, false, false, false)).operations(),
                new String[]{"asset-base", "V24.9", "V23"},
                new float[]{15.0f, 800.0f, 250.0f});
    }

    @Test
    public void assetTailPreservesPilotAndMatchingOrder() {
        assertRules(DeployPilotShipPolicy.evaluateAssetTail(
                        new DeployPilotShipPolicy.AssetTailFacts(
                                "a", "Piett", false, false, false,
                                false, 1, false, false,
                                true, true, false, true)).operations(),
                new String[]{"pilot-base", "V47", "matching-pilot-base"},
                new float[]{10.0f, -9999.0f, 30.0f});
        assertRules(DeployPilotShipPolicy.evaluateAssetTail(
                        new DeployPilotShipPolicy.AssetTailFacts(
                                "a", "Piett", false, false, false,
                                false, 1, false, false,
                                true, true, true, false)).operations(),
                new String[]{"pilot-base", "V40.1"},
                new float[]{10.0f, 300.0f});
    }

    private static void assertRules(List<PolicyOperation> operations,
                                    String[] rules, float[] deltas) {
        assertEquals(rules.length, operations.size());
        for (int i = 0; i < rules.length; i++) {
            assertEquals(rules[i], operations.get(i).ruleArmId().id());
            assertEquals(deltas[i], operations.get(i).delta(), 0.0f);
            assertTrue(operations.get(i).domainId() == TraceDomainId.DEPLOY_ATTACH
                    || operations.get(i).domainId() == TraceDomainId.DEPLOY_SITING);
        }
    }
}
