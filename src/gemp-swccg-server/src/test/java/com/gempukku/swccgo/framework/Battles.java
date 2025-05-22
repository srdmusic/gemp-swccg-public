package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;

public interface Battles extends GameProcedures {

	default void SkipToAttritionOrBattleDamage() throws DecisionResultInvalidException { SkipToAttritionOrBattleDamage(false); }
	default void SkipToAttritionOrBattleDamage(boolean drawDestiny) throws DecisionResultInvalidException {
		var currentPlayer = GetCurrentPlayer();
		var offPlayer = GetOffPlayer();

		PassBattleStartResponses();
		PassWeaponsSegmentActions();
		// before battle destinies
		BothPassResponses();
		// current player destiny
		if(DecisionAvailable(currentPlayer, "battle destiny?")) {
			if(drawDestiny) {
				PlayerChooseYes(currentPlayer);
			}
			else {
				PlayerChooseNo(currentPlayer);
			}
			// cost to draw destiny card
			BothPassResponses();
			// about to draw destiny card
			BothPassResponses();
			// destiny drawn
			BothPassResponses();
			// complete destiny draw
			BothPassResponses();
			// drawing destiny complete
			BothPassResponses();
		}
		BothPassResponses();

		// opponent destiny

		if(DecisionAvailable(offPlayer, "battle destiny?")) {
			if(drawDestiny) {
				PlayerChooseYes(offPlayer);
			}
			else {
				PlayerChooseNo(offPlayer);
			}
			// cost to draw destiny card
			BothPass();
			// about to draw destiny card
			BothPass();
			// destiny drawn
			BothPass();
			// complete destiny draw
			BothPass();
			// drawing destiny complete
			BothPass();
		}
		BothPass();


		// after battle destinies for both players
		BothPassResponses();
		// initial attrition calculated
		BothPassResponses();
	}



	default void PassBattleStartResponses() throws DecisionResultInvalidException { BothPassResponses(); }
	default void PassWeaponsSegmentActions() throws DecisionResultInvalidException { BothPass(); }
	default void PassPowerSegmentActions() throws DecisionResultInvalidException { BothPass(); }
	default void PassDamageSegmentActions() throws DecisionResultInvalidException { BothPass(); }


	default boolean AwaitingDSAttritionPayment() { return DecisionAvailable(DS, "Choose Force to lose or a card from battle to forfeit"); }
	default boolean AwaitingLSAttritionPayment() { return DecisionAvailable(LS, "Choose Force to lose or a card from battle to forfeit"); }
	default int GetUnpaidDSAttrition() { return (int) gameState().getBattleState().getAttritionRemaining(game(), DS); }
	default int GetUnpaidLSAttrition() { return (int) gameState().getBattleState().getAttritionRemaining(game(), LS); }


	default int GetUnpaidDSBattleDamage() { return (int) gameState().getBattleState().getBattleDamageRemaining(game(), DS); }
	default int GetUnpaidLSBattleDamage() { return (int) gameState().getBattleState().getBattleDamageRemaining(game(), LS); }

	default int GetDSTotalDestiny() { return (int) gameState().getBattleState().getTotalBattleDestiny(game(), DS); }
	default int GetLSTotalDestiny() { return (int) gameState().getBattleState().getTotalBattleDestiny(game(), LS); }

	default int GetDSTotalPower() { return (int) gameState().getBattleState().getTotalPower(game(), DS); }
	default int GetLSTotalPower() { return (int) gameState().getBattleState().getTotalPower(game(), LS); }

	default boolean DSWonBattle() { return gameState().getBattleState().isWinner(DS); }
	default boolean LSWonBattle() { return gameState().getBattleState().isWinner(LS); }

	default boolean IsBattleStarted() { return gameState().getBattleState().isBattleStarted(); }
	default boolean IsBattleCanceled() { return gameState().getBattleState().isCanceled(); }
	default boolean IsReachedPowerSegment() { return gameState().getBattleState().isReachedPowerSegment(); }
	default boolean IsAttritionCalculated() { return gameState().getBattleState().isBaseAttritionCalculated(); }
	default boolean IsReachedDamageSegment() { return gameState().getBattleState().isReachedDamageSegment(); }


}
