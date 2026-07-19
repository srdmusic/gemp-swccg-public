package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PassEvaluatorCharacterizationTest {

    private static final ForceReserveService.Facts RESERVES =
            new ForceReserveService.Facts(
                    true, false, false, false, 2, 1, 0, false);

    @Test
    public void baselineIsIdenticalAcrossBothBots() {
        Snapshot rando = evaluateRando(null, Phase.DEPLOY, 4,
                "Choose an action", 0, 0, 0, null);
        Snapshot chosen = evaluateChosen(null, Phase.DEPLOY, 4,
                "Choose an action", 0, 0, 0, null);

        assertSnapshot(rando, 5.0f, "Default pass option");
        assertEquals(rando, chosen);
    }

    @Test
    public void fullEarlyResourceStackMatchesLegacyScoreAndReasonOrder() {
        GameState gameState = mock(GameState.class);
        Snapshot rando = evaluateRando(gameState, Phase.MOVE, 2,
                "Choose an action", 0, 9, 4, RESERVES);
        Snapshot chosen = evaluateChosen(gameState, Phase.MOVE, 2,
                "Choose an action", 0, 9, 4, RESERVES);

        String expected = "Default pass option"
                + " | Early game - reduced pass preference (-3.0)"
                + " | Low on Force - prefer to pass (+1.0)"
                + " | Reserve deck low - conserve cards (+1.5)"
                + " | Small hand (4) - save force for drawing (+4.0)"
                + " | Move phase + low force + small hand - pass to draw (+5.0)"
                + " | V27.1 DTF RESERVE: Draw Their Fire on table! Need 3 Force for battle interrupts, only 0 left — CONSERVE! (+60.0)"
                + " | V27 MAINTENANCE RESERVE: Need 2 Force for maintenance, only 0 in pile — CONSERVE! (+50.0)";
        assertSnapshot(rando, 123.5f, expected);
        assertEquals(rando, chosen);
    }

    @Test
    public void battleTerminalReturnsBeforeEveryResourceRead() {
        GameState gameState = mock(GameState.class);
        var randoContext = randoContext(gameState, Phase.BATTLE, 2,
                "Initiate battle", 0, 0, 0, RESERVES);
        var chosenContext = chosenContext(gameState, Phase.BATTLE, 2,
                "Initiate battle", 0, 0, 0, RESERVES);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .evaluate(randoContext).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .evaluate(chosenContext).get(0);

        assertSnapshot(new Snapshot(rando.getScore(), rando.getReasoningString()),
                -8.0f, "Default pass option"
                        + " | Early game - reduced pass preference (-3.0)"
                        + " | Battle phase - should fight, not pass (-10.0)");
        assertEquals(Float.floatToRawIntBits(rando.getScore()),
                Float.floatToRawIntBits(chosen.getScore()));
        verify(randoContext, never()).getForcePileSize();
        verify(randoContext, never()).getReserveDeckSize();
        verify(randoContext, never()).getHandSize();
        verify(randoContext, never()).getForceReserveFacts();
        verify(chosenContext, never()).getForcePileSize();
        verify(chosenContext, never()).getReserveDeckSize();
        verify(chosenContext, never()).getHandSize();
        verify(chosenContext, never()).getForceReserveFacts();
    }

    @Test
    public void followthroughTerminalAlsoAvoidsResourceReads() {
        GameState gameState = mock(GameState.class);
        var randoContext = randoContext(gameState, Phase.DEPLOY, 4,
                "Choose where to deploy", 0, 0, 0, RESERVES);
        var chosenContext = chosenContext(gameState, Phase.DEPLOY, 4,
                "Choose where to deploy", 0, 0, 0, RESERVES);

        Snapshot rando = snapshot(new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .evaluate(randoContext).get(0));
        Snapshot chosen = snapshot(new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .evaluate(chosenContext).get(0));

        assertSnapshot(rando, -10.0f,
                "Default pass option | Already committed to action - follow through (-15.0)");
        assertEquals(rando, chosen);
        verify(randoContext, never()).getForcePileSize();
        verify(randoContext, never()).getForceReserveFacts();
        verify(chosenContext, never()).getForcePileSize();
        verify(chosenContext, never()).getForceReserveFacts();
    }

    @Test
    public void forceReserveFailuresRemainFailOpenAndIndependentlyCaught() {
        GameState gameState = mock(GameState.class);
        var randoContext = randoContext(gameState, Phase.DEPLOY, 4,
                "Choose an action", 3, 20, 7, null);
        var chosenContext = chosenContext(gameState, Phase.DEPLOY, 4,
                "Choose an action", 3, 20, 7, null);
        when(randoContext.getForceReserveFacts())
                .thenThrow(new IllegalStateException("fixture"));
        when(chosenContext.getForceReserveFacts())
                .thenThrow(new IllegalStateException("fixture"));

        Snapshot rando = snapshot(new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .evaluate(randoContext).get(0));
        Snapshot chosen = snapshot(new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .evaluate(chosenContext).get(0));

        assertSnapshot(rando, 5.0f, "Default pass option");
        assertEquals(rando, chosen);
        verify(randoContext, times(2)).getForceReserveFacts();
        verify(chosenContext, times(2)).getForceReserveFacts();
    }

    private static Snapshot evaluateRando(
            GameState gameState, Phase phase, int turn, String decisionText,
            int force, int reserve, int hand, ForceReserveService.Facts facts) {
        return snapshot(new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator()
                .evaluate(randoContext(gameState, phase, turn, decisionText,
                        force, reserve, hand, facts)).get(0));
    }

    private static Snapshot evaluateChosen(
            GameState gameState, Phase phase, int turn, String decisionText,
            int force, int reserve, int hand, ForceReserveService.Facts facts) {
        return snapshot(new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator()
                .evaluate(chosenContext(gameState, phase, turn, decisionText,
                        force, reserve, hand, facts)).get(0));
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            GameState gameState, Phase phase, int turn, String decisionText,
            int force, int reserve, int hand, ForceReserveService.Facts facts) {
        var context = mock(com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext.class);
        when(context.getDecisionType()).thenReturn("MULTIPLE_CHOICE");
        when(context.getActionTexts()).thenReturn(List.of());
        when(context.getGameState()).thenReturn(gameState);
        when(context.getDecisionText()).thenReturn(decisionText);
        when(context.getPhase()).thenReturn(phase);
        when(context.getTurnNumber()).thenReturn(turn);
        when(context.getForcePileSize()).thenReturn(force);
        when(context.getReserveDeckSize()).thenReturn(reserve);
        when(context.getHandSize()).thenReturn(hand);
        if (facts != null) {
            when(context.getForceReserveFacts()).thenReturn(facts);
        }
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            GameState gameState, Phase phase, int turn, String decisionText,
            int force, int reserve, int hand, ForceReserveService.Facts facts) {
        var context = mock(com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext.class);
        when(context.getDecisionType()).thenReturn("MULTIPLE_CHOICE");
        when(context.getActionTexts()).thenReturn(List.of());
        when(context.getGameState()).thenReturn(gameState);
        when(context.getDecisionText()).thenReturn(decisionText);
        when(context.getPhase()).thenReturn(phase);
        when(context.getTurnNumber()).thenReturn(turn);
        when(context.getForcePileSize()).thenReturn(force);
        when(context.getReserveDeckSize()).thenReturn(reserve);
        when(context.getHandSize()).thenReturn(hand);
        if (facts != null) {
            when(context.getForceReserveFacts()).thenReturn(facts);
        }
        return context;
    }

    private static Snapshot snapshot(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action) {
        return new Snapshot(action.getScore(), action.getReasoningString());
    }

    private static Snapshot snapshot(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction action) {
        return new Snapshot(action.getScore(), action.getReasoningString());
    }

    private static void assertSnapshot(
            Snapshot snapshot, float expectedScore, String expectedReasoning) {
        assertEquals(Float.floatToRawIntBits(expectedScore),
                Float.floatToRawIntBits(snapshot.score()));
        assertEquals(expectedReasoning, snapshot.reasoning());
    }

    private record Snapshot(float score, String reasoning) {
    }
}
