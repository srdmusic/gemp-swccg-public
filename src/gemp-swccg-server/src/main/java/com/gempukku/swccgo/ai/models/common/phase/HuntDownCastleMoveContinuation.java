package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Map;

/**
 * Carries the exact selected Vader's Castle movement action across its three
 * child card-selection decisions. MoveUsingLocationTextAction owns those
 * children but does not register a top game-text action state, so the selected
 * parent is the only reliable provenance at the AI boundary.
 */
public final class HuntDownCastleMoveContinuation {
    public static final String ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA =
            "huntDownCastleMoveActionSourcePermanentCardId";
    public static final String OUTBOUND_EXTRA =
            "huntDownCastleMoveOutbound";
    public static final String ORIGIN_PERMANENT_CARD_ID_EXTRA =
            "huntDownCastleMoveOriginPermanentCardId";
    public static final String DESTINATION_PERMANENT_CARD_ID_EXTRA =
            "huntDownCastleMoveDestinationPermanentCardId";

    public static final String OUTBOUND_ACTION =
            "Move from here to other battleground site";
    public static final String INBOUND_ACTION =
            "Move from other battleground site to here";

    public enum Step {
        ORIGIN,
        DESTINATION,
        MOVER
    }

    public enum Update {
        NONE,
        ARMED,
        ADVANCED,
        CLEARED
    }

    public record RouteFacts(
            int actionSourcePermanentCardId,
            boolean outbound,
            Integer originPermanentCardId,
            Integer destinationPermanentCardId,
            Step step) {
    }

    private Integer actionSourcePermanentCardId;
    private boolean outbound;
    private Integer originPermanentCardId;
    private Integer destinationPermanentCardId;
    private int turn = -1;
    private Step step;

    /**
     * Returns facts only for the exact next child in the selected Castle action.
     * Any unrelated decision or stale board state consumes the continuation.
     */
    public RouteFacts factsForDecision(
            AwaitingDecision decision,
            GameState gameState,
            String playerId,
            ObjectiveAnalyzer objectiveAnalyzer) {
        if (step == null) {
            return null;
        }
        if (!isLive(gameState, playerId, objectiveAnalyzer)
                || !isExpectedChild(decision, step)) {
            reset();
            return null;
        }
        return new RouteFacts(
                actionSourcePermanentCardId,
                outbound,
                originPermanentCardId,
                destinationPermanentCardId,
                step);
    }

    /**
     * Records the final validated AI response immediately before the engine
     * receives it. An exact selected Castle parent arms the continuation;
     * exact child selections advance it; cancel or unrelated input clears it.
     */
    public Update recordFinalResponse(
            AwaitingDecision decision,
            String response,
            GameState gameState,
            String playerId,
            ObjectiveAnalyzer objectiveAnalyzer) {
        Action selectedParent = exactSelectedParent(
                decision, response, gameState,
                playerId, objectiveAnalyzer);
        if (selectedParent != null) {
            PhysicalCard source =
                    AiActionSourceProvenance.selectedActionSource(
                        decision, response);
            actionSourcePermanentCardId = source.getPermanentCardId();
            outbound = OUTBOUND_ACTION.equals(selectedParent.getText());
            originPermanentCardId = null;
            destinationPermanentCardId = null;
            turn = gameState.getPlayersLatestTurnNumber(playerId);
            step = Step.ORIGIN;
            return Update.ARMED;
        }

        if (step == null) {
            return Update.NONE;
        }
        if (!isLive(gameState, playerId, objectiveAnalyzer)
                || !isExpectedChild(decision, step)
                || response == null || response.isBlank()
                || !isOfferedCard(decision, response)) {
            reset();
            return Update.CLEARED;
        }

        PhysicalCard selected = selectedCard(gameState, response);
        if (selected == null) {
            reset();
            return Update.CLEARED;
        }
        if (step == Step.ORIGIN) {
            if (!isLocation(selected)
                    || outbound
                    && selected.getPermanentCardId()
                        != actionSourcePermanentCardId) {
                reset();
                return Update.CLEARED;
            }
            originPermanentCardId = selected.getPermanentCardId();
            step = Step.DESTINATION;
            return Update.ADVANCED;
        }
        if (step == Step.DESTINATION) {
            if (!isLocation(selected)) {
                reset();
                return Update.CLEARED;
            }
            destinationPermanentCardId = selected.getPermanentCardId();
            step = Step.MOVER;
            return Update.ADVANCED;
        }

        reset();
        return Update.CLEARED;
    }

    public void reset() {
        actionSourcePermanentCardId = null;
        outbound = false;
        originPermanentCardId = null;
        destinationPermanentCardId = null;
        turn = -1;
        step = null;
    }

    private Action exactSelectedParent(
            AwaitingDecision decision,
            String response,
            GameState gameState,
            String playerId,
            ObjectiveAnalyzer objectiveAnalyzer) {
        if (decision == null
                || decision.getDecisionType()
                    != AwaitingDecisionType.CARD_ACTION_CHOICE
                || response == null || response.isBlank()
                || gameState == null
                || gameState.getCurrentPhase() != Phase.MOVE
                || playerId == null
                || objectiveAnalyzer == null
                || !objectiveAnalyzer.isHuntDownV()
                || objectiveAnalyzer.isFlipped()) {
            return null;
        }
        Action action = AiActionSourceProvenance.actionForId(
                decision, response);
        PhysicalCard source =
                AiActionSourceProvenance.selectedActionSource(
                    decision, response);
        String actionText = action != null
                ? action.getText() : null;
        if (!isOwnedCastle(source, playerId)
                || !OUTBOUND_ACTION.equals(actionText)
                    && !INBOUND_ACTION.equals(actionText)) {
            return null;
        }
        return action;
    }

    private boolean isLive(
            GameState gameState,
            String playerId,
            ObjectiveAnalyzer objectiveAnalyzer) {
        if (gameState == null || playerId == null
                || objectiveAnalyzer == null
                || !objectiveAnalyzer.isHuntDownV()
                || objectiveAnalyzer.isFlipped()
                || gameState.getCurrentPhase() != Phase.MOVE
                || gameState.getPlayersLatestTurnNumber(playerId) != turn) {
            return false;
        }
        PhysicalCard source = gameState.findCardByPermanentId(
                actionSourcePermanentCardId);
        return isOwnedCastle(source, playerId);
    }

    private boolean isOwnedCastle(
            PhysicalCard source, String playerId) {
        return source != null
                && playerId.equals(source.getOwner())
                && "209_50".equals(source.getBlueprintId(true))
                && source.getZone() != null
                && source.getZone().isInPlay();
    }

    private boolean isExpectedChild(
            AwaitingDecision decision, Step expectedStep) {
        if (decision == null
                || decision.getDecisionType()
                    != AwaitingDecisionType.CARD_SELECTION
                || decision.getText() == null) {
            return false;
        }
        String prompt = decision.getText().trim()
                .toLowerCase(java.util.Locale.ROOT);
        String optionalCancel =
                ", or click 'done' to cancel";
        if (prompt.endsWith(optionalCancel)) {
            prompt = prompt.substring(
                    0, prompt.length()
                        - optionalCancel.length()).trim();
        }
        return switch (expectedStep) {
            case ORIGIN -> "choose card to move from".equals(prompt);
            case DESTINATION -> "choose card to move to".equals(prompt);
            case MOVER -> prompt.startsWith("choose card to move to ");
        };
    }

    private boolean isOfferedCard(
            AwaitingDecision decision, String response) {
        Map<String, String[]> parameters =
                decision.getDecisionParameters();
        String[] cardIds = parameters != null
                ? parameters.get("cardId") : null;
        if (cardIds == null) {
            return false;
        }
        for (String cardId : cardIds) {
            if (response.equals(cardId)) {
                return true;
            }
        }
        return false;
    }

    private PhysicalCard selectedCard(
            GameState gameState, String response) {
        try {
            return gameState.findCardById(
                    Integer.parseInt(response));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isLocation(PhysicalCard card) {
        return card.getBlueprint() != null
                && card.getBlueprint().getCardCategory()
                    == CardCategory.LOCATION;
    }
}
