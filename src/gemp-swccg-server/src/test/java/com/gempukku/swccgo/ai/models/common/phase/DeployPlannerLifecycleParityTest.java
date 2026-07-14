package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.ai.models.common.strategy.ForceObligationVector;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.DeployDecisionWire;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PullPhysicalCardRef;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Accepted-response lifecycle parity for the two DEPLOY planners. */
public class DeployPlannerLifecycleParityTest {
    private static final DeployPhysicalCardRef SOURCE =
            new DeployPhysicalCardRef(1002, 102);
    private static final DeployDestinationRef.Card DESTINATION =
            new DeployDestinationRef.Card(new DeployPhysicalCardRef(3001, 301));
    private static final ForceObligationVector OBLIGATIONS =
            new ForceObligationVector(6, 2, 1, true, false,
                    true, true, 1, true);
    private static final SwccgCardBlueprint CHARACTER_BLUEPRINT = blueprint();

    @Test
    public void childAssessmentReusesParentFormationAndFullObligationVector() {
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);
            DeployFormationAssessment formation = safeSoloFormation();
            planner.accept(parent, assessment(parent.route(), formation), "1", null);

            DeployFacts child = destinationFacts();
            DeployAssessment childAssessment = planner.assess(child, "301", null);

            assertNotNull(planner.name(), childAssessment);
            assertSame(planner.name(), formation, childAssessment.formation());
            assertSame(planner.name(), OBLIGATIONS, childAssessment.forceObligations());
            assertEquals(planner.name(), OBLIGATIONS,
                    planner.current().forceObligations());
        }
    }

    @Test
    public void pullCursorDefersThenBindsOneFormationAtTheFirstDestinationChild() {
        for (Planner planner : planners()) {
            PullFacts pull = pullDeployChildFacts();
            PhysicalCard destination = physical(
                    DESTINATION.card(), Zone.LOCATIONS, null);
            LifecycleState state = new LifecycleState(
                    5, 20, physical(SOURCE, Zone.RESERVE_DECK, null), destination);
            SwccgGame game = gameWithOpponentPower(state, 0f);

            planner.acceptPull(pull, "temp0", game);
            assertNotNull(planner.name(), planner.current());
            assertEquals(planner.name(), DeployFormationAssessment.Verdict.UNKNOWN,
                    planner.current().formation().verdict());

            DeployFacts child = pullDestinationFacts();
            DeployAssessment childAssessment = planner.assess(child, "301", game);
            assertNotNull(planner.name(), childAssessment);
            assertSame(planner.name(), planner.current().forceObligations(),
                    childAssessment.forceObligations());
            assertEquals(planner.name(), DeployFormationAssessment.Verdict.SAFE_SOLO,
                    childAssessment.formation().verdict());

            planner.accept(child, childAssessment, "301", game);
            assertSame(planner.name(), childAssessment.formation(),
                    planner.current().formation());
            assertSame(planner.name(), childAssessment.forceObligations(),
                    planner.current().forceObligations());
        }
    }

    @Test
    public void childCancellationBlocksOnlyTheExactParentAttemptOnReplay() {
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);
            DeployFacts child = destinationFacts();
            DeployAssessment parentAssessment = assessment(
                    parent.route(), safeSoloFormation());
            DeployAssessment childAssessment = assessment(
                    child.route(), safeSoloFormation());

            planner.accept(parent, parentAssessment, "1", null);
            planner.accept(child, childAssessment, "", null);
            assertTerminated(planner, "accepted destination cancellation");
            DeployTransaction cancellation = planner.terminal();
            planner.accept(child, childAssessment, "", null);
            assertSame(planner.name(), cancellation, planner.terminal());

            DeployAssessment replay = planner.assess(parent, "1", null);
            assertEquals(planner.name(),
                    DeployFormationAssessment.Verdict.ALL_DESTINATIONS_BLOCKED,
                    replay.formation().verdict());
            planner.accept(parent, replay, "", null);
            assertNull(planner.name(), planner.current());
            assertSame(planner.name(), cancellation, planner.terminal());
        }
    }

    @Test
    public void engineRejectionClearsExactlyOnceWithoutBlockingAReplay() {
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);
            DeployFacts child = destinationFacts();
            DeployAssessment parentAssessment = assessment(
                    parent.route(), safeSoloFormation());

            planner.accept(parent, parentAssessment, "1", null);
            planner.reject(child, "engine rejected fixture response");
            assertTerminated(planner, "engine rejected fixture response");
            DeployTransaction rejection = planner.terminal();
            planner.reject(child, "second rejection must not mutate");
            assertSame(planner.name(), rejection, planner.terminal());

            planner.accept(parent, parentAssessment, "1", null);
            assertNotNull(planner.name(), planner.current());
        }
    }

    @Test
    public void phaseGameZoneAndForceInvalidationsClearOnlyTheBoundCursor() {
        for (Planner planner : planners()) {
            assertInvalidation(planner, Phase.CONTROL, Zone.HAND, 20,
                    false, "phase changed before deployment completed");
        }
        for (Planner planner : planners()) {
            assertInvalidation(planner, Phase.DEPLOY, Zone.USED_PILE, 20,
                    false, "unexpected source zone drift: USED_PILE");
        }
        for (Planner planner : planners()) {
            assertInvalidation(planner, Phase.DEPLOY, Zone.HAND, 0,
                    false, "Force changed and no longer covers deploy plus obligations");
        }
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);
            LifecycleState state = new LifecycleState(
                    5, 20, physical(SOURCE, Zone.HAND, null));
            SwccgGame first = game(state);
            planner.accept(parent, assessment(parent.route(), safeSoloFormation()),
                    "1", first);
            planner.observe(game(state), state, DeployTestFixtures.PLAYER, Phase.DEPLOY);
            assertTerminated(planner, "game identity changed");
        }
    }

    @Test
    public void expectedSourceTransitionCompletesExactlyOnceWithoutSelfInvalidation() {
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);
            PhysicalCard destination = physical(
                    DESTINATION.card(), Zone.LOCATIONS, null);
            LifecycleState hand = new LifecycleState(
                    5, 20, physical(SOURCE, Zone.HAND, null), destination);
            SwccgGame game = game(hand);
            planner.accept(parent, assessment(parent.route(), safeSoloFormation()),
                    "1", game);

            DeployPhysicalCardRef deployedSource = new DeployPhysicalCardRef(
                    SOURCE.permanentCardId(), 202);
            LifecycleState deployed = new LifecycleState(
                    5, 20,
                    physical(deployedSource, Zone.AT_LOCATION, destination),
                    destination);
            planner.observe(game, deployed, DeployTestFixtures.PLAYER, Phase.DEPLOY);

            assertNull(planner.name(), planner.current());
            assertNotNull(planner.name(), planner.terminal());
            assertEquals(planner.name(), DeployTransaction.Stage.COMPLETED,
                    planner.terminal().stage());
            assertEquals(planner.name(), List.of(
                    DeployTransaction.Stage.SNAPSHOT,
                    DeployTransaction.Stage.PARENT_PENDING,
                    DeployTransaction.Stage.COMMITTED,
                    DeployTransaction.Stage.COMPLETED),
                    planner.terminal().history());
            assertNull(planner.name(), planner.lastReason());
            DeployTransaction completed = planner.terminal();
            planner.observe(game, deployed, DeployTestFixtures.PLAYER, Phase.DEPLOY);
            assertSame(planner.name(), completed, planner.terminal());
        }
    }

    @Test
    public void locationPlacementRelationsCompleteAgainstTheExactDestination() {
        for (Planner planner : planners()) {
            assertLocationRelationCompletion(planner, "Tatooine", null, null);
        }
        for (Planner planner : planners()) {
            assertLocationRelationCompletion(planner, null, "Tatooine", null);
        }
        for (Planner planner : planners()) {
            PhysicalCard related = physical(
                    DESTINATION.card(), Zone.LOCATIONS, null);
            assertLocationRelationCompletion(planner, null, null, related);
        }
    }

    @Test
    public void convertedLocationCompletesUsingTheAcceptedPreConversionIdentity() {
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);
            PhysicalCard expectedBefore = physical(
                    DESTINATION.card(), Zone.LOCATIONS, null);
            LifecycleState hand = new LifecycleState(
                    5, 20, physical(SOURCE, Zone.HAND, null), expectedBefore);
            SwccgGame game = game(hand);
            planner.accept(parent, assessment(parent.route(), safeSoloFormation()),
                    "1", game);

            PhysicalCard converted = physical(
                    new DeployPhysicalCardRef(
                            DESTINATION.card().permanentCardId(), 777),
                    Zone.CONVERTED_LOCATIONS, null);
            PhysicalCard replacement = physical(
                    new DeployPhysicalCardRef(
                            SOURCE.permanentCardId(),
                            DESTINATION.card().currentCardId()),
                    Zone.LOCATIONS, null);
            LifecycleState deployed = new LifecycleState(
                    5, 20, replacement, converted);
            planner.observe(game, deployed, DeployTestFixtures.PLAYER, Phase.DEPLOY);

            assertCompleted(planner);
        }
    }

    @Test
    public void wrongLocationRelationInvalidatesExactlyOnce() {
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);
            PhysicalCard expected = physical(
                    DESTINATION.card(), Zone.LOCATIONS, null,
                    null, null, null, "Tatooine");
            LifecycleState hand = new LifecycleState(
                    5, 20, physical(SOURCE, Zone.HAND, null), expected);
            SwccgGame game = game(hand);
            planner.accept(parent, assessment(parent.route(), safeSoloFormation()),
                    "1", game);

            PhysicalCard misplaced = physical(
                    new DeployPhysicalCardRef(SOURCE.permanentCardId(), 202),
                    Zone.LOCATIONS, null, null, "Naboo", null, null);
            LifecycleState drifted = new LifecycleState(
                    5, 20, misplaced, expected);
            planner.observe(game, drifted, DeployTestFixtures.PLAYER, Phase.DEPLOY);

            assertTerminated(planner,
                    "deployed destination drifted from the accepted destination");
            DeployTransaction terminal = planner.terminal();
            planner.observe(game, drifted, DeployTestFixtures.PLAYER, Phase.DEPLOY);
            assertSame(planner.name(), terminal, planner.terminal());
        }
    }

    @Test
    public void wrongInPlayDestinationInvalidatesExactlyOnce() {
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);
            PhysicalCard expected = physical(
                    DESTINATION.card(), Zone.LOCATIONS, null);
            LifecycleState hand = new LifecycleState(
                    5, 20, physical(SOURCE, Zone.HAND, null), expected);
            SwccgGame game = game(hand);
            planner.accept(parent, assessment(parent.route(), safeSoloFormation()),
                    "1", game);

            DeployPhysicalCardRef wrongRef = new DeployPhysicalCardRef(3002, 302);
            PhysicalCard wrong = physical(wrongRef, Zone.LOCATIONS, null);
            LifecycleState drifted = new LifecycleState(
                    5, 20,
                    physical(SOURCE, Zone.AT_LOCATION, wrong),
                    expected, wrong);
            planner.observe(game, drifted, DeployTestFixtures.PLAYER, Phase.DEPLOY);

            assertTerminated(planner,
                    "deployed destination drifted from the accepted destination");
            DeployTransaction terminal = planner.terminal();
            planner.observe(game, drifted, DeployTestFixtures.PLAYER, Phase.DEPLOY);
            assertSame(planner.name(), terminal, planner.terminal());
        }
    }

    @Test
    public void repeatedAssessmentIsEqualAndDoesNotMutatePlannerState() {
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);

            DeployAssessment first = planner.assess(parent, "1", null);
            DeployAssessment second = planner.assess(parent, "1", null);

            assertEquals(planner.name(), first, second);
            assertNull(planner.name(), planner.current());
            assertNull(planner.name(), planner.terminal());
            assertNull(planner.name(), planner.lastReason());
        }
    }

    @Test
    public void unknownDestinationLegalityRemainsUnknown() {
        for (Planner planner : planners()) {
            DeployFacts parent = unknownLegalityParentFacts();

            DeployAssessment assessment = planner.assess(parent, "1", null);

            assertNotNull(planner.name(), assessment);
            assertEquals(planner.name(), DeployFormationAssessment.Verdict.UNKNOWN,
                    assessment.formation().verdict());
            assertEquals(planner.name(),
                    "ordered destination legality is unavailable at the parent boundary",
                    assessment.formation().reason());
            assertNull(planner.name(), planner.current());
        }
    }

    @Test
    public void failedFormationQueryRemainsUnknown() {
        for (Planner planner : planners()) {
            DeployFacts parent = parentFacts(4f);
            PhysicalCard destination = physical(
                    DESTINATION.card(), Zone.LOCATIONS, null);
            LifecycleState state = new LifecycleState(
                    5, 20, physical(SOURCE, Zone.HAND, null), destination);

            DeployAssessment assessment = planner.assess(
                    parent, "1", game(state));

            assertNotNull(planner.name(), assessment);
            assertEquals(planner.name(), DeployFormationAssessment.Verdict.UNKNOWN,
                    assessment.formation().verdict());
            assertTrue(planner.name(), assessment.formation().reason()
                    .startsWith("formation query failed:"));
            assertNull(planner.name(), planner.current());
        }
    }

    @Test
    public void abilityZeroBodyCountsAsPresentForBothPlanners() {
        PhysicalCard destination = physical(
                DESTINATION.card(), Zone.LOCATIONS, null);
        PhysicalCard abilityZeroBody = physical(
                new DeployPhysicalCardRef(4001, 401), Zone.AT_LOCATION,
                destination);
        LifecycleState state = new LifecycleState(
                5, 20, List.of(abilityZeroBody),
                physical(SOURCE, Zone.HAND, null), destination, abilityZeroBody);

        for (Planner planner : planners()) {
            DeployAssessment assessment = planner.assess(
                    parentFacts(4f), "1", gameWithOpponentPower(state, 10f));

            assertEquals(planner.name(), DeployFormationAssessment.Verdict.SAFE_SOLO,
                    assessment.formation().verdict());
            assertTrue(planner.name(), assessment.formation().allows(DESTINATION));
            assertFalse(planner.name(),
                    assessment.formation().isWeakSoloNoPlan(DESTINATION));
        }
    }

    @Test
    public void weakSoloNoPlanIsComputedOnceAndCarriedByThePlannerAssessment() {
        PhysicalCard destination = physical(
                DESTINATION.card(), Zone.LOCATIONS, null);
        LifecycleState state = new LifecycleState(
                5, 20, List.of(),
                physical(SOURCE, Zone.HAND, null), destination);

        for (Planner planner : planners()) {
            DeployAssessment assessment = planner.assess(
                    parentFacts(4f), "1", gameWithOpponentPower(state, 0f));

            assertEquals(planner.name(), DeployFormationAssessment.Verdict.SAFE_SOLO,
                    assessment.formation().verdict());
            assertTrue(planner.name(), assessment.formation().allows(DESTINATION));
            assertTrue(planner.name(),
                    assessment.formation().isWeakSoloNoPlan(DESTINATION));
        }
    }

    @Test
    public void namedStrategicIntentsRemainExplicitForBothPlanners() {
        PhysicalCard destination = physical(
                DESTINATION.card(), Zone.LOCATIONS, null);
        PhysicalCard friendlyBody = physical(
                new DeployPhysicalCardRef(4001, 401), Zone.AT_LOCATION,
                destination);
        LifecycleState state = new LifecycleState(
                5, 20, List.of(friendlyBody),
                physical(SOURCE, Zone.HAND, null), destination, friendlyBody);
        String[][] intents = {
                {"targeted rescue", "targeted rescue at the threatened site"},
                {"Tyranus direct contact", "Tyranus makes direct contact"},
                {"safe establish", "safe establish at an uncontested site"},
                {"drain denial", "stop bleeding at the opponent drain"}
        };

        for (Planner planner : planners()) {
            for (String[] intent : intents) {
                DeployAssessment assessment = planner.assessWithInstructionReason(
                        parentFacts(4f), "1", intent[1],
                        gameWithOpponentPower(state, 10f));
                DeployFormationAssessment.Verdict expected =
                        intent[0].equals("targeted rescue")
                                || intent[0].equals("drain denial")
                            ? DeployFormationAssessment.Verdict.TARGETED_RESCUE
                            : DeployFormationAssessment.Verdict.SAFE_SOLO;

                assertEquals(planner.name() + " " + intent[0], expected,
                        assessment.formation().verdict());
                assertEquals(planner.name() + " " + intent[0], intent[1],
                        assessment.formation().reason());
                assertTrue(planner.name() + " " + intent[0],
                        assessment.formation().allows(DESTINATION));
            }
        }
    }

    @Test
    public void legalOverpowerOpportunityRemainsAnExplicitIntentForBothPlanners() {
        PhysicalCard destination = physical(
                DESTINATION.card(), Zone.LOCATIONS, null);
        PhysicalCard source = physical(
                SOURCE, Zone.HAND, null,
                blueprint(8f, 6f));
        LifecycleState state = new LifecycleState(
                5, 20, List.of(), source, destination);

        for (Planner planner : planners()) {
            DeployAssessment assessment = planner.assess(
                    parentFacts(4f), "1", gameWithOpponentPower(state, 3f));

            assertEquals(planner.name(),
                    DeployFormationAssessment.Verdict.OVERPOWER_OPPORTUNITY,
                    assessment.formation().verdict());
            assertTrue(planner.name(), assessment.formation().allows(DESTINATION));
        }
    }

    @Test
    public void missingBodyFactsRemainUnknownForBothPlanners() {
        PhysicalCard destination = physical(
                DESTINATION.card(), Zone.LOCATIONS, null);
        LifecycleState state = new LifecycleState(
                5, 20, (List<PhysicalCard>) null,
                physical(SOURCE, Zone.HAND, null), destination);

        for (Planner planner : planners()) {
            DeployAssessment assessment = planner.assess(
                    parentFacts(4f), "1", gameWithOpponentPower(state, 0f));

            assertEquals(planner.name(), DeployFormationAssessment.Verdict.UNKNOWN,
                    assessment.formation().verdict());
            assertEquals(planner.name(),
                    "friendly body presence is unavailable at the destination",
                    assessment.formation().reason());
            assertTrue(planner.name(),
                    assessment.formation().allowedDestinations().isEmpty());
        }
    }

    @Test
    public void engineBoundBuddyProducesExactSafeSequenceAcrossDuplicateCopies() {
        for (Planner planner : planners()) {
            DeployFacts parent = DeployTestFixtures.facts(
                    AwaitingDecisionType.CARD_ACTION_CHOICE,
                    DeployTestFixtures.parent(true, true),
                    DeployRoute.DEPLOY_PARENT);
            DeployPhysicalCardRef buddyRef = new DeployPhysicalCardRef(4002, 402);
            LifecycleState state = new LifecycleState(
                    5, 20,
                    physical(SOURCE, Zone.HAND, null),
                    physical(buddyRef, Zone.HAND, null));

            DeployAssessment assessment = planner.assess(parent, "1", game(state));

            assertNotNull(planner.name(), assessment);
            assertEquals(planner.name(), DeployFormationAssessment.Verdict.SAFE_SEQUENCE,
                    assessment.formation().verdict());
            assertEquals(planner.name(), SOURCE, assessment.formation().exactFirstCard());
            assertEquals(planner.name(), buddyRef, assessment.formation().exactBuddyCard());
            assertEquals(planner.name(), "7_1",
                    state.findCardByPermanentId(SOURCE.permanentCardId())
                            .getBlueprintId(true));
            assertEquals(planner.name(), "7_1",
                    state.findCardByPermanentId(buddyRef.permanentCardId())
                            .getBlueprintId(true));
        }
    }

    @Test
    public void blockedOptionalParentPassNeverOpensOrReopensCursor() {
        for (Planner planner : planners()) {
            DeployFacts parent = blockedParentFacts();
            DeployAssessment assessment = planner.assess(parent, "1", null);

            assertEquals(planner.name(),
                    DeployFormationAssessment.Verdict.ALL_DESTINATIONS_BLOCKED,
                    assessment.formation().verdict());
            planner.accept(parent, assessment, "", null);
            planner.accept(parent, assessment, "", null);
            assertNull(planner.name(), planner.current());
            assertNull(planner.name(), planner.terminal());
        }
    }

    private static void assertInvalidation(Planner planner,
                                           Phase phase,
                                           Zone sourceZone,
                                           int forcePile,
                                           boolean differentPlayer,
                                           String expectedReason) {
        DeployFacts parent = parentFacts(4f);
        LifecycleState state = new LifecycleState(
                5, forcePile, physical(SOURCE, sourceZone, null));
        SwccgGame game = game(state);
        planner.accept(parent, assessment(parent.route(), safeSoloFormation()),
                "1", game);
        planner.observe(game, state,
                differentPlayer ? "other-player" : DeployTestFixtures.PLAYER,
                phase);
        assertTerminated(planner, expectedReason);
    }

    private static void assertLocationRelationCompletion(Planner planner,
                                                         String partOfSystem,
                                                         String systemOrbited,
                                                         PhysicalCard related) {
        DeployFacts parent = parentFacts(4f);
        PhysicalCard expected = related != null ? related : physical(
                DESTINATION.card(), Zone.LOCATIONS, null,
                null, null, null, "Tatooine");
        LifecycleState hand = new LifecycleState(
                5, 20, physical(SOURCE, Zone.HAND, null), expected);
        SwccgGame game = game(hand);
        planner.accept(parent, assessment(parent.route(), safeSoloFormation()),
                "1", game);

        PhysicalCard deployedSource = physical(
                new DeployPhysicalCardRef(SOURCE.permanentCardId(), 202),
                Zone.LOCATIONS, null, related,
                partOfSystem, systemOrbited, null);
        LifecycleState deployed = new LifecycleState(
                5, 20, deployedSource, expected);
        planner.observe(game, deployed, DeployTestFixtures.PLAYER, Phase.DEPLOY);

        assertCompleted(planner);
    }

    private static void assertCompleted(Planner planner) {
        assertNull(planner.name(), planner.current());
        assertNotNull(planner.name(), planner.terminal());
        assertEquals(planner.name(), DeployTransaction.Stage.COMPLETED,
                planner.terminal().stage());
        assertNull(planner.name(), planner.lastReason());
    }

    private static void assertTerminated(Planner planner, String expectedReason) {
        assertNull(planner.name(), planner.current());
        assertNotNull(planner.name(), planner.terminal());
        assertEquals(planner.name(), expectedReason, planner.terminal().terminalReason());
        assertEquals(planner.name(), expectedReason, planner.lastReason());
    }

    private static DeployAssessment assessment(DeployRoute route,
                                               DeployFormationAssessment formation) {
        return new DeployAssessment(route, formation, OBLIGATIONS, null);
    }

    private static DeployFormationAssessment safeSoloFormation() {
        return new DeployFormationAssessment(
                DeployFormationAssessment.Verdict.SAFE_SOLO,
                SOURCE, List.of(DESTINATION), SOURCE, null, "safe solo fixture");
    }

    private static DeployFacts parentFacts(float cost) {
        DeployFacts base = DeployTestFixtures.facts(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                DeployTestFixtures.parent(true, false),
                DeployRoute.DEPLOY_PARENT);
        DeployFacts.ParentCandidate original = base.parentCandidates().get(0);
        DeployFacts.ParentCandidate candidate = new DeployFacts.ParentCandidate(
                original.ordinal(), original.actionWireId(), original.attemptId(),
                original.sourceCard(), original.sourceZone(),
                original.destinationLegalityKnown(), original.orderedDestinations(),
                original.orderedBuddyCandidates(), original.selectedBuddy(),
                FactValue.known(cost, "deploy-lifecycle-test", "known fixture cost"));
        return new DeployFacts(
                base.decisionId(), base.turn(), base.playerId(), base.phase(), base.route(),
                List.of(candidate), base.attemptId(), base.parentDecisionId(),
                base.parentActionOrdinal(), base.sourceCard(), base.sourceZone(),
                base.orderedDestinationCards(), base.orderedBuddyCards(),
                base.orderedBuddyWireIds(), base.selectedBuddy(), base.forcedDestination(),
                base.results(), base.opponentActiveDrain());
    }

    private static DeployFacts destinationFacts() {
        return DeployTestFixtures.facts(
                AwaitingDecisionType.CARD_SELECTION,
                DeployTestFixtures.destination(false),
                DeployRoute.DEPLOY_DESTINATION);
    }

    private static PullFacts pullDeployChildFacts() {
        PullPhysicalCardRef actionSource = new PullPhysicalCardRef(9001, 901);
        PullPhysicalCardRef selected = new PullPhysicalCardRef(
                SOURCE.permanentCardId(), SOURCE.currentCardId());
        return new PullFacts(
                "pull-child", 5, DeployTestFixtures.PLAYER, Phase.DEPLOY,
                PullRoute.PULL_DEPLOY_CHILD, 90001L,
                new PullFacts.ParentIdentity(
                        DeployTestFixtures.PARENT_DECISION_ID,
                        DeployTestFixtures.PARENT_ORDINAL),
                List.of(),
                FactValue.known(actionSource, "fixture", "accepted pull source"),
                FactValue.known(GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD,
                        "fixture", "accepted game-text action"),
                FactValue.known(Zone.RESERVE_DECK, "fixture", "source zone"),
                FactValue.known(DeployTestFixtures.PLAYER,
                        "fixture", "source zone owner"),
                List.of(selected), List.of("temp0"), null, List.of(), null,
                FactValue.unknown("fixture", "source filter",
                        "engine filter is unavailable"));
    }

    private static DeployFacts pullDestinationFacts() {
        java.util.Map<String, String[]> params =
                DeployTestFixtures.destination(false);
        DeployTestFixtures.put(params, DeployDecisionWire.ATTEMPT_ID,
                "PULL-90001");
        DeployTestFixtures.put(params, DeployDecisionWire.SOURCE_ZONE,
                Zone.RESERVE_DECK.name());
        DeployTestFixtures.put(params, "cardId", "301");
        DeployTestFixtures.put(params, DeployDecisionWire.DESTINATION_CARD_ID,
                "301");
        DeployTestFixtures.put(params,
                DeployDecisionWire.DESTINATION_PERMANENT_CARD_ID, "3001");
        return DeployTestFixtures.facts(
                AwaitingDecisionType.CARD_SELECTION,
                params, DeployRoute.DEPLOY_DESTINATION);
    }

    private static DeployFacts unknownLegalityParentFacts() {
        java.util.Map<String, String[]> params =
                DeployTestFixtures.parent(false, false);
        DeployTestFixtures.put(params,
                DeployDecisionWire.DESTINATION_LEGALITY_KNOWN, "", "false");
        DeployTestFixtures.put(params, DeployDecisionWire.LEGAL_DESTINATIONS, "", "");
        return DeployTestFixtures.facts(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                params, DeployRoute.DEPLOY_PARENT);
    }

    private static DeployFacts blockedParentFacts() {
        java.util.Map<String, String[]> params =
                DeployTestFixtures.parent(false, false);
        DeployTestFixtures.put(params, DeployDecisionWire.LEGAL_DESTINATIONS, "", "");
        return DeployTestFixtures.facts(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                params, DeployRoute.DEPLOY_PARENT);
    }

    private static PhysicalCard physical(DeployPhysicalCardRef ref,
                                         Zone zone,
                                         PhysicalCard atLocation) {
        return physical(ref, zone, atLocation, null, null, null, null);
    }

    private static PhysicalCard physical(DeployPhysicalCardRef ref,
                                         Zone zone,
                                         PhysicalCard atLocation,
                                         SwccgCardBlueprint blueprint) {
        return physical(ref, zone, atLocation, null, null, null, null, blueprint);
    }

    private static PhysicalCard physical(DeployPhysicalCardRef ref,
                                         Zone zone,
                                         PhysicalCard atLocation,
                                         PhysicalCard related,
                                         String partOfSystem,
                                         String systemOrbited,
                                         String title) {
        return physical(ref, zone, atLocation, related, partOfSystem,
                systemOrbited, title, CHARACTER_BLUEPRINT);
    }

    private static PhysicalCard physical(DeployPhysicalCardRef ref,
                                         Zone zone,
                                         PhysicalCard atLocation,
                                         PhysicalCard related,
                                         String partOfSystem,
                                         String systemOrbited,
                                         String title,
                                         SwccgCardBlueprint blueprint) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPermanentCardId" -> ref.permanentCardId();
                    case "getCardId" -> ref.currentCardId();
                    case "getZone" -> zone;
                    case "getBlueprint" -> blueprint;
                    case "getOwner" -> DeployTestFixtures.PLAYER;
                    case "isUndercover" -> false;
                    case "getAtLocation" -> atLocation;
                    case "getAttachedTo" -> null;
                    case "getRelatedStarshipOrVehicle" -> related;
                    case "getPartOfSystem" -> partOfSystem;
                    case "getSystemOrbited" -> systemOrbited;
                    case "getTitle" -> title;
                    case "getBlueprintId" -> "7_1";
                    case "toString" -> "PhysicalCard[" + ref + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static SwccgCardBlueprint blueprint() {
        return blueprint(null, null);
    }

    private static SwccgCardBlueprint blueprint(Float power, Float ability) {
        return (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgCardBlueprint.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCardCategory" -> CardCategory.CHARACTER;
                    case "hasPowerAttribute" -> power != null;
                    case "getPower" -> power != null ? power : 0f;
                    case "hasAbilityAttribute" -> ability != null;
                    case "getAbility" -> ability != null ? ability : 0f;
                    case "toString" -> "CharacterBlueprint[7_1]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static SwccgGame game(GameState state) {
        return game(state, null);
    }

    private static SwccgGame gameWithOpponentPower(GameState state, float opponentPower) {
        com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying modifiers =
                (com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying)
                Proxy.newProxyInstance(
                    com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying.class
                            .getClassLoader(),
                    new Class<?>[]{
                        com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying.class},
                    (proxy, method, args) -> {
                        if ("getTotalPowerAtLocation".equals(method.getName())) {
                            return "opponent-player".equals(args[2])
                                    ? opponentPower : 0f;
                        }
                        return defaultValue(method.getReturnType());
                    });
        return game(state, modifiers);
    }

    private static SwccgGame game(
            GameState state,
            com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying modifiers) {
        return (SwccgGame) Proxy.newProxyInstance(
                SwccgGame.class.getClassLoader(),
                new Class<?>[]{SwccgGame.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGameState" -> state;
                    case "getModifiersQuerying" -> modifiers;
                    case "isTestEnvironment" -> true;
                    case "toString" -> "DeployLifecycleGame";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class || type == short.class || type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        throw new IllegalArgumentException("unsupported primitive " + type);
    }

    private static List<Planner> planners() {
        List<Planner> planners = new ArrayList<>();
        planners.add(new RandoPlanner());
        planners.add(new ChosenOnePlanner());
        return planners;
    }

    private interface Planner {
        String name();

        DeployAssessment assess(DeployFacts facts, String wire, SwccgGame game);

        DeployAssessment assessWithInstructionReason(
                DeployFacts facts, String wire, String reason, SwccgGame game);

        void accept(DeployFacts facts, DeployAssessment assessment,
                    String wire, SwccgGame game);

        void reject(DeployFacts facts, String reason);

        void acceptPull(PullFacts facts, String wire, SwccgGame game);

        void observe(SwccgGame game, GameState state, String player, Phase phase);

        DeployTransaction current();

        DeployTransaction terminal();

        String lastReason();
    }

    private static final class RandoPlanner implements Planner {
        private final com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner delegate =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner();

        @Override public String name() { return "Rando"; }
        @Override public DeployAssessment assess(DeployFacts facts, String wire, SwccgGame game) {
            return delegate.assessDecision(facts, wire, null, game);
        }
        @Override public DeployAssessment assessWithInstructionReason(
                DeployFacts facts, String wire, String reason, SwccgGame game) {
            com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan plan =
                    new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan(
                            com.gempukku.swccgo.ai.models.rando.strategy.DeployStrategy.REINFORCE,
                            reason);
            com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction instruction =
                    new com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction(
                            "7_1", "fixture character", "301", "fixture site", 1, reason);
            instruction.setCardPermanentCardId(SOURCE.permanentCardId());
            instruction.setCardCurrentCardId(SOURCE.currentCardId());
            instruction.setDeployCost(4);
            plan.addInstruction(instruction);
            return delegate.assessDecision(facts, wire, plan, game);
        }
        @Override public void accept(DeployFacts facts, DeployAssessment assessment,
                                     String wire, SwccgGame game) {
            delegate.acceptDecision(facts, assessment, null, wire, game);
        }
        @Override public void reject(DeployFacts facts, String reason) {
            delegate.rejectDecision(facts, reason);
        }
        @Override public void acceptPull(PullFacts facts, String wire, SwccgGame game) {
            delegate.acceptPullDeployChild(facts, wire, null, game);
        }
        @Override public void observe(SwccgGame game, GameState state,
                                      String player, Phase phase) {
            delegate.observeGameState(game, state, player, phase);
        }
        @Override public DeployTransaction current() { return delegate.getCurrentTransaction(); }
        @Override public DeployTransaction terminal() { return delegate.getLastTerminalTransaction(); }
        @Override public String lastReason() { return delegate.getLastInvalidationReason(); }
    }

    private static final class ChosenOnePlanner implements Planner {
        private final com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner delegate =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner();

        @Override public String name() { return "ChosenOne"; }
        @Override public DeployAssessment assess(DeployFacts facts, String wire, SwccgGame game) {
            return delegate.assessDecision(facts, wire, null, game);
        }
        @Override public DeployAssessment assessWithInstructionReason(
                DeployFacts facts, String wire, String reason, SwccgGame game) {
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan plan =
                    new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan(
                            com.gempukku.swccgo.ai.models.chosenone.strategy.DeployStrategy.REINFORCE,
                            reason);
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction instruction =
                    new com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction(
                            "7_1", "fixture character", "301", "fixture site", 1, reason);
            instruction.setCardPermanentCardId(SOURCE.permanentCardId());
            instruction.setCardCurrentCardId(SOURCE.currentCardId());
            instruction.setDeployCost(4);
            plan.addInstruction(instruction);
            return delegate.assessDecision(facts, wire, plan, game);
        }
        @Override public void accept(DeployFacts facts, DeployAssessment assessment,
                                     String wire, SwccgGame game) {
            delegate.acceptDecision(facts, assessment, null, wire, game);
        }
        @Override public void reject(DeployFacts facts, String reason) {
            delegate.rejectDecision(facts, reason);
        }
        @Override public void acceptPull(PullFacts facts, String wire, SwccgGame game) {
            delegate.acceptPullDeployChild(facts, wire, null, game);
        }
        @Override public void observe(SwccgGame game, GameState state,
                                      String player, Phase phase) {
            delegate.observeGameState(game, state, player, phase);
        }
        @Override public DeployTransaction current() { return delegate.getCurrentTransaction(); }
        @Override public DeployTransaction terminal() { return delegate.getLastTerminalTransaction(); }
        @Override public String lastReason() { return delegate.getLastInvalidationReason(); }
    }

    private static final class LifecycleState extends GameState {
        private final int turn;
        private final int forcePile;
        private final List<PhysicalCard> cards;
        private final List<PhysicalCard> cardsAtLocation;

        private LifecycleState(int turn, int forcePile, PhysicalCard... cards) {
            this(turn, forcePile, List.of(), cards);
        }

        private LifecycleState(int turn, int forcePile,
                               List<PhysicalCard> cardsAtLocation,
                               PhysicalCard... cards) {
            this.turn = turn;
            this.forcePile = forcePile;
            this.cards = List.of(cards);
            this.cardsAtLocation = cardsAtLocation;
        }

        @Override public int getPlayersLatestTurnNumber(String playerId) { return turn; }
        @Override public int getForcePileSize(String playerId) { return forcePile; }
        @Override public String getOpponent(String playerId) { return "opponent-player"; }
        @Override public List<PhysicalCard> getCardsAtLocation(PhysicalCard location) {
            return cardsAtLocation;
        }
        @Override public List<PhysicalCard> getAllPermanentCards() { return cards; }
        @Override public PhysicalCard findCardByPermanentId(Integer permanentCardId) {
            for (PhysicalCard card : cards) {
                if (card != null && card.getPermanentCardId() == permanentCardId) {
                    return card;
                }
            }
            return null;
        }
    }
}
