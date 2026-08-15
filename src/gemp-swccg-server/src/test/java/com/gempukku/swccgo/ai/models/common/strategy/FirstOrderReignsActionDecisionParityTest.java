package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.CardType;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Production evaluator proof for The First Order Reigns' unusual parent
 * action, its ordered child choices, and the shared low-Reserve guard.
 */
public class FirstOrderReignsActionDecisionParityTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";
    private static final String DOWNLOAD =
            "Deploy Supremacy card or battleground";
    private static final String NEAR_MATCH =
            "Deploy Supremacy card or battleground from Reserve Deck";

    private static final int OBJECTIVE_ID = 1;
    private static final int HOST_ID = 101;
    private static final int STAGE_ID = 103;
    private static final int FLEET_ID = 201;
    private static final int SUPREMACY_ID = 202;
    private static final int DISTRACTOR_ID = 203;
    private static final int COARSE_ID = 250;
    private static final int WRONG_COARSE_ID = 251;
    private static final int ALTERNATE_COARSE_ID = 252;
    private static final int OPPONENT_COARSE_ID = 253;

    @Test
    public void exactParentDownloadKeepsLowReserveGuardWithRouteCandidate() {
        List<Outcome> openWinners = new ArrayList<>();
        List<Outcome> openActions = new ArrayList<>();
        List<Outcome> closedWinners = new ArrayList<>();
        List<Outcome> closedActions = new ArrayList<>();

        for (Bot bot : Bot.values()) {
            Fixture open = fixture(bot, true);
            Decision decision = Decision.parent(DOWNLOAD);

            Outcome openAction = only(
                    actionText(bot, open, decision), "download");
            assertReason(openAction, "V60 RESERVE RISK");
            Outcome openWinner = combined(bot, open, decision);
            assertEquals("", openWinner.actionId());
            assertTrue(openAction.score() < -100.0f);
            openActions.add(openAction);
            openWinners.add(openWinner);

            Fixture closed = fixture(bot, false);
            Outcome closedAction = only(
                    actionText(bot, closed, decision), "download");
            assertReason(closedAction, "V60 RESERVE RISK");
            assertTrue(closedAction.score() < -100.0f);
            Outcome closedWinner = combined(bot, closed, decision);
            assertEquals("", closedWinner.actionId());
            closedActions.add(closedAction);
            closedWinners.add(closedWinner);
        }

        assertParity(openActions);
        assertParity(openWinners);
        assertParity(closedActions);
        assertParity(closedWinners);
    }

    @Test
    public void childSelectionStagesASystemThenChoosesThePhysicalSupremacy() {
        List<Outcome> stageWinners = new ArrayList<>();
        List<Outcome> supremacyWinners = new ArrayList<>();

        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, true);

            Outcome stage = combined(
                    bot, fixture,
                    Decision.child(
                            List.of(fixture.stage, fixture.supremacy)));
            assertEquals(String.valueOf(STAGE_ID), stage.actionId());
            assertReason(stage,
                    "Pull a missing location required by the counted objective");
            stageWinners.add(stage);

            fixture.deployStage();
            assertSame(fixture.supremacy,
                    fixture.gameState.findCardById(SUPREMACY_ID));

            Outcome supremacy = combined(
                    bot, fixture,
                    Decision.child(
                            List.of(fixture.supremacy, fixture.distractor)));
            assertEquals(String.valueOf(SUPREMACY_ID),
                    supremacy.actionId());
            assertReason(supremacy,
                    "Pull the typed actor required by the counted objective");
            supremacyWinners.add(supremacy);
        }

        assertParity(stageWinners);
        assertParity(supremacyWinners);
    }

    @Test
    public void deployEvaluatorKeepsTheSameLowReserveGuard() {
        List<Outcome> openActions = new ArrayList<>();
        List<Outcome> closedActions = new ArrayList<>();

        for (Bot bot : Bot.values()) {
            Decision decision = Decision.parent(DOWNLOAD);

            Outcome open = only(
                    deploy(bot, fixture(bot, true), decision),
                    "download");
            assertReason(open, "V60 RESERVE RISK");
            assertTrue(open.score() < -100.0f);
            openActions.add(open);

            Outcome closed = only(
                    deploy(bot, fixture(bot, false), decision),
                    "download");
            assertReason(closed, "V60 RESERVE RISK");
            assertTrue(closed.score() < -100.0f);
            closedActions.add(closed);
        }

        assertParity(openActions);
        assertParity(closedActions);
    }

    @Test
    public void genericNearMatchCannotBorrowTheObjectiveBypass() {
        List<Outcome> actionTextActions = new ArrayList<>();
        List<Outcome> deployActions = new ArrayList<>();
        List<Outcome> winners = new ArrayList<>();

        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, true);
            Decision decision = Decision.parent(NEAR_MATCH);

            Outcome actionText = only(
                    actionText(bot, fixture, decision), "download");
            Outcome deploy = only(
                    deploy(bot, fixture, decision), "download");
            assertReason(actionText, "V60 RESERVE RISK");
            assertReason(deploy, "V60 RESERVE RISK");
            actionTextActions.add(actionText);
            deployActions.add(deploy);

            Outcome winner = combined(bot, fixture, decision);
            assertEquals("", winner.actionId());
            winners.add(winner);
        }

        assertParity(actionTextActions);
        assertParity(deployActions);
        assertParity(winners);
    }

    @Test
    public void routeIdentityCannotBypassWhenTheEngineRejectsDeployment() {
        List<Outcome> actionTextActions = new ArrayList<>();
        List<Outcome> deployActions = new ArrayList<>();
        List<Outcome> winners = new ArrayList<>();

        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, true);
            setDeployable(fixture.modifiers, false);
            Decision decision = Decision.parent(DOWNLOAD);

            Outcome actionText = only(
                    actionText(bot, fixture, decision), "download");
            Outcome deploy = only(
                    deploy(bot, fixture, decision), "download");
            assertReason(actionText, "V60 RESERVE RISK");
            assertReason(deploy, "V60 RESERVE RISK");
            actionTextActions.add(actionText);
            deployActions.add(deploy);

            Outcome winner = combined(bot, fixture, decision);
            assertEquals("", winner.actionId());
            winners.add(winner);
        }

        assertParity(actionTextActions);
        assertParity(deployActions);
        assertParity(winners);
    }

    @Test
    public void coarseBattleDestinyCapBeatsPassOnlyForAnOpponentDrawAdvantage() {
        List<Outcome> actionScores = new ArrayList<>();
        List<Outcome> winners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, true);
            PhysicalCard coarse = card(
                    "Coarse And Rough And Irritating",
                    "200_109", Zone.SIDE_OF_TABLE,
                    CardCategory.EFFECT, null, COARSE_ID);
            when(fixture.gameState.findCardById(COARSE_ID))
                    .thenReturn(coarse);
            when(fixture.modifiers.getNumBattleDestinyDraws(
                    fixture.gameState, PLAYER,
                    false, false)).thenReturn(1);
            when(fixture.modifiers.getNumBattleDestinyDraws(
                    fixture.gameState, OPPONENT,
                    false, false)).thenReturn(3);

            Decision decision = Decision.coarse(
                    COARSE_ID, "200_109");
            Outcome action = only(
                    actionText(bot, fixture, decision),
                    "coarse");
            assertReason(action,
                    "COARSE");
            Outcome winner = combined(
                    bot, fixture, decision);
            assertEquals("coarse", winner.actionId());
            actionScores.add(action);
            winners.add(winner);

            when(fixture.modifiers.getNumBattleDestinyDraws(
                    fixture.gameState, PLAYER,
                    false, false)).thenReturn(2);
            when(fixture.modifiers.getNumBattleDestinyDraws(
                    fixture.gameState, OPPONENT,
                    false, false)).thenReturn(2);
            assertEquals("", combined(
                    bot, fixture, decision).actionId());

            PhysicalCard alternateCoarse = card(
                    "Coarse And Rough And Irritating",
                    "200_109^", Zone.SIDE_OF_TABLE,
                    CardCategory.EFFECT, null,
                    ALTERNATE_COARSE_ID);
            when(fixture.gameState.findCardById(
                    ALTERNATE_COARSE_ID))
                    .thenReturn(alternateCoarse);
            when(fixture.modifiers.getNumBattleDestinyDraws(
                    fixture.gameState, PLAYER,
                    false, false)).thenReturn(1);
            when(fixture.modifiers.getNumBattleDestinyDraws(
                    fixture.gameState, OPPONENT,
                    false, false)).thenReturn(3);
            assertEquals("coarse", combined(
                    bot, fixture,
                    Decision.coarse(
                        ALTERNATE_COARSE_ID,
                        "200_109^")).actionId());

            assertEquals("", combined(
                    bot, fixture,
                    Decision.coarse(
                        COARSE_ID, "200_109",
                        "Take stacked card into Lost Pile",
                        Phase.BATTLE)).actionId());
            assertEquals("", combined(
                    bot, fixture,
                    Decision.coarse(
                        COARSE_ID, "200_109",
                        "Take stacked card into hand",
                        Phase.DEPLOY)).actionId());

            PhysicalCard opponentCoarse = card(
                    "Coarse And Rough And Irritating",
                    "200_109*", Zone.SIDE_OF_TABLE,
                    CardCategory.EFFECT, null,
                    OPPONENT_COARSE_ID);
            when(opponentCoarse.getOwner()).thenReturn(OPPONENT);
            when(fixture.gameState.findCardById(
                    OPPONENT_COARSE_ID))
                    .thenReturn(opponentCoarse);
            assertEquals("", combined(
                    bot, fixture,
                    Decision.coarse(
                        OPPONENT_COARSE_ID,
                        "200_109*")).actionId());

            PhysicalCard wrongSource = card(
                    "Near Match", "fixture_200_109",
                    Zone.SIDE_OF_TABLE,
                    CardCategory.EFFECT, null,
                    WRONG_COARSE_ID);
            when(fixture.gameState.findCardById(
                    WRONG_COARSE_ID)).thenReturn(wrongSource);
            assertEquals("", combined(
                    bot, fixture,
                    Decision.coarse(
                        WRONG_COARSE_ID,
                        "fixture_200_109")).actionId());
        }
        assertParity(actionScores);
        assertParity(winners);
    }

    private static Outcome combined(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return outcome(
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .CombinedEvaluator()
                            .evaluateDecision(
                                    randoContext(fixture, decision)));
        }
        return outcome(
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .CombinedEvaluator()
                        .evaluateDecision(
                                chosenContext(fixture, decision)));
    }

    private static List<Outcome> actionText(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .ActionTextEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(FirstOrderReignsActionDecisionParityTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(FirstOrderReignsActionDecisionParityTest::outcome)
                .toList();
    }

    private static List<Outcome> deploy(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DeployEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(FirstOrderReignsActionDecisionParityTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .DeployEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(FirstOrderReignsActionDecisionParityTest::outcome)
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
                                "first-order-reigns-action-decision",
                                decision.phase());
        context.setGame(fixture.game);
        context.setSide(Side.DARK);
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
                                "first-order-reigns-action-decision",
                                decision.phase());
        context.setGame(fixture.game);
        context.setSide(Side.DARK);
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

    private static Fixture fixture(Bot bot, boolean routeCandidate) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);

        PhysicalCard objective = objective();
        PhysicalCard host = system(
                "D'Qar", "211_19", Zone.LOCATIONS,
                5, HOST_ID);
        PhysicalCard stage = system(
                "Kijimi", "214_6", Zone.RESERVE_DECK,
                6, STAGE_ID);
        PhysicalCard trackedFleet = card(
                "Tracked Fleet", "225_34",
                Zone.SIDE_OF_TABLE, CardCategory.EPIC_EVENT,
                null, FLEET_ID);
        PhysicalCard supremacy = card(
                "Supremacy", "225_27",
                Zone.RESERVE_DECK, CardCategory.STARSHIP,
                null, SUPREMACY_ID);
        PhysicalCard distractor = card(
                "Finalizer", "fixture_finalizer",
                Zone.RESERVE_DECK, CardCategory.STARSHIP,
                null, DISTRACTOR_ID);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(3);
        when(gameState.getForcePileSize(PLAYER)).thenReturn(10);
        when(gameState.getHand(PLAYER)).thenReturn(List.of());
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getLostPile(PLAYER)).thenReturn(List.of());
        when(gameState.getAllStackedCards()).thenReturn(List.of());

        when(trackedFleet.getAttachedTo()).thenReturn(host);
        when(modifiers.hasIcon(
                gameState, stage, Icon.EPISODE_VII)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, stage, null)).thenReturn(true);
        when(modifiers.getCardTypes(
                gameState, supremacy))
                .thenReturn(Set.of(CardType.STARSHIP));
        setActive(gameState, trackedFleet);

        List<PhysicalCard> permanents =
                new ArrayList<>(List.of(objective, trackedFleet));
        List<PhysicalCard> locations =
                new ArrayList<>(List.of(host));
        List<PhysicalCard> reserve = new ArrayList<>();
        if (routeCandidate) {
            reserve.add(stage);
            reserve.add(supremacy);
        } else {
            reserve.add(distractor);
        }

        when(gameState.getAllPermanentCards())
                .thenAnswer(invocation ->
                        new ArrayList<>(permanents));
        when(gameState.getLocationsInOrder())
                .thenAnswer(invocation ->
                        new ArrayList<>(locations));
        when(gameState.getTopLocations())
                .thenAnswer(invocation ->
                        new ArrayList<>(locations));
        when(gameState.getReserveDeck(PLAYER))
                .thenAnswer(invocation ->
                        new ArrayList<>(reserve));
        when(gameState.getCardPile(
                PLAYER, Zone.RESERVE_DECK))
                .thenAnswer(invocation ->
                        new ArrayList<>(reserve));
        when(gameState.getReserveDeckSize(PLAYER))
                .thenAnswer(invocation -> reserve.size());

        when(gameState.findCardById(OBJECTIVE_ID))
                .thenReturn(objective);
        when(gameState.findCardByPermanentId(OBJECTIVE_ID))
                .thenReturn(objective);
        when(gameState.findCardById(HOST_ID))
                .thenReturn(host);
        when(gameState.findCardById(STAGE_ID))
                .thenReturn(stage);
        when(gameState.findCardById(FLEET_ID))
                .thenReturn(trackedFleet);
        when(gameState.findCardById(SUPREMACY_ID))
                .thenReturn(supremacy);
        when(gameState.findCardById(DISTRACTOR_ID))
                .thenReturn(distractor);
        setDeployable(modifiers, routeCandidate);

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, Side.DARK);
        assertTrue(analyzer.isHydratedFromJson());
        assertEquals("The First Order Reigns",
                analyzer.getActivePlaybook().label);

        return new Fixture(
                analyzer, game, gameState, modifiers,
                stage, supremacy, distractor,
                locations, reserve);
    }

    private static void setDeployable(
            ModifiersQuerying modifiers,
            boolean deployable) {
        when(modifiers.isDeployable(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(),
                any(), anyBoolean(), anyFloat(),
                any(), any(), any(), any(), any(),
                any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
    }

    private static PhysicalCard objective() {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back = mock(SwccgCardBlueprint.class);

        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint()).thenReturn(front);
        when(card.getOtherSideBlueprint()).thenReturn(back);
        when(card.getBlueprintId(true)).thenReturn("225_32");
        when(card.getBlueprintId(false)).thenReturn("225_32");
        when(card.getPermanentCardId()).thenReturn(OBJECTIVE_ID);
        when(card.getCardId()).thenReturn(OBJECTIVE_ID);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isFlipped()).thenReturn(false);

        when(front.getTitle()).thenReturn("The First Order Reigns");
        when(front.getGameText()).thenReturn(
                "Deploy D'Qar and Crait systems, Salt Plateau, "
                        + "and Tracked Fleet. Once per turn, may deploy "
                        + "Supremacy card or battleground. Flip this card "
                        + "if Tracked Fleet is 'annihilated'.");
        when(front.getCardCategory())
                .thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle())
                .thenReturn("The Resistance Is Doomed");
        when(back.getCardCategory())
                .thenReturn(CardCategory.OBJECTIVE);
        return card;
    }

    private static PhysicalCard system(
            String title, String blueprintId, Zone zone,
            int parsec, int cardId) {
        PhysicalCard card = card(
                title, blueprintId, zone,
                CardCategory.LOCATION,
                CardSubtype.SYSTEM, cardId);
        when(card.getParsec()).thenReturn(parsec);
        return card;
    }

    private static PhysicalCard card(
            String title,
            String blueprintId,
            Zone zone,
            CardCategory category,
            CardSubtype subtype,
            int cardId) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);

        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isBlownAway()).thenReturn(false);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getCardCategory()).thenReturn(category);
        if (category == CardCategory.STARSHIP
                && title.contains("Supremacy")) {
            when(blueprint.hasIcon(
                    Icon.EPISODE_VII))
                    .thenReturn(true);
        }
        if (subtype != null) {
            when(blueprint.getCardSubtype()).thenReturn(subtype);
        }
        return card;
    }

    private static void setActive(
            GameState gameState, PhysicalCard card) {
        when(gameState.isCardInPlayActive(
                card, false, false, false,
                false, false, false, false, false))
                .thenReturn(true);
        when(gameState.isCardInPlayActive(
                card, true, false, false,
                false, false, false, false, false))
                .thenReturn(true);
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .EvaluatedAction action) {
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .EvaluatedAction action) {
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome only(
            List<Outcome> actions, String actionId) {
        return actions.stream()
                .filter(action -> action.actionId().equals(actionId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing action " + actionId + " in " + actions));
    }

    private static boolean hasReason(
            Outcome outcome, String fragment) {
        return outcome.reasoning().stream()
                .anyMatch(reason -> reason.contains(fragment));
    }

    private static void assertReason(
            Outcome outcome, String fragment) {
        assertTrue(
                "Expected '" + fragment + "' in "
                        + outcome.reasoning(),
                hasReason(outcome, fragment));
    }

    private static void assertParity(List<Outcome> outcomes) {
        assertEquals(2, outcomes.size());
        Outcome rando = outcomes.get(0);
        Outcome chosen = outcomes.get(1);
        assertEquals(rando.actionId(), chosen.actionId());
        assertEquals(
                Float.floatToRawIntBits(rando.score()),
                Float.floatToRawIntBits(chosen.score()));
        assertEquals(rando.reasoning(), chosen.reasoning());
        assertEquals(rando.hardVeto(), chosen.hardVeto());
        assertEquals(rando.vetoReason(), chosen.vetoReason());
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

    private record Decision(
            String type,
            String text,
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            boolean noPass,
            int min,
            Phase phase) {

        private static Decision parent(String actionText) {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    List.of("download"),
                    List.of(actionText),
                    List.of(String.valueOf(OBJECTIVE_ID)),
                    List.of("225_32"),
                    List.of("The First Order Reigns"),
                    false,
                    0,
                    Phase.DEPLOY);
        }

        private static Decision child(
                List<PhysicalCard> candidates) {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose card to deploy",
                    List.of(),
                    List.of(),
                    candidates.stream()
                            .map(card -> String.valueOf(
                                    card.getCardId()))
                            .toList(),
                    candidates.stream()
                            .map(card ->
                                    card.getBlueprintId(true))
                            .toList(),
                    candidates.stream()
                            .map(PhysicalCard::getTitle)
                            .toList(),
                    true,
                    1,
                    Phase.DEPLOY);
        }

        private static Decision coarse(
                int sourceId, String blueprintId) {
            return coarse(
                    sourceId, blueprintId,
                    "Take stacked card into hand",
                    Phase.BATTLE);
        }

        private static Decision coarse(
                int sourceId, String blueprintId,
                String actionText, Phase phase) {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose battle action or Pass",
                    List.of("coarse"),
                    List.of(actionText),
                    List.of(String.valueOf(sourceId)),
                    List.of(blueprintId),
                    List.of("Coarse And Rough And Irritating"),
                    false,
                    0,
                    phase);
        }
    }

    private record Fixture(
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard stage,
            PhysicalCard supremacy,
            PhysicalCard distractor,
            List<PhysicalCard> locations,
            List<PhysicalCard> reserve) {

        private void deployStage() {
            reserve.remove(stage);
            reserve.remove(distractor);
            reserve.add(distractor);
            when(stage.getZone()).thenReturn(Zone.LOCATIONS);
            locations.add(stage);
        }
    }
}
