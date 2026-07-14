package com.gempukku.swccgo.logic.decisions;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DeployActionMetadata;
import com.gempukku.swccgo.common.DeployDecisionWire;
import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.PullDecisionWire;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A decision that involves choosing a card from the table (or hand) on the User Interface to perform an action.
 */
public abstract class CardActionSelectionDecision extends AbstractAwaitingDecision {
    private static final AtomicLong NEXT_PULL_TRANSACTION_ID = new AtomicLong();
    private static final AtomicLong NEXT_DEPLOY_ATTEMPT_ID = new AtomicLong();
    private List<Action> _actions;

    /**
     * Creates a decision that involves choosing a card from the table (or hand) on the User Interface to perform an action.
     * @param decisionId the decision id
     * @param text the text to show the player making the decision
     * @param actions the actions to choose from
     * @param yourTurn true if the decision is during the player's turn, otherwise false
     * @param autoPassEligible true if the decision is auto-pass eligible, otherwise false
     * @param noPass true if passing is not an option, otherwise false
     * @param noLongDelay true if no long delay should be used when mimic decision time when no actions, otherwise false
     * @param revertEligible true if choosing to revert to a previous game state is supported during this decision, otherwise false
     */
    public CardActionSelectionDecision(int decisionId, String text, List<Action> actions, boolean yourTurn, boolean autoPassEligible, boolean noPass, boolean noLongDelay, boolean revertEligible) {
        super(decisionId, text, AwaitingDecisionType.CARD_ACTION_CHOICE);
        _actions = actions;
        prepareDeployAttempts(actions);
        setParam("actionId", getActionIds(actions));
        setParam("cardId", getCardIds(actions));
        setParam("blueprintId", getBlueprintIdsForVirtualActions(actions));
        setParam("testingText", getTestingTextsForVirtualActions(actions));
        setParam("backSideTestingText", getBackSideTestingTextsForVirtualActions(actions));
        setParam("horizontal", getHorizontalsForVirtualActions(actions));
        setParam("actionText", getActionTexts(actions));
        setParam(DecisionActionSemantic.WIRE_PARAMETER, getActionSemantics(actions));
        if (hasPullAction(actions)) {
            setParam(PullDecisionWire.SOURCE_CARD_ID, getActionSourceCardIds(actions));
            setParam(PullDecisionWire.SOURCE_PERMANENT_CARD_ID, getActionSourcePermanentCardIds(actions));
            setParam(PullDecisionWire.GAME_TEXT_ACTION_ID, getGameTextActionIds(actions));
        }
        if (hasDeployAction(actions)) {
            setParam(DeployDecisionWire.ATTEMPT_ID, getDeployAttemptIds(actions));
            setParam(DeployDecisionWire.PLAYER_ID, getDeployPlayerIds(actions));
            setParam(DeployDecisionWire.SOURCE_CARD_ID, getDeploySourceCardIds(actions));
            setParam(DeployDecisionWire.SOURCE_PERMANENT_CARD_ID,
                    getDeploySourcePermanentCardIds(actions));
            setParam(DeployDecisionWire.SOURCE_ZONE, getDeploySourceZones(actions));
            setParam(DeployDecisionWire.DESTINATION_LEGALITY_KNOWN,
                    getDeployDestinationKnown(actions));
            setParam(DeployDecisionWire.LEGAL_DESTINATIONS,
                    getDeployLegalDestinations(actions));
            setParam(DeployDecisionWire.LEGAL_BUDDIES,
                    getDeployLegalBuddies(actions));
            setParam(DeployDecisionWire.SELECTED_BUDDY,
                    getDeploySelectedBuddies(actions));
        }
        setParam("yourTurn", String.valueOf(yourTurn));
        setParam("autoPassEligible", String.valueOf(autoPassEligible));
        setParam("noPass", String.valueOf(noPass));
        setParam("noLongDelay", String.valueOf(noLongDelay));
        setParam("revertEligible", String.valueOf(revertEligible));
    }

    /**
     * For testing, being able to inject an extra action at any point.
     *
     * @param action
     */
    public void addAction(Action action) {
        _actions.add(action);
    }

    /**
     * Gets the temp action ids
     * @param actions the actions
     * @return the temp action ids
     */
    private String[] getActionIds(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = String.valueOf(i);
        return result;
    }

    /**
     * Gets an array of card ids.
     * @param actions the actions
     * @return the card ids
     */
    private String[] getCardIds(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            Action action = actions.get(i);
            if (action.getActionAttachedToCard() == null) {
                throw new UnsupportedOperationException("Null card id in CardActionSelectionDecision; Type: " + action.getType() + "; Text:" + action.getText() + "; Player:" + action.getPerformingPlayer() + "; Class: " + action.getClass().getSimpleName());
            }
            result[i] = String.valueOf(action.getActionAttachedToCard().getCardId());
        }
        return result;
    }

    /**
     * Gets the card blueprint ids in case the card is not currently on the table (or hand).
     * @param actions the actions
     * @return the card blueprint ids
     */
    private String[] getBlueprintIdsForVirtualActions(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            Action action = actions.get(i);
            if (action.isOptionalOffTableCardAction())
                result[i] = String.valueOf(action.getActionSource().getBlueprintId(true));
            else
                result[i] = "inPlay";
        }
        return result;
    }

    /**
     * Gets the card testing texts.
     * @param actions the actions
     * @return the card testing texts
     */
    private String[] getTestingTextsForVirtualActions(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            PhysicalCard physicalCard = actions.get(i).getActionAttachedToCard();
            if (physicalCard != null)
                result[i] = String.valueOf(physicalCard.getTestingText(null, physicalCard.getBlueprint().getCardCategory() != CardCategory.OBJECTIVE, false));
            else
                result[i] = "null";
        }
        return result;
    }

    /**
     * Gets the card backside testing texts.
     * @param actions the actions
     * @return the card testing texts
     */
    private String[] getBackSideTestingTextsForVirtualActions(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            PhysicalCard physicalCard = actions.get(i).getActionAttachedToCard();
            if (physicalCard != null)
                result[i] = String.valueOf(physicalCard.getTestingText(null, physicalCard.getBlueprint().getCardCategory() != CardCategory.OBJECTIVE, true));
            else
                result[i] = "null";
        }
        return result;
    }

    /**
     * Gets the card horizontals.
     * @param actions the actions
     * @return the card horizontals
     */
    private String[] getHorizontalsForVirtualActions(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            PhysicalCard physicalCard = actions.get(i).getActionAttachedToCard();
            if (physicalCard != null)
                result[i] = String.valueOf(physicalCard.getBlueprint().isHorizontal());
            else
                result[i] = "false";
        }
        return result;
    }

    /**
     * Gets the action texts
     * @param actions the actions
     * @return the texts to show
     */
    private String[] getActionTexts(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++)
            result[i] = actions.get(i).getText();
        return result;
    }

    /** Gets ordinal-aligned, nonblank semantic identities for all offered actions. */
    private String[] getActionSemantics(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            DecisionActionSemantic semantic = actions.get(i).getDecisionActionSemantic();
            result[i] = (semantic != null ? semantic : DecisionActionSemantic.UNKNOWN).name();
        }
        return result;
    }

    /** Gets ordinal-aligned physical source identities for typed action routes. */
    private String[] getActionSourceCardIds(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            PhysicalCard source = actions.get(i).getActionSource();
            result[i] = source != null ? String.valueOf(source.getCardId()) : "";
        }
        return result;
    }

    private boolean hasPullAction(List<Action> actions) {
        for (Action action : actions) {
            DecisionActionSemantic semantic = action.getDecisionActionSemantic();
            if (semantic == DecisionActionSemantic.PULL_DEPLOY_FROM_PILE
                    || semantic == DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE) {
                return true;
            }
        }
        return false;
    }

    private void prepareDeployAttempts(List<Action> actions) {
        for (Action action : actions) {
            if (action.getDecisionActionSemantic() == DecisionActionSemantic.DEPLOY_CARD
                    && action.getDeployAttemptId() == null) {
                action.setDeployAttemptId("DEPLOY-" + NEXT_DEPLOY_ATTEMPT_ID.incrementAndGet());
            }
        }
    }

    private boolean hasDeployAction(List<Action> actions) {
        for (Action action : actions) {
            if (action.getDecisionActionSemantic() == DecisionActionSemantic.DEPLOY_CARD) {
                return true;
            }
        }
        return false;
    }

    private String[] getDeployAttemptIds(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            Action action = actions.get(i);
            result[i] = action.getDecisionActionSemantic() == DecisionActionSemantic.DEPLOY_CARD
                    ? action.getDeployAttemptId() : "";
        }
        return result;
    }

    private String[] getDeployPlayerIds(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            Action action = actions.get(i);
            result[i] = action.getDecisionActionSemantic() == DecisionActionSemantic.DEPLOY_CARD
                    && action.getPerformingPlayer() != null ? action.getPerformingPlayer() : "";
        }
        return result;
    }

    private String[] getDeploySourceCardIds(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            DeployActionMetadata metadata = deployMetadata(actions.get(i));
            result[i] = metadata != null
                    ? String.valueOf(metadata.sourceCard().currentCardId()) : "";
        }
        return result;
    }

    private String[] getDeploySourcePermanentCardIds(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            DeployActionMetadata metadata = deployMetadata(actions.get(i));
            result[i] = metadata != null
                    ? String.valueOf(metadata.sourceCard().permanentCardId()) : "";
        }
        return result;
    }

    private String[] getDeploySourceZones(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            DeployActionMetadata metadata = deployMetadata(actions.get(i));
            result[i] = metadata != null && metadata.sourceZone() != null
                    ? metadata.sourceZone().name() : "";
        }
        return result;
    }

    private String[] getDeployDestinationKnown(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            DeployActionMetadata metadata = deployMetadata(actions.get(i));
            result[i] = metadata != null
                    ? String.valueOf(metadata.destinationLegalityKnown()) : "";
        }
        return result;
    }

    private String[] getDeployLegalDestinations(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            DeployActionMetadata metadata = deployMetadata(actions.get(i));
            if (metadata == null || !metadata.destinationLegalityKnown()) {
                result[i] = "";
                continue;
            }
            result[i] = metadata.orderedLegalDestinations().stream()
                    .map(CardActionSelectionDecision::encodeDestination)
                    .collect(java.util.stream.Collectors.joining(","));
        }
        return result;
    }

    private String[] getDeployLegalBuddies(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            DeployActionMetadata metadata = deployMetadata(actions.get(i));
            result[i] = metadata != null
                    ? metadata.orderedLegalBuddies().stream()
                            .map(CardActionSelectionDecision::encodeCard)
                            .collect(java.util.stream.Collectors.joining(","))
                    : "";
        }
        return result;
    }

    private String[] getDeploySelectedBuddies(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            DeployActionMetadata metadata = deployMetadata(actions.get(i));
            result[i] = metadata != null && metadata.selectedBuddy() != null
                    ? encodeCard(metadata.selectedBuddy()) : "";
        }
        return result;
    }

    private DeployActionMetadata deployMetadata(Action action) {
        return action.getDecisionActionSemantic() == DecisionActionSemantic.DEPLOY_CARD
                ? action.getDeployActionMetadata() : null;
    }

    private static String encodeDestination(DeployDestinationRef destination) {
        if (destination instanceof DeployDestinationRef.Card card) {
            return "CARD:" + card.card().permanentCardId() + ":"
                    + card.card().currentCardId();
        }
        DeployDestinationRef.ZoneDestination zone =
                (DeployDestinationRef.ZoneDestination) destination;
        return "ZONE:" + zone.zone().name();
    }

    private static String encodeCard(
            com.gempukku.swccgo.common.DeployPhysicalCardRef card) {
        return card.permanentCardId() + ":" + card.currentCardId();
    }

    /** Gets ordinal-aligned permanent physical identities for typed action routes. */
    private String[] getActionSourcePermanentCardIds(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            PhysicalCard source = actions.get(i).getActionSource();
            result[i] = source != null ? String.valueOf(source.getPermanentCardId()) : "";
        }
        return result;
    }

    /** Gets ordinal-aligned search-function identities where the engine exposes one. */
    private String[] getGameTextActionIds(List<Action> actions) {
        String[] result = new String[actions.size()];
        for (int i = 0; i < result.length; i++) {
            Action action = actions.get(i);
            DecisionActionSemantic semantic = action.getDecisionActionSemantic();
            boolean isPull = semantic == DecisionActionSemantic.PULL_DEPLOY_FROM_PILE
                    || semantic == DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE;
            if (!isPull) {
                result[i] = "";
                continue;
            }
            if (!action.isFromGameText() && !action.isFromPlayingInterrupt()) {
                result[i] = "";
                continue;
            }
            com.gempukku.swccgo.common.GameTextActionId gameTextActionId =
                    action.getGameTextActionId();
            result[i] = gameTextActionId != null
                    ? gameTextActionId.name()
                    : "";
        }
        return result;
    }

    /**
     * Gets the action the player selected during the decision.
     * @param result the result
     * @return the action selected
     * @throws DecisionResultInvalidException
     */
    protected Action getSelectedAction(String result) throws DecisionResultInvalidException {
        if (result.isEmpty())
            return null;

        try {
            int actionIndex = Integer.parseInt(result);
            if (actionIndex < 0 || actionIndex >= _actions.size())
                throw new DecisionResultInvalidException();

            Action selected = _actions.get(actionIndex);
            DecisionActionSemantic semantic = selected.getDecisionActionSemantic();
            if (semantic == DecisionActionSemantic.PULL_DEPLOY_FROM_PILE
                    || semantic == DecisionActionSemantic.PULL_TAKE_INTO_HAND_FROM_PILE) {
                selected.setAcceptedDecisionIdentity(getAwaitingDecisionId(), actionIndex);
                selected.setAcceptedPullTransactionId(NEXT_PULL_TRANSACTION_ID.incrementAndGet());
            } else if (semantic == DecisionActionSemantic.DEPLOY_CARD) {
                selected.setAcceptedDecisionIdentity(getAwaitingDecisionId(), actionIndex);
            }
            return selected;
        } catch (NumberFormatException exp) {
            throw new DecisionResultInvalidException();
        }
    }
}
