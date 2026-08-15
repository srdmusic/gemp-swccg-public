package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.MovePredicates;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.Locale;

/**
 * Shared MOVE drain-routing decisions. CardSelection rules consume facts read
 * by their adapters; legacy MoveEvaluator routes preserve their original
 * location scans. Adapters retain engine reads, action mutation, and logging.
 */
public final class MoveDrainRoutingPolicy {
    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    public record UncontestedDeparture(
            Contribution contribution, PhysicalCard bestAdjacent,
            float currentDrain, float bestAdjacentDrain) {
    }

    public enum DrainDirection {
        NONE, LOSS, GAIN
    }

    public record ExplicitDestinationDrain(
            Contribution contribution, DrainDirection direction,
            PhysicalCard destination, float destinationDrain,
            float currentDrain, float drainDelta) {
    }

    public record CantinaShuttle(
            Contribution contribution, boolean pairMatched,
            PhysicalCard destination, int sourceCharactersRemaining) {
    }

    public enum DestinationDrainBranch {
        DRAIN_POTENTIAL,
        TRANSIT_STAGING,
        ZERO_DRAIN
    }

    public record DestinationDrain(
            DestinationDrainBranch branch,
            Contribution contribution) {
    }

    public enum MoveToHereDrainBranch {
        NONE,
        RETREAT_EXEMPT,
        ZERO_DRAIN_PENALTY
    }

    public record MoveToHereDrain(
            MoveToHereDrainBranch branch,
            Contribution contribution) {
    }

    public record BlockedDrainEscape(
            Contribution contribution,
            boolean undercoverSpyBlock) {
        private static BlockedDrainEscape none() {
            return new BlockedDrainEscape(
                    Contribution.none(), false);
        }
    }

    private MoveDrainRoutingPolicy() {
    }

    public static Contribution contestOpponentDrain(
            String destinationTitle,
            float opponentPowerAtDestination,
            int opponentDrainAtDestination,
            int netDrainBalance,
            int opponentCardsAtDestination) {
        if (!(opponentPowerAtDestination > 0.0f)
                || opponentDrainAtDestination <= 0
                || netDrainBalance < 2) {
            return Contribution.none();
        }

        float bonus = 200.0f + Math.max(
                0.0f,
                150.0f - (opponentCardsAtDestination - 1) * 50.0f);
        return new Contribution(
                true,
                String.format(
                        "V166 CONTEST DRAIN: opponent out-draining (net>=2) — contest %s (their drain %d, %d opp cards)",
                        destinationTitle,
                        opponentDrainAtDestination,
                        opponentCardsAtDestination),
                bonus);
    }

    public static DestinationDrain destinationDrain(
            String destinationTitle,
            float expectedDrain,
            boolean transitStagingSite) {
        if (expectedDrain > 0.0f) {
            float bonus = expectedDrain * 12.0f;
            return new DestinationDrain(
                    DestinationDrainBranch.DRAIN_POTENTIAL,
                    new Contribution(
                            true,
                            String.format(
                                    "V67e DRAIN POTENTIAL: drain %.1f at %s = +%.0f opponent force loss",
                                    expectedDrain,
                                    destinationTitle,
                                    bonus),
                            bonus));
        }
        if (transitStagingSite) {
            return new DestinationDrain(
                    DestinationDrainBranch.TRANSIT_STAGING,
                    new Contribution(
                            true,
                            "V67n TRANSIT STAGING DEST: "
                                    + destinationTitle
                                    + " is the Hidden Path transit hub; prefer routing Jedi through here (+300 objective preference)",
                            300.0f));
        }
        return new DestinationDrain(
                DestinationDrainBranch.ZERO_DRAIN,
                new Contribution(
                        true,
                        "V67g ZERO DRAIN: " + destinationTitle
                                + " has no opponent force icons — wasted move!",
                        -200.0f));
    }

    public static Contribution moveFromDrain(
            boolean moveDecision,
            boolean transitStagingSite,
            String sourceTitle,
            int sourceOpponentIcons,
            String destinationTitle,
            int destinationOpponentIcons) {
        if (!moveDecision || transitStagingSite
                || sourceOpponentIcons <= destinationOpponentIcons) {
            return Contribution.none();
        }

        int drainDrop = sourceOpponentIcons - destinationOpponentIcons;
        return new Contribution(
                true,
                String.format(
                        "V67g MOVE-FROM-DRAIN: leaving %s (drain %d) for %s (drain %d) — losing %d drain!",
                        sourceTitle,
                        sourceOpponentIcons,
                        destinationTitle,
                        destinationOpponentIcons,
                        drainDrop),
                -250.0f * drainDrop);
    }

    public static boolean isMoveToHereAction(String actionLower) {
        if (actionLower == null) {
            return false;
        }
        return (actionLower.contains("move from")
                && actionLower.contains("to here"))
                || actionLower.contains("move to here")
                || actionLower.contains("relocate to here");
    }

    public static MoveToHereDrain moveToHereDrain(
            String destinationTitle,
            int destinationOpponentIcons,
            boolean retreatExempt,
            String doomedLocationTitle) {
        if (destinationOpponentIcons != 0) {
            return new MoveToHereDrain(
                    MoveToHereDrainBranch.NONE,
                    Contribution.none());
        }
        if (retreatExempt) {
            return new MoveToHereDrain(
                    MoveToHereDrainBranch.RETREAT_EXEMPT,
                    new Contribution(
                            true,
                            String.format(
                                    "V67ae RETREAT EXEMPT: '%s' hopelessly outgunned (gap >= 6, V33 standard) — retreat to non-drain allowed",
                                    doomedLocationTitle),
                            0.0f));
        }
        return new MoveToHereDrain(
                MoveToHereDrainBranch.ZERO_DRAIN_PENALTY,
                new Contribution(
                        true,
                        String.format(
                                "V67ae MOVE-TO-NON-DRAIN: '%s' destination has 0 opp icons — losing drain pressure for a 'safe' retreat!",
                                destinationTitle),
                        -300.0f));
    }

    public static boolean allowsBlockedDrainEscapeMover(
            boolean moverUndercover,
            boolean moverLocation) {
        return !moverUndercover && !moverLocation;
    }

    public static BlockedDrainEscape blockedDrainEscape(
            String locationTitle,
            boolean friendlyPresence,
            boolean opponentPresence,
            boolean opponentUndercoverSpy) {
        if (!friendlyPresence
                || !(opponentPresence || opponentUndercoverSpy)) {
            return BlockedDrainEscape.none();
        }
        float bonus = opponentUndercoverSpy ? 250.0f : 150.0f;
        return new BlockedDrainEscape(
                new Contribution(
                        true,
                        String.format(
                                "V35.4: %s blocking drain at %s — move away to drain elsewhere!",
                                opponentUndercoverSpy
                                        ? "UNDERCOVER SPY"
                                        : "Enemy presence",
                                locationTitle),
                        bonus),
                opponentUndercoverSpy);
    }

    public static boolean isVaderCastleRetreatAction(String actionLower) {
        if (actionLower == null) {
            return false;
        }
        return (actionLower.contains("vader")
                && actionLower.contains("castle"))
                || actionLower.contains("mustafar");
    }

    public static boolean isMustafarLocation(String locationTitle) {
        return locationTitle != null
                && locationTitle.toLowerCase(Locale.ROOT)
                        .contains("mustafar");
    }

    public static Contribution vaderCastleRetreat(
            String locationTitle,
            int opponentIcons) {
        if (opponentIcons <= 0) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                "V29.7 VADER RETREAT: Vader is draining "
                        + opponentIcons + " at " + locationTitle
                        + " — DON'T retreat to Mustafar!",
                -300.0f);
    }

    public static UncontestedDeparture uncontestedDeparture(
            GameState gameState, SwccgGame game, PhysicalCard source,
            String playerId) {
        float currentDrain = MovePredicates.drainAt(
                game, gameState, source, playerId);
        if (!(currentDrain > 0)) {
            return new UncontestedDeparture(
                    Contribution.none(), null, currentDrain,
                    Float.NEGATIVE_INFINITY);
        }

        float bestAdjacentDrain = Float.NEGATIVE_INFINITY;
        PhysicalCard bestAdjacent = null;
        for (PhysicalCard adjacent : gameState.getLocationsInOrder()) {
            if (adjacent == null || adjacent == source) {
                continue;
            }
            try {
                if (!game.getModifiersQuerying().isAdjacentSites(
                        gameState, source, adjacent)) {
                    continue;
                }
                float adjacentDrain = MovePredicates.drainAt(
                        game, gameState, adjacent, playerId);
                if (adjacentDrain > bestAdjacentDrain) {
                    bestAdjacentDrain = adjacentDrain;
                    bestAdjacent = adjacent;
                }
            } catch (Exception e) {
                // Preserve V85's per-location skip.
            }
        }

        if (bestAdjacent != null && bestAdjacentDrain < currentDrain) {
            return new UncontestedDeparture(
                    new Contribution(
                            true,
                            String.format(
                                    "V85 UNCONTESTED: at %s (drain %.0f) with no opponent — "
                                            + "best adjacent %s only drains %.0f. STAY for the better drain!",
                                    source.getTitle(), currentDrain,
                                    bestAdjacent.getTitle(),
                                    bestAdjacentDrain),
                            -800.0f),
                    bestAdjacent, currentDrain, bestAdjacentDrain);
        }

        return new UncontestedDeparture(
                Contribution.none(), bestAdjacent, currentDrain,
                bestAdjacentDrain);
    }

    public static ExplicitDestinationDrain explicitDestinationDrain(
            GameState gameState, SwccgGame game, PhysicalCard source,
            String playerId, String actionTextLower) {
        PhysicalCard destination = null;
        for (PhysicalCard location : gameState.getLocationsInOrder()) {
            if (location == null || location == source) {
                continue;
            }
            String locationName = location.getTitle() != null
                    ? location.getTitle().toLowerCase(Locale.ROOT) : "";
            if (!locationName.isEmpty()
                    && actionTextLower.contains(locationName)) {
                destination = location;
                break;
            }
        }

        if (destination == null) {
            return new ExplicitDestinationDrain(
                    Contribution.none(), DrainDirection.NONE,
                    null, 0.0f, 0.0f, 0.0f);
        }

        float destinationDrain = MovePredicates.drainAt(
                game, gameState, destination, playerId);
        float currentDrain = MovePredicates.drainAt(
                game, gameState, source, playerId);

        if (destinationDrain < currentDrain) {
            float drainDelta = currentDrain - destinationDrain;
            float drainPenalty = -40.0f * drainDelta;
            if (destinationDrain <= 0) {
                drainPenalty -= 80.0f;
            }
            return new ExplicitDestinationDrain(
                    new Contribution(
                            true,
                            String.format(
                                    "V29.13 BAD DRAIN SITE: %s has drain %.0f (current location has %.0f) — stay for better drain!",
                                    destination.getTitle(), destinationDrain,
                                    currentDrain),
                            drainPenalty),
                    DrainDirection.LOSS, destination, destinationDrain,
                    currentDrain, drainDelta);
        }

        if (destinationDrain > currentDrain) {
            float drainDelta = destinationDrain - currentDrain;
            float drainBonus = 40.0f * drainDelta;
            return new ExplicitDestinationDrain(
                    new Contribution(
                            true,
                            String.format(
                                    "V29.13 GOOD DRAIN SITE: %s has drain %.0f — better than current %.0f!",
                                    destination.getTitle(), destinationDrain,
                                    currentDrain),
                            drainBonus),
                    DrainDirection.GAIN, destination, destinationDrain,
                    currentDrain, drainDelta);
        }

        return new ExplicitDestinationDrain(
                Contribution.none(), DrainDirection.NONE, destination,
                destinationDrain, currentDrain, 0.0f);
    }

    public static CantinaShuttle cantinaShuttle(
            GameState gameState, PhysicalCard source,
            PhysicalCard cardToMove, String playerId,
            String actionDisplayLower) {
        String sourceTitleLower =
                source.getTitle().toLowerCase(Locale.ROOT);
        String destinationTitleLower = "";
        PhysicalCard destination = null;

        for (PhysicalCard location : gameState.getTopLocations()) {
            if (location == null || location == source) {
                continue;
            }
            String locationTitle = location.getTitle();
            if (locationTitle == null) {
                continue;
            }
            String locationTitleLower =
                    locationTitle.toLowerCase(Locale.ROOT);
            if (!locationTitleLower.isEmpty()
                    && actionDisplayLower.contains(locationTitleLower)) {
                destination = location;
                destinationTitleLower = locationTitleLower;
                break;
            }
        }

        if (destination == null) {
            return new CantinaShuttle(
                    Contribution.none(), false, null, 0);
        }

        boolean pairMatched =
                (sourceTitleLower.contains("cantina")
                        && destinationTitleLower.contains("mos eisley"))
                || (sourceTitleLower.contains("mos eisley")
                        && destinationTitleLower.contains("cantina"));
        if (!pairMatched) {
            return new CantinaShuttle(
                    Contribution.none(), false, destination, 0);
        }

        int sourceCharactersRemaining = 0;
        for (PhysicalCard card : gameState.getCardsAtLocation(source)) {
            if (card == null || card == cardToMove) {
                continue;
            }
            if (!playerId.equals(card.getOwner())) {
                continue;
            }
            if (card.getBlueprint() == null) {
                continue;
            }
            if (card.getBlueprint().getCardCategory()
                    != CardCategory.CHARACTER) {
                continue;
            }
            sourceCharactersRemaining++;
        }

        if (sourceCharactersRemaining >= 1) {
            return new CantinaShuttle(
                    new Contribution(
                            true,
                            String.format(
                                    "V73 SHUTTLE: Cantina ↔ Mos Eisley shuttle — drain BOTH this turn (%d chars stay at %s)",
                                    sourceCharactersRemaining,
                                    source.getTitle()),
                            400.0f),
                    true, destination, sourceCharactersRemaining);
        }

        return new CantinaShuttle(
                Contribution.none(), true, destination, 0);
    }
}
