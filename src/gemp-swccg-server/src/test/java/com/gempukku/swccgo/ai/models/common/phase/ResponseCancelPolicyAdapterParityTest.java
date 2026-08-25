package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResponseCancelPolicyAdapterParityTest {
    @Test
    public void mirroredAdaptersPreserveBandsAndShadowLateTwin() {
        List<String> ids = List.of(
                "critical", "high", "valuable", "destiny",
                "exact-drain", "opponent-turn");
        List<String> texts = List.of(
                "Cancel Jedi Levitation interrupt",
                "Cancel Escape Pod interrupt",
                "Cancel Alter interrupt",
                "Cancel interrupt if destiny",
                "Cancel Force drain",
                "Cancel opponent interrupt");

        var randoContext = randoContext(null, ids, texts);
        var chosenContext = chosenContext(null, ids, texts);
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext);

        assertMirrors(rando, chosen);
        assertRando(rando.get(0), 70.0f,
                "Cancel CRITICAL target: jedi levitation! (+70.0)", "CANCEL");
        assertRando(rando.get(1), 50.0f,
                "Cancel high-value target: escape pod (+50.0)", "CANCEL");
        assertRando(rando.get(2), 45.0f,
                "Cancel valuable target: alter (+45.0)", "CANCEL");
        assertRando(rando.get(3), -10.0f,
                "Destiny-based cancel (unreliable, skip) (-10.0)", "CANCEL");
        assertRando(rando.get(4), 35.0f,
                "Cancel opponent's force drain (+35.0)", "CANCEL");
        assertRando(rando.get(5), 30.0f,
                "Cancel opponent interrupt (their turn) (+30.0)", "CANCEL");
    }

    @Test
    public void gameStateGateAndSelfDrainDelegationStayMirrored() {
        GameState ownTurn = gameState("tester");
        List<String> ids = List.of("self-interrupt", "self-drain");
        List<String> texts = List.of(
                "Cancel Sniper interrupt", "Cancel Force drain");

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext(ownTurn, ids, texts));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext(ownTurn, ids, texts));

        assertMirrors(rando, chosen);
        assertRando(rando.get(0), -9999.0f,
                "V37.3 SENSE SELF-CANCEL: NEVER cancel our OWN interrupt! (-9999.0)",
                "CANCEL");
        assertRando(rando.get(1), -9999.0f,
                "V52 NEVER SELF-CANCEL DRAIN: Canceling own force drain is suicide! (-9999.0)",
                "CANCEL");

        GameState opponentTurn = gameState("opponent");
        var gated = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext(opponentTurn,
                        List.of("gated"), List.of("Cancel Sniper interrupt")));
        var ungated = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext(null,
                        List.of("ungated"), List.of("Cancel Sniper interrupt")));
        assertEquals(Float.floatToRawIntBits(-9999.0f),
                Float.floatToRawIntBits(gated.get(0).getScore()));
        assertEquals(Float.floatToRawIntBits(30.0f),
                Float.floatToRawIntBits(ungated.get(0).getScore()));
    }

    @Test
    public void v194CancelRedrawCarveOutStaysAheadOfSenseCancel() {
        List<String> ids = List.of("v194");
        List<String> texts = List.of(
                "Cancel interrupt and redraw destiny as a 6");

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext(null, ids, texts));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext(null, ids, texts));

        assertMirrors(rando, chosen);
        assertRando(rando.get(0), -300.0f,
                "V37 DON'T REDRAW: Current destiny 6 is GOOD (avg 3.0) — keep it! (-300.0)",
                "UNKNOWN");
    }

    @Test
    public void replay72318ExactSurpriseAssaultIsHardVetoedInSpaceInBothBots() {
        SwccgGame game = mock(SwccgGame.class);
        GameState gameState = gameState("opponent");
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard source = mock(PhysicalCard.class);
        PhysicalCard system = mock(PhysicalCard.class);
        SwccgCardBlueprint systemBlueprint = mock(SwccgCardBlueprint.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(gameState.findCardById(113)).thenReturn(source);
        when(source.getBlueprintId(true)).thenReturn("1_113");
        when(gameState.getForceDrainLocation()).thenReturn(system);
        when(system.getBlueprint()).thenReturn(systemBlueprint);
        when(systemBlueprint.getCardSubtype()).thenReturn(CardSubtype.SYSTEM);

        var randoContext = randoContext(
                gameState, List.of("space"), List.of("Cancel Force drain"));
        randoContext.setCardIds(List.of("113"));
        randoContext.setGame(game);
        var chosenContext = chosenContext(
                gameState, List.of("space"), List.of("Cancel Force drain"));
        chosenContext.setCardIds(List.of("113"));
        chosenContext.setGame(game);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext).get(0);

        assertEquals(rando.getReasoning(), chosen.getReasoning());
        assertTrue(rando.isHardVetoed());
        assertTrue(chosen.isHardVetoed());
        assertEquals("V300 SURPRISE ASSAULT: never risk the card in space",
                rando.getVetoReason());
        assertEquals(Float.floatToRawIntBits(35.0f),
                Float.floatToRawIntBits(rando.getScore()));
    }

    private static GameState gameState(String currentPlayer) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(currentPlayer);
        when(gameState.getLostPile("tester")).thenReturn(List.of(mock(PhysicalCard.class)));
        when(gameState.getHand("tester")).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        return gameState;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            GameState gameState, List<String> ids, List<String> texts) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose RESPONSE action",
                "response-cancel-parity", Phase.CONTROL);
        context.setActionIds(ids);
        context.setActionTexts(texts);
        context.setCardIds(ids.stream().map(ignored -> "").toList());
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            GameState gameState, List<String> ids, List<String> texts) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose RESPONSE action",
                "response-cancel-parity", Phase.CONTROL);
        context.setActionIds(ids);
        context.setActionTexts(texts);
        context.setCardIds(ids.stream().map(ignored -> "").toList());
        return context;
    }

    private static void assertMirrors(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> rando,
            List<com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction> chosen) {
        assertEquals(rando.size(), chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertEquals(Float.floatToRawIntBits(rando.get(i).getScore()),
                    Float.floatToRawIntBits(chosen.get(i).getScore()));
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
            assertEquals(rando.get(i).getActionType().name(),
                    chosen.get(i).getActionType().name());
        }
    }

    private static void assertRando(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action,
            float score, String reasoning, String actionType) {
        assertEquals(Float.floatToRawIntBits(score),
                Float.floatToRawIntBits(action.getScore()));
        assertEquals(List.of(reasoning), action.getReasoning());
        assertEquals(actionType, action.getActionType().name());
    }
}
