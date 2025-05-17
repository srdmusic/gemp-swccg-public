package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.Zone;
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
	 * @param card
	 */
	default void RemoveCardZone(PhysicalCardImpl card) {
		if(card.getZone() != null)
		{
			gameState().removeCardsFromZone(new ArrayList<>() {{
				add(card);
			}});
		}
	}

	default void MoveCardToZone(String player, PhysicalCardImpl card, Zone zone) {
		RemoveCardZone(card);
		gameState().addCardToZone(card, zone, player);
	}

	/**
	 * Reserve Deck top
	 */

	default void MoveCardsToTopOfOwnReserveDeck(PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.RESERVE_DECK, card.getOwner());
		});
	}

	default void MoveCardsToTopOfDSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(DS, cards);}

	default void MoveCardsToTopOfLSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(LS, cards);}

	default void MoveCardsToTopOfReserveDeck(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToTopOfZone(card, Zone.RESERVE_DECK, player);
		});
	}


	/**
	 * Reserve Deck bottom
	 */

	default void MoveCardsToBottomOfOwnReserveDeck(PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> {
			RemoveCardZone(card);
			gameState().addCardToZone(card, Zone.RESERVE_DECK, card.getOwner());
		});
	}

	default void MoveCardsToBottomOfDSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(DS, cards);}
	default void MoveCardsToBottomOfLSReserveDeck(PhysicalCardImpl...cards) { MoveCardsToTopOfReserveDeck(LS, cards);}

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

    default void AttachCardsTo(PhysicalCardImpl bearer, PhysicalCardImpl...cards) {
        Arrays.stream(cards).forEach(card -> {
            RemoveCardZone(card);
            gameState().attachCard(card, bearer);
        });
    }

    default void StackCardsOn(PhysicalCardImpl on, PhysicalCardImpl...cards) {
        Arrays.stream(cards).forEach(card -> {
            RemoveCardZone(card);
            gameState().stackCard(card, on, false, false, false);
        });
    }

//	default void DrawCardsFromReserve(String player, int count) {
//		for (int i = 0; i < count; ++i) {
//			var reserveDeck = gameState().getReserveDeck(player, true);
//			if (reserveDeck.size() < 2) {
//				return;
//			}
//			var card = (PhysicalCardImpl) reserveDeck.getLast();
//			MoveCardToZone(player, card, Zone.HAND);
//		}
//	}


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


    default void ShuffleDSReserveDeck() { ShuffleReserveDeck(DS); }
    default void ShuffleLSReserveDeck() { ShuffleReserveDeck(LS); }
    default void ShuffleReserveDeck(String player) {
        gameState().shuffleReserveDeck(player);
    }

	default void MoveCardsToLocation(PhysicalCardImpl location, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> gameState().moveCardToLocation(card, location, true));
	}
	default void MoveCardsToOpponentLocation(PhysicalCardImpl location, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> gameState().moveCardToLocation(card, location, false));
	}

	default void MoveCardsToDSSideOfTable(PhysicalCardImpl...cards) { MoveCardsToSideOfTable(DS, cards); }
	default void MoveCardsToLSSideOfTable(PhysicalCardImpl...cards) { MoveCardsToSideOfTable(LS, cards); }

	default void MoveCardsToSideOfTable(String player, PhysicalCardImpl...cards) {
		Arrays.stream(cards).forEach(card -> gameState().relocateCardToSideOfTable(card, player));
	}

}
