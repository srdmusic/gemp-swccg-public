package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;

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
	default String GetOffPlayer() { return gameState().getOpponent(GetCurrentPlayer()); }

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

	/**
	 * @return Gets the default starting location that was played automatically for Dark Side using the test rig setup.
	 * If you manually played a starting location, this may not be coherent.
	 */
	default PhysicalCardImpl GetDSStartingLocation() { return GetDSCard("starting-location"); }
	/**
	 * @return Gets the default starting location that was played automatically for Light Side using the test rig setup.
	 * If you manually played a starting location, this may not be coherent.
	 */
	default PhysicalCardImpl GetLSStartingLocation() { return GetLSCard("starting-location"); }
}
