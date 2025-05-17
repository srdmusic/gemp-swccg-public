package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.game.PhysicalCard;

import java.util.List;

/**
 * These functions will help you determine pile counts or add/remove a card to a pile at a particular point.
 */
public interface PileProperties extends TestBase{
	default int GetDSHandCount() { return GetDSHand().size(); }
	default int GetLSHandCount() { return GetLSHand().size(); }

	default List<? extends PhysicalCard> GetDSHand() { return GetHand(DS); }
	default List<? extends PhysicalCard> GetLSHand() { return GetHand(LS); }
	default List<? extends PhysicalCard> GetHand(String player)
	{
		return gameState().getHand(player);
	}

	default int GetDSReserveDeckCount() { return GetDSReserveDeck().size(); }
	default int GetLSReserveDeckCount() { return GetLSReserveDeck().size(); }

	default List<? extends PhysicalCard> GetDSReserveDeck() { return GetReserveDeck(DS); }
	default List<? extends PhysicalCard> GetLSReserveDeck() { return GetReserveDeck(LS); }
	default List<? extends PhysicalCard> GetReserveDeck(String player)
	{
		return gameState().getReserveDeck(player);
	}

	default int GetDSForcePileCount() { return GetDSForcePile().size(); }
	default int GetLSForcePileCount() { return GetLSForcePile().size(); }

	default List<? extends PhysicalCard> GetDSForcePile() { return GetForcePile(DS); }
	default List<? extends PhysicalCard> GetLSForcePile() { return GetForcePile(LS); }
	default List<? extends PhysicalCard> GetForcePile(String player)
	{
		return gameState().getForcePile(player);
	}

	default int GetDSUsedPileCount() { return GetDSUsedPile().size(); }
	default int GetLSUsedPileCount() { return GetLSUsedPile().size(); }

	default List<? extends PhysicalCard> GetDSUsedPile() { return GetUsedPile(DS); }
	default List<? extends PhysicalCard> GetLSUsedPile() { return GetUsedPile(LS); }
	default List<? extends PhysicalCard> GetUsedPile(String player)
	{
		return gameState().getUsedPile(player);
	}

	default int GetDSLostPileCount() { return GetDSLostPile().size(); }
	default int GetLSLostPileount() { return GetLSLostPile().size(); }

	default List<? extends PhysicalCard> GetDSLostPile() { return GetLostPile(DS); }
	default List<? extends PhysicalCard> GetLSLostPile() { return GetLostPile(LS); }
	default List<? extends PhysicalCard> GetLostPile(String player)
	{
		return gameState().getLostPile(player);
	}

//
//    default PhysicalCardImpl GetDSBottomOfDeck() { return GetPlayerBottomOfDeck(DS); }
//    default PhysicalCardImpl GetLSBottomOfDeck() { return GetPlayerBottomOfDeck(LS); }
//    default PhysicalCardImpl GetFromBottomOfDSDeck(int index) { return GetFromBottomOfPlayerDeck(DS, index); }
//    default PhysicalCardImpl GetFromBottomOfLSDeck(int index) { return GetFromBottomOfPlayerDeck(LS, index); }
//    default PhysicalCardImpl GetPlayerBottomOfDeck(String player) { return GetFromBottomOfPlayerDeck(player, 1); }
//    default PhysicalCardImpl GetFromBottomOfPlayerDeck(String player, int index)
//    {
//        var deck = gameState().getDeck(player);
//        return (PhysicalCardImpl) deck.get(deck.size() - index);
//    }
//
//    default PhysicalCardImpl GetDSTopOfDeck() { return GetPlayerTopOfDeck(DS); }
//    default PhysicalCardImpl GetLSTopOfDeck() { return GetPlayerTopOfDeck(LS); }
//    default PhysicalCardImpl GetFromTopOfDSDeck(int index) { return GetFromTopOfPlayerDeck(DS, index); }
//    default PhysicalCardImpl GetFromTopOfLSDeck(int index) { return GetFromTopOfPlayerDeck(LS, index); }
//    default PhysicalCardImpl GetPlayerTopOfDeck(String player) { return GetFromTopOfPlayerDeck(player, 1); }
//
//    /**
//     * Index is 1-based (1 is first, 2 is second, etc)
//     */
//    default PhysicalCardImpl GetFromTopOfPlayerDeck(String player, int index)
//    {
//        var deck = gameState().getDeck(player);
//        if(deck.isEmpty())
//            return null;
//
//        return (PhysicalCardImpl) deck.get(index - 1);
//    }
//    default int GetDSDiscardCount() { return GetPlayerDiscardCount(DS); }
//    default int GetLSDiscardCount() { return GetPlayerDiscardCount(LS); }
//    default int GetPlayerDiscardCount(String player) { return gameState().getDiscard(player).size(); }
//
//    default int GetDSDeadCount() { return gameState().getDeadPile(DS).size(); }
//
//
//
//
//
}
