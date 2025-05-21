package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;

import static org.junit.Assert.assertTrue;

/**
 * These functions are for progressing through the game itself.  For instance, if your test is really about battle
 * attrition and the phases before that point are just in the way, here you will find functions to skip past phases
 * so that your test can remain mostly clean of irrelevant procedure.
 *
 * Do be warned that these functions assume a best-case scenario that can be safely passed through; if a card has a
 * required decision that does not have an obvious "decline" option, then these functions will fail and you will have
 * to do it manually.  If you actually do need such a pestering card on the table, it is advised that you only place
 * it at the last possible second rather than putting it down early and requiring you to do all the manual procedure.
 */
public interface GameProcedures extends Actions, GameProperties {

	/**
	 * Causes the Dark Side player to activate the maximum amount of available force, causes Light Side to let the same
	 * amount pass without a react, and then causes both players to pass Activate phase actions.
	 * @return The total amount of force that was activated
	 * @throws DecisionResultInvalidException
	 */
	default int DSActivateMaxForceAndPass() throws DecisionResultInvalidException {
		if(DSDecisionAvailable("Choose Activate action or Pass") && DSActionAvailable("Activate Force")) {
			DSChooseAction("Activate Force");
			int max = DSGetChoiceMax();
			DSDecided(max);
			LSDecided(max);
			BothPassInverted();
			return max;
		}

		return -1;
	}

	/**
	 * Causes the Light Side player to activate the maximum amount of available force, causes Dark Side to let the same
	 * amount pass without a react, and then causes both players to pass Activate phase actions.
	 * @return The total amount of force that was activated
	 * @throws DecisionResultInvalidException
	 */
	default int LSActivateMaxForceAndPass() throws DecisionResultInvalidException {
		if(LSDecisionAvailable("Choose Activate action or Pass") && LSActionAvailable("Activate Force")) {
			LSChooseAction("Activate Force");
			int max = LSGetChoiceMax();
			LSDecided(max);
			DSDecided(max);
			BothPassInverted();
			return max;
		}

		return -1;
	}


	/**
	 * Forces the Dark Side player to move the given amount of cards from their Reserve Deck to their Force Pile.
	 * This is done out of turn and does not require that it be the appropriate phase.
	 * @param amount
	 */
	default void DSActivateForceCheat(int amount) { ActivateForceCheat(DS, amount); }
	/**
	 * Forces the Light Side player to move the given amount of cards from their Reserve Deck to their Force Pile.
	 * This is done out of turn and does not require that it be the appropriate phase.
	 * @param amount
	 */
	default void LSActivateForceCheat(int amount) { ActivateForceCheat(LS, amount); }

	/**
	 * Forces the given player to move the given amount of cards from their Reserve Deck to their Force Pile.
	 * This is done out of turn and does not require that it be the appropriate phase.
	 * @param player
	 * @param amount
	 */
	default void ActivateForceCheat(String player, int amount) {
		for(int i = 0; i < amount; ++i) {
			gameState().playerActivatesForce(player, false, false);
		}
	}


	/**
	 * Forces the Dark Side player to move the given amount of cards from their Force Pile to their Used Pile.
	 * This is done out of turn and does not require that it be the appropriate phase.
	 * @param amount
	 */
	default void DSUseForceCheat(int amount) { UseForceCheat(DS, amount); }
	/**
	 * Forces the Light Side player to move the given amount of cards from their Force Pile to their Used Pile.
	 * This is done out of turn and does not require that it be the appropriate phase.
	 * @param amount
	 */
	default void LSUseForceCheat(int amount) { UseForceCheat(LS, amount); }

	/**
	 * Forces the given player to move the given amount of cards from their Force Pile to their Used Pile.
	 * This is done out of turn and does not require that it be the appropriate phase.
	 * @param player
	 * @param amount
	 */
	default void UseForceCheat(String player, int amount) {
		for(int i = 0; i < amount; ++i) {
			gameState().playerUsesForce(player, false, false);
		}
	}

	/**
	 * During the Deploy phase, causes Dark Side to deploy the given card to the given location and automatically pass
	 * any force use optional responses and deployment responses for both players.
	 * @param card The card to be deployed
	 * @param location Which location the card should be deployed to (should be in play already)
	 * @throws DecisionResultInvalidException
	 */
	default void DSDeployCardAndPassResponses(PhysicalCardImpl card, PhysicalCardImpl location) throws DecisionResultInvalidException {
		DSDeployCard(card);
		assertTrue(DSDecisionAvailable("Choose where to deploy"));
		DSChooseCard(location);

		BothPassInverted("Force - Optional responses");
		BothPassInverted("Optional response");
	}

	/**
	 * During the Deploy phase, causes Light Side to deploy the given card to the given location and automatically pass
	 * any force use optional responses and deployment responses for both players.
	 * @param card The card to be deployed
	 * @param location Which location the card should be deployed to (should be in play already)
	 * @throws DecisionResultInvalidException
	 */
	default void LSDeployCardAndPassResponses(PhysicalCardImpl card, PhysicalCardImpl location) throws DecisionResultInvalidException {
		LSDeployCard(card);
		assertTrue(LSDecisionAvailable("Choose where to deploy"));
		LSChooseCard(location);

		BothPassInverted("Force - Optional responses");
		BothPassInverted("Optional response");
	}

	/**
	 * Causes both players to pass during the Activate phase.
	 * @throws DecisionResultInvalidException
	 */
	default void PassActivateActions() throws DecisionResultInvalidException { BothPass(); }
	/**
	 * Causes both players to pass during the Control phase.
	 * @throws DecisionResultInvalidException
	 */
	default void PassControlActions() throws DecisionResultInvalidException { BothPass(); }
	/**
	 * Causes both players to pass during the Deploy phase.
	 * @throws DecisionResultInvalidException
	 */
	default void PassDeployActions() throws DecisionResultInvalidException { BothPass(); }
	/**
	 * Causes both players to pass during the Move phase.
	 * @throws DecisionResultInvalidException
	 */
	default void PassMoveActions() throws DecisionResultInvalidException { BothPass(); }
	/**
	 * Causes both players to pass during the Battle phase.
	 * @throws DecisionResultInvalidException
	 */
	default void PassBattleActions() throws DecisionResultInvalidException { BothPass(); }
	/**
	 * Causes both players to pass during the Draw phase.
	 * @throws DecisionResultInvalidException
	 */
	default void PassDrawActions() throws DecisionResultInvalidException { BothPass(); }

	/**
	 * Causes both players to pass, first by making the current player pass and then their opponent. Both will check
	 * to ensure that they have a currently available decision to be passing first.
	 * @throws DecisionResultInvalidException
	 */
	default void BothPass() throws DecisionResultInvalidException {
		var currentPlayer = GetCurrentPlayer();
		var offPlayer = GetOffPlayer();
		if(AnyDecisionsAvailable(currentPlayer)) {
			PlayerDecided(currentPlayer, "");
		}

		if(AnyDecisionsAvailable(offPlayer)) {
			PlayerDecided(offPlayer, "");
		}
	}

	/**
	 * Causes both players to pass any decisions that contain the provided text.
	 * @param text
	 * @throws DecisionResultInvalidException
	 */
	default void BothPass(String text) throws DecisionResultInvalidException {
		var currentPlayer = GetCurrentPlayer();
		var offPlayer = GetOffPlayer();
		if(DecisionAvailable(currentPlayer, text)) {
			PlayerDecided(currentPlayer, "");
		}

		if(DecisionAvailable(offPlayer, text)) {
			PlayerDecided(offPlayer, "");
		}
	}

	/**
	 * Causes both players to pass, but makes the opponent pass before the current player. Both will check
	 * to ensure that they have a currently available decision to be passing first.
	 * @throws DecisionResultInvalidException
	 */
	default void BothPassInverted() throws DecisionResultInvalidException {
		var currentPlayer = GetCurrentPlayer();
		var offPlayer = GetOffPlayer();

		if(AnyDecisionsAvailable(offPlayer)) {
			PlayerDecided(offPlayer, "");
		}

		if(AnyDecisionsAvailable(currentPlayer)) {
			PlayerDecided(currentPlayer, "");
		}
	}

	default void PassCardPlayResponses() throws DecisionResultInvalidException { BothPassInverted(); }
	default void PassForceUseResponses() throws DecisionResultInvalidException { BothPassInverted(); }

	default void DSPassForceUseResponse() throws DecisionResultInvalidException { DSPass(); }
	default void LSPassForceUseResponse() throws DecisionResultInvalidException { LSPass();}

	/**
	 * Causes both players to pass any decisions that contain the provided text.  First the off-player will pass, and
	 * then the current player.
	 * @param text
	 * @throws DecisionResultInvalidException
	 */
	default void BothPassInverted(String text) throws DecisionResultInvalidException {
		var currentPlayer = GetCurrentPlayer();
		var offPlayer = GetOffPlayer();

		if(DecisionAvailable(offPlayer, text)) {
			PlayerDecided(offPlayer, "");
		}

		if(DecisionAvailable(currentPlayer, text)) {
			PlayerDecided(currentPlayer, "");
		}
	}


	/**
	 * Skips to the Battle phase.
	 * @throws DecisionResultInvalidException
	 */
    default void SkipToBattle() throws DecisionResultInvalidException { SkipToPhase(Phase.BATTLE); }

	/**
	 * Causes players to spam pass until the provided target phase is current.  This process attempts to choose the
	 * first option of any required triggers, but may be brittle if there are any reacts that interrupt the pass-fest.
	 * Only 20 rounds of passing will be attempted to avoid infinite loops.
	 * @param target The phase the tester actually wants to be in
	 * @throws DecisionResultInvalidException
	 */
    default void SkipToPhase(Phase target) throws DecisionResultInvalidException {
        for(int attempts = 1; attempts <= 20; attempts++)
        {
            Phase current = gameState().getCurrentPhase();
            if(current == target)
                break;

            if(current == Phase.ACTIVATE) {
				if(gameState().getCurrentPlayerId().equals(LS)) {
					LSActivateMaxForceAndPass();
				}
				else {
					DSActivateMaxForceAndPass();
				}
            }
            else {
                var dsDecision = DSGetDecision();
                var lsDecision = LSGetDecision();
                if(dsDecision != null && dsDecision.getText().toLowerCase().contains("required")) {
                    DSChooseAction("0");
                }
                else if(lsDecision != null && lsDecision.getText().toLowerCase().contains("required")){
                    LSChooseAction("0");
                }
                else {
                    BothPass();
                }
            }

            if(attempts == 20)
            {
                throw new DecisionResultInvalidException("Could not arrive at target '" + target + "' after 20 attempts!");
            }
        }
    }

	/**
	 * Regardless of the current player, skips to the Activate phase of the next player's turn.
	 * @throws DecisionResultInvalidException
	 */
	default void SkipToNextTurn() throws DecisionResultInvalidException {
		SkipToNextTurn(game().getOpponent(gameState().getCurrentPlayerId()));
	}

	/**
	 * Skips to the Light Side player's next turn.  If Light Side is the current player, this will skip over an entire
	 * Dark Side turn.
	 * @throws DecisionResultInvalidException
	 */
	default void SkipToLSTurn() throws DecisionResultInvalidException { SkipToNextTurn(LS);	}
	/**
	 * Skips to the Dark Side player's next turn.  If Dark Side is the current player, this will skip over an entire
	 * Light Side turn.
	 * @throws DecisionResultInvalidException
	 */
	default void SkipToDSTurn() throws DecisionResultInvalidException { SkipToNextTurn(DS);	}

	/**
	 * Skips to a given player's next turn.  If they are the current player, this will skip over their opponent's turn.
	 * @param player The player whose turn it should be once we stop.
	 * @throws DecisionResultInvalidException
	 */
	default void SkipToNextTurn(String player) throws DecisionResultInvalidException {
		SkipToTurn(player, gameState().getPlayersLatestTurnNumber(player) + 1);
	}

	/**
	 * Skips forward in time by causing both players to pass until it is the given player's turn.  All the same
	 * caveats that affect SkipToPhase apply here.  This will attempt to move at most 20 turns in the future to avoid
	 * infinite loops.
	 * @param player Who should be the current player once we stop.
	 * @param targetTurn What number turn that player should be on.
	 * @throws DecisionResultInvalidException
	 */
	default void SkipToTurn(String player, int targetTurn) throws DecisionResultInvalidException {
		for(int attempts = 1; attempts <= 20; attempts++)
		{
			String currentPlayer = gameState().getCurrentPlayerId();
			int currentTurn = gameState().getPlayersLatestTurnNumber(currentPlayer);

			if(player.equals(currentPlayer) && currentTurn == targetTurn)
				break;

			SkipToPhase(Phase.DRAW);
			PassDrawActions();

			if(attempts == 20)
			{
				throw new DecisionResultInvalidException("Could not arrive at target turn '" + targetTurn + "' for '"
						+ player + "'after 20 attempts!");
			}
		}
	}





//
//    default void DSDismissRevealedCards() throws DecisionResultInvalidException { DSPassCurrentPhaseAction(); }
//    default void LSDismissRevealedCards() throws DecisionResultInvalidException { LSPassCurrentPhaseAction(); }
//    default void DismissRevealedCards() throws DecisionResultInvalidException {
//        DSDismissRevealedCards();
//        LSDismissRevealedCards();
//    }
//
//

}
