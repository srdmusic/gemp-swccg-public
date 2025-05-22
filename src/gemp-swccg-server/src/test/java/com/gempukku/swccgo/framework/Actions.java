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

	default Boolean DSLostInterruptPlayAvailable(PhysicalCardImpl card) { return DSActionAvailable(card, "LOST: "); }
	default Boolean LSLostInterruptPlayAvailable(PhysicalCardImpl card) { return LSActionAvailable(card, "LOST: "); }
	default Boolean DSUsedInterruptPlayAvailable(PhysicalCardImpl card) { return DSActionAvailable(card, "USED: "); }
	default Boolean LSUsedInterruptPlayAvailable(PhysicalCardImpl card) { return LSActionAvailable(card, "USED: "); }

	default Boolean DSDeployAvailable(PhysicalCardImpl card) { return DSActionAvailable(card, "Deploy"); }
	default Boolean LSDeployAvailable(PhysicalCardImpl card) { return LSActionAvailable(card, "Deploy"); }

	default Boolean DSTransferAvailable(PhysicalCardImpl card) { return DSActionAvailable(card, "Transfer"); }
	default Boolean LSTransferAvailable(PhysicalCardImpl card) { return LSActionAvailable(card, "Transfer"); }

	default Boolean DSCardActionAvailable(PhysicalCardImpl card) { return DSActionAvailable(card); }
	default Boolean LSCardActionAvailable(PhysicalCardImpl card) { return LSActionAvailable(card); }



	/**
	 * Checks whether the Dark Side player has an action available containing the provided text.
	 * @param text The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default Boolean DSActionAvailable(String text) { return ActionAvailable(DS, null, text); }
	/**
	 * Checks whether the Dark Side player has an action available containing the provided text.
	 * @param card The card ID to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default Boolean DSActionAvailable(PhysicalCardImpl card) { return ActionAvailable(DS, card, null); }
	/**
	 * Checks whether the Dark Side player has an action available containing the provided text.
	 * @param card The card ID to search for.
	 *             @param text The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default Boolean DSActionAvailable(PhysicalCardImpl card, String text) { return ActionAvailable(DS, card, text); }
	/**
	 * Checks whether the Light Side player has an action available containing the provided text.
	 * @param text The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default Boolean LSActionAvailable(String text) { return ActionAvailable(LS, null, text); }
	/**
	 * Checks whether the Light Side player has an action available containing the provided text.
	 * @param card The card ID to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default Boolean LSActionAvailable(PhysicalCardImpl card) { return ActionAvailable(LS, card, null); }
	/**
	 * Checks whether the Light Side player has an action available containing the provided text.
	 * @param card The card ID to search for.
	 * @param text The text to search for.
	 * @return True if an active decision has an action matching text, otherwise false.
	 */
	default Boolean LSActionAvailable(PhysicalCardImpl card, String text) { return ActionAvailable(LS, card, text); }
	/**
	 * Checks whether the given player has an action available containing the provided text.
	 * @param playerId The player to check for.
	 * @param card The card ID to search for.
	 * @param text The text to search for.
	 * @return True if an active decision has an action matching card and/or text, otherwise false.
	 */
	default Boolean ActionAvailable(String playerId, PhysicalCardImpl card, String text) {
		return GetCardActionId(playerId, card, text) != null;
	}

	//TODO: Identify whether all of these actions are valid SWCCG nomenclature and match how Gemp presents these actions.

	default void DSPlayLostInterrupt(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "LOST: ")); }
	default void LSPlayLostInterrupt(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "LOST: ")); }

	default void DSPlayUsedInterrupt(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "USED: ")); }
	default void LSPlayUsedInterrupt(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "USED: ")); }

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

	default void DSTransferCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "Transfer")); }
	default void LSTransferCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "Transfer ")); }

	default void DSUseCardAction(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card)); }
	default void LSUseCardAction(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card)); }




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
