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
                0.0f, false, 400.0f, "Gate Card",
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

    private static DeploySitingPolicy.Facts directFacts(
            boolean evazanWithoutArmedFriend, float v136Score,
            boolean v193Eligible, float v193PlaybookWeight,
            boolean v96Applicable, float friendlyPower, float opponentPower) {
        return new DeploySitingPolicy.Facts(
                "action-1", "Dr. Evazan", "Endor: Bunker",
                evazanWithoutArmedFriend, DeploySitingPolicy.FormationState.ALLOW, "", v136Score,
                v193Eligible, v193PlaybookWeight, "Establish Secret Base",
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
                v136Score, v193Eligible, v193PlaybookWeight,
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
