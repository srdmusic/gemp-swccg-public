package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Decisions will always come with at least one choice, even if that single choice is "pass".  These functions will
 * let you inspect the choices available and offer shortcuts for e.g. selecting a physical card which you have
 * previously stored.
 */
public interface Choices extends Decisions {

	/**
	 * Determines whether the Dark Side player has any choices on the current decision whose description matches the
	 * provided search text.
	 * @param choice The search text the choice description must contain.
	 * @return True if the Dark Side player has an active decision with a choice description matching the given text.
	 */
	default Boolean DSChoiceAvailable(String choice) { return ChoiceAvailable(DS, choice); }
	/**
	 * Determines whether the Light Side player has any choices on the current decision whose description matches the
	 * provided search text.
	 * @param choice The search text the choice description must contain.
	 * @return True if the Light Side player has an active decision with a choice description matching the given text.
	 */
	default Boolean LSChoiceAvailable(String choice) { return ChoiceAvailable(LS, choice); }
	/**
	 * Determines whether the given player has any choices on the current decision whose description matches the
	 * provided search text.
	 * @param player The player which must have a currently active decision.
	 * @param choice The search text the choice description must contain.
	 * @return True if the given player has an active decision with a choice description matching the given text.
	 */
	default Boolean ChoiceAvailable(String player, String choice) {
		List<String> actions = GetADParamAsList(player, "results");
		if(actions == null)
			return false;
		String lowerChoice = choice.toLowerCase();
		return actions.stream().anyMatch(x -> x.toLowerCase().contains(lowerChoice));
	}

	/**
	 * Causes the Dark Side player to choose the given option.
	 * This is a catch-all that either selects the provided choice if part of a multiple choice decision, or else
	 * falls back on providing the provided choice as a top-level response to the current decision.
	 * @param choice The choice (or decision response)
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void DSChoose(String choice) throws DecisionResultInvalidException {
		if(DSGetChoiceCount() > 0) {
			DSChooseOption(choice);
		}
		else {
			PlayerDecided(DS, choice);
		}
	}

	/**
	 * Causes the Dark Side player to choose the given options.  This will automatically format the response to contain
	 * all the provided options in a comma-separated list.
	 * @param choices The choices the player wishes to make.
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void DSChoose(String...choices) throws DecisionResultInvalidException { PlayerDecided(DS, String.join(",", choices)); }
	/**
	 * Causes the Light Side player to choose the given option.
	 * This is a catch-all that either selects the provided choice if part of a multiple choice decision, or else
	 * falls back on providing the provided choice as a top-level response to the current decision.
	 * @param choice The choice (or decision response)
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void LSChoose(String choice) throws DecisionResultInvalidException {
		if(LSGetChoiceCount() > 0) {
			LSChooseOption(choice);
		}
		else {
			PlayerDecided(LS, choice);
		}
	}
	/**
	 * Causes the Light Side player to choose the given options.  This will automatically format the response to contain
	 * all the provided options in a comma-separated list.
	 * @param choices The choices the player wishes to make.
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void LSChoose(String...choices) throws DecisionResultInvalidException { PlayerDecided(LS, String.join(",", choices)); }


	/**
	 * Causes the Dark Side player to return a canned "Yes" response to a Yes or No question.
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void DSChooseYes() throws DecisionResultInvalidException { ChooseOption(DS, "Yes"); }
	/**
	 * Causes the Light Side player to return a canned "Yes" response to a Yes or No question.
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void LSChooseYes() throws DecisionResultInvalidException { ChooseOption(LS, "Yes"); }
	/**
	 * Causes the given player to return a canned "Yes" response to a Yes or No question.
	 * @param player The player to make the decision for
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void PlayerChooseYes(String player) throws DecisionResultInvalidException { ChooseOption(player, "Yes"); }
	/**
	 * Causes the Dark Side player to return a canned "No" response to a Yes or No question.
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void DSChooseNo() throws DecisionResultInvalidException { ChooseOption(DS, "No"); }
	/**
	 * Causes the Light Side player to return a canned "No" response to a Yes or No question.
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void LSChooseNo() throws DecisionResultInvalidException { ChooseOption(LS, "No"); }
	/**
	 * Causes the given player to return a canned "No" response to a Yes or No question.
	 * @param player The player to make the decision for
	 * @throws DecisionResultInvalidException This error will be thrown if the response is invalid for the current decision.
	 */
	default void PlayerChooseNo(String player) throws DecisionResultInvalidException { ChooseOption(player, "No"); }

	default void DSChooseOption(String option) throws DecisionResultInvalidException { ChooseOption(DS, option); }
	default void LSChooseOption(String option) throws DecisionResultInvalidException { ChooseOption(LS, option); }

	default void DSChooseAction(String paramName, String option) throws DecisionResultInvalidException { ChooseAction(DS, paramName, option); }
	default void DSChooseAction(String option) throws DecisionResultInvalidException { ChooseAction(DS, "actionText", option); }
	default void LSChooseAction(String paramName, String option) throws DecisionResultInvalidException { ChooseAction(LS, paramName, option); }
	default void LSChooseAction(String option) throws DecisionResultInvalidException { ChooseAction(LS, "actionText", option); }
	default void ChooseAction(String playerID, String option) throws DecisionResultInvalidException { ChooseAction(playerID, "actionText", option); }



	//The reason this is commented out is because I am unsure how rule timing resolution occurs in the SWCCG.
	//In LOTR, the Free Peoples player gets to pick the order in event of a tie, which is then followed here
//
//    default void DSChooseAny() throws DecisionResultInvalidException {
//        if (GetChoiceCount(DSGetActionChoices()) > 0){
//            ChooseAction(DS, "actionId", DSGetActionChoices().getFirst());
//        }
//        else if(DSGetBPChoices().size() > 1) {
//            ChooseCardBPFromSelection(DS, DSGetBPChoices().getFirst());
//        }
//        else {
//            DSResolveRuleFirst();
//        }
//    }
//
//    default void DSResolveRuleFirst() throws DecisionResultInvalidException { DSResolveActionOrder(GetADParamAsList(DS, "actionText").getFirst()); }
//    default void DSResolveActionOrder(String option) throws DecisionResultInvalidException { ChooseAction(DS, "actionText", option); }
//

	/**
	 * @return Gets the min parameter on the current choice for the Dark Side player.  This may be a minimum
	 * number of responses, or the smallest acceptable numeric answer, depending on context.
	 */
	default int DSGetChoiceMin() { return Integer.parseInt(DSGetFirstADParam("min")); }
	/**
	 * @return Gets the max parameter on the current choice for the Dark Side player.  This may be a maximum
	 * number of responses, or the largest acceptable numeric answer, depending on context.
	 */
	default int DSGetChoiceMax() { return Integer.parseInt(DSGetFirstADParam("max")); }
	/**
	 * @return Gets the min parameter on the current choice for the Light Side player.  This may be a minimum
	 * number of responses, or the smallest acceptable numeric answer, depending on context.
	 */
	default int LSGetChoiceMin() { return Integer.parseInt(LSGetFirstADParam("min")); }
	/**
	 * @return Gets the max parameter on the current choice for the Light Side player.  This may be a maximum
	 * number of responses, or the largest acceptable numeric answer, depending on context.
	 */
	default int LSGetChoiceMax() { return Integer.parseInt(LSGetFirstADParam("max")); }

	/**
	 * @return Gets how many of the currently displayed cards are selectable for a Dark Side decision.
	 */
	default int DSGetSelectableCount() {
		return GetADParamEqualsCount(DS, "selectable", "true");
	}

	/**
	 * @return Gets how many of the currently displayed cards are selectable for a Light Side decision.
	 */
	default int LSGetSelectableCount() {
		return GetADParamEqualsCount(LS, "selectable", "true");
	}

	default boolean DSHasBPChoice(PhysicalCardImpl card) { return DSGetBPChoices().contains(card.getBlueprintId(true)); }
	default boolean LSHasBPChoice(PhysicalCardImpl card) { return LSGetBPChoices().contains(card.getBlueprintId(true)); }
	default List<String> DSGetBPChoices() { return GetADParamAsList(DS, "blueprintId"); }
	default List<String> LSGetBPChoices() { return GetADParamAsList(LS, "blueprintId"); }
	default List<String> DSGetActionChoices() { return GetADParamAsList(DS, "actionId"); }
	default List<String> LSGetActionChoices() { return GetADParamAsList(LS, "actionId"); }
	default List<String> DSGetMultipleChoices() { return GetADParamAsList(DS, "results"); }
	default List<String> LSGetMultipleChoices() { return GetADParamAsList(LS, "results"); }
	default List<String> DSGetCardChoices() { return GetADParamAsList(DS, "cardId"); }
	default List<String> LSGetCardChoices() { return GetADParamAsList(LS, "cardId"); }
	default int DSGetChoiceCount() { return GetChoiceCount(DSGetMultipleChoices()); }
	default int LSGetChoiceCount() { return GetChoiceCount(LSGetMultipleChoices()); }

	default int GetChoiceCount(List<String> list) {
		if(list == null)
			return 0;
		return list.size();
	}

	default List<String> DSGetADParamAsList(String paramName) { return GetADParamAsList(DS, paramName); }
	default List<String> LSGetADParamAsList(String paramName) { return GetADParamAsList(LS, paramName); }


	default int GetADParamEqualsCount(String playerID, String paramName, String value) {
		return (int) Arrays.stream(GetAwaitingDecisionParam(playerID, paramName)).filter(s -> s.equals(value)).count();
	}
	default String[] DSGetADParam(String paramName) { return GetAwaitingDecisionParam(DS, paramName); }
	default String[] LSGetADParam(String paramName) { return GetAwaitingDecisionParam(LS, paramName); }
	default String DSGetFirstADParam(String paramName) { return GetAwaitingDecisionParam(DS, paramName)[0]; }
	default String LSGetFirstADParam(String paramName) { return GetAwaitingDecisionParam(LS, paramName)[0]; }


	default Map<String, String[]> GetAwaitingDecisionParams(String playerID) {
		var decision = userFeedback().getAwaitingDecision(playerID);
		return decision.getDecisionParameters();
	}



	default void DSChooseCard(String name) throws DecisionResultInvalidException { DSChooseCards(GetDSCard(name)); }
	default void DSChooseCard(PhysicalCardImpl card) throws DecisionResultInvalidException { DSChooseCards(card); }
	default void LSChooseCard(String name) throws DecisionResultInvalidException { LSChooseCards(GetLSCard(name)); }
	default void LSChooseCard(PhysicalCardImpl card) throws DecisionResultInvalidException { LSChooseCards(card); }

	default void DSChooseAnyCard() throws DecisionResultInvalidException { DSChoose(DSGetCardChoices().getFirst()); }
	default void LSChooseAnyCard() throws DecisionResultInvalidException { LSChoose(LSGetCardChoices().getFirst()); }

	default void DSChooseCards(PhysicalCardImpl...cards) throws DecisionResultInvalidException {
		if(GetChoiceCount(DSGetBPChoices()) > 0) {
			ChooseCardBPFromSelection(DS, cards);
		}
		else {
			ChooseCards(DS, cards);
		}
	}
	default void LSChooseCards(PhysicalCardImpl...cards) throws DecisionResultInvalidException {
		if(GetChoiceCount(LSGetBPChoices()) > 0) {
			ChooseCardBPFromSelection(LS, cards);
		}
		else {
			ChooseCards(LS, cards);
		}
	}
	default void ChooseCards(String player, PhysicalCardImpl...cards) throws DecisionResultInvalidException {
		String[] ids = new String[cards.length];

		for(int i = 0; i < cards.length; i++)
		{
			ids[i] = String.valueOf(cards[i].getCardId());
		}

		PlayerDecided(player, String.join(",", ids));
	}

	default int DSGetCardChoiceCount() { return DSGetCardChoices().size(); }
	default int LSGetCardChoiceCount() { return LSGetCardChoices().size(); }

	default void DSChooseCardBPFromSelection(PhysicalCardImpl...cards) throws DecisionResultInvalidException { ChooseCardBPFromSelection(DS, cards);}
	default void LSChooseCardBPFromSelection(PhysicalCardImpl...cards) throws DecisionResultInvalidException { ChooseCardBPFromSelection(LS, cards);}

	/**
	 * Causes the given player to issue a decision response composed of a comma-separated list of the provided card
	 * blueprint IDs. This will only succeed if being used to target currently out-of-play cards such as when selecting
	 * cards from the reserve deck; it will not work if being presented with a choice of in-play cards to target (such
	 * as when choosing active cards to target for a card effect).
	 * @param player The player to issue a decision for.
	 * @param cards The cards to include in the decision response.
	 * @throws DecisionResultInvalidException This error will be thrown if the current decision does not accept blueprint
	 * IDs.
	 */
	default void ChooseCardBPFromSelection(String player, PhysicalCardImpl...cards) throws DecisionResultInvalidException {
		String[] choices = GetAwaitingDecisionParam(player,"blueprintId");
		ArrayList<String> bps = new ArrayList<>();
		ArrayList<PhysicalCardImpl> found = new ArrayList<>();

		for(int i = 0; i < choices.length; i++)
		{
			for(PhysicalCardImpl card : cards)
			{
				if(found.contains(card))
					continue;

				if(card.getBlueprintId(true).equals(choices[i]))
				{
					// I have no idea why the spacing is required, but the BP parser skips to the fourth position
					bps.add("    " + i);
					found.add(card);
					break;
				}
			}
		}

		PlayerDecided(player, String.join(",", bps));
		//ChooseCardBPFromSelection(player, Arrays.stream(cards).distinct().map(PhysicalCardImpl::getBlueprintId).toArray(String[]::new));
	}


	/**
	 * Causes the given player to issue a decision response composed of a comma-separated list of the provided card
	 * blueprint IDs. This will only succeed if being used to target currently out-of-play cards such as when selecting
	 * cards from the reserve deck; it will not work if being presented with a choice of in-play cards to target (such
	 * as when choosing active cards to target for a card effect).
	 * @param player The player to issue a decision for.
	 * @param bpids The card blueprint IDs to include in the decision response.
	 * @throws DecisionResultInvalidException This error will be thrown if the current decision does not accept blueprint
	 * IDs.
	 */
	default void ChooseCardBPFromSelection(String player, String...bpids) throws DecisionResultInvalidException {
		String[] choices = GetAwaitingDecisionParam(player,"blueprintId");
		ArrayList<String> bps = new ArrayList<>();
		ArrayList<String> found = new ArrayList<>();

		for(int i = 0; i < choices.length; i++)
		{
			for(String card : bpids)
			{
				if(found.contains(card))
					continue;
				if(card.equals(choices[i]))
				{
					// I have no idea why the spacing is required, but the BP parser skips to the fourth position
					bps.add("    " + i);
					found.add(card);
					break;
				}
			}
		}

		PlayerDecided(player, String.join(",", bps));
	}

	default boolean DSHasCardChoiceAvailable(PhysicalCardImpl card) throws DecisionResultInvalidException { return HasCardChoiceAvailable(DS, card);}
	default boolean LSHasCardChoiceAvailable(PhysicalCardImpl card) throws DecisionResultInvalidException { return HasCardChoiceAvailable(LS, card);}

	default boolean DSHasCardChoicesAvailable(PhysicalCardImpl...cards) throws DecisionResultInvalidException {
		for(var card : cards) {
			if(!HasCardChoiceAvailable(DS, card))
				return false;
		}
		return true;
	}
	default boolean LSHasCardChoicesAvailable(PhysicalCardImpl...cards) throws DecisionResultInvalidException {
		for(var card : cards) {
			if(!HasCardChoiceAvailable(LS, card))
				return false;
		}
		return true;
	}

	default boolean DSHasCardChoiceNotAvailable(PhysicalCardImpl card) throws DecisionResultInvalidException { return !HasCardChoiceAvailable(DS, card);}
	default boolean LSHasCardChoiceNotAvailable(PhysicalCardImpl card) throws DecisionResultInvalidException { return !HasCardChoiceAvailable(LS, card);}

	default boolean DSHasCardChoicesNotAvailable(PhysicalCardImpl...cards) throws DecisionResultInvalidException {
		for(var card : cards) {
			if(HasCardChoiceAvailable(DS, card))
				return false;
		}
		return true;
	}
	default boolean LSHasCardChoicesNotAvailable(PhysicalCardImpl...cards) throws DecisionResultInvalidException {
		for(var card : cards) {
			if(HasCardChoiceAvailable(LS, card))
				return false;
		}
		return true;
	}

	default boolean HasCardChoiceAvailable(String player, PhysicalCardImpl card) throws DecisionResultInvalidException {
		String[] choices = GetAwaitingDecisionParam(player,"blueprintId");
		if(choices != null) {
			for (String choice : choices) {
				if (card.getBlueprintId(true).equals(choice))
					return true;
			}
			return false;
		}

		choices = GetAwaitingDecisionParam(player,"cardId");
		if(choices != null) {
			for (String choice : choices) {
				if (card.getCardId() == Integer.parseInt(choice))
					return true;
			}
			return false;
		}


		return false;
	}

	/**
	 * Causes the Dark Side player to issue a decision response composed of a comma-separated list of the provided
	 * card IDs.  This is used when e.g. the player must choose one or more targets for an effect.  This will only
	 * succeed if being used to target currently live cards; it will not work if being presented with a choice of
	 * out-of-play cards (such as when choosing from the reserve deck).
	 * @param cards The cards to include in the decision response.
	 * @throws DecisionResultInvalidException This error will be thrown if the current decision does not accept card IDs.
	 */
	default void DSChooseCardIDFromSelection(PhysicalCardImpl...cards) throws DecisionResultInvalidException { ChooseCardIDFromSelection(DS, cards);}
	/**
	 * Causes the Light Side player to issue a decision response composed of a comma-separated list of the provided
	 * card IDs.  This is used when e.g. the player must choose one or more targets for an effect.  This will only
	 * succeed if being used to target currently live cards; it will not work if being presented with a choice of
	 * out-of-play cards (such as when choosing from the reserve deck).
	 * @param cards The cards to include in the decision response.
	 * @throws DecisionResultInvalidException This error will be thrown if the current decision does not accept card IDs.
	 */
	default void LSChooseCardIDFromSelection(PhysicalCardImpl...cards) throws DecisionResultInvalidException { ChooseCardIDFromSelection(LS, cards);}

	/**
	 * Causes the given player to issue a decision response composed of a comma-separated list of the provided card IDs.
	 * This will only succeed if being used to target currently live cards; it will not work if being presented with a
	 * choice of out-of-play cards (such as when choosing from the reserve deck).
	 * @param player The player to issue a decision for.
	 * @param cards The cards to include in the decision response.
	 * @throws DecisionResultInvalidException This error will be thrown if the current decision does not accept card IDs.
	 */
	default void ChooseCardIDFromSelection(String player, PhysicalCardImpl...cards) throws DecisionResultInvalidException {
		AwaitingDecision decision = userFeedback().getAwaitingDecision(player);
		//PlayerDecided(player, "" + card.getCardId());

		String[] choices = GetAwaitingDecisionParam(player,"cardId");
		ArrayList<String> ids = new ArrayList<>();
		ArrayList<PhysicalCardImpl> found = new ArrayList<>();

		for (String choice : choices) {
			for (PhysicalCardImpl card : cards) {
				if (found.contains(card))
					continue;

				if (("" + card.getCardId()).equals(choice)) {
					ids.add(choice);
					found.add(card);
					break;
				}
			}
		}

		PlayerDecided(player, String.join(",", ids));
	}
}
