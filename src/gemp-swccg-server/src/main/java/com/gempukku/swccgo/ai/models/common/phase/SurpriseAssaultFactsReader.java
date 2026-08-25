package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

import java.util.List;
import java.util.Objects;

/** Reads the exact game facts used by Surprise Assault's card source. */
public final class SurpriseAssaultFactsReader {
    public enum LocationKind {
        SITE,
        SYSTEM,
        SECTOR,
        OTHER,
        UNKNOWN
    }

    public record Facts(
            String actionId,
            LocationKind locationKind,
            boolean complete,
            int opponentPresentCards,
            int reserveCards,
            double averageDestiny,
            float opponentPower,
            boolean opponentCanUseDarkForces) {
        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(locationKind, "locationKind");
        }

        public int effectiveDraws() {
            return Math.min(Math.max(0, opponentPresentCards),
                    Math.max(0, reserveCards));
        }

        public double projectedDestinyTotal() {
            return effectiveDraws() * averageDestiny;
        }

        public double projectedMargin() {
            return projectedDestinyTotal() - opponentPower;
        }
    }

    private SurpriseAssaultFactsReader() {
    }

    public static Facts read(
            String actionId,
            SwccgGame game,
            String playerId,
            PhysicalCard surpriseAssault) {
        if (game == null || playerId == null || surpriseAssault == null) {
            return unknown(actionId);
        }

        LocationKind locationKind = LocationKind.UNKNOWN;
        try {
            GameState gameState = game.getGameState();
            ModifiersQuerying modifiers = game.getModifiersQuerying();
            if (gameState == null || modifiers == null) {
                return unknown(actionId);
            }
            PhysicalCard location = gameState.getForceDrainLocation();
            if (location == null
                    || location.getBlueprint() == null) {
                return unknown(actionId);
            }

            locationKind = classify(
                    location.getBlueprint().getCardSubtype());
            if (locationKind == LocationKind.SYSTEM
                    || locationKind == LocationKind.SECTOR) {
                return new Facts(actionId, locationKind, true,
                        0, 0, 0.0, 0.0f, false);
            }
            String opponent = game.getOpponent(playerId);
            if (opponent == null) {
                return new Facts(actionId, locationKind, false,
                        0, 0, 0.0, 0.0f, false);
            }

            int presentCards = Filters.countActive(
                    game, surpriseAssault,
                    Filters.and(
                            Filters.owner(opponent),
                            Filters.or(Filters.character, Filters.starship,
                                    Filters.vehicle),
                            Filters.present(location)));
            float opponentPower = modifiers.getTotalPowerAtLocation(
                    gameState, location, opponent, false, false);
            if (!Float.isFinite(opponentPower)) {
                return new Facts(actionId, locationKind, false,
                        presentCards, 0, 0.0, 0.0f, false);
            }

            List<PhysicalCard> reserve = gameState.getReserveDeck(playerId);
            if (reserve == null) {
                return new Facts(actionId, locationKind, false,
                        presentCards, 0, 0.0, opponentPower, false);
            }

            double destinyTotal = 0.0;
            for (PhysicalCard card : reserve) {
                if (card == null) {
                    return new Facts(actionId, locationKind, false,
                            presentCards, reserve.size(), 0.0,
                            opponentPower, false);
                }
                float destiny = modifiers.getDestinyForDestinyDraw(
                        gameState, card, surpriseAssault);
                if (!Float.isFinite(destiny)) {
                    return new Facts(actionId, locationKind, false,
                            presentCards, reserve.size(), 0.0,
                            opponentPower, false);
                }
                destinyTotal += destiny;
            }
            double averageDestiny = reserve.isEmpty()
                    ? 0.0 : destinyTotal / reserve.size();

            boolean darkForcesCanRespond = Filters.countActive(
                    game, surpriseAssault,
                    Filters.and(Filters.owner(opponent), Filters.Dark_Forces)) > 0
                    && GameConditions.canUseForce(game, opponent, 1);

            return new Facts(actionId, locationKind, true,
                    presentCards, reserve.size(), averageDestiny,
                    opponentPower, darkForcesCanRespond);
        } catch (Exception ignored) {
            return incomplete(actionId, locationKind);
        }
    }

    private static LocationKind classify(CardSubtype subtype) {
        if (subtype == CardSubtype.SITE) {
            return LocationKind.SITE;
        }
        if (subtype == CardSubtype.SYSTEM) {
            return LocationKind.SYSTEM;
        }
        if (subtype == CardSubtype.SECTOR) {
            return LocationKind.SECTOR;
        }
        return subtype == null ? LocationKind.UNKNOWN : LocationKind.OTHER;
    }

    private static Facts unknown(String actionId) {
        return incomplete(actionId, LocationKind.UNKNOWN);
    }

    private static Facts incomplete(String actionId, LocationKind locationKind) {
        return new Facts(actionId == null ? "" : actionId,
                locationKind, false, 0, 0, 0.0, 0.0f, false);
    }
}
