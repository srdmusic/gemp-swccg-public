package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.Locale;

/** Shared stock-state reader for DEPLOY-1 phase-envelope facts. */
public final class DeploySequencingFactsReader {
    private DeploySequencingFactsReader() {
    }

    public static DeploySequencingFacts.PowerGap firstEndangeredLocation(
            GameState gameState, SwccgGame game, String playerId) {
        if (gameState == null || game == null || playerId == null) {
            return null;
        }
        String opponentId = gameState.getOpponent(playerId);
        if (opponentId == null) {
            return null;
        }
        for (PhysicalCard location : gameState.getTopLocations()) {
            if (location == null || !hasExposedOwnedCard(gameState, location, playerId)) {
                continue;
            }
            float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                    gameState, location, playerId, false, false);
            float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                    gameState, location, opponentId, false, false);
            if (theirPower > ourPower) {
                return new DeploySequencingFacts.PowerGap(
                        location.getTitle(), ourPower, theirPower);
            }
        }
        return null;
    }

    public static DeploySequencingFacts.PowerGap firstWinnableBattle(
            GameState gameState, SwccgGame game, String playerId) {
        if (gameState == null || game == null || playerId == null) {
            return null;
        }
        String opponentId = gameState.getOpponent(playerId);
        if (opponentId == null) {
            return null;
        }
        for (PhysicalCard location : gameState.getTopLocations()) {
            if (location == null
                    || !hasExposedOwnedCard(gameState, location, playerId)
                    || !hasOwnedCard(gameState, location, opponentId)) {
                continue;
            }
            float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                    gameState, location, playerId, false, false);
            float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                    gameState, location, opponentId, false, false);
            if (ourPower > theirPower) {
                return new DeploySequencingFacts.PowerGap(
                        location.getTitle(), ourPower, theirPower);
            }
        }
        return null;
    }

    public static boolean hasAnakinsFuneralPyre(GameState gameState) {
        if (gameState == null) {
            return false;
        }
        for (PhysicalCard location : gameState.getLocationsInOrder()) {
            if (location != null && location.getTitle() != null
                    && location.getTitle().toLowerCase(Locale.ROOT).contains("anakin's funeral pyre")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExposedOwnedCard(GameState gameState,
                                                PhysicalCard location,
                                                String playerId) {
        for (PhysicalCard card : gameState.getCardsAtLocation(location)) {
            if (card != null && playerId.equals(card.getOwner()) && !card.isUndercover()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOwnedCard(GameState gameState,
                                        PhysicalCard location,
                                        String playerId) {
        for (PhysicalCard card : gameState.getCardsAtLocation(location)) {
            if (card != null && playerId.equals(card.getOwner())) {
                return true;
            }
        }
        return false;
    }
}
