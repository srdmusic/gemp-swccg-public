package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.game.layout.LocationPlacement;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * While the ability to programmatically execute games is a boon to testing efforts, the real strength of this test rig
 * is in bald-faced cheating to arrange the table however we like without needing to abide by all the costs,
 * requirements, and game rules that slow things down.  Use these functions to stack the table just the way you want,
 * and then your tests will only be a handful of true steps and assertions.
 *
 * Note that these functions can be called at any time and they will be executed by Gemp, even if it is currently
 * awaiting a decision.  This can be used and abused by a clever tester, but remember that if you were hoping to
 * utilize the action on a card, that card must be in the proper zone at the time that Gemp checks for pending
 * decisions, meaning you may need to perform some other action (or a pass) to get Gemp to realize that you have moved
 * a card.
 */
public interface ZoneManipulation extends TestBase{

	/**
	 * Removes a physical card's current zone.  This is a prerequisite to a card actually properly moving to a new zone.
	 * This shouldn't be necessary to call directly within tests.
	 * @param card The card to make zoneless.
	 */
	default void RemoveCardZone(PhysicalCardImpl card) {
		if(card.getZone() != null)
		{
			gameState().removeCardsFromZone(new ArrayList<>() {{
				add(card);
			}});
		}
	}

	/**
	 * Manually moves a given card to a given player's zone.  This ignores any game costs, requirements, or rules and
	 * effectively teleports the card to whatever zone you like.  Do be careful where you stick weird things.
	 * @param player Which player's version of a zone to use (i.e. which side of a location, which deck, etc)
	 * @param card The card to teleport
	 * @param zone The zone to teleport into
	 */
	default void MoveCardToZone(String player, PhysicalCardImpl card, Zone zone) {
		RemoveCardZone(card);
		gameState().addCardToZone(card, zone, player);
	}

	/**
	 * Moves a card out of play, where it cannot affect anything.
	 * @param card The card to remove from the game.
	 */
	default void MoveOutOfPlay(PhysicalCardImpl card) { MoveCardToZone(card.getOwner(), card, Zone.OUT_OF_PLAY); }

	default void MoveLocationToTable(PhysicalCardImpl card) {
		RemoveCardZone(card);
		var placements = gameState().getLocationPlacement(game(), card, null, null);
		gameState().addLocationToTable(game(), card, placements.getFirst());
	}

	/**
	 * Moves one or more cards to a given location, on their owner's side of that location.  This is equivalent to
	 * deploying to a location, except that no costs, requirements, or other rules will be respected.
	 * This is unrelated to transporting a card during the Move phase.
	 * @param location The location to move to
	 * @param cards The cards to move
	 */
	default void MoveCardsToLocation(PhysicalCardImpl location, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			//If it's not in play, we'll temporarily stick it on the side of the table so that it goes through
			// all the correct "entering play" startup
			if(!card.getZone().isInPlay()) {
				MoveCardsToSideOfTable(card);
			}
			gameState().moveCardToLocation(card, location, true);
		});
	}
	/**
	 * Moves one or more cards to a given location, on their owner's opponent's side of that location.  This is
	 * equivalent to deploying to a location, except that no costs, requirements, or other rules will be respected.
	 * This is unrelated to transporting a card during the Move phase.
	 * @param location The location to move to
	 * @param cards The cards to move
	 */
	default void MoveCardsToOpponentLocation(PhysicalCardImpl location, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> gameState().moveCardToLocation(card, location, false));
	}

	/**
	 * Moves one or more cards to the Dark Side player's side of the table.  This is equivalent to playing to that
	 * side of the table, except that no costs, requirements, or other rules will be respected.
	 * @param cards The cards to move
	 */
	default void MoveCardsToDSSideOfTable(PhysicalCardImpl...cards) { MoveCardsToSideOfTable(DS, cards); }
	/**
	 * Moves one or more cards to the LIght Side player's side of the table.  This is equivalent to playing to that
	 * side of the table, except that no costs, requirements, or other rules will be respected.
	 * @param cards The cards to move
	 */
	default void MoveCardsToLSSideOfTable(PhysicalCardImpl...cards) { MoveCardsToSideOfTable(LS, cards); }

	/**
	 * Moves one or more cards to their owner's side of the table.  This is equivalent to playing to that side of the
	 * table, except that no costs, requirements, or other rules will be respected.
	 * @param cards The cards to move
	 */
	default void MoveCardsToSideOfTable(PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			if(!card.getZone().isInPlay()) {
				MoveCardToZone(card.getOwner(), card, Zone.SIDE_OF_TABLE);
			}
			else {
				gameState().relocateCardToSideOfTable(card, card.getOwner());
			}
		});
	}

	/**
	 * Moves one or more cards to a given player's side of the table.  This is equivalent to playing to that side of the
	 * table, except that no costs, requirements, or other rules will be respected.
	 * @param player Which player's side of the table to use
	 * @param cards The cards to move
	 */
	default void MoveCardsToSideOfTable(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			if(!card.getZone().isInPlay()) {
				MoveCardToZone(player, card, Zone.SIDE_OF_TABLE);
			}
			else {
				gameState().relocateCardToSideOfTable(card, player);
			}
		});
	}


	/**
	 * Moves one or more cards to the top of their owner's reserve deck.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToTopOfOwnReserveDeck(PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.RESERVE_DECK, card.getOwner());
		});
	}

	/**
	 * Moves one or more cards to the top of the Dark Side player's reserve deck.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToTopOfDSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(DS, cards);}

	/**
	 * Stacks the Dark Side deck with a destiny card of the given amount.  This function will take that card from
	 * out of play and place it on top of the Reserve deck, but be warned: it will then be in play ever after, so if
	 * you have long complicated chains of actions you might want to handle the stacking yourself.
	 * @param amount The destiny you would like to be shortly drawing.  Limited to values from 0-7.
	 */
	default void PrepareDSDestiny(int amount) { MoveCardsToTopOfDSReserveDeck(GetDSDestiny(amount)); }

	/**
	 * Moves one or more cards to the top of the Light Side player's reserve deck.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToTopOfLSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(LS, cards);}
	/**
	 * Stacks the Light Side deck with a destiny card of the given amount.  This function will take that card from
	 * out of play and place it on top of the Reserve deck, but be warned: it will then be in play ever after, so if
	 * you have long complicated chains of actions you might want to handle the stacking yourself.
	 * @param amount The destiny you would like to be shortly drawing.  Limited to values from 0-7.
	 */
	default void PrepareLSDestiny(int amount) { MoveCardsToTopOfLSReserveDeck(GetLSDestiny(amount)); }

	/**
	 * Moves one or more cards to the top of the given player's reserve deck.
	 * @param player Which player's reserve deck to be using
	 * @param cards The cards to move.
	 */
	default void MoveCardsToTopOfReserveDeck(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.RESERVE_DECK, player);
		});
	}

	/**
	 * Moves one or more cards to the bottom of their owner's reserve deck.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToBottomOfOwnReserveDeck(PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToZone(card, Zone.RESERVE_DECK, card.getOwner());
		});
	}

	/**
	 * Moves one or more cards to the bottom of the Dark Side player's reserve deck.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToBottomOfDSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToBottomOfReserveDeck(DS, cards);}
	/**
	 * Moves one or more cards to the bottom of the Light Side player's reserve deck.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToBottomOfLSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToBottomOfReserveDeck(LS, cards);}

	/**
	 * Moves one or more cards to the bottom of the given player's reserve deck.
	 * @param player Which player's reserve deck to be using
	 * @param cards The cards to move.
	 */
	default void MoveCardsToBottomOfReserveDeck(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToZone(card, Zone.RESERVE_DECK, player);
		});
	}


	/**
	 * Force Pile top
	 */

	default void MoveCardsToTopOfOwnForcePile(PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.FORCE_PILE, card.getOwner());
		});
	}

	default void MoveCardsToTopOfDSForcePile(PhysicalCardImpl...cards) { MoveCardsToTopOfForcePile(DS, cards);}
	default void MoveCardsToTopOfLSForcePile(PhysicalCardImpl...cards) { MoveCardsToTopOfForcePile(LS, cards);}

	default void MoveCardsToTopOfForcePile(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.FORCE_PILE, player);
		});
	}

	/**
	 * Used Pile top
	 */

	default void MoveCardsToTopOfOwnUsedPile(PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.USED_PILE, card.getOwner());
		});
	}

	default void MoveCardsToTopOfDSUsedPile(PhysicalCardImpl...cards) { MoveCardsToTopOfUsedPile(DS, cards);}
	default void MoveCardsToTopOfLSUsedPile(PhysicalCardImpl...cards) { MoveCardsToTopOfUsedPile(LS, cards);}

	default void MoveCardsToTopOfUsedPile(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.USED_PILE, player);
		});
	}

	/**
	 * Lost Pile top
	 */

	default void MoveCardsToTopOfOwnLostPile(PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.LOST_PILE, card.getOwner());
		});
	}

	default void MoveCardsToTopOfDSLostPile(PhysicalCardImpl...cards) { MoveCardsToTopOfLostPile(DS, cards);}

	default void MoveCardsToTopOfLSLostPile(PhysicalCardImpl...cards) { MoveCardsToTopOfLostPile(LS, cards);}

	default void MoveCardsToTopOfLostPile(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.LOST_PILE, player);
		});
	}


	/**
	 * Moves the given cards to the Dark Side player's hand.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToDSHand(PhysicalCardImpl...cards) {
		for(PhysicalCardImpl card : cards) {
			RemoveCardZone(card);
			MoveCardToZone(DS, card, Zone.HAND);
		}
	}

	/**
	 * Moves the given cards to the Light Side player's hand.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToLSHand(PhysicalCardImpl...cards) {
		for(PhysicalCardImpl card : cards) {
			RemoveCardZone(card);
			MoveCardToZone(LS, card, Zone.HAND);
		}
	}

	/**
	 * Moves the given cards to their owner's hands.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToHand(PhysicalCardImpl...cards) {
		for(PhysicalCardImpl card : cards) {
			RemoveCardZone(card);
			MoveCardToZone(card.getOwner(), card, Zone.HAND);
		}
	}

	/**
	 * Seizes a card, making it a captive and assigning it to be escorted by a given captor.
	 * @param escort The card to attach to.
	 * @param captive The card to seize.
	 */
	default void CaptureCardWith(PhysicalCardImpl escort, PhysicalCardImpl captive) {
		gameState().seizeCharacter(game(), captive, escort);
	}

	/**
	 * Freezes a given card in carbonite, rending it immobile, insensate, and inactive for most purposes.
	 * @param captive The card to freeze.
	 */
	default void FreezeCard(PhysicalCardImpl captive) {
		gameState().freezeCharacter(captive);
	}

	default void BoardAsPassenger(PhysicalCardImpl vehicle, PhysicalCardImpl...passengers) {
		Arrays.stream(passengers).forEach(passenger -> {
			var originalZone = passenger.getZone();
			RemoveCardZone(passenger);
			if(originalZone.isInPlay()) {
				gameState().moveCardToAttachedInPassengerCapacitySlot(passenger, vehicle);
			}
			else {
				gameState().attachCardInPassengerCapacitySlot(passenger, vehicle);
			}
		});
	}

	default void BoardAsPilot(PhysicalCardImpl vehicle, PhysicalCardImpl...pilots) {
		Arrays.stream(pilots).forEach(pilot -> {
			var originalZone = pilot.getZone();
			RemoveCardZone(pilot);
			if(originalZone.isInPlay()) {
				gameState().moveCardToAttachedInPilotCapacitySlot(pilot, vehicle);
			}
			else {
				gameState().attachCardInPilotCapacitySlot(pilot, vehicle);
			}
		});
	}


	/**
	 * Directly attaches one or more cards to a target card, regardless of legality or costs.  This is often used once
	 * a card has already proven to deploy properly for expedience in follow-up tests.
	 * @param bearer Which card should be bearing the given cards.
	 * @param cards One or more cards to attach
	 */
    default void AttachCardsTo(PhysicalCardImpl bearer, PhysicalCardImpl...cards) {
        Arrays.stream(cards).forEach(card -> {
            RemoveCardZone(card);
            gameState().attachCard(card, bearer);
        });
    }

	/**
	 * Directly stacks one or more cards on a target card, regardless of legality or costs.  This is often used once
	 * a card has already proven to stack properly for expedience in follow-up tests.
	 * @param on Which card the given cards should be stacked on.
	 * @param cards One or more cards to stack
	 */
    default void StackCardsOn(PhysicalCardImpl on, PhysicalCardImpl...cards) {
        Arrays.stream(cards).forEach(card -> {
            RemoveCardZone(card);
            gameState().stackCard(card, on, false, false, false);
        });
    }

	// Unsure if this is ever really necessary or relevant, but here it is.
	default void DrawCardsFromReserve(String player, int count) {
		for (int i = 0; i < count; ++i) {
			var reserveDeck = gameState().getReserveDeck(player, true);
			if (reserveDeck.size() < 2) {
				return;
			}
			var card = (PhysicalCardImpl) reserveDeck.getLast();
			MoveCardToZone(player, card, Zone.HAND);
		}
	}


	default void ShuffleCardsIntoDSReserveDeck(PhysicalCardImpl...cards) { ShuffleCardsIntoReserveDeck(DS, cards); }
	default void ShuffleCardsIntoLSReserveDeck(PhysicalCardImpl...cards) { ShuffleCardsIntoReserveDeck(LS, cards); }
	default void ShuffleCardsIntoReserveDeck(String player, PhysicalCardImpl...cards) {
		gameState().shuffleCardsIntoPile(Arrays.stream(cards).toList(), player, Zone.RESERVE_DECK);
	}

	default void ShuffleCardsIntoDSForcePile(PhysicalCardImpl...cards) { ShuffleCardsIntoForcePile(DS, cards); }
	default void ShuffleCardsIntoLSForcePile(PhysicalCardImpl...cards) { ShuffleCardsIntoForcePile(LS, cards); }
	default void ShuffleCardsIntoForcePile(String player, PhysicalCardImpl...cards) {
		gameState().shuffleCardsIntoPile(Arrays.stream(cards).toList(), player, Zone.FORCE_PILE);
	}


	/**
	 * Shuffles the Dark Side player's reserve deck.
	 */
    default void ShuffleDSReserveDeck() { ShuffleReserveDeck(DS); }
	/**
	 * Shuffles the Light Side player's reserve deck.
	 */
    default void ShuffleLSReserveDeck() { ShuffleReserveDeck(LS); }

	/**
	 * Shuffles the given player's reserve deck.
	 * @param player The player's deck to shuffle
	 */
    default void ShuffleReserveDeck(String player) {
        gameState().shuffleReserveDeck(player);
    }




}
