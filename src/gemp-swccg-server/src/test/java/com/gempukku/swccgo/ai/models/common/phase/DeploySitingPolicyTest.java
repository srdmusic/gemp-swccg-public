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

public class DeploySitingPolicyTest {
    @Test
    public void directRoutePreservesFullAdditiveOrder() {
        PolicyResult result = DeploySitingPolicy.evaluateDirect(directFacts(
                true, -275.0f, true, 400.0f,
                true, 8.0f, 7.0f));

        assertEquals("DEPLOY_SITING_DIRECT_POLICY", result.producerId());
        assertOperations(result.operations(),
                new String[]{"V89", "V136", "V193", "V96"},
                new float[]{-1500.0f, -275.0f, 400.0f, 500.0f},
                new PolicyOperationKind[]{PolicyOperationKind.ADD, PolicyOperationKind.ADD,
                        PolicyOperationKind.ADD, PolicyOperationKind.ADD});
        assertOutputKinds(result.operations(),
                TraceOutputKind.VETO, TraceOutputKind.BANDED,
                TraceOutputKind.BANDED, TraceOutputKind.BANDED);
        assertSitingMetadata(result.operations());
    }

    @Test
    public void destinationRoutePreservesFullControlAndAdditiveOrder() {
        PolicyResult result = DeploySitingPolicy.evaluateDestination(destinationFacts(
                true, DeploySitingPolicy.FormationState.HARD_BLOCK,
                "buddy plan cannot be funded", 325.0f, true, 400.0f));

        assertEquals("DEPLOY_SITING_DESTINATION_POLICY", result.producerId());
        assertOperations(result.operations(),
                new String[]{"V89-CS", "FS-L3-solo-deploy-hard", "V136-CS", "V193-CS"},
                new float[]{-1500.0f, 0.0f, 325.0f, 2000.0f},
                new PolicyOperationKind[]{PolicyOperationKind.ADD, PolicyOperationKind.HARD_VETO,
                        PolicyOperationKind.ADD, PolicyOperationKind.ADD});
        assertEquals(TraceDomainId.SOLO_FORMATION,
                result.operations().get(1).domainId());
        assertOutputKinds(result.operations(),
                TraceOutputKind.VETO, TraceOutputKind.VETO,
                TraceOutputKind.BANDED, TraceOutputKind.BANDED);
    }

    @Test
    public void formationVerdictsMapWithoutSuppressingLaterScores() {
        PolicyResult defer = DeploySitingPolicy.evaluateDestination(destinationFacts(
                false, DeploySitingPolicy.FormationState.DEFER_UNSUPPORTED_SOLO,
                "no exact same-site buddy plan", 150.0f, true, 400.0f));
        assertOperations(defer.operations(),
                new String[]{"V201-deploy-siting", "V136-CS", "V193-CS"},
                new float[]{-800.0f, 150.0f, 2000.0f},
                new PolicyOperationKind[]{PolicyOperationKind.DEFER, PolicyOperationKind.ADD,
                        PolicyOperationKind.ADD});

        PolicyResult unknown = DeploySitingPolicy.evaluateDestination(destinationFacts(
                false, DeploySitingPolicy.FormationState.UNKNOWN,
                "facts incomplete", 150.0f, false, 400.0f));
        assertOperations(unknown.operations(),
                new String[]{"V201-deploy-siting-unknown", "V136-CS"},
                new float[]{0.0f, 150.0f},
                new PolicyOperationKind[]{PolicyOperationKind.ADD, PolicyOperationKind.ADD});
        assertOutputKinds(unknown.operations(),
                TraceOutputKind.BANDED, TraceOutputKind.BANDED);
        assertTrue(unknown.operations().get(0).reason().contains("facts incomplete"));

        PolicyResult allow = DeploySitingPolicy.evaluateDestination(destinationFacts(
                false, DeploySitingPolicy.FormationState.ALLOW,
                "", 150.0f, false, 400.0f));
        assertOperations(allow.operations(),
                new String[]{"V136-CS"}, new float[]{150.0f},
                new PolicyOperationKind[]{PolicyOperationKind.ADD});
    }

    @Test(expected = IllegalArgumentException.class)
    public void activeFormationVerdictRequiresReason() {
        destinationFacts(false, DeploySitingPolicy.FormationState.HARD_BLOCK,
                "", 0.0f, false, 400.0f);
    }

    @Test
    public void v193PreservesDeliberateRouteAsymmetry() {
        PolicyOperation direct = DeploySitingPolicy.evaluateDirect(directFacts(
                false, 0.0f, true, 475.0f,
                false, 0.0f, 0.0f)).operations().get(0);
        PolicyOperation destination = DeploySitingPolicy.evaluateDestination(destinationFacts(
                false, DeploySitingPolicy.FormationState.ALLOW,
                "", 0.0f, true, 475.0f)).operations().get(0);

        assertEquals("V193", direct.ruleArmId().id());
        assertEquals(475.0f, direct.delta(), 0.0f);
        assertEquals("V193-CS", destination.ruleArmId().id());
        assertEquals(2075.0f, destination.delta(), 0.0f);
    }

    @Test
    public void invasionFlipGateWeightRequiresExistingOrFundedBuddy() {
        DeploySitingPolicy.Facts unsupported = new DeploySitingPolicy.Facts(
                "action-1", "Neimoidian", "Naboo: Theed Palace Throne Room",
                false, DeploySitingPolicy.FormationState.DEFER_UNSUPPORTED_SOLO,
                "no exact same-site buddy plan", 0.0f,
                true, false, 1600.0f, "Neimoidian at Throne Room",
                false, 0.0f, 0.0f);
        PolicyResult unsupportedDirect =
                DeploySitingPolicy.evaluateDirect(unsupported);
        PolicyResult unsupportedDestination =
                DeploySitingPolicy.evaluateDestination(unsupported);

        assertTrue(unsupportedDirect.operations().isEmpty());
        assertOperations(unsupportedDestination.operations(),
                new String[]{"V201-deploy-siting"}, new float[]{-800.0f},
                new PolicyOperationKind[]{PolicyOperationKind.DEFER});

        DeploySitingPolicy.Facts supported = new DeploySitingPolicy.Facts(
                "action-1", "Neimoidian", "Naboo: Theed Palace Throne Room",
                false, DeploySitingPolicy.FormationState.ALLOW, "", 0.0f,
                true, true, 1600.0f, "Neimoidian at Throne Room",
                false, 0.0f, 0.0f);
        assertEquals(1600.0f, DeploySitingPolicy.evaluateDirect(supported)
                .operations().get(0).delta(), 0.0f);
        assertEquals(3200.0f, DeploySitingPolicy.evaluateDestination(supported)
                .operations().get(0).delta(), 0.0f);
    }

    @Test
    public void v136ZeroIsSilentButNegativeAndPositiveScoresAreApplied() {
        assertTrue(DeploySitingPolicy.evaluateDirect(directFacts(
                false, 0.0f, false, 400.0f,
                false, 0.0f, 0.0f)).operations().isEmpty());
        assertEquals(-900.0f, DeploySitingPolicy.evaluateDirect(directFacts(
                false, -900.0f, false, 400.0f,
                false, 0.0f, 0.0f)).operations().get(0).delta(), 0.0f);
        assertEquals(600.0f, DeploySitingPolicy.evaluateDestination(destinationFacts(
                false, DeploySitingPolicy.FormationState.ALLOW,
                "", 600.0f, false, 400.0f)).operations().get(0).delta(), 0.0f);
    }

    @Test
    public void v89RemainsAdditiveBadSiteScoreOnBothRoutes() {
        PolicyOperation direct = DeploySitingPolicy.evaluateDirect(directFacts(
                true, 0.0f, false, 400.0f,
                false, 0.0f, 0.0f)).operations().get(0);
        PolicyOperation destination = DeploySitingPolicy.evaluateDestination(destinationFacts(
                true, DeploySitingPolicy.FormationState.ALLOW,
                "", 0.0f, false, 400.0f)).operations().get(0);

        assertEquals(PolicyOperationKind.ADD, direct.kind());
        assertEquals(-1500.0f, direct.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, destination.kind());
        assertEquals(-1500.0f, destination.delta(), 0.0f);
    }

    @Test
    public void v96PreservesEveryPowerBoundary() {
        assertV96(null, true, 20.0f, 0.0f);
        assertV96(null, false, 20.0f, 10.0f);
        assertV96(500.0f, true, 0.0f, 10.0f);
        assertV96(500.0f, true, 20.0f, 10.0f);
        assertV96(100.0f, true, 20.01f, 10.0f);
        assertV96(null, true, 0.0f, 10.01f);
    }

    private static void assertV96(Float expected, boolean applicable,
                                  float friendlyPower, float opponentPower) {
        List<PolicyOperation> operations = DeploySitingPolicy.evaluateDirect(directFacts(
                false, 0.0f, false, 400.0f,
                applicable, friendlyPower, opponentPower)).operations();
        if (expected == null) {
            assertTrue(operations.isEmpty());
        } else {
            assertEquals(1, operations.size());
            assertEquals("V96", operations.get(0).ruleArmId().id());
            assertEquals(expected, operations.get(0).delta(), 0.0f);
        }
    }

    @Test
    public void directAndDestinationEntryPointsIgnoreOtherRouteInputs() {
        DeploySitingPolicy.Facts facts = new DeploySitingPolicy.Facts(
                "action-1", "Character", "Site", false,
                DeploySitingPolicy.FormationState.HARD_BLOCK, "direct ignores formation",
                0.0f, false, true, 400.0f, "Gate Card",
                true, 10.0f, 10.0f);

        PolicyResult direct = DeploySitingPolicy.evaluateDirect(facts);
        assertOperations(direct.operations(),
                new String[]{"V96"}, new float[]{500.0f},
                new PolicyOperationKind[]{PolicyOperationKind.ADD});

        PolicyResult destination = DeploySitingPolicy.evaluateDestination(facts);
        assertOperations(destination.operations(),
                new String[]{"FS-L3-solo-deploy-hard"}, new float[]{0.0f},
                new PolicyOperationKind[]{PolicyOperationKind.HARD_VETO});
    }

    @Test
    public void shipReferenceGroundPenaltyKeepsExactScoreAndReason() {
        PolicyOperation operation = DeploySitingPolicy.evaluateShipReferenceGround(
                new DeploySitingPolicy.ShipReferenceGroundFacts(
                        "action-1", "executor")).operations().get(0);

        assertEquals("V29-ship-ground", operation.ruleArmId().id());
        assertEquals(-200.0f, operation.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
        assertEquals("V29 SHIP CHARACTER ON GROUND: Game text mentions executor — should deploy to space!",
                operation.reason());
        assertTrue(DeploySitingPolicy.evaluateShipReferenceGround(
                new DeploySitingPolicy.ShipReferenceGroundFacts(
                        "action-1", null)).operations().isEmpty());
    }

    @Test
    public void starshipDestinationPreservesEveryLegacyTier() {
        assertDestination(DeploySitingPolicy.StarshipDestinationState.SITE_BLOCKED,
                -1500.0f, TraceOutputKind.VETO,
                "⚠️ STARSHIP TO SITE = 0 POWER! (V190: ships deploy to systems)",
                0.0f, 0.0f);
        assertDestination(DeploySitingPolicy.StarshipDestinationState.SPACE_FALLBACK,
                20.0f, TraceOutputKind.BANDED,
                "Starship to space system", 0.0f, 0.0f);
        assertDestination(DeploySitingPolicy.StarshipDestinationState.SPACE_UNCONTESTED,
                30.0f, TraceOutputKind.BANDED,
                "Uncontested space system", 0.0f, 0.0f);
        assertDestination(DeploySitingPolicy.StarshipDestinationState.SPACE_DISADVANTAGE,
                -80.0f, TraceOutputKind.BANDED,
                "⚠️ SPACE POWER DISADVANTAGE: 4 vs 7 after deploy", 4.0f, 7.0f);
        assertDestination(DeploySitingPolicy.StarshipDestinationState.SPACE_ADVANTAGE,
                30.0f, TraceOutputKind.BANDED,
                "Good space position: 10 vs 7 after deploy", 10.0f, 7.0f);
        assertDestination(DeploySitingPolicy.StarshipDestinationState.SPACE_CLOSE,
                10.0f, TraceOutputKind.BANDED,
                "Close space fight: 8 vs 7 after deploy", 8.0f, 7.0f);
        assertTrue(DeploySitingPolicy.evaluateStarshipDestination(
                new DeploySitingPolicy.StarshipDestinationFacts(
                        "action-1", DeploySitingPolicy.StarshipDestinationState.NONE,
                        0.0f, 0.0f)).operations().isEmpty());
    }

    @Test
    public void vehicleDestinationPreservesSpaceInteriorAndExteriorTiers() {
        assertVehicle(DeploySitingPolicy.VehicleDestinationState.SPACE_INVALID,
                -150.0f, "VEHICLE TO SPACE - invalid!");
        assertVehicle(DeploySitingPolicy.VehicleDestinationState.INTERIOR_INVALID,
                -150.0f, "VEHICLE TO INTERIOR-ONLY - can't deploy!");
        assertVehicle(DeploySitingPolicy.VehicleDestinationState.EXTERIOR_VALID,
                10.0f, "Vehicle to exterior ground - good");
        assertTrue(DeploySitingPolicy.evaluateVehicleDestination(
                new DeploySitingPolicy.VehicleDestinationFacts(
                        "action-1", DeploySitingPolicy.VehicleDestinationState.NONE))
                .operations().isEmpty());
    }

    @Test
    public void permanentWeaponDestinationPreservesSpaceAndGroundTiers() {
        PolicyOperation space = DeploySitingPolicy.evaluatePermanentWeaponDestination(
                new DeploySitingPolicy.PermanentWeaponDestinationFacts(
                        "action-1",
                        DeploySitingPolicy.PermanentWeaponDestinationState.SPACE))
                .operations().get(0);
        assertEquals(-300.0f, space.delta(), 0.0f);
        assertEquals("V24.14B WEAPON CHAR TO SPACE: Permanent weapon can't fire at system locations — useless in space!",
                space.reason());

        PolicyOperation ground = DeploySitingPolicy.evaluatePermanentWeaponDestination(
                new DeploySitingPolicy.PermanentWeaponDestinationFacts(
                        "action-1",
                        DeploySitingPolicy.PermanentWeaponDestinationState.GROUND))
                .operations().get(0);
        assertEquals(100.0f, ground.delta(), 0.0f);
        assertEquals("V24.14B WEAPON CHAR ON GROUND: Strong battle presence — weapon fires here!",
                ground.reason());
    }

    @Test
    public void emptyBayAndBattlegroundBonusesRemainIndependent() {
        PolicyOperation emptyBay = DeploySitingPolicy.evaluateEmptyDockingBay(
                new DeploySitingPolicy.EmptyDockingBayFacts(
                        "action-1", true)).operations().get(0);
        PolicyOperation battleground = DeploySitingPolicy.evaluateBattlegroundLocation(
                new DeploySitingPolicy.BattlegroundLocationFacts(
                        "action-1", true)).operations().get(0);

        assertEquals(80.0f, emptyBay.delta(), 0.0f);
        assertEquals("V29.7-empty-bay", emptyBay.ruleArmId().id());
        assertEquals(50.0f, battleground.delta(), 0.0f);
        assertEquals("V29.6-battleground", battleground.ruleArmId().id());
        assertTrue(DeploySitingPolicy.evaluateEmptyDockingBay(
                new DeploySitingPolicy.EmptyDockingBayFacts(
                        "action-1", false)).operations().isEmpty());
        assertTrue(DeploySitingPolicy.evaluateBattlegroundLocation(
                new DeploySitingPolicy.BattlegroundLocationFacts(
                        "action-1", false)).operations().isEmpty());
    }

    @Test
    public void opponentForceIconBonusPreservesZeroAndLinearScores() {
        assertTrue(DeploySitingPolicy.evaluateOpponentForceIcons(
                new DeploySitingPolicy.OpponentForceIconsFacts(
                        "action-1", 0)).operations().isEmpty());

        PolicyOperation one = DeploySitingPolicy.evaluateOpponentForceIcons(
                new DeploySitingPolicy.OpponentForceIconsFacts(
                        "action-1", 1)).operations().get(0);
        assertEquals("V23-force-icons", one.ruleArmId().id());
        assertEquals(30.0f, one.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, one.kind());
        assertEquals(TraceDomainId.DEPLOY_SITING, one.domainId());
        assertEquals(TraceOutputKind.BANDED, one.outputKind());
        assertEquals("V23 FORCE DRAIN: 1 opponent force icon(s) — better drain target!",
                one.reason());

        PolicyOperation three = DeploySitingPolicy.evaluateOpponentForceIcons(
                new DeploySitingPolicy.OpponentForceIconsFacts(
                        "action-1", 3)).operations().get(0);
        assertEquals(90.0f, three.delta(), 0.0f);
        assertEquals("V23 FORCE DRAIN: 3 opponent force icon(s) — better drain target!",
                three.reason());
    }

    @Test
    public void mapuzoDestinationPreservesSurvivorDefenseAndTrapBranches() {
        assertTrue(DeploySitingPolicy.evaluateMapuzoDestination(
                new DeploySitingPolicy.MapuzoDestinationFacts(
                        "action-1", "Mapuzo: Streets", true, 0.0f))
                .operations().isEmpty());

        PolicyOperation defense = DeploySitingPolicy.evaluateMapuzoDestination(
                new DeploySitingPolicy.MapuzoDestinationFacts(
                        "action-1", "Mapuzo: Streets", false, 0.01f))
                .operations().get(0);
        assertEquals("V64-mapuzo-defense", defense.ruleArmId().id());
        assertEquals(30.0f, defense.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, defense.kind());
        assertEquals(TraceOutputKind.BANDED, defense.outputKind());
        assertEquals("V64 MAPUZO DEFENSE: Opponent at Mapuzo: Streets"
                        + " (power 0) — non-Jedi defender OK here",
                defense.reason());

        PolicyOperation trap = DeploySitingPolicy.evaluateMapuzoDestination(
                new DeploySitingPolicy.MapuzoDestinationFacts(
                        "action-1", null, false, 0.0f))
                .operations().get(0);
        assertEquals("V64-mapuzo-trap", trap.ruleArmId().id());
        assertEquals(-1500.0f, trap.delta(), 0.0f);
        assertEquals(PolicyOperationKind.ADD, trap.kind());
        assertEquals(TraceOutputKind.VETO, trap.outputKind());
        assertEquals("V64 MAPUZO TRAP: Non-Jedi character at null"
                        + " will be STUCK — only Jedi Survivors transit off Mapuzo!",
                trap.reason());
    }

    private static void assertDestination(
            DeploySitingPolicy.StarshipDestinationState state,
            float expectedDelta, TraceOutputKind expectedKind,
            String expectedReason, float projectedPower, float opponentPower) {
        PolicyOperation operation = DeploySitingPolicy.evaluateStarshipDestination(
                new DeploySitingPolicy.StarshipDestinationFacts(
                        "action-1", state, projectedPower, opponentPower))
                .operations().get(0);
        assertEquals(expectedDelta, operation.delta(), 0.0f);
        assertEquals(expectedKind, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(expectedReason, operation.reason());
        assertEquals(TraceDomainId.DEPLOY_SITING, operation.domainId());
    }

    private static void assertVehicle(
            DeploySitingPolicy.VehicleDestinationState state,
            float expectedDelta, String expectedReason) {
        PolicyOperation operation = DeploySitingPolicy.evaluateVehicleDestination(
                new DeploySitingPolicy.VehicleDestinationFacts(
                        "action-1", state)).operations().get(0);
        assertEquals(expectedDelta, operation.delta(), 0.0f);
        assertEquals(expectedReason, operation.reason());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(TraceOutputKind.BANDED, operation.outputKind());
    }

    private static DeploySitingPolicy.Facts directFacts(
            boolean evazanWithoutArmedFriend, float v136Score,
            boolean v193Eligible, float v193PlaybookWeight,
            boolean v96Applicable, float friendlyPower, float opponentPower) {
        return new DeploySitingPolicy.Facts(
                "action-1", "Dr. Evazan", "Endor: Bunker",
                evazanWithoutArmedFriend, DeploySitingPolicy.FormationState.ALLOW, "", v136Score,
                v193Eligible, true, v193PlaybookWeight, "Establish Secret Base",
                v96Applicable, friendlyPower, opponentPower);
    }

    private static DeploySitingPolicy.Facts destinationFacts(
            boolean evazanWithoutArmedFriend,
            DeploySitingPolicy.FormationState formationState,
            String formationReason, float v136Score,
            boolean v193Eligible, float v193PlaybookWeight) {
        return new DeploySitingPolicy.Facts(
                "action-1", "Dr. Evazan", "Endor: Bunker",
                evazanWithoutArmedFriend, formationState, formationReason,
                v136Score, v193Eligible, true, v193PlaybookWeight,
                "Establish Secret Base", false, 0.0f, 0.0f);
    }

    private static void assertSitingMetadata(List<PolicyOperation> operations) {
        for (PolicyOperation operation : operations) {
            assertEquals("action-1", operation.actionId());
            assertEquals(TraceDomainId.DEPLOY_SITING, operation.domainId());
        }
    }

    private static void assertOperations(List<PolicyOperation> operations,
                                         String[] rules, float[] deltas,
                                         PolicyOperationKind[] kinds) {
        assertEquals(rules.length, operations.size());
        for (int i = 0; i < rules.length; i++) {
            assertEquals(rules[i], operations.get(i).ruleArmId().id());
            assertEquals(deltas[i], operations.get(i).delta(), 0.0f);
            assertEquals(kinds[i], operations.get(i).kind());
        }
    }

    private static void assertOutputKinds(List<PolicyOperation> operations,
                                          TraceOutputKind... kinds) {
        assertEquals(kinds.length, operations.size());
        for (int i = 0; i < kinds.length; i++) {
            assertEquals(kinds[i], operations.get(i).outputKind());
        }
    }
}
