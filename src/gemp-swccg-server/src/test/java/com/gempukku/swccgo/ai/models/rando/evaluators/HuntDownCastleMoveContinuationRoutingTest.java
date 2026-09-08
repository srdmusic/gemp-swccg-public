package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.HuntDownCastleMoveContinuation;
import com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HuntDownCastleMoveContinuationRoutingTest {
    @Test
    public void selectedParentTokenRoutesChildWhenTopGameTextStateIsAbsent() {
        String playerId = "rando";
        PhysicalCard castle = mock(PhysicalCard.class);
        when(castle.getPermanentCardId()).thenReturn(50);
        when(castle.getCardId()).thenReturn(215);
        when(castle.getOwner()).thenReturn(playerId);
        when(castle.getBlueprintId(true)).thenReturn("209_50");
        when(castle.getZone()).thenReturn(Zone.LOCATIONS);

        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber(playerId))
                .thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(playerId);
        when(gameState.findCardByPermanentId(50))
                .thenReturn(castle);
        when(gameState.findCardById(215)).thenReturn(castle);
        when(gameState.getTopGameTextActionState()).thenReturn(null);

        ObjectiveAnalyzer objective = mock(ObjectiveAnalyzer.class);
        when(objective.isHuntDownV()).thenReturn(true);
        when(objective.isFlipped()).thenReturn(false);
        when(objective.assessVaderCastleRoutes(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(playerId),
                org.mockito.ArgumentMatchers.eq(castle),
                org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of());

        DecisionContext context = new DecisionContext(
                gameState, playerId, "CARD_SELECTION",
                "Choose card to move from, or click 'Done' to cancel",
                "17", Phase.MOVE);
        context.setGame(mock(SwccgGame.class));
        context.setObjectiveAnalyzer(objective);
        context.setCardIds(List.of("215"));
        context.setExtra(
                HuntDownCastleMoveContinuation
                    .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA,
                50);
        context.setExtra(
                HuntDownCastleMoveContinuation.OUTBOUND_EXTRA,
                true);

        List<EvaluatedAction> actions =
                new CardSelectionEvaluator().evaluate(context);

        assertEquals(1, actions.size());
        assertEquals("215", actions.get(0).getActionId());
        assertEquals("Choose Vader's Castle move",
                actions.get(0).getDisplayText());
    }
}
