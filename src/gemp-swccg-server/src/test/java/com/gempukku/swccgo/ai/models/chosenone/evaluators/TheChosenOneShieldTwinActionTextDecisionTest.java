package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.AbstractShieldTwinActionTextDecisionTest;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.cards.set222.dark.Card222_014;
import com.gempukku.swccgo.cards.set222.dark.Card222_014_BACK;
import com.gempukku.swccgo.cards.set222.dark.Card222_030;
import com.gempukku.swccgo.cards.set222.dark.Card222_030_BACK;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TheChosenOneShieldTwinActionTextDecisionTest
        extends AbstractShieldTwinActionTextDecisionTest {
    private static final String PLAYER = "dark";

    @Override
    protected ObjectiveAnalyzer analyze(String objectiveBlueprintId) {
        var analyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer();
        analyze(analyzer, objectiveBlueprintId);
        return analyzer;
    }

    @Override
    protected List<Candidate> evaluate(
            ObjectiveAnalyzer analyzer, List<String> actionIds,
            List<String> actionTexts, List<String> sourceBlueprintIds) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.getHand(PLAYER)).thenReturn(List.of());
        when(gameState.getLocationsInOrder()).thenReturn(List.of());
        var cardIds = new java.util.ArrayList<String>();
        for (int i = 0; i < sourceBlueprintIds.size(); i++) {
            int cardId = 100 + i;
            PhysicalCard source = mock(PhysicalCard.class);
            when(source.getBlueprintId(true))
                    .thenReturn(sourceBlueprintIds.get(i));
            when(gameState.findCardById(cardId)).thenReturn(source);
            cardIds.add(String.valueOf(cardId));
        }
        DecisionContext context = new DecisionContext(
                gameState, PLAYER, "CARD_ACTION_CHOICE",
                "Choose deploy action", "1", Phase.DEPLOY);
        context.setGame(game);
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer)
                        analyzer);
        context.setActionIds(actionIds);
        context.setActionTexts(actionTexts);
        context.setCardIds(cardIds);
        return new ActionTextEvaluator().evaluate(context).stream()
                .map(action -> new Candidate(
                        action.getActionId(), action.getScore(),
                        action.isHardVetoed(),
                        action.getReasoningString()))
                .toList();
    }

    private static void analyze(
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer analyzer,
            String objectiveBlueprintId) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        SwccgCardBlueprint front = "222_14".equals(objectiveBlueprintId)
                ? new Card222_014() : new Card222_030();
        SwccgCardBlueprint back = "222_14".equals(objectiveBlueprintId)
                ? new Card222_014_BACK() : new Card222_030_BACK();

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(objective));
        when(objective.getOwner()).thenReturn(PLAYER);
        when(objective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint()).thenReturn(front);
        when(objective.getOtherSideBlueprint()).thenReturn(back);
        when(objective.getBlueprintId(true))
                .thenReturn(objectiveBlueprintId);
        when(objective.isFlipped()).thenReturn(false);

        analyzer.analyze(game, PLAYER, Side.DARK);
    }
}
