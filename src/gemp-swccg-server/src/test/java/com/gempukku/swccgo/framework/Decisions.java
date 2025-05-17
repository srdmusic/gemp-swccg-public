package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;

/**
 * A set of functions within the test rig that pertain to decisions.  Decisions in Gemp are a catch-all term referring
 * to any point where the game simulation cannot continue until a player makes a choice.  Thus, awaiting passes,
 * choosing targets, selecting cards from a group, or initiating actions are all "decisions".
 *
 * These functions should give you everything you need to assert that a decision is or is not available, or give you
 * the tools to make a decision properly.  See Choices for selecting among multiple options, and see Actions for
 * top-level card actions.
 */
public interface Decisions extends TestBase  {

	/**
	 * @return Gets the Dark Side decision that Gemp is currently waiting on.  Will be null if DS is not currently
	 * pending any decision.
	 */
	default AwaitingDecision DSGetDecision() { return GetAwaitingDecision(DS); }
	/**
	 * @return Gets the Light Side decision that Gemp is currently waiting on.  Will be null if LS is not currently
	 * pending any decision.
	 */
	default AwaitingDecision LSGetDecision() { return GetAwaitingDecision(LS); }

	/**
	 * Gets a decision for a given player that Gemp is currently waiting on.  Will be null if that player is not
	 * currently pending any decision.
	 * @param playerID The player's decision to retrieve
	 * @return
	 */
	default AwaitingDecision GetAwaitingDecision(String playerID) { return userFeedback().getAwaitingDecision(playerID); }

	/**
	 * @return Gets the currently pending decision.  Defaults to returning the Dark Side decision if both players are
	 * currently pending (which only rarely happens in situations such as the starting popup or dismissing cards
	 * revealed to both players).
	 */
	default AwaitingDecision GetCurrentDecision() {
		var DS = DSGetDecision();
		if(DS != null)
			return DS;
		return LSGetDecision();
	}

	/**
	 * Determines if the Dark Side player is currently presented with a decision which contains the given text.
	 * @param text The text snippet to search for.
	 * @return False if Dark Side has no current decisions or if the current decision does not contain the given text.
	 */
	default Boolean DSDecisionAvailable(String text) { return DecisionAvailable(DS, text); }
	/**
	 * Determines if the Light Side player is currently presented with a decision which contains the given text.
	 * @param text The text snippet to search for.
	 * @return False if Light Side has no current decisions or if the current decision does not contain the given text.
	 */
	default Boolean LSDecisionAvailable(String text) { return DecisionAvailable(LS, text); }

	/**
	 * Determines if the given player is currently presented with a decision which contains the given text.
	 * @param text The text snippet to search for.
	 * @return False if the given player has no current decisions or if the current decision does not contain the given text.
	 */
	default Boolean DecisionAvailable(String playerID, String text)
	{
		AwaitingDecision ad = GetAwaitingDecision(playerID);
		if(ad == null)
			return false;
		String lowerText = text.toLowerCase();
		return ad.getText().toLowerCase().contains(lowerText);
	}

	/**
	 * @return Returns true if Dark Side is currently presented with any decision at all.
	 */
	default Boolean DSAnyDecisionsAvailable() { return AnyDecisionsAvailable(DS); }
	/**
	 * @return Returns true if Light Side is currently presented with any decision at all.
	 */
	default Boolean LSAnyDecisionsAvailable() { return AnyDecisionsAvailable(LS); }

	/**
	 * Returns whether the given player is currently presented with any decision at all.
	 * @param player The player to check for pending decisions
	 * @return True if the given player has a pending decision, else false.
	 */
	default Boolean AnyDecisionsAvailable(String player) {
		var ad = GetAwaitingDecision(player);
		return ad != null;
	}

	/**
	 * Causes the Dark Side player to pass the current decision.
	 * @throws DecisionResultInvalidException This operation will fail if the current decision is not passable.
	 */
	// If this seems out of place organization-wise, it's because of the chain of inheritance between the various test interfaces.
	default void DSPass() throws DecisionResultInvalidException {
		if(DSAnyDecisionsAvailable()) {
			PlayerDecided(DS, "");
		}
	}
	/**
	 * Causes the Light Side player to pass the current decision.
	 * @throws DecisionResultInvalidException This operation will fail if the current decision is not passable.
	 */
	// If this seems out of place organization-wise, it's because of the chain of inheritance between the various test interfaces.
	default void LSPass() throws DecisionResultInvalidException {
		if(LSAnyDecisionsAvailable()) {
			PlayerDecided(LS, "");
		}
	}

	/**
	 * Causes Dark Side to decide to use the given answer.  Integers usually indicate an index between multiple choices,
	 * but they may be literal integers if the player is asked to choose a number.
	 * @param answer The integer answer to return to the server
	 * @throws DecisionResultInvalidException This operation will fail if the answer is incompatible with the current
	 * decision.
	 */
	default void DSDecided(int answer) throws DecisionResultInvalidException { PlayerDecided(DS, String.valueOf(answer));}

	/**
	 * Causes Dark Side to decide to use the given answer.  Answers may take different forms depending on the exact
	 * nature of the decision at hand.
	 * @param answer The answer to return to the server
	 * @throws DecisionResultInvalidException This operation will fail if the answer is incompatible with the current
	 * decision.
	 */
	default void DSDecided(String answer) throws DecisionResultInvalidException { PlayerDecided(DS, answer);}

	/**
	 * Causes Light Side to decide to use the given answer.  Integers usually indicate an index between multiple choices,
	 * but they may be literal integers if the player is asked to choose a number.
	 * @param answer The integer answer to return to the server
	 * @throws DecisionResultInvalidException This operation will fail if the answer is incompatible with the current
	 * decision.
	 */
	default void LSDecided(int answer) throws DecisionResultInvalidException { PlayerDecided(LS, String.valueOf(answer));}
	/**
	 * Causes Light Side to decide to use the given answer.  Answers may take different forms depending on the exact
	 * nature of the decision at hand.
	 * @param answer The answer to return to the server
	 * @throws DecisionResultInvalidException This operation will fail if the answer is incompatible with the current
	 * decision.
	 */
	default void LSDecided(String answer) throws DecisionResultInvalidException { PlayerDecided(LS, answer);}

	// As this is actually related to the heart of the table simulation, this is left to be implemented on the main Scenario class.
	void PlayerDecided(String player, String answer) throws DecisionResultInvalidException;

	//
//    public boolean DSHasOptionalTriggerAvailable() { return DSDecisionAvailable("Optional"); }
//    public boolean LSHasOptionalTriggerAvailable() { return LSDecisionAvailable("Optional"); }
//
//    public void DSAcceptOptionalTrigger() throws DecisionResultInvalidException { PlayerDecided(DS, "0"); }
//    public void DSDeclineOptionalTrigger() throws DecisionResultInvalidException { PlayerDecided(DS, ""); }
//    public void LSAcceptOptionalTrigger() throws DecisionResultInvalidException { PlayerDecided(LS, "0"); }
//    public void LSDeclineOptionalTrigger() throws DecisionResultInvalidException { PlayerDecided(LS, ""); }
//    public void DSDeclineChoosing() throws DecisionResultInvalidException { PlayerDecided(DS, ""); }
//    public void LSDeclineChoosing() throws DecisionResultInvalidException { PlayerDecided(LS, ""); }



}
