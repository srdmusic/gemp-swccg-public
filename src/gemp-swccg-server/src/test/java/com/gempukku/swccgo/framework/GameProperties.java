package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;

import java.util.Arrays;
import java.util.List;

public interface GameProperties extends TestBase {
	/**
	 * @return Whether the game has finished one way or another.
	 */
	default boolean GameIsFinished() { return game().isFinished(); }

	/**
	 * @return Gets the current game phase
	 */
	default Phase GetCurrentPhase() { return gameState().getCurrentPhase(); }

	default boolean IsActiveBattle() { return gameState().isDuringBattle(); }
	default PhysicalCard GetBattleLocation() { return gameState().getBattleLocation(); }

	/**
	 * @return Gets the player who is currently playing their turn.
	 */
	default String GetCurrentPlayer() { return gameState().getCurrentPlayerId(); }

	/**
	 * @return Gets the player whose turn it isn't.
	 */
	default String GetOpponent() { return gameState().getOpponent(GetCurrentPlayer()); }

	/**
	 * @return Gets the player who is currently making a decision.
	 */
	default String GetDecidingPlayer() { return userFeedback().getUsersPendingDecision().stream().findFirst().get(); }

	/**
	 * @return Gets the player who is not currently making a decision.
	 */
	default String GetNextDecider() { return gameState().getOpponent(GetDecidingPlayer()); }

	/**
	 * @return Gets the number of turns that the Dark Side player has had, including the current one.
	 */
	default int GetDSTurnCount() { return GetPlayerTurnCount(DS); }
	/**
	 * @return Gets the number of turns that the Light Side player has had, including the current one.
	 */
	default int GetLSTurnCount() { return GetPlayerTurnCount(LS); }

	/**
	 * @param player The player you are interested in
	 * @return Gets the number of turns that the given player has had, including the current one.
	 */
	default int GetPlayerTurnCount(String player) { return gameState().getPlayersLatestTurnNumber(player); }

	default int GetDSLifeForceRemaining() { return GetPlayerLifeForceRemaining(DS); }
	default int GetLSLifeForceRemaining() { return GetPlayerLifeForceRemaining(LS); }
	default int GetPlayerLifeForceRemaining(String player) { return gameState().getPlayerLifeForce(player); }

	default List<PhysicalCard> GetCardsAtLocation(PhysicalCardImpl site) { return gameState().getCardsAtLocation(site); }
	default boolean CardsAtLocation(PhysicalCardImpl site, PhysicalCardImpl...cards) {
		return GetCardsAtLocation(site).containsAll(Arrays.stream(cards).toList());
	}
}
