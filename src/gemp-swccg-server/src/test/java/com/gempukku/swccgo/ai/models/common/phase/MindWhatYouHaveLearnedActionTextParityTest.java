package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MindWhatYouHaveLearnedActionTextParityTest {
    private static final String PLAYER_ID = "tester";

    @Test
    public void exactFlipScoresIdenticallyAndReturnsBeforeGenericDispatch() {
        GameState gameState = gameState();
        PhysicalCard objective = objective(
                PLAYER_ID, "225_53", Zone.SIDE_OF_TABLE, false);
        when(gameState.findCardById(53)).thenReturn(objective);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoContext(
                        gameState, null,
                        List.of("Flip"), List.of("53"))).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator().evaluate(chosenContext(
                        gameState, null,
                        List.of("Flip"), List.of("53"))).get(0);

        assertEquals(600.0f, rando.getScore(), 0.0f);
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        assertEquals(rando.getReasoning(), chosen.getReasoning());
        assertEquals(1, rando.getReasoning().size());
        assertTrue(rando.getReasoning().get(0).contains(
                ObjectiveFlipActionPolicy.MWYHL_FLIP_RULE_ID));
        assertFalse(rando.getReasoning().get(0).contains(
                "Unknown action type"));
    }

    @Test
    public void usefulSameSourceEffectPullSuppressesFlipAndWinsForBothBots() {
        GameState gameState = gameState();
        PhysicalCard objective = objective(
                PLAYER_ID, "225_53", Zone.SIDE_OF_TABLE, false);
        when(gameState.findCardById(53)).thenReturn(objective);
        PhysicalCard effect = effect("Wise Advice");
        SwccgGame game = gameWithDeployableReserve(
                gameState, objective, List.of(effect), true);

        List<String> actionTexts = List.of(
                "Flip", "Deploy Effect from Reserve Deck");
        List<String> sourceIds = List.of("53", "53");
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoContext(
                        gameState, game, actionTexts, sourceIds));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator().evaluate(chosenContext(
                        gameState, game, actionTexts, sourceIds));

        assertEquals(rando.get(0).getScore(), chosen.get(0).getScore(), 0.0f);
        assertEquals(rando.get(0).getReasoning(),
                chosen.get(0).getReasoning());
        assertEquals(rando.get(1).getScore(), chosen.get(1).getScore(), 0.0f);
        assertEquals(rando.get(1).getReasoning(),
                chosen.get(1).getReasoning());
        assertFalse(rando.get(0).getReasoning().toString().contains(
                ObjectiveFlipActionPolicy.MWYHL_FLIP_RULE_ID));
        assertTrue(rando.get(1).getScore() >= 100.0f);
        assertTrue(rando.get(1).getScore() > rando.get(0).getScore());
    }

    @Test
    public void usefulSameSourceDagobahPullSuppressesFlipAndWinsForBothBots() {
        GameState gameState = gameState();
        PhysicalCard objective = objective(
                PLAYER_ID, "225_53", Zone.SIDE_OF_TABLE, false);
        when(gameState.findCardById(53)).thenReturn(objective);
        PhysicalCard location = dagobahLocation();
        SwccgGame game = gameWithDeployableReserve(
                gameState, objective, List.of(location), true);

        List<String> actionTexts = List.of(
                "Flip",
                "Deploy Dagobah location from Reserve Deck");
        List<String> sourceIds = List.of("53", "53");
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoContext(
                        gameState, game, actionTexts, sourceIds));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator().evaluate(chosenContext(
                        gameState, game, actionTexts, sourceIds));

        assertEquals(rando.get(0).getScore(), chosen.get(0).getScore(), 0.0f);
        assertEquals(rando.get(0).getReasoning(),
                chosen.get(0).getReasoning());
        assertEquals(rando.get(1).getScore(), chosen.get(1).getScore(), 0.0f);
        assertEquals(rando.get(1).getReasoning(),
                chosen.get(1).getReasoning());
        assertFalse(rando.get(0).getReasoning().toString().contains(
                ObjectiveFlipActionPolicy.MWYHL_FLIP_RULE_ID));
        assertTrue(rando.get(1).getScore() >= 100.0f);
        assertTrue(rando.get(1).getScore() > rando.get(0).getScore());
    }

    @Test
    public void blockedUsefulSetupCannotSuppressFlipForEitherBot() {
        GameState gameState = gameState();
        PhysicalCard objective = objective(
                PLAYER_ID, "225_53", Zone.SIDE_OF_TABLE, false);
        when(gameState.findCardById(53)).thenReturn(objective);
        SwccgGame game = gameWithDeployableReserve(
                gameState, objective,
                List.of(effect("Wise Advice")), true);
        List<String> actionTexts = List.of(
                "Flip", "Deploy Effect from Reserve Deck");
        List<String> sourceIds = List.of("53", "53");
        var randoContext = randoContext(
                gameState, game, actionTexts, sourceIds);
        var chosenContext = chosenContext(
                gameState, game, actionTexts, sourceIds);
        randoContext.setBlockedResponses(Set.of("setup"));
        chosenContext.setBlockedResponses(Set.of("setup"));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator().evaluate(chosenContext);

        assertEquals(600.0f, rando.get(0).getScore(), 0.0f);
        assertEquals(rando.get(0).getScore(), chosen.get(0).getScore(), 0.0f);
        assertEquals(rando.get(0).getReasoning(),
                chosen.get(0).getReasoning());
        assertTrue(rando.get(0).getReasoning().toString().contains(
                ObjectiveFlipActionPolicy.MWYHL_FLIP_RULE_ID));
        assertTrue(rando.get(1).getScore() < -100.0f);
        assertEquals(rando.get(1).getScore(), chosen.get(1).getScore(), 0.0f);
    }

    @Test
    public void offeredButAbsentOrUnaffordableSetupCannotStrandFlip() {
        assertFlipStillScoresWithUnusableEffect(List.of(), true);
        assertFlipStillScoresWithUnusableEffect(
                List.of(effect("Yoda's Hope")), false);
    }

    @Test
    public void usefulSetupFromDifferentSourceDoesNotSuppressFlip() {
        GameState gameState = gameState();
        PhysicalCard objective = objective(
                PLAYER_ID, "225_53", Zone.SIDE_OF_TABLE, false);
        when(gameState.findCardById(53)).thenReturn(objective);
        SwccgGame game = gameWithDeployableReserve(
                gameState, objective,
                List.of(effect("Wise Advice")), true);

        List<String> actionTexts = List.of(
                "Flip", "Deploy Effect from Reserve Deck");
        List<String> sourceIds = List.of("53", "99");
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoContext(
                        gameState, game, actionTexts, sourceIds)).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator().evaluate(chosenContext(
                        gameState, game, actionTexts, sourceIds)).get(0);

        assertEquals(600.0f, rando.getScore(), 0.0f);
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        assertEquals(rando.getReasoning(), chosen.getReasoning());
    }

    @Test
    public void bespinPullNeverSuppressesBecauseItRemainsAfterFlip() {
        GameState gameState = gameState();
        PhysicalCard objective = objective(
                PLAYER_ID, "225_53", Zone.SIDE_OF_TABLE, false);
        when(gameState.findCardById(53)).thenReturn(objective);
        SwccgGame game = gameWithDeployableReserve(
                gameState, objective,
                List.of(mock(PhysicalCard.class)), true);

        List<String> actionTexts = List.of(
                "Flip",
                "Deploy Bespin location from Reserve Deck");
        List<String> sourceIds = List.of("53", "53");
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoContext(
                        gameState, game, actionTexts, sourceIds));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator().evaluate(chosenContext(
                        gameState, game, actionTexts, sourceIds));

        assertEquals(600.0f, rando.get(0).getScore(), 0.0f);
        assertEquals(rando.get(0).getScore(), chosen.get(0).getScore(), 0.0f);
        assertEquals(rando.get(0).getReasoning(),
                chosen.get(0).getReasoning());
    }

    private static GameState gameState() {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber(PLAYER_ID))
                .thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER_ID);
        when(gameState.getHand(PLAYER_ID)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getReserveDeckSize(PLAYER_ID)).thenReturn(5);
        when(gameState.getForcePileSize(PLAYER_ID)).thenReturn(10);
        return gameState;
    }

    private static PhysicalCard objective(
            String owner, String blueprintId,
            Zone zone, boolean flipped) {
        PhysicalCard objective = mock(PhysicalCard.class);
        when(objective.getPermanentCardId()).thenReturn(530);
        when(objective.getOwner()).thenReturn(owner);
        when(objective.getBlueprintId(true)).thenReturn(blueprintId);
        when(objective.getZone()).thenReturn(zone);
        when(objective.isFlipped()).thenReturn(flipped);
        return objective;
    }

    private static PhysicalCard effect(String title) {
        PhysicalCard effect = mock(PhysicalCard.class);
        when(effect.getTitles()).thenReturn(List.of(title));
        when(effect.isBlownAway()).thenReturn(false);
        return effect;
    }

    private static PhysicalCard dagobahLocation() {
        PhysicalCard location = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(location.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.LOCATION);
        when(location.getPartOfSystem()).thenReturn(Title.Dagobah);
        return location;
    }

    private static SwccgGame gameWithDeployableReserve(
            GameState gameState,
            PhysicalCard objective,
            List<PhysicalCard> reserve,
            boolean deployable) {
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.findCardByPermanentId(530))
                .thenReturn(objective);
        when(gameState.getReserveDeck(PLAYER_ID))
                .thenReturn(reserve);
        when(modifiers.isDeployable(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
        return game;
    }

    private static void assertFlipStillScoresWithUnusableEffect(
            List<PhysicalCard> reserve,
            boolean deployable) {
        GameState gameState = gameState();
        PhysicalCard objective = objective(
                PLAYER_ID, "225_53", Zone.SIDE_OF_TABLE, false);
        when(gameState.findCardById(53)).thenReturn(objective);
        SwccgGame game = gameWithDeployableReserve(
                gameState, objective, reserve, deployable);
        List<String> actionTexts = List.of(
                "Flip", "Deploy Effect from Reserve Deck");
        List<String> sourceIds = List.of("53", "53");

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoContext(
                        gameState, game, actionTexts, sourceIds)).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator().evaluate(chosenContext(
                        gameState, game, actionTexts, sourceIds)).get(0);

        assertEquals(600.0f, rando.getScore(), 0.0f);
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        assertEquals(rando.getReasoning(), chosen.getReasoning());
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    GameState gameState,
                    SwccgGame game,
                    List<String> actionTexts,
                    List<String> sourceIds) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                                gameState, PLAYER_ID,
                                "CARD_ACTION_CHOICE",
                                "Choose an action",
                                "mwyhl-flip", Phase.DEPLOY);
        context.setActionIds(List.of("flip", "setup")
                .subList(0, actionTexts.size()));
        context.setActionTexts(actionTexts);
        context.setCardIds(sourceIds);
        context.setGame(game);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    GameState gameState,
                    SwccgGame game,
                    List<String> actionTexts,
                    List<String> sourceIds) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                                gameState, PLAYER_ID,
                                "CARD_ACTION_CHOICE",
                                "Choose an action",
                                "mwyhl-flip", Phase.DEPLOY);
        context.setActionIds(List.of("flip", "setup")
                .subList(0, actionTexts.size()));
        context.setActionTexts(actionTexts);
        context.setCardIds(sourceIds);
        context.setGame(game);
        return context;
    }
}
