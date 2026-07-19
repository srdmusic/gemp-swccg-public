package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;

/**
 * Shared MOVE pilot safety and movement-type scoring.
 * Adapters retain score application and logging.
 */
public final class MoveTransitPolicy {
    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    public record PilotLock(Contribution contribution, String pilotName,
                            String shipName) {
    }

    public record DefensiveShuttle(Contribution contribution,
                                   String locationTitle,
                                   float ourPower, float theirPower) {
    }

    public record MovementTypes(boolean shuttleAction,
                                DefensiveShuttle defensiveShuttle,
                                Contribution dockingBayTransit,
                                Contribution takeOff) {
    }

    private MoveTransitPolicy() {
    }

    public static PilotLock pilotLock(PhysicalCard cardToMove) {
        if (cardToMove == null || !cardToMove.isPilotOf()) {
            return new PilotLock(Contribution.none(), null, null);
        }

        PhysicalCard ship = cardToMove.getAttachedTo();
        String shipName = ship != null && ship.getTitle() != null
                ? ship.getTitle() : "unknown ship";
        String pilotName = cardToMove.getTitle() != null
                ? cardToMove.getTitle() : "pilot";
        return new PilotLock(
                new Contribution(true,
                        "V25 PILOT LOCK: " + pilotName + " is piloting "
                                + shipName + " — NEVER leave the ship!",
                        -500.0f),
                pilotName, shipName);
    }

    public static MovementTypes movementTypes(
            String actionLower, GameState gameState, String playerId) {
        boolean shuttleAction = actionLower.contains("shuttle")
                || actionLower.contains("transport");
        DefensiveShuttle defensiveShuttle = new DefensiveShuttle(
                Contribution.none(), null, 0.0f, 0.0f);

        if (shuttleAction && gameState != null) {
            String opponentId = gameState.getOpponent(playerId);
            for (PhysicalCard location : gameState.getLocationsInOrder()) {
                String locationTitle = location.getTitle();
                if (locationTitle != null && actionLower.contains(
                        locationTitle.toLowerCase(java.util.Locale.ROOT))) {
                    float ourPower = 0;
                    float theirPower = 0;
                    for (PhysicalCard card :
                            gameState.getCardsAtLocation(location)) {
                        if (card == null) {
                            continue;
                        }
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        if (blueprint == null
                                || !blueprint.hasPowerAttribute()) {
                            continue;
                        }
                        Float power = blueprint.getPower();
                        if (power == null) {
                            power = 0f;
                        }
                        if (playerId.equals(card.getOwner())) {
                            ourPower += power;
                        } else if (opponentId != null
                                && opponentId.equals(card.getOwner())) {
                            theirPower += power;
                        }
                    }
                    if (ourPower > 0 && theirPower >= ourPower * 2) {
                        defensiveShuttle = new DefensiveShuttle(
                                new Contribution(true,
                                        "V25 Defensive shuttle — opponent has "
                                                + (int) theirPower + " vs our "
                                                + (int) ourPower + " at "
                                                + locationTitle,
                                        20.0f),
                                locationTitle, ourPower, theirPower);
                    }
                    break;
                }
            }
        }

        Contribution dockingBay = actionLower.contains("docking bay")
                ? new Contribution(true, "Docking bay transit", 15.0f)
                : Contribution.none();
        Contribution takeOff = actionLower.contains("take off")
                ? new Contribution(true, "Take off (space deployment)", 10.0f)
                : Contribution.none();

        return new MovementTypes(
                shuttleAction, defensiveShuttle, dockingBay, takeOff);
    }
}
