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
 *
 * Actions are top-level decisions that involve legal game operations available to players.  For example, deploying
 * a card, playing an interrupt, and activating a card ability are all Actions.
 */
public interface Actions extends Decisions, Choices {

	default String GetCardActionId(String playerId, String text) { return GetCardActionId(playerId, null, text); }
	default String GetCardActionId(String playerId, PhysicalCardImpl card) { return GetCardActionId(playerId, card, null); }
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

	default List<String> DSGetAvailableActions() { return GetAvailableActions(DS); }
	default List<String> LSGetAvailableActions() { return GetAvailableActions(LS); }
	default List<String> GetAvailableActions(String playerID) {
		AwaitingDecision decision = GetAwaitingDecision(playerID);
		if(decision == null) {
			return new ArrayList<>();
		}
		return Arrays.asList(decision.getDecisionParameters().get("actionText"));
	}

	default Boolean DSAnyActionsAvailable() { return AnyActionsAvailable(DS); }
	default Boolean LSAnyActionsAvailable() { return AnyActionsAvailable(LS); }
	default Boolean AnyActionsAvailable(String player) {
		List<String> actions = GetAvailableActions(player);
		return !actions.isEmpty();
	}

	default Boolean DSActionAvailable(String action) { return ActionAvailable(DS, action); }
	default Boolean LSActionAvailable(String action) { return ActionAvailable(LS, action); }

	default Boolean DSActionAvailable(PhysicalCardImpl card) { return ActionAvailable(DS, "Use " + GameUtils.getFullName(card)); }
	default Boolean LSActionAvailable(PhysicalCardImpl card) { return ActionAvailable(LS, "Use " + GameUtils.getFullName(card)); }

	default Boolean DSPlayAvailable(PhysicalCardImpl card) { return ActionAvailable(DS, "Play " + GameUtils.getFullName(card)); }
	default Boolean LSPlayAvailable(PhysicalCardImpl card) { return ActionAvailable(LS, "Play " + GameUtils.getFullName(card)); }

	default Boolean DSTransferAvailable(PhysicalCardImpl card) { return ActionAvailable(DS, "Transfer " + GameUtils.getFullName(card)); }
	default Boolean LSTransferAvailable(PhysicalCardImpl card) { return ActionAvailable(LS, "Transfer " + GameUtils.getFullName(card)); }
	default Boolean ActionAvailable(String player, String action) {
		List<String> actions = GetAvailableActions(player);
		if(actions == null)
			return false;
		String lowerAction = action.toLowerCase();
		return actions.stream().anyMatch(x -> x.toLowerCase().contains(lowerAction));
	}

	default void DSUseCardAction(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card)); }
	default void LSUseCardAction(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card)); }

	default void DSTransferCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "Transfer")); }
	default void LSTransferCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "Transfer ")); }

	default void DSPlayCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "Play")); }
	default void LSPlayCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSDecided(GetCardActionId(LS, card, "Play")); }

	default void DSDeployCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(DS, card, "Deploy")); }
	default void LSDeployCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSDecided(GetCardActionId(LS, card, "Deploy")); }




}
