package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;

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
	 * Moves one or more cards to a given location, on their owner's side of that location.  This is equivalent to
	 * deploying to a location, except that no costs, requirements, or other rules will be respected.
	 * This is unrelated to transporting a card during the Move phase.
	 * @param location The location to move to
	 * @param cards The cards to move
	 */
	default void MoveCardsToLocation(PhysicalCardImpl location, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> gameState().moveCardToLocation(card, location, true));
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
	 * Moves one or more cards to a given player's side of the table.  This is equivalent to playing to that side of the
	 * table, except that no costs, requirements, or other rules will be respected.
	 * @param player Which player's side of the table to use
	 * @param cards The cards to move
	 */
	default void MoveCardsToSideOfTable(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> gameState().relocateCardToSideOfTable(card, player));
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
	 * Moves one or more cards to the top of the Light Side player's reserve deck.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToTopOfLSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(LS, cards);}

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
	default void MoveCardsToBottomOfDSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(DS, cards);}
	/**
	 * Moves one or more cards to the bottom of the Light Side player's reserve deck.
	 * @param cards The cards to move.
	 */
	default void MoveCardsToBottomOfLSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(LS, cards);}

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

	default void MoveCardsToTopOfDSForcePile(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(DS, cards);}
	default void MoveCardsToTopOfLSForcePile(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(LS, cards);}

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

	default void MoveCardsToTopOfDSUsedPile(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(DS, cards);}
	default void MoveCardsToTopOfLSUsedPile(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(LS, cards);}

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

	default void MoveCardsToTopOfDSLostPile(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(DS, cards);}

	default void MoveCardsToTopOfLSLostPile(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(LS, cards);}

	default void MoveCardsToTopOfLostPile(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.LOST_PILE, player);
		});
	}


	/**
	 * Hand manipulation
	 */
	default void MoveCardsToDSHand(PhysicalCardImpl...cards) {
		for(PhysicalCardImpl card : cards) {
			RemoveCardZone(card);
			MoveCardToZone(DS, card, Zone.HAND);
		}
	}

    default void MoveCardsToLSHand(PhysicalCardImpl...cards) {
        for(PhysicalCardImpl card : cards) {
            RemoveCardZone(card);
            MoveCardToZone(LS, card, Zone.HAND);
        }
    }

	default void CaptureCardWith(PhysicalCardImpl captor, PhysicalCardImpl captive) {
		gameState().seizeCharacter(game(), captive, captor);
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
