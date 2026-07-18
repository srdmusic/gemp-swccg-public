package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ControlActionTextPolicyParityTest {
    @Test
    public void mirroredAdaptersRouteEachControlCandidateThroughItsSharedOwner() {
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        null, "tester", "ACTION_CHOICE", "Choose CONTROL action",
                        "control-parity", Phase.CONTROL);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                        null, "tester", "ACTION_CHOICE", "Choose CONTROL action",
                        "control-parity", Phase.CONTROL);
        List<String> ids = List.of("drain", "modifier");
        List<String> texts = List.of("Force drain", "Add 1 to Force drain");
        List<String> cardIds = List.of("", "");
        randoContext.setActionIds(ids);
        randoContext.setActionTexts(texts);
        randoContext.setCardIds(cardIds);
        chosenContext.setActionIds(ids);
        chosenContext.setActionTexts(texts);
        chosenContext.setCardIds(cardIds);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext);

        assertEquals(2, rando.size());
        assertEquals(2, chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertEquals(Float.floatToRawIntBits(rando.get(i).getScore()),
                    Float.floatToRawIntBits(chosen.get(i).getScore()));
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
        assertEquals(Float.floatToRawIntBits(70.0f),
                Float.floatToRawIntBits(rando.get(0).getScore()));
        assertEquals(Float.floatToRawIntBits(80.0f),
                Float.floatToRawIntBits(rando.get(1).getScore()));
    }

    @Test
    public void noEscapeAndSelfCancelKeepTheirLiveResponsePositions() {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn("tester");
        when(gameState.getLostPile("tester")).thenReturn(List.of(mock(PhysicalCard.class)));
        when(gameState.getHand("tester")).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());

        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context =
                new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                        gameState, "tester", "ACTION_CHOICE", "Choose CONTROL response",
                        "control-live-response", Phase.CONTROL);
        context.setActionIds(List.of("no-escape", "self-cancel"));
        context.setActionTexts(List.of(
                "Take top card of Lost Pile into hand",
                "Cancel Force drain"));
        context.setCardIds(List.of("", ""));

        var actions = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(context);

        assertEquals(2, actions.size());
        assertEquals(Float.floatToRawIntBits(200.0f),
                Float.floatToRawIntBits(actions.get(0).getScore()));
        assertEquals(Float.floatToRawIntBits(-9999.0f),
                Float.floatToRawIntBits(actions.get(1).getScore()));
    }
}
