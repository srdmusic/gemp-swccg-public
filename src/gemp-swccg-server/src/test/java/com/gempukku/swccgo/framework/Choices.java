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
	default List<String> DSGetCardChoices() { return GetADParamAsList(DS, "cardId"); }
	default List<String> LSGetCardChoices() { return GetADParamAsList(LS, "cardId"); }

	default Boolean DSChoiceAvailable(String choice) { return ChoiceAvailable(DS, choice); }
	default Boolean LSChoiceAvailable(String choice) { return ChoiceAvailable(LS, choice); }
	default Boolean ChoiceAvailable(String player, String choice) {
		List<String> actions = GetADParamAsList(player, "results");
		if(actions == null)
			return false;
		String lowerChoice = choice.toLowerCase();
		return actions.stream().anyMatch(x -> x.toLowerCase().contains(lowerChoice));
	}

	default void DSChooseYes() throws DecisionResultInvalidException { ChooseMultipleChoiceOption(DS, "Yes"); }
	default void LSChooseYes() throws DecisionResultInvalidException { ChooseMultipleChoiceOption(LS, "Yes"); }
	default void DSChooseNo() throws DecisionResultInvalidException { ChooseMultipleChoiceOption(DS, "No"); }
	default void LSChooseNo() throws DecisionResultInvalidException { ChooseMultipleChoiceOption(LS, "No"); }
	default void DSChooseMultipleChoiceOption(String option) throws DecisionResultInvalidException { ChooseMultipleChoiceOption(DS, option); }
	default void LSChooseMultipleChoiceOption(String option) throws DecisionResultInvalidException { ChooseMultipleChoiceOption(LS, option); }
	default void ChooseMultipleChoiceOption(String playerID, String option) throws DecisionResultInvalidException { ChooseAction(playerID, "results", option); }
	default void DSChooseAction(String paramName, String option) throws DecisionResultInvalidException { ChooseAction(DS, paramName, option); }
	default void DSChooseAction(String option) throws DecisionResultInvalidException { ChooseAction(DS, "actionText", option); }
	default void LSChooseAction(String paramName, String option) throws DecisionResultInvalidException { ChooseAction(LS, paramName, option); }
	default void LSChooseAction(String option) throws DecisionResultInvalidException { ChooseAction(LS, "actionText", option); }
	default void ChooseAction(String playerID, String paramName, String option) throws DecisionResultInvalidException {
		List<String> choices = GetADParamAsList(playerID, paramName);
		for(String choice : choices){
			if(option == null && choice == null // This only happens when a rule is the source of an action
					|| choice.toLowerCase().contains(option.toLowerCase())) {
				PlayerDecided(playerID, String.valueOf(choices.indexOf(choice)));
				return;
			}
		}
		//couldn't find an exact match, so maybe it's a direct index:
		PlayerDecided(playerID, option);
	}

	default void DSChoose(String choice) throws DecisionResultInvalidException {
		if(DSGetChoiceCount() > 0) {
			DSChooseMultipleChoiceOption(choice);
		}
		else {
			PlayerDecided(DS, choice);
		}
	}
	default void DSChoose(String...choices) throws DecisionResultInvalidException { PlayerDecided(DS, String.join(",", choices)); }
	default void LSChoose(String choice) throws DecisionResultInvalidException {
		if(LSGetChoiceCount() > 0) {
			LSChooseMultipleChoiceOption(choice);
		}
		else {
			PlayerDecided(LS, choice);
		}
	}
	default void LSChoose(String...choices) throws DecisionResultInvalidException { PlayerDecided(LS, String.join(",", choices)); }


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

	default int DSGetChoiceMin() { return Integer.parseInt(DSGetFirstADParam("min")); }
	default int DSGetChoiceMax() { return Integer.parseInt(DSGetFirstADParam("max")); }
	default int LSGetChoiceMin() { return Integer.parseInt(LSGetFirstADParam("min")); }
	default int LSGetChoiceMax() { return Integer.parseInt(LSGetFirstADParam("max")); }

	default int DSGetSelectableCount() {
		return GetADParamEqualsCount(DS, "selectable", "true");
	}

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
	default int DSGetChoiceCount() { return GetChoiceCount(DSGetMultipleChoices()); }
	default int LSGetChoiceCount() { return GetChoiceCount(LSGetMultipleChoices()); }

	default int GetChoiceCount(List<String> list) {
		if(list == null)
			return 0;
		return list.size();
	}

	default List<String> DSGetADParamAsList(String paramName) { return GetADParamAsList(DS, paramName); }
	default List<String> LSGetADParamAsList(String paramName) { return GetADParamAsList(LS, paramName); }
	default List<String> GetADParamAsList(String playerID, String paramName) {
		var paramList = GetAwaitingDecisionParam(playerID, paramName);
		if(paramList == null)
			return null;

		return Arrays.asList(paramList);
	}

	default int GetADParamEqualsCount(String playerID, String paramName, String value) {
		return (int) Arrays.stream(GetAwaitingDecisionParam(playerID, paramName)).filter(s -> s.equals(value)).count();
	}
	default String[] DSGetADParam(String paramName) { return GetAwaitingDecisionParam(DS, paramName); }
	default String[] LSGetADParam(String paramName) { return GetAwaitingDecisionParam(LS, paramName); }
	default String DSGetFirstADParam(String paramName) { return GetAwaitingDecisionParam(DS, paramName)[0]; }
	default String LSGetFirstADParam(String paramName) { return GetAwaitingDecisionParam(LS, paramName)[0]; }
	default String[] GetAwaitingDecisionParam(String playerID, String paramName) {
		var decision = userFeedback().getAwaitingDecision(playerID);
		return decision.getDecisionParameters().get(paramName);
	}

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


//    default boolean DSCanChooseCharacter(PhysicalCardImpl card) { return DSGetCardChoices().contains(String.valueOf(card.getCardId())); }
//    default boolean LSCanChooseCharacter(PhysicalCardImpl card) { return LSGetCardChoices().contains(String.valueOf(card.getCardId())); }

	default int DSGetCardChoiceCount() { return DSGetCardChoices().size(); }
	default int LSGetCardChoiceCount() { return LSGetCardChoices().size(); }


	default void DSChooseCardBPFromSelection(String name) throws DecisionResultInvalidException { ChooseCardBPFromSelection(DS, GetDSCard(name));}
	default void LSChooseCardBPFromSelection(String name) throws DecisionResultInvalidException { ChooseCardBPFromSelection(LS, GetLSCard(name));}

	default void DSChooseCardBPFromSelection(PhysicalCardImpl...cards) throws DecisionResultInvalidException { ChooseCardBPFromSelection(DS, cards);}
	default void LSChooseCardBPFromSelection(PhysicalCardImpl...cards) throws DecisionResultInvalidException { ChooseCardBPFromSelection(LS, cards);}

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

	default void DSChooseCardIDFromSelection(PhysicalCardImpl...cards) throws DecisionResultInvalidException { ChooseCardIDFromSelection(DS, cards);}
	default void LSChooseCardIDFromSelection(PhysicalCardImpl...cards) throws DecisionResultInvalidException { ChooseCardIDFromSelection(LS, cards);}

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
