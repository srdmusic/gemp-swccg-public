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

	default AwaitingDecision DSGetDecision() { return GetAwaitingDecision(DS); }
	default AwaitingDecision LSGetDecision() { return GetAwaitingDecision(LS); }

	default AwaitingDecision GetAwaitingDecision(String playerID) { return userFeedback().getAwaitingDecision(playerID); }
	default AwaitingDecision GetCurrentDecision() {
		var DS = DSGetDecision();
		if(DS != null)
			return DS;
		return LSGetDecision();
	}

	default Boolean DSDecisionAvailable(String text) { return DecisionAvailable(DS, text); }
	default Boolean LSDecisionAvailable(String text) { return DecisionAvailable(LS, text); }

	default Boolean DecisionAvailable(String playerID, String text)
	{
		AwaitingDecision ad = GetAwaitingDecision(playerID);
		if(ad == null)
			return false;
		String lowerText = text.toLowerCase();
		return ad.getText().toLowerCase().contains(lowerText);
	}

	default Boolean DSAnyDecisionsAvailable() { return AnyDecisionsAvailable(DS); }
	default Boolean LSAnyDecisionsAvailable() { return AnyDecisionsAvailable(LS); }
	default Boolean AnyDecisionsAvailable(String player) {
		var ad = GetAwaitingDecision(player);
		return ad != null;
	}

	default void DSPass() throws DecisionResultInvalidException {
		if(DSAnyDecisionsAvailable()) {
			PlayerDecided(DS, "");
		}
	}

	default void LSPass() throws DecisionResultInvalidException {
		if(LSAnyDecisionsAvailable()) {
			PlayerDecided(LS, "");
		}
	}

	default void DSDecided(int answer) throws DecisionResultInvalidException { PlayerDecided(DS, String.valueOf(answer));}
	default void DSDecided(String answer) throws DecisionResultInvalidException { PlayerDecided(DS, answer);}

	default void LSDecided(int answer) throws DecisionResultInvalidException { PlayerDecided(LS, String.valueOf(answer));}
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
