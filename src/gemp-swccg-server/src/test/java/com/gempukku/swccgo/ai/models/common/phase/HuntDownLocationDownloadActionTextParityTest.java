package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HuntDownLocationDownloadActionTextParityTest {
    private static final String PLAYER_ID = "tester";
    private static final String ACTION_TEXT =
            "Deploy a location from Reserve Deck";

    @Test
    public void exactVirtualObjectiveActionScoresForBothBots() {
        GameState gameState = gameState();
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard objective = objective();
        when(gameState.findCardById(31)).thenReturn(objective);

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer.class);
        when(randoAnalyzer.isAnalyzed()).thenReturn(true);
        when(randoAnalyzer.isVirtualHuntDownObjective()).thenReturn(true);
        when(randoAnalyzer.hasVirtualHuntDownLocationDownloadInReserve(
                game, PLAYER_ID)).thenReturn(true);
        var randoContext = randoContext(gameState, game, randoAnalyzer);

        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer.class);
        when(chosenAnalyzer.isAnalyzed()).thenReturn(true);
        when(chosenAnalyzer.isVirtualHuntDownObjective()).thenReturn(true);
        when(chosenAnalyzer.hasVirtualHuntDownLocationDownloadInReserve(
                game, PLAYER_ID)).thenReturn(true);
        var chosenContext = chosenContext(
                gameState, game, chosenAnalyzer);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoContext).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator().evaluate(chosenContext).get(0);

        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        assertEquals(rando.getReasoning(), chosen.getReasoning());
        assertTrue(rando.getReasoning().toString(),
                rando.getScore() > 300.0f);
        assertTrue(rando.getReasoning().stream().anyMatch(
                reason -> reason.contains(
                        "objective action")));
    }

    @Test
    public void castlePriorityRequiresExactSourceAndDoesNotLeakToWeaponLevitation() {
        GameState gameState = gameState();
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);
        when(gameState.getForcePileSize(PLAYER_ID)).thenReturn(6);

        PhysicalCard castle = sourceCard(
                "Mustafar: Vader's Castle", "209_50");
        PhysicalCard weaponLevitation = sourceCard(
                "Weapon Levitation", "601_98");
        when(gameState.findCardById(50)).thenReturn(castle);
        when(gameState.findCardById(98)).thenReturn(weaponLevitation);

        var randoAnalyzer = mock(
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer.class);
        when(randoAnalyzer.isAnalyzed()).thenReturn(true);
        when(randoAnalyzer.isHuntDownV()).thenReturn(true);
        when(randoAnalyzer.hasLegalVaderCastleDeployInReserve(
                game, PLAYER_ID)).thenReturn(true);
        when(randoAnalyzer.hasVaderCastleDeployWithMoveReserve(
                game, PLAYER_ID)).thenReturn(true);
        var chosenAnalyzer = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer.class);
        when(chosenAnalyzer.isAnalyzed()).thenReturn(true);
        when(chosenAnalyzer.isHuntDownV()).thenReturn(true);
        when(chosenAnalyzer.hasLegalVaderCastleDeployInReserve(
                game, PLAYER_ID)).thenReturn(true);
        when(chosenAnalyzer.hasVaderCastleDeployWithMoveReserve(
                game, PLAYER_ID)).thenReturn(true);

        var randoCastle = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoVaderContext(
                        gameState, game, randoAnalyzer, "50")).get(0);
        var chosenCastle =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .ActionTextEvaluator().evaluate(chosenVaderContext(
                                gameState, game, chosenAnalyzer, "50")).get(0);
        assertEquals(randoCastle.getScore(), chosenCastle.getScore(), 0.0f);
        assertEquals(randoCastle.getReasoning(),
                chosenCastle.getReasoning());
        assertTrue(randoCastle.getReasoning().toString(),
                randoCastle.getReasoning().stream().anyMatch(
                        reason -> reason.contains(
                                "V25 HUNT DOWN: DEPLOY VADER NOW")));

        var randoWeapon = new com.gempukku.swccgo.ai.models.rando.evaluators
                .ActionTextEvaluator().evaluate(randoVaderContext(
                        gameState, game, randoAnalyzer, "98")).get(0);
        var chosenWeapon =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .ActionTextEvaluator().evaluate(chosenVaderContext(
                                gameState, game, chosenAnalyzer, "98")).get(0);
        assertEquals(randoWeapon.getScore(), chosenWeapon.getScore(), 0.0f);
        assertEquals(randoWeapon.getReasoning(),
                chosenWeapon.getReasoning());
        assertFalse(randoWeapon.getReasoning().toString(),
                randoWeapon.getReasoning().stream().anyMatch(
                        reason -> reason.contains("V25 HUNT DOWN")));
    }

    private static GameState gameState() {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber(PLAYER_ID))
                .thenReturn(1);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER_ID);
        when(gameState.getHand(PLAYER_ID)).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getReserveDeckSize(PLAYER_ID)).thenReturn(5);
        when(gameState.getReserveDeck(PLAYER_ID)).thenReturn(List.of(
                mock(PhysicalCard.class),
                mock(PhysicalCard.class),
                mock(PhysicalCard.class),
                mock(PhysicalCard.class),
                mock(PhysicalCard.class)));
        return gameState;
    }

    private static PhysicalCard objective() {
        PhysicalCard objective = mock(PhysicalCard.class);
        when(objective.getTitle()).thenReturn(
                "Hunt Down And Destroy The Jedi (V)");
        when(objective.getBlueprintId(true)).thenReturn("213_31");
        return objective;
    }

    private static PhysicalCard sourceCard(
            String title, String blueprintId) {
        PhysicalCard source = mock(PhysicalCard.class);
        when(source.getTitle()).thenReturn(title);
        when(source.getBlueprintId(true)).thenReturn(blueprintId);
        return source;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    GameState gameState,
                    SwccgGame game,
                    com.gempukku.swccgo.ai.models.rando.strategy
                            .ObjectiveAnalyzer analyzer) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                                gameState, PLAYER_ID,
                                "CARD_ACTION_CHOICE",
                                "Choose deploy action",
                                "hunt-download", Phase.DEPLOY);
        context.setActionIds(List.of("download"));
        context.setActionTexts(List.of(ACTION_TEXT));
        context.setCardIds(List.of("31"));
        context.setGame(game);
        context.setObjectiveAnalyzer(analyzer);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    GameState gameState,
                    SwccgGame game,
                    com.gempukku.swccgo.ai.models.chosenone.strategy
                            .ObjectiveAnalyzer analyzer) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                                gameState, PLAYER_ID,
                                "CARD_ACTION_CHOICE",
                                "Choose deploy action",
                                "hunt-download", Phase.DEPLOY);
        context.setActionIds(List.of("download"));
        context.setActionTexts(List.of(ACTION_TEXT));
        context.setCardIds(List.of("31"));
        context.setGame(game);
        context.setObjectiveAnalyzer(analyzer);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoVaderContext(
                    GameState gameState,
                    SwccgGame game,
                    com.gempukku.swccgo.ai.models.rando.strategy
                            .ObjectiveAnalyzer analyzer,
                    String sourceCardId) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                                gameState, PLAYER_ID,
                                "CARD_ACTION_CHOICE",
                                "Choose deploy action",
                                "castle-download", Phase.DEPLOY);
        context.setActionIds(List.of("download-vader"));
        context.setActionTexts(List.of(
                "Deploy Vader from Reserve Deck"));
        context.setCardIds(List.of(sourceCardId));
        context.setGame(game);
        context.setObjectiveAnalyzer(analyzer);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenVaderContext(
                    GameState gameState,
                    SwccgGame game,
                    com.gempukku.swccgo.ai.models.chosenone.strategy
                            .ObjectiveAnalyzer analyzer,
                    String sourceCardId) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                                gameState, PLAYER_ID,
                                "CARD_ACTION_CHOICE",
                                "Choose deploy action",
                                "castle-download", Phase.DEPLOY);
        context.setActionIds(List.of("download-vader"));
        context.setActionTexts(List.of(
                "Deploy Vader from Reserve Deck"));
        context.setCardIds(List.of(sourceCardId));
        context.setGame(game);
        context.setObjectiveAnalyzer(analyzer);
        return context;
    }
}
