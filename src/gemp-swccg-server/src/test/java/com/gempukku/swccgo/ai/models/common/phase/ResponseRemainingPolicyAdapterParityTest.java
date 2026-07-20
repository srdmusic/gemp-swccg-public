package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResponseRemainingPolicyAdapterParityTest {
    @Test
    public void mirroredBarrierAdaptersRememberTargetsAndSkipReadsOnRepeat() {
        GameState randoState = barrierState();
        var randoEvaluator = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator();
        var randoContext = randoActionContext(randoState, List.of("barrier"),
                List.of("Prevent Vader from battling or moving"));
        var randoFirst = randoEvaluator.evaluate(randoContext).get(0);
        clearInvocations(randoState);
        var randoRepeat = randoEvaluator.evaluate(randoContext).get(0);

        GameState chosenState = barrierState();
        var chosenEvaluator = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator();
        var chosenContext = chosenActionContext(chosenState, List.of("barrier"),
                List.of("Prevent Vader from battling or moving"));
        var chosenFirst = chosenEvaluator.evaluate(chosenContext).get(0);
        clearInvocations(chosenState);
        var chosenRepeat = chosenEvaluator.evaluate(chosenContext).get(0);

        assertAction(randoFirst, 50.0f,
                "Barrier on HIGH POWER target (5)! (+50.0)");
        assertAction(randoRepeat, -50.0f,
                "Already barriered Vader this turn - wasteful! (-50.0)");
        assertChosenAction(chosenFirst, randoFirst);
        assertChosenAction(chosenRepeat, randoRepeat);
        verify(randoState, never()).getAllPermanentCards();
        verify(chosenState, never()).getAllPermanentCards();
    }

    @Test
    public void mirroredGrabAdaptersPreserveConfirmedBothParseFallbackAndLegacyTotal() {
        GameState confirmedBothState = grabState("opponent", true, true);
        var randoBoth = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoActionContext(confirmedBothState, List.of("grab"), List.of("Grab interrupt")));
        var chosenBoth = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenActionContext(confirmedBothState, List.of("grab"), List.of("Grab interrupt")));
        assertAction(randoBoth.get(0), 30.0f,
                "V53 GRAB OPPONENT: Confirmed opponent's interrupt — grab it! (+30.0)");
        assertChosenAction(chosenBoth.get(0), randoBoth.get(0));

        GameState parseFallbackState = grabState("opponent", false, false);
        var randoParse = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoActionContext(parseFallbackState, List.of("not-a-number"),
                        List.of("Grab interrupt")));
        var chosenParse = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenActionContext(parseFallbackState, List.of("not-a-number"),
                        List.of("Grab interrupt")));
        assertAction(randoParse.get(0), 30.0f,
                "Grab unknown card (opponent's turn — likely theirs) (+30.0)");
        assertChosenAction(chosenParse.get(0), randoParse.get(0));

        GameState ownState = grabState("tester", true, false);
        var randoOwn = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                .evaluate(randoActionContext(ownState, List.of("grab"), List.of("Grab interrupt")));
        var chosenOwn = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                .evaluate(chosenActionContext(ownState, List.of("grab"), List.of("Grab interrupt")));
        assertAction(randoOwn.get(0), -19998.0f,
                "V53 NEVER GRAB OWN: Grabbing own interrupt is suicide! (-9999.0)");
        assertChosenAction(chosenOwn.get(0), randoOwn.get(0));
    }

    @Test
    public void mirroredCancelSelectionAdaptersPreserveResolvedAndUnresolvedScores() {
        GameState gameState = cancelSelectionState();
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoCancelContext(gameState));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenCancelContext(gameState));

        assertAction(rando.get(0), -150.0f, "Our card - don't cancel! (-200.0)");
        assertAction(rando.get(1), 150.0f, "Opponent's card - cancel! (+100.0)");
        assertEquals(Float.floatToRawIntBits(50.0f),
                Float.floatToRawIntBits(rando.get(2).getScore()));
        assertEquals(List.of(), rando.get(2).getReasoning());
        for (int i = 0; i < rando.size(); i++) {
            assertChosenAction(chosen.get(i), rando.get(i));
        }
    }

    private static GameState barrierState() {
        GameState gameState = baseState("tester");
        PhysicalCard target = mock(PhysicalCard.class);
        PhysicalCard ours = mock(PhysicalCard.class);
        PhysicalCard location = mock(PhysicalCard.class);
        SwccgCardBlueprint targetBlueprint = power(5.0f);
        SwccgCardBlueprint ownBlueprint = power(1.0f);
        when(target.getTitle()).thenReturn("Vader");
        when(target.getOwner()).thenReturn("opponent");
        when(target.getBlueprint()).thenReturn(targetBlueprint);
        when(target.getAtLocation()).thenReturn(location);
        when(ours.getOwner()).thenReturn("tester");
        when(ours.getBlueprint()).thenReturn(ownBlueprint);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(target));
        when(gameState.getCardsAtLocation(location)).thenReturn(List.of(target, ours));
        return gameState;
    }

    private static GameState grabState(String currentPlayer, boolean own, boolean opponent) {
        GameState gameState = baseState(currentPlayer);
        if (own) {
            PhysicalCard ownCard = mock(PhysicalCard.class);
            when(ownCard.getOwner()).thenReturn("tester");
            when(gameState.findCardById(1)).thenReturn(ownCard);
        }
        if (opponent) {
            PhysicalCard opponentCard = mock(PhysicalCard.class);
            when(opponentCard.getOwner()).thenReturn("opponent");
            when(gameState.findCardById(2)).thenReturn(opponentCard);
        }
        return gameState;
    }

    private static GameState cancelSelectionState() {
        GameState gameState = baseState("tester");
        PhysicalCard ownCard = mock(PhysicalCard.class);
        PhysicalCard opponentCard = mock(PhysicalCard.class);
        when(ownCard.getOwner()).thenReturn("tester");
        when(opponentCard.getOwner()).thenReturn("opponent");
        when(gameState.findCardById(1)).thenReturn(ownCard);
        when(gameState.findCardById(2)).thenReturn(opponentCard);
        return gameState;
    }

    private static GameState baseState(String currentPlayer) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(4);
        when(gameState.getCurrentPlayerId()).thenReturn(currentPlayer);
        when(gameState.getOpponent("tester")).thenReturn("opponent");
        return gameState;
    }

    private static SwccgCardBlueprint power(float value) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(value);
        return blueprint;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoActionContext(
            GameState gameState, List<String> ids, List<String> texts) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose response", "response", Phase.CONTROL);
        context.setSide(Side.DARK);
        context.setActionIds(ids);
        context.setActionTexts(texts);
        context.setCardIds(ids.equals(List.of("grab")) ? List.of("1", "2") : ids);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenActionContext(
            GameState gameState, List<String> ids, List<String> texts) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", "ACTION_CHOICE", "Choose response", "response", Phase.CONTROL);
        context.setSide(Side.DARK);
        context.setActionIds(ids);
        context.setActionTexts(texts);
        context.setCardIds(ids.equals(List.of("grab")) ? List.of("1", "2") : ids);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoCancelContext(
            GameState gameState) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", "CARD_SELECTION", "Choose card to cancel", "cancel", Phase.CONTROL);
        context.setCardIds(List.of("1", "2", "not-a-number"));
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenCancelContext(
            GameState gameState) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", "CARD_SELECTION", "Choose card to cancel", "cancel", Phase.CONTROL);
        context.setCardIds(List.of("1", "2", "not-a-number"));
        return context;
    }

    private static void assertAction(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action,
            float score, String reasoning) {
        assertEquals(Float.floatToRawIntBits(score), Float.floatToRawIntBits(action.getScore()));
        assertEquals(List.of(reasoning), action.getReasoning());
    }

    private static void assertChosenAction(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosen,
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando) {
        assertEquals(rando.getActionId(), chosen.getActionId());
        assertEquals(Float.floatToRawIntBits(rando.getScore()), Float.floatToRawIntBits(chosen.getScore()));
        assertEquals(rando.getReasoning(), chosen.getReasoning());
    }
}
