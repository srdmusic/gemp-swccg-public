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

	default void PassActivateActions() throws DecisionResultInvalidException { BothPass(); }
	default void PassControlActions() throws DecisionResultInvalidException { BothPass(); }
	default void PassDeployActions() throws DecisionResultInvalidException { BothPass(); }
	default void PassMoveActions() throws DecisionResultInvalidException { BothPass(); }
	default void PassBattleActions() throws DecisionResultInvalidException { BothPass(); }
	default void PassDrawActions() throws DecisionResultInvalidException { BothPass(); }

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


    default void SkipToBattle() throws DecisionResultInvalidException { SkipToPhase(Phase.BATTLE); }

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

	default void SkipToNextTurn() throws DecisionResultInvalidException {
		SkipToNextTurn(game().getOpponent(gameState().getCurrentPlayerId()));
	}

	default void SkipToLSTurn() throws DecisionResultInvalidException { SkipToNextTurn(LS);	}
	default void SkipToDSTurn() throws DecisionResultInvalidException { SkipToNextTurn(DS);	}

	default void SkipToNextTurn(String player) throws DecisionResultInvalidException {
		SkipToTurn(player, gameState().getPlayersLatestTurnNumber(player) + 1);
	}

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


	default void DSDeployCardAndPassResponses(PhysicalCardImpl card, PhysicalCardImpl location) throws DecisionResultInvalidException {
		DSDecided(GetCardActionId(DS, card, "Deploy"));
		assertTrue(DSDecisionAvailable("Choose where to deploy"));
		DSChooseCard(location);

		BothPassInverted("Force - Optional responses");
		BothPassInverted("Optional response");
	}

	default void LSDeployCardAndPassResponses(PhysicalCardImpl card, PhysicalCardImpl location) throws DecisionResultInvalidException {
		LSDecided(GetCardActionId(LS, card, "Deploy"));
		assertTrue(LSDecisionAvailable("Choose where to deploy"));
		LSChooseCard(location);

		BothPassInverted("Force - Optional responses");
		BothPassInverted("Optional response");
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
