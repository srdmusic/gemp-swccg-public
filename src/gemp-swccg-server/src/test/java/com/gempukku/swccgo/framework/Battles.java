package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;

public interface Battles extends Decisions, GameProcedures, PileProperties {

	default boolean DSCanInitiateBattle() { return DSActionAvailable("Initiate battle"); }
	default boolean LSCanInitiateBattle() { return DSActionAvailable("Initiate battle"); }

	default void DSInitiateBattle(PhysicalCardImpl location) throws DecisionResultInvalidException { InitiateBattle(DS, location); }
	default void LSInitiateBattle(PhysicalCardImpl location) throws DecisionResultInvalidException { InitiateBattle(LS, location); }

	default void InitiateBattle(String player, PhysicalCardImpl location) throws DecisionResultInvalidException {
		ChooseAction(player, "Initiate battle");
		PassForceUseResponses();
		PassBattleStartResponses();
		//TODO: Add handling for choosing the location, I am certain that this should be needed but it clearly auto-chose here
		//ChooseCards(player, location);
	}


	default void SkipToPowerSegment() throws DecisionResultInvalidException {
		PassBattleStartResponses();
		PassWeaponsSegmentActions();
		PassResponses("BEFORE_BATTLE_DESTINY_DRAWS");
	}

	default void SkipBattleDestinyDraws(boolean drawDestiny) throws DecisionResultInvalidException {
		var currentPlayer = GetCurrentPlayer();
		var offPlayer = GetOpponent();

		PassResponses("BEFORE_BATTLE_DESTINY_DRAWS");
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
		PassResponses("BATTLE_DESTINY_DRAWS_COMPLETE_FOR_PLAYER");

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
		PassResponses("BATTLE_DESTINY_DRAWS_COMPLETE_FOR_PLAYER");
		PassResponses("BATTLE_DESTINY_DRAWS_COMPLETE_FOR_BOTH_PLAYERS");
	}

	default void SkipToEndOfPowerSegment(boolean drawDestiny) throws DecisionResultInvalidException {
		SkipToPowerSegment();
		SkipBattleDestinyDraws(drawDestiny);
	}

	default void SkipToDamageSegment() throws DecisionResultInvalidException { SkipToDamageSegment(false); }
	default void SkipToDamageSegment(boolean drawDestiny) throws DecisionResultInvalidException {
		SkipToEndOfPowerSegment(drawDestiny);
		PassResponses("INITIAL_ATTRITION_CALCULATED");
	}

	default void PassWeaponFireWithDestinyDraw() throws DecisionResultInvalidException {
		// weapon firing
		PassResponses("Fire ");
		PassDestinyDrawResponses();
		PassResponses("ABOUT_TO_BE_HIT");
		PassResponses("HIT -");
		PassResponses("FIRED_WEAPON");
	}

	default void PassDestinyDrawResponses() throws DecisionResultInvalidException {
		PassResponses("COST_TO_DRAW_DESTINY_CARD");
		PassResponses("ABOUT_TO_DRAW_DESTINY_CARD");
		PassResponses("DESTINY_DRAWN");
		PassResponses("COMPLETE_DESTINY_DRAW");
		PassResponses("DRAWING_DESTINY_COMPLETE");
	}

	default void PassBattleStartResponses() throws DecisionResultInvalidException { PassResponses("BATTLE_INITIATED"); }

	default boolean AwaitingDSWeaponsSegmentActions() { return DSDecisionAvailable("Choose weapons segment action to play or Pass"); }
	default boolean AwaitingLSWeaponsSegmentActions() { return LSDecisionAvailable("Choose weapons segment action to play or Pass"); }

	default boolean AwaitingDSPowerSegmentActions() { return DSDecisionAvailable("Choose power segment action to play or Pass"); }
	default boolean AwaitingLSPowerSegmentActions() { return LSDecisionAvailable("Choose power segment action to play or Pass"); }

	default boolean AwaitingDSDamageSegmentActions() { return DSDecisionAvailable("Choose damage segment action to play or Pass"); }
	default boolean AwaitingLSDamageSegmentActions() { return LSDecisionAvailable("Choose damage segment action to play or Pass"); }

	default void PassWeaponsSegmentActions() throws DecisionResultInvalidException { PassResponses("Choose weapons segment action to play or Pass"); }
	default void PassPowerSegmentActions() throws DecisionResultInvalidException { PassResponses("Choose power segment action to play or Pass"); }
	default void PassDamageSegmentActions() throws DecisionResultInvalidException { PassResponses("Choose damage segment action to play or Pass"); }


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
	default void DSPayAttritionFromCardInPlay(PhysicalCardImpl card) throws DecisionResultInvalidException {
		DSChooseCard(card);
		PassCardLeavingTable();
	}

	/**
	 * Pays for 1 or more Force worth of Light Side attrition by sacrificing the provided card in play.
	 * @param card The LS card in play to sacrifice for attrition.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying attrition.
	 */
	default void LSPayAttritionFromCardInPlay(PhysicalCardImpl card) throws DecisionResultInvalidException {
		LSChooseCard(card);
		PassCardLeavingTable();
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
	 * Pays for the remaining Dark Side battle damage using cards on the top of the DS Reserve deck.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void DSPayRemainingBattleDamageFromReserveDeck() throws DecisionResultInvalidException {
		DSPayBattleDamageFromReserveDeck(GetUnpaidDSBattleDamage());
	}
	/**
	 * Pays for the given amount of Force worth of Dark Side battle damage using cards on the top of the DS Reserve deck.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void DSPayBattleDamageFromReserveDeck(int amount) throws DecisionResultInvalidException {
		for(int i = 0; i < amount; ++i) {
			DSPayBattleDamageFromReserveDeck();
		}
	}
	/**
	 * Pays for 1 Force worth of Dark Side battle damage using the card on the top of the DS Reserve deck.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void DSPayBattleDamageFromReserveDeck() throws DecisionResultInvalidException {
		DSChooseCard(GetTopOfDSReserveDeck());
		PassCardLeavingTable();
	}
	/**
	 * Pays for 1 Force worth of Dark Side battle damage using the card on the top of the DS Force Pile.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void DSPayBattleDamageFromForcePile() throws DecisionResultInvalidException {
		DSChooseCard(GetTopOfDSForcePile());
		PassCardLeavingTable();
	}
	/**
	 * Pays for 1 Force worth of Dark Side battle damage using the card on the top of the DS Used Pile.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void DSPayBattleDamageFromUsedPile() throws DecisionResultInvalidException {
		DSChooseCard(GetTopOfDSUsedPile());
		PassCardLeavingTable();
	}
	/**
	 * Pays for 1 or more Force worth of Dark Side battle damage by sacrificing the provided card in play.
	 * @param card The DS card in play to sacrifice for battle damage.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void DSPayBattleDamageFromCardInPlay(PhysicalCardImpl card) throws DecisionResultInvalidException {
		DSChooseCard(card);
		PassResponses("FORFEITED_TO_LOST_PILE_FROM_TABLE");
		PassCardLeavingTable();
	}

	/**
	 * Pays for the remaining Light Side battle damage using cards on the top of the DS Reserve deck.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void LSPayRemainingBattleDamageFromReserveDeck() throws DecisionResultInvalidException {
		LSPayBattleDamageFromReserveDeck(GetUnpaidLSBattleDamage());
	}
	/**
	 * Pays for the given amount of Force worth of Light Side battle damage using cards on the top of the DS Reserve deck.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void LSPayBattleDamageFromReserveDeck(int amount) throws DecisionResultInvalidException {
		for(int i = 0; i < amount; ++i) {
			DSPayBattleDamageFromReserveDeck();
		}
	}
	/**
	 * Pays for 1 Force worth of Light Side battle damage using the card on the top of the LS Reserve deck.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void LSPayBattleDamageFromReserveDeck() throws DecisionResultInvalidException {
		LSChooseCard(GetTopOfLSReserveDeck());
		PassCardLeavingTable();
	}
	/**
	 * Pays for 1 Force worth of Light Side battle damage using the card on the top of the LS Force Pile.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void LSPayBattleDamageFromForcePile() throws DecisionResultInvalidException {
		LSChooseCard(GetTopOfLSForcePile());
		PassCardLeavingTable();
	}
	/**
	 * Pays for 1 Force worth of Light Side battle damage using the card on the top of the LS Force Pile.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void LSPayBattleDamageFromUsedPile() throws DecisionResultInvalidException {
		LSChooseCard(GetTopOfLSUsedPile());
		PassCardLeavingTable();
	}
	/**
	 * Pays for 1 or more Force worth of Light Side battle damage by sacrificing the provided card in play.
	 * @param card The LS card in play to sacrifice for battle damage.
	 * @throws DecisionResultInvalidException Throws this error if the player is not currently paying battle damage.
	 */
	default void LSPayBattleDamageFromCardInPlay(PhysicalCardImpl card) throws DecisionResultInvalidException {
		LSChooseCard(card);
		PassResponses("FORFEITED_TO_LOST_PILE_FROM_TABLE");
		PassCardLeavingTable();
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
