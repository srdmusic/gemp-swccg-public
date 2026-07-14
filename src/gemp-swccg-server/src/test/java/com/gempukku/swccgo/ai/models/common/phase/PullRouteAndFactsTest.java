package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PullDecisionWire;
import com.gempukku.swccgo.common.PullPhysicalCardRef;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Pure route matrix and stable PULL fact parsing. */
public class PullRouteAndFactsTest {

    @Test
    public void deployParentRoutesFromTypedSemantic() {
        assertRoute(PullRoute.PULL_PARENT, AwaitingDecisionType.CARD_ACTION_CHOICE,
                PullTestFixtures.parent(DecisionActionSemantic.PULL_DEPLOY_FROM_PILE));
    }

    @Test
    public void takeParentRoutesFromTypedSemantic() {
        assertRoute(PullRoute.PULL_PARENT, AwaitingDecisionType.CARD_ACTION_CHOICE,
                PullTestFixtures.parent(DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE));
    }

    @Test
    public void deployAndTakeChildrenShareWireButRemainDistinctRoutes() {
        assertRoute(PullRoute.PULL_DEPLOY_CHILD, AwaitingDecisionType.ARBITRARY_CARDS,
                PullTestFixtures.child(DecisionOrigin.PULL_DEPLOY_CHILD));
        assertRoute(PullRoute.PULL_TAKE_CHILD, AwaitingDecisionType.ARBITRARY_CARDS,
                PullTestFixtures.child(DecisionOrigin.PULL_TAKE_CHILD));
    }

    @Test
    public void destinationAndFailedVerifyRouteByExactShape() {
        assertRoute(PullRoute.PULL_DESTINATION, AwaitingDecisionType.CARD_SELECTION,
                PullTestFixtures.destination(false));
        assertRoute(PullRoute.PULL_FAILED_VERIFY, AwaitingDecisionType.ARBITRARY_CARDS,
                PullTestFixtures.failedVerify());
    }

    @Test
    public void failedVerifyRequiresExactZeroRange() {
        Map<String, String[]> params = PullTestFixtures.failedVerify();
        PullTestFixtures.put(params, "max", "1");
        assertRoute(PullRoute.LEGACY_UNOWNED, AwaitingDecisionType.ARBITRARY_CARDS, params);
    }

    @Test
    public void missingTransactionIdIsUnownedAndFactsAreUnknown() {
        Map<String, String[]> params = PullTestFixtures.child(DecisionOrigin.PULL_DEPLOY_CHILD);
        params.remove(PullDecisionWire.TRANSACTION_ID);
        assertUnknownFacts(params, PullRoute.PULL_DEPLOY_CHILD);
    }

    @Test
    public void parentRouteRejectsPreAcceptanceTransactionMetadata() {
        Map<String, String[]> params = PullTestFixtures.parent(
                DecisionActionSemantic.PULL_DEPLOY_FROM_PILE);
        PullTestFixtures.put(params, PullDecisionWire.TRANSACTION_ID,
                String.valueOf(PullTestFixtures.TRANSACTION_ID));

        assertRoute(PullRoute.LEGACY_UNOWNED, AwaitingDecisionType.CARD_ACTION_CHOICE, params);
    }

    @Test
    public void missingPermanentCandidateIdentityIsUnownedAndFactsAreUnknown() {
        Map<String, String[]> params = PullTestFixtures.child(DecisionOrigin.PULL_DEPLOY_CHILD);
        params.remove(PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID);
        assertUnknownFacts(params, PullRoute.PULL_DEPLOY_CHILD);
    }

    @Test
    public void partialSourceIdentityIsUnownedAndFactsAreUnknown() {
        Map<String, String[]> params = PullTestFixtures.child(DecisionOrigin.PULL_TAKE_CHILD);
        params.remove(PullDecisionWire.SOURCE_PERMANENT_CARD_ID);
        assertUnknownFacts(params, PullRoute.PULL_TAKE_CHILD);
    }

    @Test
    public void malformedOrUnrecognizedMetadataFailsClosed() {
        Map<String, String[]> parent = PullTestFixtures.parent(
                DecisionActionSemantic.PULL_DEPLOY_FROM_PILE);
        PullTestFixtures.put(parent, DecisionActionSemantic.WIRE_PARAMETER,
                DecisionActionSemantic.UNKNOWN.name(), "PULL_BY_PROMPT_TEXT");
        assertRoute(PullRoute.LEGACY_UNOWNED, AwaitingDecisionType.CARD_ACTION_CHOICE, parent);

        Map<String, String[]> child = PullTestFixtures.child(DecisionOrigin.PULL_DEPLOY_CHILD);
        PullTestFixtures.put(child, PullDecisionWire.PHYSICAL_PERMANENT_CARD_ID, "1001", "1002");
        assertRoute(PullRoute.LEGACY_UNOWNED, AwaitingDecisionType.ARBITRARY_CARDS, child);
    }

    @Test
    public void destinationRequiresCurrentAndPermanentOrderToAlign() {
        Map<String, String[]> params = PullTestFixtures.destination(false);
        PullTestFixtures.put(params, PullDecisionWire.DESTINATION_PERMANENT_CARD_ID,
                "3001");
        assertRoute(PullRoute.LEGACY_UNOWNED, AwaitingDecisionType.CARD_SELECTION, params);
    }

    @Test
    public void capturePreservesPresenceOrderAndExcludesPromptText() {
        Map<String, String[]> params = PullTestFixtures.child(DecisionOrigin.PULL_DEPLOY_CHILD);
        PullTestFixtures.put(params, PullDecisionWire.SELECTED_CARD_ID);
        PullRouteInput first = PullRouteInput.capture(PullTestFixtures.decision(
                AwaitingDecisionType.ARBITRARY_CARDS, "Deploy a named card", params));
        PullRouteInput second = PullRouteInput.capture(PullTestFixtures.decision(
                AwaitingDecisionType.ARBITRARY_CARDS, "Completely different prompt", params));

        assertEquals(first, second);
        assertEquals(List.of("101", "102", "103"), first.physicalCardIds());
        assertTrue(first.selectedCardIds().isEmpty());
        assertNull(first.destinationCardIds());
        try {
            first.physicalCardIds().add("104");
            fail("captured raw order must be immutable");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void parentFactsKeepOriginalOrdinalAndExactSourceIdentity() {
        Map<String, String[]> params = PullTestFixtures.parent(
                DecisionActionSemantic.PULL_DEPLOY_FROM_PILE);
        PullFacts facts = PullTestFixtures.facts(AwaitingDecisionType.CARD_ACTION_CHOICE,
                "Choose action or Pass", params, PullRoute.PULL_PARENT);

        assertEquals("41", facts.decisionId());
        assertEquals(5, facts.turn());
        assertEquals(PullTestFixtures.PLAYER, facts.playerId());
        assertEquals(Phase.DEPLOY, facts.phase());
        assertNull(facts.transactionId());
        assertEquals(1, facts.parentCandidates().size());
        assertEquals(1, facts.parentCandidates().get(0).ordinal());
        assertEquals(new PullPhysicalCardRef(9002, 902),
                facts.parentCandidates().get(0).sourceCard());
    }

    @Test
    public void duplicateCardCopiesRetainDistinctStableIdentityAndOrder() {
        Map<String, String[]> params = PullTestFixtures.child(DecisionOrigin.PULL_DEPLOY_CHILD);
        PullFacts facts = PullTestFixtures.facts(AwaitingDecisionType.ARBITRARY_CARDS,
                "duplicate copies", params, PullRoute.PULL_DEPLOY_CHILD);

        assertEquals(List.of(
                new PullPhysicalCardRef(1001, 101),
                new PullPhysicalCardRef(1002, 102),
                new PullPhysicalCardRef(1003, 103)), facts.candidateCards());
        assertEquals(List.of("temp0", "temp1", "temp2"), facts.candidateWireIds());
        assertFalse(facts.candidateCards().get(0).equals(facts.candidateCards().get(1)));
        assertEquals(Long.valueOf(PullTestFixtures.TRANSACTION_ID), facts.transactionId());
    }

    @Test
    public void destinationFactsCarrySelectedAndOrderedIdentity() {
        Map<String, String[]> params = PullTestFixtures.destination(true);
        PullFacts facts = PullTestFixtures.facts(AwaitingDecisionType.CARD_SELECTION,
                "choose destination", params, PullRoute.PULL_DESTINATION);

        assertEquals(new PullPhysicalCardRef(1002, 102), facts.selectedChild());
        assertEquals(List.of(new PullPhysicalCardRef(3001, 301)), facts.orderedDestinations());
        assertNull(facts.forcedDestination());
        assertTrue(facts.sourceFilter().isUnknown());
    }

    @Test
    public void promptedDestinationRejectsAutoSelectionEvidence() {
        Map<String, String[]> params = PullTestFixtures.destination(true);
        PullTestFixtures.put(params, PullDecisionWire.FORCED_DESTINATION_CARD_ID, "301");
        PullTestFixtures.put(params,
                PullDecisionWire.FORCED_DESTINATION_PERMANENT_CARD_ID, "3001");

        assertRoute(PullRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.CARD_SELECTION, params);
    }

    @Test
    public void assessmentHasAllThreeVerdictsAndImmutableTypedEvidence() {
        PullFacts facts = PullTestFixtures.facts(AwaitingDecisionType.ARBITRARY_CARDS,
                "choose", PullTestFixtures.child(DecisionOrigin.PULL_TAKE_CHILD),
                PullRoute.PULL_TAKE_CHILD);
        PullAssessment compatibility = PullAssessment.compatibility(facts);
        PullAssessment allow = PullAssessment.allow(facts.route(), PullAssessment.Evidence.POLICY_ALLOW);
        PullAssessment block = PullAssessment.block(facts.route(), PullAssessment.Evidence.POLICY_BLOCK);

        assertEquals(PullAssessment.Verdict.DEFER, compatibility.verdict());
        assertEquals(PullAssessment.Verdict.ALLOW, allow.verdict());
        assertEquals(PullAssessment.Verdict.BLOCK, block.verdict());
        assertTrue(compatibility.evidence().contains(
                PullAssessment.Evidence.ACCEPTED_PARENT_TRANSACTION_ID));
        try {
            compatibility.evidence().add(PullAssessment.Evidence.POLICY_ALLOW);
            fail("assessment evidence must be immutable");
        } catch (UnsupportedOperationException expected) {
        }
    }

    private static void assertUnknownFacts(Map<String, String[]> params, PullRoute requestedRoute) {
        PullRouteInput input = PullTestFixtures.input(AwaitingDecisionType.ARBITRARY_CARDS, params);
        assertEquals(PullRoute.LEGACY_UNOWNED, PullRouteResolver.resolve(input));
        DecisionSnapshot snapshot = PullTestFixtures.snapshot(
                AwaitingDecisionType.ARBITRARY_CARDS, "prompt", params);
        FactValue<PullFacts> facts = PullFacts.parse(snapshot, input, requestedRoute);
        assertTrue(facts.isUnknown());
    }

    private static void assertRoute(PullRoute expected,
                                    AwaitingDecisionType type,
                                    Map<String, String[]> params) {
        assertEquals(expected, PullRouteResolver.resolve(PullTestFixtures.input(type, params)));
    }
}
