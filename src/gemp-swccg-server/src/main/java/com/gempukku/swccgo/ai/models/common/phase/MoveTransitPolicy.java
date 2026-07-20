package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;

import java.util.Locale;

/**
 * Shared MOVE pilot safety, capacity-loop, transit classification, and
 * movement-type scoring.
 * Adapters retain engine reads, score application, and logging.
 */
public final class MoveTransitPolicy {
    public enum HiddenPathBranch {
        NONE,
        SAFEHOUSE_TO_CORRIDOR,
        CORRIDOR_LANDSPEED_BLOCK,
        MAPUZO_EXIT
    }

    public enum CapacitySlotBranch {
        NONE,
        PASSENGER_SKIP,
        PILOT_PREFER
    }

    public enum EmbarkBranch {
        NO_CONTEXT,
        NO_CARD,
        NON_PILOT,
        POWER_FOUR_PLUS,
        NO_LOCATION,
        UNMANNED_TARGET,
        NO_UNMANNED_TARGET,
        ERROR
    }

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

    public record HiddenPathTransit(
            HiddenPathBranch branch,
            Contribution contribution,
            boolean claimMandatoryTransit,
            String claimIdentity,
            boolean hardVeto,
            String hardVetoReason,
            String characterName) {
        private static HiddenPathTransit none(String characterName) {
            return new HiddenPathTransit(
                    HiddenPathBranch.NONE, Contribution.none(),
                    false, null, false, null, characterName);
        }
    }

    public record CapacitySlot(
            CapacitySlotBranch branch,
            float baseScore,
            Contribution contribution) {
        private static CapacitySlot none() {
            return new CapacitySlot(
                    CapacitySlotBranch.NONE, 0.0f,
                    Contribution.none());
        }
    }

    public record CapacityChoice(
            CapacitySlotBranch branch,
            float replacementScore,
            Contribution contribution) {
        private static CapacityChoice none() {
            return new CapacityChoice(
                    CapacitySlotBranch.NONE, 0.0f,
                    Contribution.none());
        }
    }

    public record EmbarkEvaluation(
            EmbarkBranch branch,
            Contribution contribution) {
    }

    public record SpaceDestinationPenalties(
            Contribution permanentWeaponCharacter,
            Contribution vehicle) {
    }

    public static Contribution capacitySlotSwap(String actionLower) {
        if (actionLower.contains("move to passenger capacity slot")
                || actionLower.contains("move to pilot capacity slot")) {
            return new Contribution(
                    true,
                    "V87 NO SWAP: pilot↔passenger capacity slot rearrangement is pointless — hard block",
                    -3000.0f);
        }
        return Contribution.none();
    }

    private MoveTransitPolicy() {
    }

    public static boolean isDrainTransitStagingSite(String locationTitle) {
        return locationTitle != null
                && locationTitle.toLowerCase(Locale.ROOT)
                        .contains("underground corridor");
    }

    public static boolean isPositiveHiddenPathTransitAction(
            String actionLower) {
        if (actionLower == null) {
            return false;
        }
        return actionLower.contains("move jedi survivor here to a site")
                || (actionLower.contains("move jedi")
                        && actionLower.contains("to a site"));
    }

    public static Contribution positiveHiddenPathTransit(
            boolean hiddenPathObjective) {
        if (hiddenPathObjective) {
            return new Contribution(
                    true,
                    "V60 HIDDEN PATH TRANSIT: Move Jedi OUT of Corridor — flips objective! (R4 band)",
                    20000.0f);
        }
        return new Contribution(
                true,
                "Move Jedi transit action — tactical mobility",
                200.0f);
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

    public static CapacitySlot capacitySlot(String actionLower) {
        if (actionLower.contains("passenger capacity slot")) {
            return new CapacitySlot(
                    CapacitySlotBranch.PASSENGER_SKIP,
                    0.0f, Contribution.none());
        }
        if (actionLower.contains("pilot capacity slot")) {
            return new CapacitySlot(
                    CapacitySlotBranch.PILOT_PREFER,
                    100.0f,
                    new Contribution(
                            true,
                            "Move to pilot slot - adds power!",
                            50.0f));
        }
        return CapacitySlot.none();
    }

    public static CapacityChoice capacityChoice(
            boolean pilotMarker,
            boolean passengerMarker) {
        if (pilotMarker) {
            return new CapacityChoice(
                    CapacitySlotBranch.PILOT_PREFER,
                    100.0f,
                    new Contribution(
                            true,
                            "Pilot slot adds power to ship!",
                            100.0f));
        }
        if (passengerMarker) {
            return new CapacityChoice(
                    CapacitySlotBranch.PASSENGER_SKIP,
                    -50.0f,
                    new Contribution(
                            true,
                            "Passenger gives NO power bonus!",
                            -50.0f));
        }
        return CapacityChoice.none();
    }

    public static EmbarkEvaluation embark(
            boolean contextAvailable,
            boolean cardAvailable,
            boolean eligiblePilotOrTrooper,
            Float power,
            boolean locationAvailable,
            boolean unmannedTargetAtSite,
            boolean readFailed,
            String embarkerTitle,
            String unmannedTargetTitle) {
        if (readFailed) {
            return new EmbarkEvaluation(
                    EmbarkBranch.ERROR,
                    new Contribution(true, "Embark action (error)", 0.0f));
        }
        if (!contextAvailable) {
            return new EmbarkEvaluation(
                    EmbarkBranch.NO_CONTEXT,
                    new Contribution(true, "Embark action (no context)", 0.0f));
        }
        if (!cardAvailable) {
            return new EmbarkEvaluation(
                    EmbarkBranch.NO_CARD,
                    new Contribution(true, "Embark action (no card)", 0.0f));
        }
        if (!eligiblePilotOrTrooper) {
            return new EmbarkEvaluation(
                    EmbarkBranch.NON_PILOT,
                    new Contribution(true, "Embark action (non-pilot)", 0.0f));
        }
        if (power != null && power >= 4.0f) {
            return new EmbarkEvaluation(
                    EmbarkBranch.POWER_FOUR_PLUS,
                    new Contribution(
                            true,
                            "Embark action (skipped: power " + power.intValue()
                                    + " — better as ground troop)",
                            0.0f));
        }
        if (!locationAvailable) {
            return new EmbarkEvaluation(
                    EmbarkBranch.NO_LOCATION,
                    new Contribution(true, "Embark action (no location)", 0.0f));
        }
        if (unmannedTargetAtSite) {
            return new EmbarkEvaluation(
                    EmbarkBranch.UNMANNED_TARGET,
                    new Contribution(
                            true,
                            "EMBARK PILOT: '" + embarkerTitle
                                    + "' boarding unmanned '" + unmannedTargetTitle
                                    + "' — vehicle gets power & protection",
                            500.0f));
        }
        return new EmbarkEvaluation(
                EmbarkBranch.NO_UNMANNED_TARGET,
                new Contribution(
                        true,
                        "Embark action (no unmanned target at site)",
                        0.0f));
    }

    public static Contribution residualTransfer() {
        return new Contribution(
                true,
                "Usually avoid disembark/relocate/transfer",
                -50.0f);
    }

    public static Contribution shipDock() {
        return new Contribution(true, "Avoid ship-docking", -50.0f);
    }

    public static SpaceDestinationPenalties spaceDestination(
            boolean systemDestination,
            boolean permanentWeaponCharacter,
            boolean vehicle) {
        Contribution weaponPenalty = systemDestination
                && permanentWeaponCharacter
                ? new Contribution(
                        true,
                        "V24.14B WEAPON CHAR TO SPACE: Permanent weapon can't fire at system locations — don't shuttle here!",
                        -300.0f)
                : Contribution.none();
        Contribution vehiclePenalty = systemDestination && vehicle
                ? new Contribution(
                        true,
                        "V24.14B VEHICLE TO SPACE: Vehicles don't belong in space!",
                        -300.0f)
                : Contribution.none();
        return new SpaceDestinationPenalties(
                weaponPenalty, vehiclePenalty);
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

    public static HiddenPathTransit hiddenPathTransit(
            String objectiveTitle, PhysicalCard cardToMove,
            String actionLower) {
        if (objectiveTitle == null
                || !objectiveTitle.toLowerCase(Locale.ROOT)
                        .contains("hidden path")
                || cardToMove == null) {
            return HiddenPathTransit.none(null);
        }

        PhysicalCard sourceLocation = cardToMove.getAtLocation();
        String sourceName = sourceLocation != null
                && sourceLocation.getTitle() != null
                ? sourceLocation.getTitle().toLowerCase(Locale.ROOT) : "";
        String characterName = cardToMove.getTitle() != null
                ? cardToMove.getTitle() : "character";
        boolean landspeed = actionLower.contains("move using landspeed")
                || actionLower.equals("move");

        if (sourceName.contains("safehouse") && landspeed) {
            return new HiddenPathTransit(
                    HiddenPathBranch.SAFEHOUSE_TO_CORRIDOR,
                    new Contribution(
                            true,
                            "V53b HIDDEN PATH MANDATORY: Landspeed Safehouse → Corridor — FREE move, MUST flip objective!",
                            800.0f),
                    true, "V53b SAFEHOUSE→CORRIDOR",
                    false, null, characterName);
        } else if (sourceName.contains("underground corridor")
                || sourceName.contains("underground")) {
            if (landspeed) {
                return new HiddenPathTransit(
                        HiddenPathBranch.CORRIDOR_LANDSPEED_BLOCK,
                        Contribution.none(),
                        false, null, true,
                        "V60 HIDDEN PATH LANDSPEED BLOCK: Landspeed from Corridor only goes back to Mapuzo — use the transit game text instead!",
                        characterName);
            }
        } else if (sourceName.contains("mapuzo") && landspeed) {
            return new HiddenPathTransit(
                    HiddenPathBranch.MAPUZO_EXIT,
                    new Contribution(
                            true,
                            "V53b HIDDEN PATH: Leaving Mapuzo via landspeed — objective progress!",
                            800.0f),
                    true, "V53b MAPUZO EXIT",
                    false, null, characterName);
        }

        return HiddenPathTransit.none(characterName);
    }
}
