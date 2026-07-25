package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployActionTextPolicyTest {

    @Test
    public void amsdBespinAndRetryGatesContinueWithoutRecordingFailure() {
        assertAmsdBlocked(amsd(false, false,
                        DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL,
                        true, true, true, false, 1, 10),
                "V24-amsd-no-bespin", false,
                "V24 AMSD BLOCKED: No Bespin system on table — Star Destroyer has nowhere to deploy!");
        assertAmsdBlocked(amsd(true, true,
                        DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL,
                        true, true, true, false, 1, 10),
                "V24.10-amsd-retry-block", false,
                "V24.10 AMSD BLOCKED: Already failed this turn — save for next turn after recirculation!");
    }

    @Test
    public void amsdOracleUnavailableFallsThroughWithoutScoreOrMutation() {
        for (DeployActionTextFacts.AmsdActionKind kind : new DeployActionTextFacts.AmsdActionKind[] {
                DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL,
                DeployActionTextFacts.AmsdActionKind.PIETT_SPECIFIC}) {
            DeployActionTextPolicy.Evaluation evaluation = DeployActionTextPolicy.evaluateAmsd(
                    amsd(true, false, kind, false,
                            false, false, false, 4, 0));
            assertTrue(evaluation.result().operations().isEmpty());
            assertEquals(DeployActionTextPolicy.AdapterStep.FALL_THROUGH,
                    evaluation.adapterStep());
            assertFalse(evaluation.recordFailedTurn());
        }
    }

    @Test
    public void amsdNonPiettSpecificContinuesAndRequestsFailureMutation() {
        assertAmsdBlocked(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.OTHER_SPECIFIC,
                        false, false, false, false, 3, 10),
                "V24.10-amsd-non-piett-specific", true,
                "V24.10 AMSD BLOCKED: Only Piett may use AMSD — this action targets a different pilot!");
    }

    @Test
    public void amsdMissingPiettRetainsGenericAndSpecificReasons() {
        assertAmsdBlocked(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL,
                        true, false, true, false, 3, 10),
                "V24.10-amsd-missing-piett-generic", true,
                "V24.10 AMSD BLOCKED: Piett NOT in hand — can't use AMSD!");
        assertAmsdBlocked(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.PIETT_SPECIFIC,
                        true, false, true, false, 3, 10),
                "V24.10-amsd-missing-piett-specific", true,
                "V24.10 AMSD BLOCKED: Piett is NOT in hand — can't use AMSD!");
    }

    @Test
    public void amsdMissingExecutorRetainsGenericAndSpecificReasons() {
        assertAmsdBlocked(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL,
                        true, true, false, false, 3, 10),
                "V29.4-amsd-missing-executor-generic", true,
                "V29.4 AMSD BLOCKED: Piett in hand but Executor NOT in hand or reserve (may be in force/used pile)!");
        assertAmsdBlocked(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.PIETT_SPECIFIC,
                        true, true, false, false, 3, 10),
                "V29.4-amsd-missing-executor-specific", true,
                "V29.4 AMSD BLOCKED: Piett in hand but Executor NOT in hand or reserve!");
    }

    @Test
    public void amsdForceGateContinuesWithoutRecordingFailure() {
        assertAmsdBlocked(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL,
                        true, true, true, false, 3, 6),
                "V45-amsd-unaffordable", false,
                "V45 AMSD UNAFFORDABLE: Need 7 force for Piett+Executor but only 6 available!");
    }

    @Test
    public void amsdApprovedPathsRetainTurnBoundarySourceAndFallThrough() {
        assertAmsdApproved(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL,
                        true, true, true, true, 2, 7),
                "V24.15-amsd-approved-early-generic", 1500.0f,
                "V24.15 AMSD MEGA PRIORITY: Turn 2 — Executor (from hand) MUST deploy NOW to control Bespin!");
        assertAmsdApproved(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL,
                        true, true, false, true, 3, 7),
                "V24.10-amsd-approved-generic", 500.0f,
                "V24.10 AMSD APPROVED: Piett + Executor (from reserve) ready — fire AMSD!");
        assertAmsdApproved(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.PIETT_SPECIFIC,
                        true, true, true, false, 1, 7),
                "V24.15-amsd-approved-early-specific", 1500.0f,
                "V24.15 AMSD MEGA PRIORITY: Turn 1 — Executor (from hand) MUST deploy NOW!");
        assertAmsdApproved(amsd(true, false,
                        DeployActionTextFacts.AmsdActionKind.PIETT_SPECIFIC,
                        true, true, false, true, 3, 7),
                "V24.10-amsd-approved-specific", 500.0f,
                "V24.10 AMSD APPROVED: Piett + Executor (from reserve) ready!");
    }

    @Test
    public void dockingBayRetainsAllFourOrderedBranches() {
        assertOperation(DeployActionTextPolicy.scoreDockingBay(
                        new DeployActionTextFacts.DockingBayFacts("bay", 2, 3)),
                "V29.7-docking-bay-empty", -200.0f,
                "V29.7 DOCKING BAY: Already have 2 empty bay(s) — deploy characters there first, don't give opponent more locations!");
        assertOperation(DeployActionTextPolicy.scoreDockingBay(
                        new DeployActionTextFacts.DockingBayFacts("bay", 0, 2)),
                "V29.7-docking-bay-enough", -50.0f,
                "V29.7 DOCKING BAY: Already have 2 bays — enough for transit");
        assertOperation(DeployActionTextPolicy.scoreDockingBay(
                        new DeployActionTextFacts.DockingBayFacts("bay", 0, 0)),
                "V29.7-docking-bay-first", 200.0f,
                "V29.7 FIRST DOCKING BAY: Deploy FIRST to create battleground for characters!");
        assertOperation(DeployActionTextPolicy.scoreDockingBay(
                        new DeployActionTextFacts.DockingBayFacts("bay", 0, 1)),
                "V29.7-docking-bay-second", 30.0f,
                "V29.7 DOCKING BAY: Deploy second bay for transit network");
    }

    @Test
    public void vaderCastleDownloadRetainsOrdinaryDeployCostBudget() {
        assertOperation(DeployActionTextPolicy.scoreVaderCastle(
                        new DeployActionTextFacts.VaderCastleFacts(
                                "vader", false, false, false,
                                false, false, 0)),
                "V25-vader-castle-generic", 50.0f,
                "Deploy Vader from reserve");
        assertOperation(DeployActionTextPolicy.scoreVaderCastle(
                        new DeployActionTextFacts.VaderCastleFacts(
                                "vader", true, true, true,
                                false, false, 0)),
                "V25-vader-castle-vader-present", 0.0f,
                "Vader already on table — Castle deploy not urgent");
        PolicyResult lowForce = DeployActionTextPolicy.scoreVaderCastle(
                new DeployActionTextFacts.VaderCastleFacts(
                        "vader", true, true, false,
                        false, false, 8));
        assertEquals(1, lowForce.operations().size());
        assertEquals("V25-vader-castle-no-legal-candidate",
                lowForce.operations().get(0).ruleArmId().id());
        assertRawFloat(-500.0f, lowForce.operations().get(0).delta());
        assertOperation(DeployActionTextPolicy.scoreVaderCastle(
                        new DeployActionTextFacts.VaderCastleFacts(
                                "vader", true, true, false,
                                true, true, 7)),
                "V25-vader-castle-priority", 550.0f,
                "V25 HUNT DOWN: DEPLOY VADER NOW! Have 7 Force and preserve the Castle move cost");
        assertOperation(DeployActionTextPolicy.scoreVaderCastle(
                        new DeployActionTextFacts.VaderCastleFacts(
                                "vader", true, true, false,
                                true, false, 6)),
                "V25-vader-castle-deploy-only", 250.0f,
                "V25 HUNT DOWN: Deploy Vader now, but 6 Force cannot also preserve the Castle's exact move cost this turn");
    }

    @Test
    public void diningRoomLandoRetainsObjectiveAndBuddyBranches() {
        assertOperation(DeployActionTextPolicy.scoreDiningRoomLando(
                        new DeployActionTextFacts.DiningRoomLandoFacts("lando", true, true, 2)),
                "V29.6-dining-room-objective-safe", 150.0f,
                "V29.6 DINING ROOM: Deploy Lando with 2 friendlies — safe!");
        assertOperation(DeployActionTextPolicy.scoreDiningRoomLando(
                        new DeployActionTextFacts.DiningRoomLandoFacts("lando", true, true, 0)),
                "V29.6-dining-room-objective-alone", -30.0f,
                "V29.6 DINING ROOM: Lando would be ALONE — deploy a buddy first!");
        assertOperation(DeployActionTextPolicy.scoreDiningRoomLando(
                        new DeployActionTextFacts.DiningRoomLandoFacts("lando", false, false, 1)),
                "V29.6-dining-room-generic-safe", 30.0f,
                "Dining Room: Deploy Lando from reserve (friendlies present)");
        assertOperation(DeployActionTextPolicy.scoreDiningRoomLando(
                        new DeployActionTextFacts.DiningRoomLandoFacts("lando", true, false, 0)),
                "V29.6-dining-room-generic-alone", -20.0f,
                "V29.6 Dining Room: Lando alone — risky!");
    }

    @Test
    public void bespinShipAndSimultaneousDeployRetainExactDeltas() {
        assertOperation(DeployActionTextPolicy.scoreBespinShip(
                        new DeployActionTextFacts.BespinShipFacts("ship", false)),
                "V22.5-bespin-ship-critical", 300.0f,
                "V22.5 CRITICAL: Deploy ship to Bespin! Enables Dark Deal + CC Occupation!");
        assertOperation(DeployActionTextPolicy.scoreBespinShip(
                        new DeployActionTextFacts.BespinShipFacts("ship", true)),
                "V22.5-bespin-ship-present", 100.0f,
                "V22.5: Deploy ship (Bespin already occupied)");
        assertOperation(DeployActionTextPolicy.scoreSimultaneousDeploy(
                        new DeployActionTextFacts.ActionFacts("simultaneous")),
                "V22.5-simultaneous-deploy", 120.0f,
                "V22.5: Deploy pilot+ship combo - efficient!");
    }

    @Test
    public void mainGeneratorRetainsFlipEnginePriority() {
        PolicyResult result = DeployActionTextPolicy.scoreMainGenerator(
                new DeployActionTextFacts.MainGeneratorFacts("generator"));
        assertOperation(result, "V160-main-generator", 800.0f,
                "V160 PUSH TARGET THE MAIN GENERATOR: deck's flip engine — deploy/fire to enable AT-AT vs Main Power Generators");
        assertEquals(TraceOutputKind.ORDERING,
                result.operations().get(0).outputKind());
    }

    @Test
    public void genericDeployRetainsLateArmScores() {
        PolicyResult projection = DeployActionTextPolicy.scoreGenericDeploy(
                new DeployActionTextFacts.GenericDeployFacts(
                        "deploy",
                        DeployActionTextFacts.GenericDeployKind.PROJECTION_ON_SIDE));
        assertOperation(projection, "generic-deploy-projection", -50.0f,
                "Never put projection on side of table");
        assertEquals(TraceOutputKind.VETO,
                projection.operations().get(0).outputKind());

        PolicyResult deployOn = DeployActionTextPolicy.scoreGenericDeploy(
                new DeployActionTextFacts.GenericDeployFacts(
                        "deploy",
                        DeployActionTextFacts.GenericDeployKind.DEPLOY_ON));
        assertOperation(deployOn, "generic-deploy-on", 30.0f,
                "Deploy on location/table");
        assertEquals(TraceOutputKind.BANDED,
                deployOn.operations().get(0).outputKind());

        PolicyResult unique = DeployActionTextPolicy.scoreGenericDeploy(
                new DeployActionTextFacts.GenericDeployFacts(
                        "deploy",
                        DeployActionTextFacts.GenericDeployKind.DEPLOY_UNIQUE));
        assertOperation(unique, "generic-deploy-unique", 30.0f,
                "Special battleground deploy");
        assertEquals(TraceOutputKind.BANDED,
                unique.operations().get(0).outputKind());
    }

    @Test
    public void genericPlayCardRetainsForceBoundary() {
        assertOperation(DeployActionTextPolicy.scoreGenericPlayCard(
                        new DeployActionTextFacts.PlayCardFacts("play", 0)),
                "generic-play-card-no-force", -50.0f,
                "No Force available - can't play cards!");
        assertOperation(DeployActionTextPolicy.scoreGenericPlayCard(
                        new DeployActionTextFacts.PlayCardFacts("play", 1)),
                "generic-play-card-low-force", -30.0f,
                "Very low Force (1) - unlikely to afford cards");
        assertOperation(DeployActionTextPolicy.scoreGenericPlayCard(
                        new DeployActionTextFacts.PlayCardFacts("play", 2)),
                "generic-play-card", 5.0f,
                "Generic play card — moderate priority");
    }

    @Test
    public void legacyDeployFallbackRetainsExactBandsAndThresholds() {
        assertEquals(100,
                DeployActionTextPolicy.scoreLegacyFallbackDeployLocation(100));

        assertEquals(90, DeployActionTextPolicy.scoreLegacyFallbackReinforce(
                80, -5.0f, true));
        assertEquals(105, DeployActionTextPolicy.scoreLegacyFallbackReinforce(
                80, -5.01f, true));
        assertEquals(95, DeployActionTextPolicy.scoreLegacyFallbackReinforce(
                80, -6.0f, false));

        assertEquals(0, DeployActionTextPolicy.scoreLegacyFallbackGainGround(
                60, false, true, 9.0f));
        assertEquals(75, DeployActionTextPolicy.scoreLegacyFallbackGainGround(
                60, true, true, 8.0f));
        assertEquals(65, DeployActionTextPolicy.scoreLegacyFallbackGainGround(
                60, true, true, 8.01f));
        assertEquals(60, DeployActionTextPolicy.scoreLegacyFallbackGainGround(
                60, true, false, 8.0f));

        assertEquals(5, DeployActionTextPolicy.scoreLegacyFallbackDomainMatch(
                true, false));
        assertEquals(0, DeployActionTextPolicy.scoreLegacyFallbackDomainMatch(
                false, false));
        assertEquals(-20, DeployActionTextPolicy.scoreLegacyFallbackDomainMatch(
                false, true));
        assertEquals(-15, DeployActionTextPolicy.scoreLegacyFallbackDomainMatch(
                true, true));

        assertEquals(40,
                DeployActionTextPolicy.scoreLegacyFallbackMatchingPilot(40));
    }

    private static DeployActionTextFacts.AmsdFacts amsd(
            boolean bespin,
            boolean alreadyFailed,
            DeployActionTextFacts.AmsdActionKind actionKind,
            boolean oracleAnalyzed,
            boolean piettInHand,
            boolean executorInHand,
            boolean executorInReserve,
            int currentTurn,
            int forceAvailable) {
        return new DeployActionTextFacts.AmsdFacts(
                "amsd", bespin, alreadyFailed, actionKind, oracleAnalyzed,
                piettInHand, executorInHand, executorInReserve,
                currentTurn, forceAvailable);
    }

    private static void assertAmsdBlocked(
            DeployActionTextFacts.AmsdFacts facts,
            String ruleId,
            boolean recordFailedTurn,
            String reason) {
        DeployActionTextPolicy.Evaluation evaluation =
                DeployActionTextPolicy.evaluateAmsd(facts);
        assertEquals(DeployActionTextPolicy.AdapterStep.CONTINUE_ACTION,
                evaluation.adapterStep());
        assertEquals(recordFailedTurn, evaluation.recordFailedTurn());
        assertOperation(evaluation.result(), ruleId, -9999.0f, reason);
    }

    private static void assertAmsdApproved(
            DeployActionTextFacts.AmsdFacts facts,
            String ruleId,
            float delta,
            String reason) {
        DeployActionTextPolicy.Evaluation evaluation =
                DeployActionTextPolicy.evaluateAmsd(facts);
        assertEquals(DeployActionTextPolicy.AdapterStep.FALL_THROUGH,
                evaluation.adapterStep());
        assertFalse(evaluation.recordFailedTurn());
        assertOperation(evaluation.result(), ruleId, delta, reason);
    }

    private static void assertOperation(
            PolicyResult result,
            String ruleId,
            float delta,
            String reason) {
        assertEquals("DEPLOY_ACTION_TEXT_POLICY", result.producerId());
        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertEquals(ruleId, operation.ruleArmId().id());
        assertRawFloat(delta, operation.delta());
        assertEquals(reason, operation.reason());
        assertEquals(TraceDomainId.DEPLOY_SEQUENCING, operation.domainId());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
    }

    private static void assertRawFloat(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }
}
