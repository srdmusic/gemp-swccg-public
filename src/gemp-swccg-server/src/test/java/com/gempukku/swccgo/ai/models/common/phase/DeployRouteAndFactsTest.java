package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.ai.models.common.strategy.ForceObligationVector;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.DeployDecisionWire;
import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Closed DEPLOY route matrix and immutable physical fact parsing. */
public class DeployRouteAndFactsTest {

    @Test
    public void everyOwnedWireShapeRoutesWithoutPromptText() {
        assertRoute(DeployRoute.DEPLOY_PARENT, AwaitingDecisionType.CARD_ACTION_CHOICE,
                DeployTestFixtures.parent(false, false));
        assertRoute(DeployRoute.DEPLOY_DESTINATION, AwaitingDecisionType.CARD_SELECTION,
                DeployTestFixtures.destination(false));
        assertRoute(DeployRoute.DEPLOY_BUDDY, AwaitingDecisionType.CARD_SELECTION,
                DeployTestFixtures.buddy(AwaitingDecisionType.CARD_SELECTION));
        assertRoute(DeployRoute.DEPLOY_BUDDY, AwaitingDecisionType.ARBITRARY_CARDS,
                DeployTestFixtures.buddy(AwaitingDecisionType.ARBITRARY_CARDS));
        assertRoute(DeployRoute.DEPLOY_V170_UNDERCOVER,
                AwaitingDecisionType.MULTIPLE_CHOICE,
                DeployTestFixtures.choice(
                        DecisionOrigin.DEPLOY_V170_UNDERCOVER, false));
        assertRoute(DeployRoute.DEPLOY_CAPACITY, AwaitingDecisionType.MULTIPLE_CHOICE,
                DeployTestFixtures.choice(DecisionOrigin.DEPLOY_CAPACITY, true));
        assertRoute(DeployRoute.DEPLOY_CONFIRMATION,
                AwaitingDecisionType.MULTIPLE_CHOICE,
                DeployTestFixtures.confirmation());
    }

    @Test
    public void phaseAndPromptAloneNeverClaimOwnership() {
        Map<String, String[]> params = DeployTestFixtures.parent(false, false);
        params.remove(DecisionOrigin.WIRE_PARAMETER);
        assertRoute(DeployRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.CARD_ACTION_CHOICE, params);

        params = DeployTestFixtures.confirmation();
        DeployTestFixtures.put(params, DecisionOrigin.WIRE_PARAMETER,
                DecisionOrigin.ACTIVATE_ZERO_CONFIRM.name());
        assertRoute(DeployRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.MULTIPLE_CHOICE, params);
    }

    @Test
    public void promptedDestinationCannotClaimForcedAutoSelection() {
        assertRoute(DeployRoute.LEGACY_UNOWNED, AwaitingDecisionType.CARD_SELECTION,
                DeployTestFixtures.destination(true));
    }

    @Test
    public void forcedDestinationFlowsToLaterTypedChoice() {
        Map<String, String[]> params = DeployTestFixtures.choice(
                DecisionOrigin.DEPLOY_CAPACITY, true);
        DeployFacts facts = DeployTestFixtures.facts(
                AwaitingDecisionType.MULTIPLE_CHOICE, params,
                DeployRoute.DEPLOY_CAPACITY);

        assertTrue(facts.forcedDestination());
        assertEquals(List.of(new DeployPhysicalCardRef(3001, 301)),
                facts.orderedDestinationCards());
    }

    @Test
    public void parentKeepsExactForcedDestinationAndSimultaneousBuddy() {
        DeployFacts facts = DeployTestFixtures.facts(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                DeployTestFixtures.parent(true, true), DeployRoute.DEPLOY_PARENT);
        DeployFacts.ParentCandidate candidate = facts.parentCandidates().get(0);

        assertEquals(List.of(new DeployDestinationRef.Card(
                new DeployPhysicalCardRef(3001, 301))),
                candidate.orderedDestinations());
        assertEquals(List.of(
                new DeployPhysicalCardRef(4001, 401),
                new DeployPhysicalCardRef(4002, 402)),
                candidate.orderedBuddyCandidates());
        assertEquals(new DeployPhysicalCardRef(4002, 402),
                candidate.selectedBuddy());
    }

    @Test
    public void arbitraryBuddyMapsTempWireToSelectablePhysicalCopies() {
        DeployFacts facts = DeployTestFixtures.facts(
                AwaitingDecisionType.ARBITRARY_CARDS,
                DeployTestFixtures.buddy(AwaitingDecisionType.ARBITRARY_CARDS),
                DeployRoute.DEPLOY_BUDDY);

        assertEquals(List.of("temp1", "temp2"), facts.orderedBuddyWireIds());
        assertEquals(List.of(
                new DeployPhysicalCardRef(4001, 401),
                new DeployPhysicalCardRef(4002, 402)),
                facts.orderedBuddyCards());
    }

    @Test
    public void malformedPhysicalIdentityFailsClosed() {
        Map<String, String[]> params = DeployTestFixtures.parent(false, true);
        DeployTestFixtures.put(params, DeployDecisionWire.SELECTED_BUDDY,
                "", "bad:identity:shape");
        DeployRouteInput input = DeployTestFixtures.input(
                AwaitingDecisionType.CARD_ACTION_CHOICE, params);

        assertEquals(DeployRoute.LEGACY_UNOWNED,
                DeployRouteResolver.resolve(input));
        FactValue<DeployFacts> facts = DeployFacts.parse(
                DeployTestFixtures.snapshot(
                        AwaitingDecisionType.CARD_ACTION_CHOICE, params),
                input, DeployRoute.DEPLOY_PARENT, null);
        assertTrue(facts.isUnknown());
    }

    @Test
    public void v170MalformedYesNoResultsFailClosed() {
        Map<String, String[]> missingNo = DeployTestFixtures.choice(
                DecisionOrigin.DEPLOY_V170_UNDERCOVER, false);
        DeployTestFixtures.put(missingNo, "results", "Yes", "Maybe");
        assertRoute(DeployRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.MULTIPLE_CHOICE, missingNo);

        Map<String, String[]> duplicateYes = DeployTestFixtures.choice(
                DecisionOrigin.DEPLOY_V170_UNDERCOVER, false);
        DeployTestFixtures.put(duplicateYes, "results", "Yes", "No", "Yes");
        assertRoute(DeployRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.MULTIPLE_CHOICE, duplicateYes);
    }

    @Test
    public void v170AssessmentPreservesResultOrderAndKnownZero() {
        Map<String, String[]> reversed = DeployTestFixtures.choice(
                DecisionOrigin.DEPLOY_V170_UNDERCOVER, false);
        DeployTestFixtures.put(reversed, "results", "No", "Yes");
        DeployFacts raw = DeployTestFixtures.facts(
                AwaitingDecisionType.MULTIPLE_CHOICE, reversed,
                DeployRoute.DEPLOY_V170_UNDERCOVER);

        assertEquals(Integer.valueOf(1), assessment(withDrain(raw, 2f)).ownedChoiceOrdinal());
        assertEquals(Integer.valueOf(0), assessment(withDrain(raw, 0f)).ownedChoiceOrdinal());
    }

    @Test
    public void v170UnknownDrainCannotCreateAssessment() {
        DeployFacts facts = DeployTestFixtures.facts(
                AwaitingDecisionType.MULTIPLE_CHOICE,
                DeployTestFixtures.choice(
                        DecisionOrigin.DEPLOY_V170_UNDERCOVER, false),
                DeployRoute.DEPLOY_V170_UNDERCOVER);
        assertTrue(facts.opponentActiveDrain().isUnknown());

        try {
            assessment(facts);
            throw new AssertionError("unknown V170 drain must not create an owned assessment");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unknown opponent drain"));
        }
    }

    private static DeployAssessment assessment(DeployFacts facts) {
        return DeployAssessment.compatibility(
                facts,
                DeployFormationAssessment.unknown(
                        facts.sourceCard(), List.of(), "fixture"),
                new ForceObligationVector(
                        0, 0, 0, false, false, false, false, 0, false));
    }

    private static DeployFacts withDrain(DeployFacts facts, float drain) {
        return new DeployFacts(
                facts.decisionId(), facts.turn(), facts.playerId(), facts.phase(),
                facts.route(), facts.parentCandidates(), facts.attemptId(),
                facts.parentDecisionId(), facts.parentActionOrdinal(), facts.sourceCard(),
                facts.sourceZone(), facts.orderedDestinationCards(),
                facts.orderedBuddyCards(), facts.orderedBuddyWireIds(),
                facts.selectedBuddy(), facts.forcedDestination(), facts.results(),
                FactValue.known(drain, "fixture", "opponent active drain"));
    }

    private static void assertRoute(DeployRoute expected,
                                    AwaitingDecisionType type,
                                    Map<String, String[]> params) {
        assertEquals(expected, DeployRouteResolver.resolve(
                DeployTestFixtures.input(type, params)));
    }
}
