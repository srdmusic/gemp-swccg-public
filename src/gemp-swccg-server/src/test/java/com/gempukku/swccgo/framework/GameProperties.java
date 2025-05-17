package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCardImpl;

public interface GameProperties extends TestBase {
	default boolean GameIsFinished() { return game().isFinished(); }
	default Phase GetCurrentPhase() { return gameState().getCurrentPhase(); }

	default String GetCurrentPlayer() { return gameState().getCurrentPlayerId(); }
	default String GetOffPlayer() { return gameState().getOpponent(GetCurrentPlayer()); }
	default int GetDSTurnCount() { return GetPlayerTurnCount(DS); }
	default int GetLSTurnCount() { return GetPlayerTurnCount(LS); }
	default int GetPlayerTurnCount(String player) { return gameState().getPlayersLatestTurnNumber(player); }

	default PhysicalCardImpl GetDSStartingLocation() { return GetDSCard("starting-location"); }
	default PhysicalCardImpl GetLSStartingLocation() { return GetLSCard("starting-location"); }
}
