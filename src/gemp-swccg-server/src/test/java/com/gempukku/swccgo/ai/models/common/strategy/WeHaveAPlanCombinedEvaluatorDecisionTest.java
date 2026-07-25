package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end AI decision proof for We Have A Plan through each bot's public,
 * production {@code CombinedEvaluator}. The physical cards are mocks, but every
 * blueprint is the real card-source blueprint: WHAP 14_52, Padme 11_8, and the
 * exact Naboo sites deployed by the objective.
 */
public class WeHaveAPlanCombinedEvaluatorDecisionTest {
    private static final String PLAYER = "light";
    private static final String OPPONENT = "dark";

    private static final int THRONE_ID = 101;
    private static final int HALLWAY_ID = 102;
    private static final int COURTYARD_ID = 103;
    private static final int SWAMP_ID = 104;
    private static final int PADME_ID = 201;

    private static final String DEPLOY_ROUTE_RULE =
            "DEPLOY.OBJECTIVE.ACTOR_ROUTE_STAGING";
    private static final String MOVE_DESTINATION_RULE =
            "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION";
    private static final String MOVE_START_RULE =
            "MOVE.OBJECTIVE.ACTOR_ROUTE_START";
    private static final String FLIP_BACK_HOLD_RULE =
            "MOVE.OBJECTIVE.FLIP_BACK_BLOCKER_HOLD";

    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void legalPadmeDeployActionBeatsPass() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false, null,
                    0.0f, 0.0f, null, 0.0f, false);

            Outcome result = combined(
                    bot, fixture, Decision.topLevelDeploy());
            assertEquals("deploy-padme", result.actionId());
            assertContains(result, "OBJECTIVE ACTOR STAGING");
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void deployPadmeToCourtyardBeatsLegalExteriorNabooDistractor() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false, null, 0.0f, 0.0f,
                    null, 0.0f, false);

            assertTrue(fixture.padme.getBlueprint().hasIcon(Icon.REPUBLIC));
            assertTrue(fixture.courtyard.getBlueprint()
                    .hasIcon(Icon.EXTERIOR_SITE));
            assertTrue(fixture.swamp.getBlueprint()
                    .hasIcon(Icon.EXTERIOR_SITE));

            Decision decision = Decision.deploy(
                    "Choose where to deploy <div class='cardHint' value='11_8'>"
                            + "Padme Naberrie</div>",
                    List.of(String.valueOf(COURTYARD_ID),
                            String.valueOf(SWAMP_ID)),
                    List.of("12_81", "12_80"),
                    List.of(fixture.courtyard.getTitle(),
                            fixture.swamp.getTitle()));
            TracedOutcome traced = tracedCombined(bot, fixture, decision);
            Outcome result = traced.outcome();
            assertEquals(String.valueOf(COURTYARD_ID), result.actionId());
            assertContains(result, "stage the exact flip-gate route");
            assertHasTypedRule(
                    traced.trace(), String.valueOf(COURTYARD_ID),
                    DEPLOY_ROUTE_RULE);
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void courtyardToHallwayBeatsAdjacentNabooDistractor() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false, fixtureSite(COURTYARD_ID),
                    3.0f, 0.0f, null, 0.0f, false);
            assertEquals(1, fixture.distance(
                    fixture.courtyard, fixture.hallway));
            assertEquals(1, fixture.distance(
                    fixture.courtyard, fixture.swamp));

            Outcome result = combined(bot, fixture, Decision.moveDestination(
                    List.of(String.valueOf(HALLWAY_ID),
                            String.valueOf(SWAMP_ID)),
                    List.of("14_51", "12_80"),
                    List.of(fixture.hallway.getTitle(),
                            fixture.swamp.getTitle())));
            assertEquals(String.valueOf(HALLWAY_ID), result.actionId());
            assertContains(result, MOVE_DESTINATION_RULE);
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void hallwayToThroneBeatsBackwardCourtyard() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false, fixtureSite(HALLWAY_ID),
                    3.0f, 0.0f, null, 0.0f, false);

            Outcome result = combined(bot, fixture, Decision.moveDestination(
                    List.of(String.valueOf(THRONE_ID),
                            String.valueOf(COURTYARD_ID)),
                    List.of("12_83", "12_81"),
                    List.of(fixture.throne.getTitle(),
                            fixture.courtyard.getTitle())));
            assertEquals(String.valueOf(THRONE_ID), result.actionId());
            assertContains(result, MOVE_DESTINATION_RULE);
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void boostedLegalLandspeedCanAdvanceDirectlyToTheGate() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false,
                    fixtureSite(COURTYARD_ID),
                    3.0f, 0.0f, null, 0.0f, false);
            when(fixture.modifiers.getLandspeed(
                    fixture.gameState, fixture.padme))
                    .thenReturn(2.0f);

            Outcome result = combined(bot, fixture,
                    Decision.moveDestination(
                            List.of(String.valueOf(THRONE_ID),
                                    String.valueOf(SWAMP_ID)),
                            List.of("12_83", "12_80"),
                            List.of(fixture.throne.getTitle(),
                                    fixture.swamp.getTitle())));
            assertEquals(String.valueOf(THRONE_ID), result.actionId());
            assertContains(result, MOVE_DESTINATION_RULE);
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void safePreFlipLandspeedActionBeatsPass() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false, fixtureSite(COURTYARD_ID),
                    3.0f, 0.0f, null, 0.0f, false);

            Outcome result = combined(
                    bot, fixture, Decision.topLevelMove());
            assertEquals("move-padme", result.actionId());
            assertContains(result, MOVE_START_RULE);
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void prohibitedTowardHopCannotClaimTheParentMoveDoctrine() {
        List<Outcome> moves = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false,
                    fixtureSite(COURTYARD_ID),
                    3.0f, 0.0f, null, 0.0f, false);
            when(fixture.modifiers
                    .mayNotMoveFromLocationToLocationUsingLandspeed(
                            fixture.gameState, fixture.padme,
                            fixture.courtyard, fixture.hallway, false))
                    .thenReturn(true);

            Outcome move = only(
                    moveAdapter(bot, fixture, Decision.topLevelMove()),
                    "move-padme");
            assertNotContains(move, MOVE_START_RULE);
            moves.add(move);
        }
        assertParity(moves);
    }

    @Test
    public void postFlipContestedSoleBlockerHoldsAtGapSixButMayRetreatAtSeven() {
        List<Outcome> holdResults = new ArrayList<>();
        List<Outcome> holdMoves = new ArrayList<>();
        List<Outcome> retreatResults = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture hold = fixture(bot, true, fixtureSite(THRONE_ID),
                    8.0f, 14.0f, null, 0.0f, true);
            Decision move = Decision.topLevelMove();

            Outcome holdMove = only(
                    moveAdapter(bot, hold, move), "move-padme");
            assertContains(holdMove, FLIP_BACK_HOLD_RULE);
            assertTrue(holdMove.score() <= -90000.0f);
            holdMoves.add(holdMove);

            Outcome holdWinner = combined(bot, hold, move);
            assertEquals("pass", holdWinner.actionId());
            holdResults.add(holdWinner);

            Fixture retreat = fixture(bot, true, fixtureSite(THRONE_ID),
                    8.0f, 15.0f, null, 0.0f, true);
            Outcome retreatMove = only(
                    moveAdapter(bot, retreat, move), "move-padme");
            assertNotContains(retreatMove, FLIP_BACK_HOLD_RULE);
            assertTrue(retreatMove.score() > -90000.0f);
            retreatResults.add(retreatMove);
        }
        assertParity(holdResults);
        assertParity(holdMoves);
        assertParity(retreatResults);
    }

    @Test
    public void formationSafetyVetoesDoomedCloserHopAndSafeDistractorWins() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, false, fixtureSite(COURTYARD_ID),
                    3.0f, 0.0f, fixtureSite(HALLWAY_ID), 12.0f, false);
            Decision decision = Decision.moveDestination(
                    List.of(String.valueOf(HALLWAY_ID),
                            String.valueOf(SWAMP_ID)),
                    List.of("14_51", "12_80"),
                    List.of(fixture.hallway.getTitle(),
                            fixture.swamp.getTitle()));

            Outcome doomedRoute = only(
                    cardSelectionAdapter(bot, fixture, decision),
                    String.valueOf(HALLWAY_ID));
            assertContains(doomedRoute, MOVE_DESTINATION_RULE);
            assertTrue(doomedRoute.hardVeto());
            assertTrue(doomedRoute.vetoReason().contains(
                    "L4 SOLO CHARGE"));

            Outcome result = combined(bot, fixture, decision);
            assertEquals(String.valueOf(SWAMP_ID), result.actionId());
            results.add(result);
        }
        assertParity(results);
    }

    private static Outcome combined(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            var result =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .CombinedEvaluator()
                            .evaluateDecision(
                                    randoContext(fixture, decision));
            return outcome(result);
        }
        var result =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .CombinedEvaluator()
                        .evaluateDecision(
                                chosenContext(fixture, decision));
        return outcome(result);
    }

    private static TracedOutcome tracedCombined(
            Bot bot, Fixture fixture, Decision decision) {
        List<String> rawCandidates =
                "CARD_SELECTION".equals(decision.type())
                        ? decision.cardIds() : decision.actionIds();
        assertTrue(TraceSession.open(
                bot.name(), "whap-production-decision",
                decision.type(), decision.text(),
                rawCandidates, null,
                List.of("focused evaluator fixture omits the bot-boundary snapshot"),
                false));
        Outcome outcome;
        DecisionTrace trace;
        try {
            outcome = combined(bot, fixture, decision);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedOutcome(outcome, trace);
    }

    private static List<Outcome> moveAdapter(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .MoveEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream().map(
                            WeHaveAPlanCombinedEvaluatorDecisionTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .MoveEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream().map(
                        WeHaveAPlanCombinedEvaluatorDecisionTest::outcome)
                .toList();
    }

    private static List<Outcome> cardSelectionAdapter(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream().map(
                            WeHaveAPlanCombinedEvaluatorDecisionTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CardSelectionEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream().map(
                        WeHaveAPlanCombinedEvaluatorDecisionTest::outcome)
                .toList();
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture, Decision decision) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                                fixture.gameState, PLAYER,
                                decision.type(), decision.text(),
                                "whap-production-decision",
                                decision.phase());
        context.setGame(fixture.game);
        context.setSide(Side.LIGHT);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    Fixture fixture, Decision decision) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                                fixture.gameState, PLAYER,
                                decision.type(), decision.text(),
                                "whap-production-decision",
                                decision.phase());
        context.setGame(fixture.game);
        context.setSide(Side.LIGHT);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds());
        context.setActionTexts(decision.actionTexts());
        context.setCardIds(decision.cardIds());
        context.setBlueprints(decision.blueprints());
        context.setTestingTexts(decision.testingTexts());
        context.setSelectable(decision.cardIds().stream()
                .map(ignored -> true).toList());
        context.setNoPass(decision.noPass());
        context.setMin(decision.min());
        context.setMax(1);
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds());
        context.setActionTexts(decision.actionTexts());
        context.setCardIds(decision.cardIds());
        context.setBlueprints(decision.blueprints());
        context.setTestingTexts(decision.testingTexts());
        context.setSelectable(decision.cardIds().stream()
                .map(ignored -> true).toList());
        context.setNoPass(decision.noPass());
        context.setMin(decision.min());
        context.setMax(1);
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome only(List<Outcome> actions, String actionId) {
        return actions.stream()
                .filter(action -> action.actionId().equals(actionId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing action " + actionId + " in " + actions));
    }

    private static void assertContains(Outcome outcome, String marker) {
        assertTrue(
                "Expected '" + marker + "' in " + outcome.reasoning(),
                outcome.reasoning().stream()
                        .anyMatch(reason -> reason.contains(marker)));
    }

    private static void assertNotContains(Outcome outcome, String marker) {
        assertFalse(
                "Did not expect '" + marker + "' in " + outcome.reasoning(),
                outcome.reasoning().stream()
                        .anyMatch(reason -> reason.contains(marker)));
    }

    private static void assertHasTypedRule(
            DecisionTrace trace, String actionId, String ruleId) {
        assertTrue(
                "Expected typed rule '" + ruleId + "' for " + actionId
                        + " in " + trace.getOperations(),
                trace.getOperations().stream().anyMatch(
                        operation -> actionId.equals(operation.getActionId())
                                && ruleId.equals(
                                        operation.getRuleId().id())));
    }

    private static void assertParity(List<Outcome> outcomes) {
        assertEquals(2, outcomes.size());
        Outcome rando = outcomes.get(0);
        Outcome chosen = outcomes.get(1);
        assertEquals(rando.actionId(), chosen.actionId());
        assertEquals(Float.floatToRawIntBits(rando.score()),
                Float.floatToRawIntBits(chosen.score()));
        assertEquals(rando.reasoning(), chosen.reasoning());
        assertEquals(rando.hardVeto(), chosen.hardVeto());
        assertEquals(rando.vetoReason(), chosen.vetoReason());
    }

    private static Fixture fixture(
            Bot bot,
            boolean flipped,
            Integer originId,
            float friendlyPowerAtOrigin,
            float opponentPowerAtOrigin,
            Integer doomedDestinationId,
            float opponentPowerAtDoomedDestination,
            boolean opponentOccupiesThrone) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);

        PhysicalCard throne = location("12_83", THRONE_ID);
        PhysicalCard hallway = location("14_51", HALLWAY_ID);
        PhysicalCard courtyard = location("12_81", COURTYARD_ID);
        PhysicalCard swamp = location("12_80", SWAMP_ID);
        PhysicalCard objective = objective(flipped);
        PhysicalCard padme = padme(originId == null
                ? Zone.HAND : Zone.AT_LOCATION);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        setDeployable(modifiers, true);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(3);
        when(gameState.getForcePileSize(PLAYER)).thenReturn(10);
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(20);
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getAllStackedCards()).thenReturn(List.of());
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(throne, hallway, courtyard, swamp));
        when(gameState.getTopLocations()).thenReturn(
                List.of(throne, hallway, courtyard, swamp));

        when(gameState.findCardById(THRONE_ID)).thenReturn(throne);
        when(gameState.findCardById(HALLWAY_ID)).thenReturn(hallway);
        when(gameState.findCardById(COURTYARD_ID)).thenReturn(courtyard);
        when(gameState.findCardById(SWAMP_ID)).thenReturn(swamp);
        when(gameState.findCardById(PADME_ID)).thenReturn(padme);
        when(gameState.findCardByPermanentId(THRONE_ID))
                .thenReturn(throne);
        when(gameState.findCardByPermanentId(HALLWAY_ID))
                .thenReturn(hallway);
        when(gameState.findCardByPermanentId(COURTYARD_ID))
                .thenReturn(courtyard);
        when(gameState.findCardByPermanentId(SWAMP_ID))
                .thenReturn(swamp);
        when(gameState.findCardByPermanentId(PADME_ID))
                .thenReturn(padme);

        PhysicalCard origin = site(
                originId, throne, hallway, courtyard, swamp);
        when(padme.getAtLocation()).thenReturn(origin);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, padme)).thenReturn(origin);
        when(modifiers.getLocationThatCardIsAt(
                gameState, padme)).thenReturn(origin);
        when(modifiers.hasPersona(
                gameState, padme, Persona.AMIDALA)).thenReturn(true);
        when(modifiers.hasAbility(
                gameState, padme, true)).thenReturn(true);

        List<PhysicalCard> permanents = new ArrayList<>();
        permanents.add(objective);
        if (origin != null) {
            permanents.add(padme);
        }
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getHand(PLAYER)).thenReturn(
                origin == null ? List.of(padme) : List.of());

        for (PhysicalCard location
                : List.of(throne, hallway, courtyard, swamp)) {
            when(modifiers.getLocationHere(gameState, location))
                    .thenReturn(location);
            List<PhysicalCard> cardsHere =
                    location == origin ? List.of(padme) : List.of();
            when(gameState.getCardsAtLocation(location))
                    .thenReturn(cardsHere);
            when(modifiers.isBattleground(
                    gameState, location, null)).thenReturn(true);
            when(gameState.getAttachedCards(padme)).thenReturn(List.of());
        }

        setPower(modifiers, gameState, throne, PLAYER, 0.0f);
        setPower(modifiers, gameState, throne, OPPONENT, 0.0f);
        setPower(modifiers, gameState, hallway, PLAYER, 0.0f);
        setPower(modifiers, gameState, hallway, OPPONENT, 0.0f);
        setPower(modifiers, gameState, courtyard, PLAYER, 0.0f);
        setPower(modifiers, gameState, courtyard, OPPONENT, 0.0f);
        setPower(modifiers, gameState, swamp, PLAYER, 0.0f);
        setPower(modifiers, gameState, swamp, OPPONENT, 0.0f);
        if (origin != null) {
            setPower(modifiers, gameState, origin, PLAYER,
                    friendlyPowerAtOrigin);
            setPower(modifiers, gameState, origin, OPPONENT,
                    opponentPowerAtOrigin);
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, PLAYER, origin)).thenReturn(3.0f);
        }
        PhysicalCard doomedDestination = site(
                doomedDestinationId, throne, hallway, courtyard, swamp);
        if (doomedDestination != null) {
            setPower(modifiers, gameState, doomedDestination,
                    OPPONENT, opponentPowerAtDoomedDestination);
        }

        when(modifiers.controlsLocation(
                gameState, throne, PLAYER,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(false);
        when(modifiers.controlsLocation(
                gameState, throne, OPPONENT,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(false);
        when(modifiers.occupiesLocation(
                gameState, throne, OPPONENT,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(opponentOccupiesThrone);

        distance(modifiers, gameState, throne, hallway, 1);
        distance(modifiers, gameState, hallway, courtyard, 1);
        distance(modifiers, gameState, courtyard, swamp, 1);
        distance(modifiers, gameState, throne, courtyard, 2);
        distance(modifiers, gameState, throne, swamp, 3);
        distance(modifiers, gameState, hallway, swamp, 2);
        distance(modifiers, gameState, throne, throne, 0);
        distance(modifiers, gameState, hallway, hallway, 0);
        distance(modifiers, gameState, courtyard, courtyard, 0);
        distance(modifiers, gameState, swamp, swamp, 0);
        when(modifiers.getSitesBetween(
                gameState, courtyard, throne))
                .thenReturn(List.of(hallway));
        when(modifiers.getSitesBetween(
                gameState, throne, courtyard))
                .thenReturn(List.of(hallway));
        when(modifiers.getSitesBetween(
                gameState, hallway, throne))
                .thenReturn(List.of());
        when(modifiers.getSitesBetween(
                gameState, throne, hallway))
                .thenReturn(List.of());
        when(modifiers.getSitesBetween(
                gameState, courtyard, hallway))
                .thenReturn(List.of());
        when(modifiers.getSitesBetween(
                gameState, hallway, courtyard))
                .thenReturn(List.of());

        when(modifiers.getLandspeed(gameState, padme))
                .thenReturn(1.0f);
        when(modifiers.getForceAvailableToUse(gameState, PLAYER))
                .thenReturn(10);
        if (origin != null) {
            for (PhysicalCard destination
                    : List.of(throne, hallway, courtyard, swamp)) {
                Integer required = modifiers.getDistanceBetweenSites(
                        gameState, origin, destination);
                when(modifiers.getLandspeedRequired(
                        gameState, padme, destination))
                        .thenReturn(required);
                when(modifiers.getMoveUsingLandspeedCost(
                        gameState, padme, origin, destination,
                        false, 0.0f)).thenReturn(1.0f);
            }
        }

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, Side.LIGHT);
        assertEquals("We Have A Plan", analyzer.getActivePlaybook().label);

        return new Fixture(
                analyzer, game, gameState, modifiers,
                throne, hallway, courtyard, swamp, padme);
    }

    private static void setPower(
            ModifiersQuerying modifiers,
            GameState gameState,
            PhysicalCard location,
            String playerId,
            float power) {
        when(modifiers.getTotalPowerAtLocation(
                gameState, location, playerId, false, false))
                .thenReturn(power);
    }

    private static void setDeployable(
            ModifiersQuerying modifiers, boolean deployable) {
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
    }

    private static void distance(
            ModifiersQuerying modifiers,
            GameState gameState,
            PhysicalCard first,
            PhysicalCard second,
            int distance) {
        when(modifiers.getDistanceBetweenSites(
                gameState, first, second)).thenReturn(distance);
        when(modifiers.getDistanceBetweenSites(
                gameState, second, first)).thenReturn(distance);
    }

    private static PhysicalCard objective(boolean flipped) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front = blueprint("14_52");
        SwccgCardBlueprint back = blueprint("14_52_BACK");
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint()).thenReturn(front);
        when(card.getOtherSideBlueprint()).thenReturn(back);
        when(card.getBlueprintId(true)).thenReturn("14_52");
        when(card.getBlueprintId(false)).thenReturn("14_52");
        when(card.getTitle()).thenReturn(front.getTitle());
        when(card.getTitles()).thenReturn(List.of(front.getTitle()));
        when(card.isFlipped()).thenReturn(flipped);
        return card;
    }

    private static PhysicalCard padme(Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint("11_8");
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn("11_8");
        when(card.getBlueprintId(false)).thenReturn("11_8");
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getPermanentCardId()).thenReturn(PADME_ID);
        when(card.getCardId()).thenReturn(PADME_ID);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isUndercover()).thenReturn(false);
        when(card.isCaptive()).thenReturn(false);
        return card;
    }

    private static PhysicalCard location(String blueprintId, int cardId) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.LOCATIONS);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isBlownAway()).thenReturn(false);
        return card;
    }

    private static SwccgCardBlueprint blueprint(String id) {
        SwccgCardBlueprint blueprint = CARDS.getSwccgoCardBlueprint(id);
        assertNotNull("Missing real blueprint " + id, blueprint);
        return blueprint;
    }

    private static Integer fixtureSite(int cardId) {
        return cardId;
    }

    private static PhysicalCard site(
            Integer cardId,
            PhysicalCard throne,
            PhysicalCard hallway,
            PhysicalCard courtyard,
            PhysicalCard swamp) {
        if (cardId == null) return null;
        return switch (cardId) {
            case THRONE_ID -> throne;
            case HALLWAY_ID -> hallway;
            case COURTYARD_ID -> courtyard;
            case SWAMP_ID -> swamp;
            default -> throw new IllegalArgumentException(
                    "Unknown fixture site " + cardId);
        };
    }

    private enum Bot {
        RANDO,
        CHOSEN_ONE
    }

    private record Outcome(
            String actionId,
            float score,
            List<String> reasoning,
            boolean hardVeto,
            String vetoReason) {
    }

    private record TracedOutcome(
            Outcome outcome,
            DecisionTrace trace) {
    }

    private record Decision(
            String type,
            String text,
            Phase phase,
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            boolean noPass,
            int min) {

        private static Decision deploy(
                String text,
                List<String> cardIds,
                List<String> blueprints,
                List<String> testingTexts) {
            return new Decision(
                    "CARD_SELECTION", text, Phase.DEPLOY,
                    List.of(), List.of(),
                    cardIds, blueprints, testingTexts,
                    true, 1);
        }

        private static Decision topLevelDeploy() {
            return new Decision(
                    "CARD_ACTION_CHOICE", "Choose deploy action",
                    Phase.DEPLOY,
                    List.of("deploy-padme", "pass"),
                    List.of(
                            "Deploy <div class='cardHint' value='11_8'>"
                                    + "Padme Naberrie</div>",
                            "Pass"),
                    List.of(String.valueOf(PADME_ID), ""),
                    List.of("11_8", ""),
                    List.of(fixtureTitle("11_8"), "Pass"),
                    false, 0);
        }

        private static Decision moveDestination(
                List<String> cardIds,
                List<String> blueprints,
                List<String> testingTexts) {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to move "
                            + "<div class='cardHint' value='11_8'>"
                            + "Padme Naberrie</div> using landspeed",
                    Phase.MOVE,
                    List.of(), List.of(),
                    cardIds, blueprints, testingTexts,
                    true, 1);
        }

        private static Decision topLevelMove() {
            return new Decision(
                    "ACTION_CHOICE", "Choose move action",
                    Phase.MOVE,
                    List.of("move-padme", "pass"),
                    List.of("Move using landspeed", "Pass"),
                    List.of(String.valueOf(PADME_ID), ""),
                    List.of(), List.of(),
                    false, 0);
        }

        private static String fixtureTitle(String blueprintId) {
            return blueprint(blueprintId).getTitle();
        }
    }

    private record Fixture(
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard throne,
            PhysicalCard hallway,
            PhysicalCard courtyard,
            PhysicalCard swamp,
            PhysicalCard padme) {

        private int distance(PhysicalCard first, PhysicalCard second) {
            return modifiers.getDistanceBetweenSites(
                    gameState, first, second);
        }
    }
}
