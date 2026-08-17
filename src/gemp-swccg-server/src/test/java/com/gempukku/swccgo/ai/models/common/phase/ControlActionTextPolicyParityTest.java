package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.rando.evaluators.ActionType;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

    @Test
    public void revealRetrieveBoundariesAndV184StackingStayMirrored() {
        assertUtilityBoundary(6, 15, -50.0f, 270.0f);
        assertUtilityBoundary(7, 16, 50.0f, 330.0f);
    }

    @Test
    public void woklingSacrificeIsHeldUntilTheOriginalLocationRampCompletes() {
        for (String blueprintId : List.of("200_47", "601_61")) {
            assertWoklingBoundary(blueprintId, false, 1,
                    true, 0.0f);
            assertWoklingBoundary(blueprintId, true, 16,
                    false, 30.0f);
        }
    }

    @Test
    public void classicWoklingTitleDoesNotTriggerTheVirtualWoklingHold() {
        EvaluatedPair pair = evaluateWokling("8_42", false, 1);

        assertFalse(pair.rando().isHardVetoed());
        assertFalse(pair.chosen().isHardVetoed());
        assertEquals("wokling", pair.randoWinner().getActionId());
        assertEquals("wokling", pair.chosenWinner().getActionId());
        assertEquals(Float.floatToRawIntBits(270.0f),
                Float.floatToRawIntBits(pair.rando().getScore()));
        assertTrue(pair.rando().getReasoning().stream().anyMatch(reason ->
                reason.startsWith("V184 WHEN-DEPLOYED TRIGGER:")));
    }

    @Test
    public void stealAndDangerousCardKeepExactScoresAndMirrorParity() {
        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, "tester", "ACTION_CHOICE", "Choose CONTROL utility",
                "control-final-utility", Phase.CONTROL);
        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, "tester", "ACTION_CHOICE", "Choose CONTROL utility",
                "control-final-utility", Phase.CONTROL);
        List<String> ids = List.of("steal", "stardust", "on-edge");
        List<String> texts = List.of("Steal a card", "Stardust", "On The Edge");
        randoContext.setActionIds(ids);
        randoContext.setActionTexts(texts);
        randoContext.setCardIds(List.of("", "", ""));
        chosenContext.setActionIds(ids);
        chosenContext.setActionTexts(texts);
        chosenContext.setCardIds(List.of("", "", ""));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext);

        assertEquals(3, rando.size());
        assertEquals(3, chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertEquals(Float.floatToRawIntBits(rando.get(i).getScore()),
                    Float.floatToRawIntBits(chosen.get(i).getScore()));
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
        assertEquals(ActionType.STEAL, rando.get(0).getActionType());
        assertEquals(Float.floatToRawIntBits(30.0f),
                Float.floatToRawIntBits(rando.get(0).getScore()));
        assertEquals(Float.floatToRawIntBits(-50.0f),
                Float.floatToRawIntBits(rando.get(1).getScore()));
        assertEquals(Float.floatToRawIntBits(-50.0f),
                Float.floatToRawIntBits(rando.get(2).getScore()));
    }

    private static void assertUtilityBoundary(int opponentHandSize,
                                              int lostPileSize,
                                              float revealScore,
                                              float retrieveScore) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn("tester");
        when(gameState.getOpponent("tester")).thenReturn("opponent");
        when(gameState.getHand("opponent")).thenReturn(
                java.util.Collections.nCopies(opponentHandSize,
                        mock(PhysicalCard.class)));
        when(gameState.getLostPile("tester")).thenReturn(
                java.util.Collections.nCopies(lostPileSize,
                        mock(PhysicalCard.class)));
        when(gameState.getHand("tester")).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of());

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose CONTROL action",
                "control-utility-boundary", Phase.CONTROL);
        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose CONTROL action",
                "control-utility-boundary", Phase.CONTROL);
        List<String> ids = List.of("reveal", "retrieve", "peek", "make-lose");
        List<String> texts = List.of(
                "LOST: Reveal opponent's hand",
                "retrieve Force",
                "USED: Peek at top card",
                "Make opponent lose 1 Force");
        randoContext.setActionIds(ids);
        randoContext.setActionTexts(texts);
        randoContext.setCardIds(List.of("", "", "", ""));
        chosenContext.setActionIds(ids);
        chosenContext.setActionTexts(texts);
        chosenContext.setCardIds(List.of("", "", "", ""));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext);

        assertEquals(4, rando.size());
        assertEquals(4, chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertEquals(Float.floatToRawIntBits(rando.get(i).getScore()),
                    Float.floatToRawIntBits(chosen.get(i).getScore()));
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
        assertEquals(Float.floatToRawIntBits(revealScore),
                Float.floatToRawIntBits(rando.get(0).getScore()));
        assertEquals(Float.floatToRawIntBits(retrieveScore),
                Float.floatToRawIntBits(rando.get(1).getScore()));
        assertEquals(Float.floatToRawIntBits(30.0f),
                Float.floatToRawIntBits(rando.get(2).getScore()));
        assertEquals(Float.floatToRawIntBits(30.0f),
                Float.floatToRawIntBits(rando.get(3).getScore()));
        assertEquals(2, rando.get(1).getReasoning().size());
        assertEquals(true, rando.get(1).getReasoning().get(0)
                .startsWith("V184 WHEN-DEPLOYED TRIGGER:"));
    }

    private static void assertWoklingBoundary(String blueprintId,
                                              boolean locationsComplete,
                                              int lostPileSize,
                                              boolean expectedHardVeto,
                                              float expectedScore) {
        EvaluatedPair pair = evaluateWokling(
                blueprintId, locationsComplete, lostPileSize);

        assertEquals(pair.rando().getActionId(), pair.chosen().getActionId());
        assertEquals(Float.floatToRawIntBits(pair.rando().getScore()),
                Float.floatToRawIntBits(pair.chosen().getScore()));
        assertEquals(pair.rando().getReasoning(), pair.chosen().getReasoning());
        assertEquals(pair.rando().isHardVetoed(),
                pair.chosen().isHardVetoed());
        assertEquals(pair.rando().getVetoReason(),
                pair.chosen().getVetoReason());
        assertEquals(expectedHardVeto, pair.rando().isHardVetoed());
        assertEquals(expectedHardVeto ? "" : "wokling",
                pair.randoWinner().getActionId());
        assertEquals(pair.randoWinner().getActionId(),
                pair.chosenWinner().getActionId());
        assertEquals(Float.floatToRawIntBits(expectedScore),
                Float.floatToRawIntBits(pair.rando().getScore()));
        assertFalse("Wokling's self-sacrifice is not a free V184 trigger",
                pair.rando().getReasoning().stream().anyMatch(reason ->
                        reason.startsWith("V184 WHEN-DEPLOYED TRIGGER:")));
        if (expectedHardVeto) {
            assertEquals(
                    "V53d WOKLING HOLD: preserve +1 Force generation until every original deck location is deployed",
                    pair.rando().getVetoReason());
        }
    }

    private static EvaluatedPair evaluateWokling(String blueprintId,
                                                 boolean locationsComplete,
                                                 int lostPileSize) {
        GameState gameState = mock(GameState.class);
        PhysicalCard source = mock(PhysicalCard.class);
        when(source.getBlueprintId(true)).thenReturn(blueprintId);
        when(source.getTitle()).thenReturn("Wokling (V)");
        when(gameState.findCardById(243)).thenReturn(source);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(1);
        when(gameState.getCurrentPlayerId()).thenReturn("tester");
        when(gameState.getLostPile("tester")).thenReturn(
                java.util.Collections.nCopies(lostPileSize,
                        mock(PhysicalCard.class)));
        when(gameState.getHand("tester")).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(List.of(source));

        var randoOracle = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.class);
        var chosenOracle = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.class);
        when(randoOracle.areAllOriginalDeckLocationsInPlay())
                .thenReturn(locationsComplete);
        when(chosenOracle.areAllOriginalDeckLocationsInPlay())
                .thenReturn(locationsComplete);

        var randoContext = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose ACTIVATE action",
                "wokling-location-ramp", Phase.ACTIVATE);
        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose ACTIVATE action",
                "wokling-location-ramp", Phase.ACTIVATE);
        randoContext.setActionIds(List.of("wokling"));
        randoContext.setActionTexts(List.of(
                "Place out of play to retrieve 1 Force"));
        randoContext.setCardIds(List.of("243"));
        randoContext.setDeckOracle(randoOracle);
        randoContext.setNoPass(false);
        randoContext.setMin(0);
        randoContext.setMax(1);
        chosenContext.setActionIds(List.of("wokling"));
        chosenContext.setActionTexts(List.of(
                "Place out of play to retrieve 1 Force"));
        chosenContext.setCardIds(List.of("243"));
        chosenContext.setDeckOracle(chosenOracle);
        chosenContext.setNoPass(false);
        chosenContext.setMin(0);
        chosenContext.setMax(1);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoContext).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenContext).get(0);
        var randoWinner = new com.gempukku.swccgo.ai.models.rando.evaluators.CombinedEvaluator()
                .evaluateDecision(randoContext);
        var chosenWinner = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CombinedEvaluator()
                .evaluateDecision(chosenContext);
        return new EvaluatedPair(rando, chosen, randoWinner, chosenWinner);
    }

    private record EvaluatedPair(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando,
            com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosen,
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction randoWinner,
            com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosenWinner) {
    }
}
