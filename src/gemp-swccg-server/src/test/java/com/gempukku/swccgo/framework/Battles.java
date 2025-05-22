package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;

public interface Battles extends GameProcedures, PileProperties {

	default void SkipToAttritionOrBattleDamage() throws DecisionResultInvalidException { SkipToAttritionOrBattleDamage(false); }
	default void SkipToAttritionOrBattleDamage(boolean drawDestiny) throws DecisionResultInvalidException {
		var currentPlayer = GetCurrentPlayer();
		var offPlayer = GetOpponent();

		PassBattleStartResponses();
		PassWeaponsSegmentActions();
		BothPassResponses("BEFORE_BATTLE_DESTINY_DRAWS");
		// current player destiny
		if(DecisionAvailable(currentPlayer, "battle destiny?")) {
			if(drawDestiny) {
				PlayerChooseYes(currentPlayer);
			}
			else {
				PlayerChooseNo(currentPlayer);
			}
			PassDestinyDrawResponses();
		}
		BothPassResponses("BATTLE_DESTINY_DRAWS_COMPLETE_FOR_PLAYER");

		// opponent destiny
		if(DecisionAvailable(offPlayer, "battle destiny?")) {
			if(drawDestiny) {
				PlayerChooseYes(offPlayer);
			}
			else {
				PlayerChooseNo(offPlayer);
			}
			PassDestinyDrawResponses();
		}
		BothPassResponses("BATTLE_DESTINY_DRAWS_COMPLETE_FOR_PLAYER");
		BothPassResponses("BATTLE_DESTINY_DRAWS_COMPLETE_FOR_BOTH_PLAYERS");
		BothPassResponses("INITIAL_ATTRITION_CALCULATED");
	}

	default void PassWeaponFireWithDestinyDraw() throws DecisionResultInvalidException {
		// weapon firing
		BothPassResponses("Fire ");
		PassDestinyDrawResponses();
		BothPassResponses("ABOUT_TO_BE_HIT");
		BothPassResponses("HIT -");
		BothPassResponses("FIRED_WEAPON");
	}

	default void PassDestinyDrawResponses() throws DecisionResultInvalidException {
		BothPassResponses("COST_TO_DRAW_DESTINY_CARD");
		BothPassResponses("ABOUT_TO_DRAW_DESTINY_CARD");
		BothPassResponses("DESTINY_DRAWN");
		BothPassResponses("COMPLETE_DESTINY_DRAW");
		BothPassResponses("DRAWING_DESTINY_COMPLETE");
	}

	default void PassBattleStartResponses() throws DecisionResultInvalidException { BothPassResponses("BATTLE_INITIATED"); }
	default void PassWeaponsSegmentActions() throws DecisionResultInvalidException { BothPassResponses("Choose weapons segment action"); }
	default void PassPowerSegmentActions() throws DecisionResultInvalidException { BothPassResponses(); }
	default void PassDamageSegmentActions() throws DecisionResultInvalidException { BothPassResponses(); }


	default boolean AwaitingDSAttritionPayment() {
		return (DecisionAvailable(DS, "Choose Force to lose or a card from battle to forfeit")
					|| DecisionAvailable(DS, "Choose a card from battle to forfeit"))
				&& IsReachedDamageSegment() && GetUnpaidDSAttrition() > 0;
	}
	default boolean AwaitingLSAttritionPayment() {
		return (DecisionAvailable(LS, "Choose Force to lose or a card from battle to forfeit")
				|| DecisionAvailable(LS, "Choose a card from battle to forfeit"))
				&& IsReachedDamageSegment() && GetUnpaidLSAttrition() > 0;
	}
	default int GetUnpaidDSAttrition() { return (int) gameState().getBattleState().getAttritionRemaining(game(), DS); }
	default int GetUnpaidLSAttrition() { return (int) gameState().getBattleState().getAttritionRemaining(game(), LS); }


	/**
	 * Pays for 1 or more Force worth of Dark Side attrition by sacrificing the provided card in play.
	 * @param card The DS card in play to sacrifice for attrition.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying attrition.
	 */
	default void PayDSAttritionFromCardInPlay(PhysicalCardImpl card) throws DecisionResultInvalidException {
		DSChooseCard(card);
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}

	/**
	 * Pays for 1 or more Force worth of Light Side attrition by sacrificing the provided card in play.
	 * @param card The LS card in play to sacrifice for attrition.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying attrition.
	 */
	default void PayLSAttritionFromCardInPlay(PhysicalCardImpl card) throws DecisionResultInvalidException {
		LSChooseCard(card);
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}


	default boolean AwaitingDSBattleDamagePayment() {
		return DecisionAvailable(DS, "Choose Force to lose or a card from battle to forfeit")
				&& IsReachedDamageSegment() && GetUnpaidDSBattleDamage() > 0;
	}
	default boolean AwaitingLSBattleDamagePayment() {
		return DecisionAvailable(LS, "Choose Force to lose or a card from battle to forfeit")
				&& IsReachedDamageSegment() && GetUnpaidLSBattleDamage() > 0;
	}
	default int GetUnpaidDSBattleDamage() { return (int) gameState().getBattleState().getBattleDamageRemaining(game(), DS); }
	default int GetUnpaidLSBattleDamage() { return (int) gameState().getBattleState().getBattleDamageRemaining(game(), LS); }

	/**
	 * Pays for 1 Force worth of Dark Side battle damage using the card on the top of the DS Reserve deck.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void PayDSBattleDamageFromReserveDeck() throws DecisionResultInvalidException {
		DSChooseCard(GetTopOfDSReserveDeck());
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}
	/**
	 * Pays for 1 Force worth of Dark Side battle damage using the card on the top of the DS Force Pile.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void PayDSBattleDamageFromForcePile() throws DecisionResultInvalidException {
		DSChooseCard(GetTopOfDSForcePile());
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}
	/**
	 * Pays for 1 Force worth of Dark Side battle damage using the card on the top of the DS Used Pile.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void PayDSBattleDamageFromUsedPile() throws DecisionResultInvalidException {
		DSChooseCard(GetTopOfDSUsedPile());
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}
	/**
	 * Pays for 1 or more Force worth of Dark Side battle damage by sacrificing the provided card in play.
	 * @param card The DS card in play to sacrifice for battle damage.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void PayDSBattleDamageFromCardInPlay(PhysicalCardImpl card) throws DecisionResultInvalidException {
		DSChooseCard(card);
		BothPassResponses("FORFEITED_TO_LOST_PILE_FROM_TABLE");
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}

	/**
	 * Pays for 1 Force worth of Light Side battle damage using the card on the top of the LS Reserve deck.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void PayLSBattleDamageFromReserveDeck() throws DecisionResultInvalidException {
		LSChooseCard(GetTopOfLSReserveDeck());
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}
	/**
	 * Pays for 1 Force worth of Light Side battle damage using the card on the top of the LS Force Pile.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void PayLSBattleDamageFromForcePile() throws DecisionResultInvalidException {
		LSChooseCard(GetTopOfLSForcePile());
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}
	/**
	 * Pays for 1 Force worth of Light Side battle damage using the card on the top of the LS Force Pile.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void PayLSBattleDamageFromUsedPile() throws DecisionResultInvalidException {
		LSChooseCard(GetTopOfLSUsedPile());
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}
	/**
	 * Pays for 1 or more Force worth of Light Side battle damage by sacrificing the provided card in play.
	 * @param card The LS card in play to sacrifice for battle damage.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void PayLSBattleDamageFromCardInPlay(PhysicalCardImpl card) throws DecisionResultInvalidException {
		LSChooseCard(card);
		BothPassResponses("FORFEITED_TO_LOST_PILE_FROM_TABLE");
		BothPassResponses("PUT_IN_CARD_PILE_FROM_OFF_TABLE");
	}




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
