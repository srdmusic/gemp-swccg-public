package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
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
                                "a", true, false, false, false, false, false,
                                5, 4, false, "")).operations(),
                new String[]{"V30-crew-required"}, new float[]{-1500.0f});
        List<PolicyOperation> unaffordable = DeployPilotShipPolicy.evaluateCrew(
                new DeployPilotShipPolicy.CrewFacts(
                        "a", true, false, false, true, false, false,
                        5, 4, false, "")).operations();
        assertRules(unaffordable,
                new String[]{"V30-crew-required"}, new float[]{-1500.0f});
        assertTrue(unaffordable.get(0).reason().contains("unaffordable"));
        assertTrue(DeployPilotShipPolicy.evaluateCrew(
                new DeployPilotShipPolicy.CrewFacts(
                        "a", true, false, false, true, true, false,
                        5, 8, false, "")).operations().isEmpty());
        assertTrue(DeployPilotShipPolicy.evaluateCrew(
                new DeployPilotShipPolicy.CrewFacts(
                        "a", true, true, false, false, false, false,
                        5, 4, false, "")).operations().isEmpty());
        assertTrue(DeployPilotShipPolicy.evaluateCrew(
                new DeployPilotShipPolicy.CrewFacts(
                        "a", true, false, true, true, false, false,
                        5, 4, false, "")).operations().isEmpty());
    }

    @Test
    public void crewPolicyBoostsLowPowerPilotForUnmannedAsset() {
        assertRules(DeployPilotShipPolicy.evaluateCrew(
                        new DeployPilotShipPolicy.CrewFacts(
                                "a", false, false, false, false, false, false,
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

    @Test
    public void genericPilotCandidatePreservesAbilityPowerCostOrder() {
        List<PolicyOperation> operations = DeployPilotShipPolicy.evaluatePilotCandidate(
                new DeployPilotShipPolicy.PilotCandidateFacts(
                        "a", 3.0f, 4.0f, 2.0f)).operations();

        assertRules(operations,
                new String[]{"pilot-ability", "pilot-power", "pilot-deploy-cost"},
                new float[]{30.0f, 20.0f, 20.0f});
        assertEquals("Ability 3", operations.get(0).reason());
        assertEquals("Good power bonus (4)", operations.get(1).reason());
        assertEquals("Deploy cost 2", operations.get(2).reason());

        List<PolicyOperation> zeroCostScore =
                DeployPilotShipPolicy.evaluatePilotCandidate(
                        new DeployPilotShipPolicy.PilotCandidateFacts(
                                "a", null, 2.0f, 8.0f)).operations();
        assertRules(zeroCostScore,
                new String[]{"pilot-deploy-cost"}, new float[]{0.0f});
    }

    @Test
    public void executorDestinationPreservesBespinAndWrongSystemScores() {
        PolicyOperation bespin = DeployPilotShipPolicy.evaluateExecutorDestination(
                new DeployPilotShipPolicy.ExecutorDestinationFacts(
                        "a", true, "Bespin"))
                .operations().get(0);
        assertEquals("V24.10-executor-bespin", bespin.ruleArmId().id());
        assertEquals(500.0f, bespin.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, bespin.kind());
        assertEquals(TraceDomainId.DEPLOY_SITING, bespin.domainId());
        assertEquals(TraceOutputKind.BANDED, bespin.outputKind());
        assertEquals("V24.10 EXECUTOR TO BESPIN: This is THE correct system — entire TDIGWATT engine depends on it!",
                bespin.reason());

        PolicyOperation wrong = DeployPilotShipPolicy.evaluateExecutorDestination(
                new DeployPilotShipPolicy.ExecutorDestinationFacts(
                        "a", false, "Kashyyyk"))
                .operations().get(0);
        assertEquals("V24.10-executor-wrong-system", wrong.ruleArmId().id());
        assertEquals(-9999.0f, wrong.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, wrong.kind());
        assertEquals(TraceDomainId.DEPLOY_SITING, wrong.domainId());
        assertEquals(TraceOutputKind.VETO, wrong.outputKind());
        assertEquals("V24.10 EXECUTOR WRONG SYSTEM: Executor MUST go to Bespin, not Kashyyyk!",
                wrong.reason());
    }

    @Test
    public void shipReferenceBoardingPreservesSixHundredAndDrainBonus() {
        DeployPilotShipPolicy.Evaluation ordinary =
                DeployPilotShipPolicy.evaluateShipBoarding(
                        new DeployPilotShipPolicy.ShipBoardingFacts(
                                "a", true, false, "executor", true,
                                true, false));
        assertRules(ordinary.result().operations(),
                new String[]{"V29-ship-reference"}, new float[]{600.0f});
        assertEquals(DeployPilotShipPolicy.AdapterStep.FALL_THROUGH,
                ordinary.adapterStep());
        assertEquals(null, ordinary.resetScore());

        DeployPilotShipPolicy.Evaluation drain =
                DeployPilotShipPolicy.evaluateShipBoarding(
                        new DeployPilotShipPolicy.ShipBoardingFacts(
                                "a", true, false, "executor", true,
                                true, true));
        assertRules(drain.result().operations(),
                new String[]{"V29-ship-reference"}, new float[]{650.0f});
        assertEquals("V29 SHIP-REF: Game text mentions executor"
                        + " — abilities activate aboard this ship!",
                drain.result().operations().get(0).reason());
    }

    @Test
    public void otherCharacterBoardingBranchesRemainExclusive() {
        assertRules(DeployPilotShipPolicy.evaluateShipBoarding(
                        new DeployPilotShipPolicy.ShipBoardingFacts(
                                "a", true, false, "home one", false,
                                true, false)).result().operations(),
                new String[]{"V29-other-ship-reference"},
                new float[]{50.0f});
        assertRules(DeployPilotShipPolicy.evaluateShipBoarding(
                        new DeployPilotShipPolicy.ShipBoardingFacts(
                                "a", true, false, null, false,
                                true, false)).result().operations(),
                new String[]{"V29-executor"}, new float[]{100.0f});
        assertRules(DeployPilotShipPolicy.evaluateShipBoarding(
                        new DeployPilotShipPolicy.ShipBoardingFacts(
                                "a", true, false, "", false,
                                false, false)).result().operations(),
                new String[]{"V29-character-aboard"}, new float[]{50.0f});
    }

    @Test
    public void cargoPenaltyRemainsAdditiveThenContinuesCandidate() {
        DeployPilotShipPolicy.Evaluation cargo =
                DeployPilotShipPolicy.evaluateShipBoarding(
                        new DeployPilotShipPolicy.ShipBoardingFacts(
                                "a", false, false, "", false,
                                false, false));

        assertRules(cargo.result().operations(),
                new String[]{"V29-cargo"}, new float[]{-300.0f});
        PolicyOperation operation = cargo.result().operations().get(0);
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(TraceDomainId.DEPLOY_ATTACH, operation.domainId());
        assertEquals(TraceOutputKind.VETO, operation.outputKind());
        assertEquals("⚠️ DEPLOY TO CARGO BAY = 0 POWER!", operation.reason());
        assertEquals(DeployPilotShipPolicy.AdapterStep.CONTINUE_CANDIDATE,
                cargo.adapterStep());
        assertEquals(null, cargo.resetScore());
    }

    @Test
    public void legalAttachedCardIsNotMisclassifiedAsCargo() {
        DeployPilotShipPolicy.Evaluation attachment =
                DeployPilotShipPolicy.evaluateShipBoarding(
                        new DeployPilotShipPolicy.ShipBoardingFacts(
                                "a", false, true, "", false,
                                false, false));

        assertTrue(attachment.result().operations().isEmpty());
        assertEquals(DeployPilotShipPolicy.AdapterStep.FALL_THROUGH,
                attachment.adapterStep());
    }

    @Test
    public void simultaneousStarDestroyerGuardPreservesSetThenAddContract() {
        DeployPilotShipPolicy.Evaluation blocked =
                DeployPilotShipPolicy.evaluateSimultaneousPilotGuard(
                        new DeployPilotShipPolicy.SimultaneousPilotGuardFacts(
                                "a", true, false, false));

        assertEquals(DeployPilotShipPolicy.AdapterStep.CONTINUE_CANDIDATE,
                blocked.adapterStep());
        assertEquals(-500.0f, blocked.resetScore(), 0.0f);
        assertRules(blocked.result().operations(),
                new String[]{"pilot-sd-block"}, new float[]{-500.0f});
        assertEquals(TraceOutputKind.VETO,
                blocked.result().operations().get(0).outputKind());

        DeployPilotShipPolicy.Evaluation valid =
                DeployPilotShipPolicy.evaluateSimultaneousPilotGuard(
                        new DeployPilotShipPolicy.SimultaneousPilotGuardFacts(
                                "a", true, true, false));
        assertEquals(DeployPilotShipPolicy.AdapterStep.FALL_THROUGH,
                valid.adapterStep());
        assertEquals(null, valid.resetScore());
        assertRules(valid.result().operations(),
                new String[]{"pilot-sd-valid"}, new float[]{100.0f});
    }

    @Test
    public void simultaneousPlannedPilotSuppressesQualityAndMatching() {
        List<PolicyOperation> operations =
                DeployPilotShipPolicy.evaluateSimultaneousPilotChoice(
                        new DeployPilotShipPolicy.SimultaneousPilotChoiceFacts(
                                "a", "Executor", true,
                                1.0f, 6.0f, true)).operations();

        assertRules(operations,
                new String[]{"pilot-plan-match"}, new float[]{200.0f});
        assertEquals("PLANNED pilot for Executor", operations.get(0).reason());
    }

    @Test
    public void simultaneousUnplannedPilotPreservesCostAbilityMatchingOrder() {
        List<PolicyOperation> operations =
                DeployPilotShipPolicy.evaluateSimultaneousPilotChoice(
                        new DeployPilotShipPolicy.SimultaneousPilotChoiceFacts(
                                "a", "Executor", false,
                                2.0f, 4.0f, true)).operations();

        assertRules(operations,
                new String[]{"pilot-deploy-cost", "pilot-ability", "pilot-ship-match"},
                new float[]{20.0f, 40.0f, 50.0f});
        assertEquals("Deploy cost 2", operations.get(0).reason());
        assertEquals("Ability 4", operations.get(1).reason());
        assertEquals("Matching pilot for Executor!", operations.get(2).reason());
    }

    @Test
    public void lowAbilityPilotBoardsWhenCompatibleAssetIsOffered() {
        assertRules(DeployPilotShipPolicy.evaluateLowAbilityPilotBoarding(
                        new DeployPilotShipPolicy.LowAbilityPilotBoardingFacts(
                                "a", true, 4.99f, true, true)).operations(),
                new String[]{"V30-low-ability-pilot-boarding"},
                new float[]{3000.0f});
        assertRules(DeployPilotShipPolicy.evaluateLowAbilityPilotBoarding(
                        new DeployPilotShipPolicy.LowAbilityPilotBoardingFacts(
                                "a", true, 4.99f, true, false)).operations(),
                new String[]{"V30-low-ability-pilot-boarding"},
                new float[]{-5000.0f});
    }

    @Test
    public void abilityFivePilotAndNoAssetOfferRemainUnforced() {
        assertTrue(DeployPilotShipPolicy.evaluateLowAbilityPilotBoarding(
                new DeployPilotShipPolicy.LowAbilityPilotBoardingFacts(
                        "a", true, 5.0f, true, false)).operations().isEmpty());
        assertTrue(DeployPilotShipPolicy.evaluateLowAbilityPilotBoarding(
                new DeployPilotShipPolicy.LowAbilityPilotBoardingFacts(
                        "a", true, 3.0f, false, false)).operations().isEmpty());
    }

    @Test
    public void fullPassengerOnlyAssetsDoNotBorrowPilotProtection() {
        assertTrue(DeployPilotShipPolicy.evaluateLowAbilityPilotBoarding(
                new DeployPilotShipPolicy.LowAbilityPilotBoardingFacts(
                        "full-walker", true, 3.0f,
                        false, true)).operations().isEmpty());
    }

    @Test
    public void simultaneousPilotCannotConsumeReservedEopBunkerGarrison() {
        List<PolicyOperation> operations =
                DeployPilotShipPolicy.evaluateSimultaneousPilotChoice(
                        new DeployPilotShipPolicy.SimultaneousPilotChoiceFacts(
                                "ozzel", "Any ship", false,
                                2.0f, 2.0f, false, true))
                        .operations();

        assertRules(operations,
                new String[]{"DEPLOY.EOP.BUNKER_GARRISON_RESERVE"},
                new float[]{-9999.0f});
        assertEquals(TraceOutputKind.VETO,
                operations.get(0).outputKind());
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
