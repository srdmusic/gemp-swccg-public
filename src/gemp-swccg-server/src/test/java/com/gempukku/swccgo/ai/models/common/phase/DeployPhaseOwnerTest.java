package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.strategy.ForceObligationVector;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Exact compatibility wire through one typed DEPLOY finalizer. */
public class DeployPhaseOwnerTest {

    @Test
    public void optionalBlockedParentOverridesSelectedCompatibilityWireWithPass() {
        Owned owned = owned(AwaitingDecisionType.CARD_ACTION_CHOICE,
                DeployTestFixtures.parent(false, false), DeployRoute.DEPLOY_PARENT,
                null, DeployFormationAssessment.Verdict.ALL_DESTINATIONS_BLOCKED);
        AiDecisionResult result = decide(owned, "1");

        assertAccepted(result, "");
    }

    @Test
    public void mandatoryBlockedParentUsesExactLeastBadCompatibilityWire() {
        Map<String, String[]> params = DeployTestFixtures.parent(false, false);
        DeployTestFixtures.put(params, "noPass", "true");
        Owned owned = owned(AwaitingDecisionType.CARD_ACTION_CHOICE,
                params, DeployRoute.DEPLOY_PARENT, null,
                DeployFormationAssessment.Verdict.ALL_DESTINATIONS_BLOCKED);

        assertAccepted(decide(owned, "1"), "1");
        assertRejected(decide(owned, ""));
    }

    @Test
    public void parentAndDestinationPreserveExactWire() {
        Owned parent = owned(AwaitingDecisionType.CARD_ACTION_CHOICE,
                DeployTestFixtures.parent(false, false), DeployRoute.DEPLOY_PARENT, null);
        Owned destination = owned(AwaitingDecisionType.CARD_SELECTION,
                DeployTestFixtures.destination(false), DeployRoute.DEPLOY_DESTINATION, null);

        assertAccepted(decide(parent, "1"), "1");
        assertAccepted(decide(destination, "302"), "302");
    }

    @Test
    public void arbitraryBuddyPreservesTempWire() {
        Owned owned = owned(AwaitingDecisionType.ARBITRARY_CARDS,
                DeployTestFixtures.buddy(AwaitingDecisionType.ARBITRARY_CARDS),
                DeployRoute.DEPLOY_BUDDY, null);

        assertAccepted(decide(owned, "temp2"), "temp2");
        assertRejected(decide(owned, "temp0"));
    }

    @Test
    public void capacityAndConfirmationPreserveOrdinalWire() {
        Owned capacity = owned(AwaitingDecisionType.MULTIPLE_CHOICE,
                DeployTestFixtures.choice(DecisionOrigin.DEPLOY_CAPACITY, true),
                DeployRoute.DEPLOY_CAPACITY, null);
        Owned confirmation = owned(AwaitingDecisionType.MULTIPLE_CHOICE,
                DeployTestFixtures.confirmation(),
                DeployRoute.DEPLOY_CONFIRMATION, null);

        assertAccepted(decide(capacity, "0"), "0");
        assertAccepted(decide(confirmation, "1"), "1");
    }

    @Test
    public void v170UsesSourceProvenOrdinalInsteadOfCompatibilityLane() {
        Owned owned = owned(AwaitingDecisionType.MULTIPLE_CHOICE,
                DeployTestFixtures.choice(
                        DecisionOrigin.DEPLOY_V170_UNDERCOVER, false),
                DeployRoute.DEPLOY_V170_UNDERCOVER, 1);
        AiDecisionResult result = DeployPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.facts(),
                owned.assessment(), (route, facts, assessment) -> "0");

        assertAccepted(result, "1");
    }

    @Test
    public void unknownCompatibilityWireIsRejected() {
        Owned owned = owned(AwaitingDecisionType.CARD_SELECTION,
                DeployTestFixtures.destination(false), DeployRoute.DEPLOY_DESTINATION, null);
        assertRejected(decide(owned, "999"));
    }

    private static AiDecisionResult decide(Owned owned, String wire) {
        return DeployPhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.facts(),
                owned.assessment(), (route, facts, assessment) -> wire);
    }

    private static Owned owned(AwaitingDecisionType type,
                               Map<String, String[]> params,
                               DeployRoute route,
                               Integer ownedOrdinal) {
        return owned(type, params, route, ownedOrdinal,
                DeployFormationAssessment.Verdict.UNKNOWN);
    }

    private static Owned owned(AwaitingDecisionType type,
                               Map<String, String[]> params,
                               DeployRoute route,
                               Integer ownedOrdinal,
                               DeployFormationAssessment.Verdict verdict) {
        DecisionSnapshot snapshot = DeployTestFixtures.snapshot(type, params);
        DeployFacts facts = DeployFacts.parse(snapshot,
                DeployTestFixtures.input(type, params), route, null).value();
        DeployPhysicalCardRef source = route == DeployRoute.DEPLOY_PARENT
                ? facts.parentCandidates().get(0).sourceCard() : facts.sourceCard();
        DeployFormationAssessment formation = verdict
                == DeployFormationAssessment.Verdict.UNKNOWN
                ? DeployFormationAssessment.unknown(
                    source, java.util.List.of(), "fixture assessment")
                : new DeployFormationAssessment(
                    verdict, source, java.util.List.of(), null, null,
                    "fixture structural block");
        ForceObligationVector obligations = new ForceObligationVector(
                0, 0, 0, false, false, false, false, 0, false);
        DeployAssessment assessment = new DeployAssessment(
                route, formation, obligations, ownedOrdinal);
        return new Owned(snapshot, facts, assessment);
    }

    private static void assertAccepted(AiDecisionResult result, String wire) {
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals(wire, result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON,
                result.mutationMode());
        assertTrue(result.fromTypedFinalizer());
        assertNotNull(result.trackerMutation());
    }

    private static void assertRejected(AiDecisionResult result) {
        assertEquals(AiDecisionResult.Status.TYPED_REJECTION, result.status());
        assertEquals(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                result.rejectionCode());
        assertNull(result.wireResponse());
        assertNull(result.trackerMutation());
    }

    private record Owned(DecisionSnapshot snapshot,
                         DeployFacts facts,
                         DeployAssessment assessment) {
    }
}
