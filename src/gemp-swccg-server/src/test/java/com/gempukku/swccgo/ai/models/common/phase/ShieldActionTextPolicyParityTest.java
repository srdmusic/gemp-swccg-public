package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ShieldStrategy;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShieldActionTextPolicyParityTest {

    @Test
    public void mirroredAdaptersRouteTheDefensiveShieldWindowThroughOnePolicy() {
        GameState gameState = baseGameState(true);
        var randoContext = randoContext(gameState, "shield-window", "Play a Defensive Shield", "");
        var chosenContext = chosenContext(gameState, "shield-window", "Play a Defensive Shield", "");
        randoContext.setShieldStrategy(new ShieldStrategy(Side.DARK));
        chosenContext.setShieldStrategy(new ShieldStrategy(Side.DARK));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext).get(0);

        assertParity(rando.getScore(), rando.getReasoning(),
                chosen.getScore(), chosen.getReasoning());
        assertBits(50.0f, rando.getScore());
    }

    @Test
    public void opponentTurnShieldWindowKeepsMinusTenAheadOfPacing() {
        GameState gameState = baseGameState(false);
        ShieldStrategy randoStrategy = pacedStrategy();
        ShieldStrategy chosenStrategy = pacedStrategy();
        var randoContext = randoContext(gameState, "shield-response", "Play a Defensive Shield", "");
        var chosenContext = chosenContext(gameState, "shield-response", "Play a Defensive Shield", "");
        randoContext.setShieldStrategy(randoStrategy);
        chosenContext.setShieldStrategy(chosenStrategy);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext).get(0);

        assertParity(rando.getScore(), rando.getReasoning(),
                chosen.getScore(), chosen.getReasoning());
        assertBits(-10.0f, rando.getScore());
    }

    @Test
    public void bothStackedPileAliasesBypassGenericPlayCardScoring() {
        GameState gameState = baseGameState(true);
        PhysicalCard knowledgeAndDefense = mock(PhysicalCard.class);
        PhysicalCard angerFearAggression = mock(PhysicalCard.class);
        when(knowledgeAndDefense.getTitle()).thenReturn("Knowledge And Defense (V)");
        when(angerFearAggression.getTitle()).thenReturn("Anger, Fear, Aggression (V)");
        when(gameState.findCardById(7)).thenReturn(knowledgeAndDefense);
        when(gameState.findCardById(8)).thenReturn(angerFearAggression);

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose SHIELDS action",
                "shield-parent", Phase.DEPLOY);
        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose SHIELDS action",
                "shield-parent", Phase.DEPLOY);
        List<String> ids = List.of("knd", "afa");
        List<String> texts = List.of("Play a card", "Play a card");
        List<String> cardIds = List.of("7", "8");
        configure(randoContext, ids, texts, cardIds);
        configure(chosenContext, ids, texts, cardIds);
        randoContext.setShieldStrategy(new ShieldStrategy(Side.DARK));
        chosenContext.setShieldStrategy(new ShieldStrategy(Side.LIGHT));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext);

        assertEquals(2, rando.size());
        assertEquals(2, chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertParity(rando.get(i).getScore(), rando.get(i).getReasoning(),
                    chosen.get(i).getScore(), chosen.get(i).getReasoning());
            assertBits(50.0f, rando.get(i).getScore());
        }
    }

    private static GameState baseGameState(boolean myTurn) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(1);
        when(gameState.getCurrentPlayerId()).thenReturn(myTurn ? "tester" : "opponent");
        when(gameState.getHand("tester")).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        return gameState;
    }

    private static ShieldStrategy pacedStrategy() {
        ShieldStrategy strategy = new ShieldStrategy(Side.DARK);
        strategy.recordShieldPlayed("one", "One");
        strategy.recordShieldPlayed("two", "Two");
        return strategy;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            GameState gameState, String actionId, String actionText, String cardId) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose SHIELDS action",
                actionId, Phase.DEPLOY);
        configure(context, List.of(actionId), List.of(actionText), List.of(cardId));
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            GameState gameState, String actionId, String actionText, String cardId) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose SHIELDS action",
                actionId, Phase.DEPLOY);
        configure(context, List.of(actionId), List.of(actionText), List.of(cardId));
        return context;
    }

    private static void configure(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context,
            List<String> ids, List<String> texts, List<String> cardIds) {
        context.setActionIds(ids);
        context.setActionTexts(texts);
        context.setCardIds(cardIds);
    }

    private static void configure(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context,
            List<String> ids, List<String> texts, List<String> cardIds) {
        context.setActionIds(ids);
        context.setActionTexts(texts);
        context.setCardIds(cardIds);
    }

    private static void assertParity(float randoScore, List<String> randoReasoning,
                                     float chosenScore, List<String> chosenReasoning) {
        assertBits(randoScore, chosenScore);
        assertEquals(randoReasoning, chosenReasoning);
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }
}
