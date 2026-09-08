package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.timing.Action;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HuntDownCastleMoveContinuationTest {
    private static final String PLAYER = "rando";

    @Test
    public void selectedParentCarriesExactRouteAcrossAllThreeChildren() {
        PhysicalCard castle = location(
                215, 50, "209_50");
        PhysicalCard battleground = location(
                217, 51, "1_123");
        PhysicalCard vader = card(286, 52);
        GameState gameState = gameState(
                castle, battleground, vader,
                Phase.MOVE, 4);
        ObjectiveAnalyzer objective = huntDown(false);
        HuntDownCastleMoveContinuation continuation =
                new HuntDownCastleMoveContinuation();

        assertEquals(
                HuntDownCastleMoveContinuation.Update.ARMED,
                continuation.recordFinalResponse(
                    parent(castle,
                        HuntDownCastleMoveContinuation
                            .OUTBOUND_ACTION),
                    "0", gameState, PLAYER, objective));

        AwaitingDecision origin = child(
                "Choose card to move from, or click 'Done' to cancel",
                "215");
        HuntDownCastleMoveContinuation.RouteFacts originFacts =
                continuation.factsForDecision(
                    origin, gameState, PLAYER, objective);
        assertEquals(
                HuntDownCastleMoveContinuation.Step.ORIGIN,
                originFacts.step());
        assertEquals(50,
                originFacts.actionSourcePermanentCardId());
        assertTrue(originFacts.outbound());
        assertNull(originFacts.originPermanentCardId());
        assertEquals(
                HuntDownCastleMoveContinuation.Update.ADVANCED,
                continuation.recordFinalResponse(
                    origin, "215", gameState,
                    PLAYER, objective));

        AwaitingDecision destination = child(
                "Choose card to move to, or click 'Done' to cancel",
                "217");
        HuntDownCastleMoveContinuation.RouteFacts destinationFacts =
                continuation.factsForDecision(
                    destination, gameState, PLAYER,
                    objective);
        assertEquals(
                HuntDownCastleMoveContinuation.Step.DESTINATION,
                destinationFacts.step());
        assertEquals(Integer.valueOf(50),
                destinationFacts.originPermanentCardId());
        assertEquals(
                HuntDownCastleMoveContinuation.Update.ADVANCED,
                continuation.recordFinalResponse(
                    destination, "217", gameState,
                    PLAYER, objective));

        AwaitingDecision mover = child(
                "Choose card to move to <div class='cardHint' "
                    + "value='1_123'>site</div>, or click 'Done' to cancel",
                "286");
        HuntDownCastleMoveContinuation.RouteFacts moverFacts =
                continuation.factsForDecision(
                    mover, gameState, PLAYER, objective);
        assertEquals(
                HuntDownCastleMoveContinuation.Step.MOVER,
                moverFacts.step());
        assertEquals(Integer.valueOf(50),
                moverFacts.originPermanentCardId());
        assertEquals(Integer.valueOf(51),
                moverFacts.destinationPermanentCardId());
        assertEquals(
                HuntDownCastleMoveContinuation.Update.CLEARED,
                continuation.recordFinalResponse(
                    mover, "286", gameState,
                    PLAYER, objective));
        assertNull(continuation.factsForDecision(
                origin, gameState, PLAYER, objective));
    }

    @Test
    public void promptAloneNeverAuthenticatesCastleMovement() {
        PhysicalCard castle = location(
                215, 50, "209_50");
        GameState gameState = gameState(
                castle, null, null,
                Phase.MOVE, 4);
        HuntDownCastleMoveContinuation continuation =
                new HuntDownCastleMoveContinuation();

        assertNull(continuation.factsForDecision(
                child("Choose card to move from, or click 'Done' to cancel",
                    "215"),
                gameState, PLAYER, huntDown(false)));
    }

    @Test
    public void cancelUnrelatedDecisionAndLifecycleChangeClearTheLatch() {
        PhysicalCard castle = location(
                215, 50, "209_50");
        ObjectiveAnalyzer objective = huntDown(false);
        GameState gameState = gameState(
                castle, null, null,
                Phase.MOVE, 4);
        HuntDownCastleMoveContinuation continuation =
                new HuntDownCastleMoveContinuation();

        continuation.recordFinalResponse(
                parent(castle,
                    HuntDownCastleMoveContinuation.OUTBOUND_ACTION),
                "0", gameState, PLAYER, objective);
        AwaitingDecision origin = child(
                "Choose card to move from, or click 'Done' to cancel",
                "215");
        assertEquals(
                HuntDownCastleMoveContinuation.Update.CLEARED,
                continuation.recordFinalResponse(
                    origin, "", gameState, PLAYER,
                    objective));
        assertNull(continuation.factsForDecision(
                origin, gameState, PLAYER, objective));

        continuation.recordFinalResponse(
                parent(castle,
                    HuntDownCastleMoveContinuation.OUTBOUND_ACTION),
                "0", gameState, PLAYER, objective);
        assertNull(continuation.factsForDecision(
                child("Choose a different card", "215"),
                gameState, PLAYER, objective));

        continuation.recordFinalResponse(
                parent(castle,
                    HuntDownCastleMoveContinuation.OUTBOUND_ACTION),
                "0", gameState, PLAYER, objective);
        GameState nextTurn = gameState(
                castle, null, null,
                Phase.MOVE, 5);
        assertNull(continuation.factsForDecision(
                origin, nextTurn, PLAYER, objective));

        continuation.recordFinalResponse(
                parent(castle,
                    HuntDownCastleMoveContinuation.OUTBOUND_ACTION),
                "0", gameState, PLAYER, objective);
        GameState wrongPhase = gameState(
                castle, null, null,
                Phase.CONTROL, 4);
        assertNull(continuation.factsForDecision(
                origin, wrongPhase, PLAYER, objective));
    }

    @Test
    public void wrongSourceActionOrFlippedObjectiveCannotArm() {
        PhysicalCard castle = location(
                215, 50, "209_50");
        PhysicalCard impostor = location(
                216, 53, "1_123");
        GameState gameState = gameState(
                castle, impostor, null,
                Phase.MOVE, 4);
        HuntDownCastleMoveContinuation continuation =
                new HuntDownCastleMoveContinuation();

        assertEquals(
                HuntDownCastleMoveContinuation.Update.NONE,
                continuation.recordFinalResponse(
                    parent(impostor,
                        HuntDownCastleMoveContinuation
                            .OUTBOUND_ACTION),
                    "0", gameState, PLAYER,
                    huntDown(false)));
        assertEquals(
                HuntDownCastleMoveContinuation.Update.NONE,
                continuation.recordFinalResponse(
                    parent(castle, "Move using landspeed"),
                    "0", gameState, PLAYER,
                    huntDown(false)));
        assertEquals(
                HuntDownCastleMoveContinuation.Update.NONE,
                continuation.recordFinalResponse(
                    parent(castle,
                        HuntDownCastleMoveContinuation
                            .OUTBOUND_ACTION),
                    "0", gameState, PLAYER,
                    huntDown(true)));
    }

    private static ObjectiveAnalyzer huntDown(
            boolean flipped) {
        ObjectiveAnalyzer objective =
                mock(ObjectiveAnalyzer.class);
        when(objective.isHuntDownV()).thenReturn(true);
        when(objective.isFlipped()).thenReturn(flipped);
        return objective;
    }

    private static GameState gameState(
            PhysicalCard castle,
            PhysicalCard second,
            PhysicalCard third,
            Phase phase,
            int turn) {
        GameState gameState = mock(GameState.class);
        when(gameState.getCurrentPhase()).thenReturn(phase);
        when(gameState.getPlayersLatestTurnNumber(PLAYER))
                .thenReturn(turn);
        when(gameState.findCardByPermanentId(50))
                .thenReturn(castle);
        if (castle != null) {
            when(gameState.findCardById(castle.getCardId()))
                    .thenReturn(castle);
        }
        if (second != null) {
            when(gameState.findCardById(second.getCardId()))
                    .thenReturn(second);
        }
        if (third != null) {
            when(gameState.findCardById(third.getCardId()))
                    .thenReturn(third);
        }
        return gameState;
    }

    private static PhysicalCard location(
            int cardId, int permanentId,
            String blueprintId) {
        PhysicalCard card = card(cardId, permanentId);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.LOCATION);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true))
                .thenReturn(blueprintId);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.LOCATIONS);
        return card;
    }

    private static PhysicalCard card(
            int cardId, int permanentId) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getPermanentCardId())
                .thenReturn(permanentId);
        when(card.getTestingText(
                any(), anyBoolean(), anyBoolean()))
                .thenReturn("test card");
        return card;
    }

    private static CardActionSelectionDecision parent(
            PhysicalCard source, String actionText) {
        Action action = mock(Action.class);
        when(action.getActionSource()).thenReturn(source);
        when(action.getActionAttachedToCard())
                .thenReturn(source);
        when(action.getText()).thenReturn(actionText);
        return new CardActionSelectionDecision(
                1, "Choose Move action or Pass",
                List.of(action),
                true, false, false, false, false) {
            @Override
            public void decisionMade(String result)
                    throws DecisionResultInvalidException {
            }
        };
    }

    private static AwaitingDecision child(
            String prompt, String... cardIds) {
        AwaitingDecision decision =
                mock(AwaitingDecision.class);
        when(decision.getDecisionType())
                .thenReturn(
                    AwaitingDecisionType.CARD_SELECTION);
        when(decision.getText()).thenReturn(prompt);
        when(decision.getDecisionParameters())
                .thenReturn(Map.of("cardId", cardIds));
        return decision;
    }
}
