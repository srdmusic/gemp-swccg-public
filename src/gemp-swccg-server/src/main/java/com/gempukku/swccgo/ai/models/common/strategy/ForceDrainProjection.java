package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;

/** Shared projection of the Force loss a legal Force drain can actually cause. */
public final class ForceDrainProjection {

    private ForceDrainProjection() {
    }

    public static float projectedDamage(GameState gameState,
                                        SwccgGame game,
                                        PhysicalCard location,
                                        String drainingPlayer) {
        if (gameState == null || game == null || location == null
                || drainingPlayer == null) {
            return 0.0f;
        }
        try {
            String losingPlayer = gameState.getOpponent(drainingPlayer);
            if (losingPlayer == null) {
                losingPlayer = game.getOpponent(drainingPlayer);
            }
            if (losingPlayer == null) return 0.0f;

            float nominal = game.getModifiersQuerying().getForceDrainAmount(
                    gameState, location, drainingPlayer);
            float lossLimit = game.getModifiersQuerying()
                    .getForceToLoseFromForceDrainLimit(
                            gameState, losingPlayer, location);
            if (!Float.isFinite(nominal) || Float.isNaN(lossLimit)) {
                return 0.0f;
            }
            return Math.max(0.0f, Math.min(nominal, lossLimit));
        } catch (Exception ignored) {
            return 0.0f;
        }
    }

    /**
     * Preserves the existing potential-drain scan while using projected Force
     * loss instead of the nominal drain number.
     */
    public static int netDamageBalance(GameState gameState,
                                       SwccgGame game,
                                       String playerId) {
        if (gameState == null || game == null || playerId == null) return 0;
        String opponent = gameState.getOpponent(playerId);
        if (opponent == null) return 0;

        int opponentTotal = 0;
        int ourTotal = 0;
        List<PhysicalCard> locations = gameState.getLocationsInOrder();
        if (locations == null) return 0;
        for (PhysicalCard location : locations) {
            if (location == null) continue;
            boolean ours = false;
            boolean theirs = false;
            List<PhysicalCard> cards = gameState.getCardsAtLocation(location);
            if (cards != null) {
                for (PhysicalCard card : cards) {
                    if (card == null) continue;
                    ours |= playerId.equals(card.getOwner());
                    theirs |= opponent.equals(card.getOwner());
                }
            }
            if (theirs) {
                opponentTotal += (int) projectedDamage(
                        gameState, game, location, opponent);
            }
            if (ours) {
                ourTotal += (int) projectedDamage(
                        gameState, game, location, playerId);
            }
        }
        return opponentTotal - ourTotal;
    }
}
