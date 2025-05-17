package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Actions are top-level decisions that involve legal game operations available to players.  For example, deploying
 * a card, playing an interrupt, and activating a card ability are all Actions.
 */
public interface Actions extends Decisions, Choices {

	/**
	 * @return Gets the text descriptions of all current actions available to the Dark Side player. Will return an empty
	 * list if that player does not have a currently pending decision.
	 */
	default List<String> DSGetAvailableActions() { return GetAvailableActions(DS); }
	/**
	 * @return Gets the text descriptions of all current actions available to the Light Side player. Will return an empty
	 * list if that player does not have a currently pending decision.
	 */
	default List<String> LSGetAvailableActions() { return GetAvailableActions(LS); }
	/**
	 * @param playerID The player with a current decision
	 * @return Gets the text descriptions of all current actions available to the given player.  Will return an empty
	 * list if that player does not have a currently pending decision.
	 */
	default List<String> GetAvailableActions(String playerID) {
		AwaitingDecision decision = GetAwaitingDecision(playerID);
		if(decision == null) {
			return new ArrayList<>();
		}
		return Arrays.asList(decision.getDecisionParameters().get("actionText"));
	}

	/**
	 * @return True if an action is available as part of the current decision, false if there are no actions or the
	 * Dark Side player has no pending decisions.
	 */
	default Boolean DSAnyActionsAvailable() { return AnyActionsAvailable(DS); }

	/**
	 * @return True if an action is available as part of the current decision, false if there are no actions or the
	 * Light Side player has no pending decisions.
	 */
	default Boolean LSAnyActionsAvailable() { return AnyActionsAvailable(LS); }

	/**
	 * Returns whether the given player has any action at all available as part of the currently pending decision.
	 * @param player The player to check for.
	 * @return True if an action is available as part of the current decision, false if there are no actions or the
	 * current player has no pending decisions.
	 */
	default Boolean AnyActionsAvailable(String player) {
		List<String> actions = GetAvailableActions(player);
		return !actions.isEmpty();
	}



	//TODO: Clean these up to use the correct search text
	default Boolean DSActionAvailable(PhysicalCardImpl card) { return ActionAvailable(DS, "Use " + GameUtils.getFullName(card)); }
	default Boolean LSActionAvailable(PhysicalCardImpl card) { return ActionAvailable(LS, "Use " + GameUtils.getFullName(card)); }

	default Boolean DSPlayAvailable(PhysicalCardImpl card) { return ActionAvailable(DS, "Play " + GameUtils.getFullName(card)); }
	default Boolean LSPlayAvailable(PhysicalCardImpl card) { return ActionAvailable(LS, "Play " + GameUtils.getFullName(card)); }

	default Boolean DSTransferAvailable(PhysicalCardImpl card) { return ActionAvailable(DS, "Transfer " + GameUtils.getFullName(card)); }
	default Boolean LSTransferAvailable(PhysicalCardImpl card) { return ActionAvailable(LS, "Transfer " + GameUtils.getFullName(card)); }
	/**
	 * Checks whether the Dark Side player has an action available containing the provided text.
	 * @param action The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default Boolean DSActionAvailable(String action) { return ActionAvailable(DS, action); }
	/**
	 * Checks whether the Light Side player has an action available containing the provided text.
	 * @param action The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default Boolean LSActionAvailable(String action) { return ActionAvailable(LS, action); }
	/**
	 * Checks whether the given player has an action available containing the provided text.
	 * @param player The player to check for.
	 * @param action The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default Boolean ActionAvailable(String player, String action) {
		List<String> actions = GetAvailableActions(player);
		if(actions == null)
			return false;
		String lowerAction = action.toLowerCase();
		return actions.stream().anyMatch(x -> x.toLowerCase().contains(lowerAction));
	}

	//TODO: Identify whether all of these actions are valid SWCCG nomenclature and match how Gemp presents these actions.
	default void DSUseCardAction(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card)); }
	default void LSUseCardAction(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card)); }

	default void DSTransferCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "Transfer")); }
	default void LSTransferCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "Transfer ")); }

	default void DSPlayCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "Play")); }
	default void LSPlayCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "Play")); }

	/**
	 * Causes the Dark Side player to perform  a legal deployment action of the given card (i.e. plays that card from hand).
	 * @param card The card to deploy.
	 * @throws DecisionResultInvalidException This error will be thrown if the card is not in hand or is otherwise not
	 * legal to deploy (due to costs, requirements, or other rules).
	 */
	default void DSDeployCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "Deploy")); }
	/**
	 * Causes the Light Side player to perform  a legal deployment action of the given card (i.e. plays that card from hand).
	 * @param card The card to deploy.
	 * @throws DecisionResultInvalidException This error will be thrown if the card is not in hand or is otherwise not
	 * legal to deploy (due to costs, requirements, or other rules).
	 */
	default void LSDeployCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "Deploy")); }


	/**
	 * Searches the currently available actions on the current decision for the given player and returns the ID of an
	 * action which contains the provided text in its description.
	 * @param playerId The player which must have a currently active decision.
	 * @param text Constrains the result to only actions whose description contains the provided text
	 * @return The action ID of a matching action (which can be passed as a decision answer).  Returns null if no
	 * actions matched.
	 */
	default String GetCardActionId(String playerId, String text) { return GetCardActionId(playerId, null, text); }
	/**
	 * Searches the currently available actions on the current decision for the given player and returns the ID of an
	 * action which was sourced by the provided card's ID.
	 * @param playerId The player which must have a currently active decision.
	 * @param card Constrains the result to only actions which are source from this card.
	 * @return The action ID of a matching action (which can be passed as a decision answer).  Returns null if no
	 * actions matched.
	 */
	default String GetCardActionId(String playerId, PhysicalCardImpl card) { return GetCardActionId(playerId, card, null); }

	/**
	 * Searches the currently available actions on the current decision for the given player.  If card is provided, the
	 * card's ID must be the source of one of the given actions.  If text is provided, the action description must match
	 * the given text.  If both are provided, both are checked.
	 * @param playerId The player which must have a currently active decision.
	 * @param card If provided, constrains the result to only actions which are source from this card.
	 * @param text If provided, constrains the result to only actions whose description contains the provided text
	 * @return The action ID of a matching action (which can be passed as a decision answer).  Returns null if no
	 * actions matched.
	 */
	default String GetCardActionId(String playerId, PhysicalCardImpl card, String text) {
		String id = card != null ? String.valueOf(card.getCardId()) : null;
		String[] cardIds = GetAwaitingDecisionParam(playerId, "cardId");
		String[] actionTexts = GetAwaitingDecisionParam(playerId, "actionText");

		for (int i = 0; i < cardIds.length; i++) {
			if ((id == null || cardIds[i].equals(id)) && (text == null || actionTexts[i].contains(text))) {
				return GetAwaitingDecisionParam(playerId, "actionId")[i];
			}
		}
		return null;
	}

}
