package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Shared MOVE destination and landed-ship safety analysis.
 * Adapters retain score, ladder, veto, exception-log, and action-log ownership.
 */
public final class MoveDestinationPolicy {
    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    public record LandedShipEscape(
            Contribution contribution, boolean takeOff,
            boolean disembark, boolean moveAboard,
            boolean landedShipFound) {
    }

    public record CompanionVeto(boolean hardVeto, String reason) {
        private static CompanionVeto none() {
            return new CompanionVeto(false, null);
        }
    }

    public record DestinationContest(
            PhysicalCard destination,
            Contribution contestContribution,
            float opponentPowerAtDestination,
            float ourPowerAtDestination,
            boolean destinationWasUncontested,
            boolean moverArmed,
            boolean jediAtDestination,
            Contribution battlegroundAdvanceContribution,
            boolean wrongDirectionVeto,
            String wrongDirectionReason,
            String opponentUncontestedLocation,
            boolean castleVeto,
            String castleVetoReason) {
    }

    private MoveDestinationPolicy() {
    }

    public static PhysicalCard resolveDestination(
            GameState gameState,
            PhysicalCard source,
            String actionLower) {
        for (PhysicalCard location : gameState.getLocationsInOrder()) {
            if (location == null || location == source) {
                continue;
            }
            String locationName = location.getTitle() != null
                    ? location.getTitle().toLowerCase(Locale.ROOT) : "";
            if (!locationName.isEmpty()
                    && actionLower.contains(locationName)) {
                return location;
            }
        }
        return null;
    }

    public static Contribution battlegroundRetreat(
            String sourceTitle,
            String destinationTitle,
            boolean sourceBattleground,
            boolean destinationBattleground) {
        if (!sourceBattleground || destinationBattleground) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                String.format(
                        "V37 NO RETREAT: Moving from battleground %s"
                                + " to non-battleground %s"
                                + " — lose drain and battle ability!",
                        sourceTitle,
                        destinationTitle),
                -800.0f);
    }

    public static boolean isSelfMoveToFriend(String gameText) {
        if (gameText == null) {
            return false;
        }
        String lower = gameText.toLowerCase(Locale.ROOT);
        return lower.contains("may move to same site as")
                || lower.contains("moves to same site as");
    }

    public static CompanionVeto companionVeto(
            String moverTitle,
            String destinationTitle,
            boolean selfMoveToFriend,
            int friendlyCharactersAtDestination) {
        if (!selfMoveToFriend || friendlyCharactersAtDestination != 0) {
            return CompanionVeto.none();
        }
        return new CompanionVeto(
                true,
                String.format(
                        "V135 SELF-MOVE-TO-FRIEND ALONE: '%s' would land alone at %s"
                                + " — no friendly characters there",
                        moverTitle,
                        destinationTitle));
    }

    public static LandedShipEscape landedShipEscape(
            GameState gameState, SwccgGame game, PhysicalCard location,
            String playerId, Supplier<String> actionTextSupplier) {
        boolean currentIsSystem = false;
        try {
            currentIsSystem = location.getBlueprint().getCardSubtype()
                    == CardSubtype.SYSTEM;
        } catch (Exception e) {
            // Preserve V91's fail-open classification as a site.
        }

        if (currentIsSystem) {
            return new LandedShipEscape(
                    Contribution.none(), false, false,
                    false, false);
        }

        String actionText = actionTextSupplier.get();
        String actionLower = actionText != null
                ? actionText.toLowerCase(Locale.ROOT) : "";
        boolean takeOff = actionLower.contains("take off");
        boolean disembark = actionLower.contains("disembark");
        boolean moveAboard = actionLower.contains("embark") && !disembark;
        if (!takeOff && !disembark) {
            return new LandedShipEscape(
                    Contribution.none(), takeOff, disembark,
                    moveAboard, false);
        }

        boolean landedShipFound = false;
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card == null) {
                continue;
            }
            if (!playerId.equals(card.getOwner())) {
                continue;
            }
            if (card.getBlueprint() == null) {
                continue;
            }
            if (card.getBlueprint().getCardCategory()
                    != CardCategory.STARSHIP) {
                continue;
            }
            PhysicalCard cardLocation = null;
            try {
                cardLocation = game.getModifiersQuerying()
                        .getLocationThatCardIsAt(gameState, card);
            } catch (Exception e) {
                // Preserve V91's per-ship location failure.
            }
            if (cardLocation == location) {
                landedShipFound = true;
                break;
            }
        }

        if (!landedShipFound) {
            return new LandedShipEscape(
                    Contribution.none(), takeOff, disembark,
                    moveAboard, false);
        }

        float bonus = takeOff ? 800.0f : 600.0f;
        return new LandedShipEscape(
                new Contribution(
                        true,
                        String.format(
                                "V91 ESCAPE LANDED SHIP: %s at site %s — %s to restore ship power / use character on ground",
                                takeOff ? "Take off" : "Disembark",
                                location.getTitle(),
                                takeOff ? "lift to system"
                                        : "drop pilot to ground"),
                        bonus),
                takeOff, disembark, moveAboard, true);
    }

    public static DestinationContest destinationContest(
            GameState gameState, SwccgGame game,
            PhysicalCard source, PhysicalCard cardToMove,
            String playerId, String opponentId,
            String actionLower, Predicate<String> jediDetector,
            Consumer<PhysicalCard> uncontestedDestinationObserver) {
        PhysicalCard destination = resolveDestination(
                gameState, source, actionLower);

        if (destination == null) {
            return noneDestination();
        }

        float opponentPowerAtDestination = 0;
        try {
            opponentPowerAtDestination = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(
                            gameState, destination, opponentId,
                            false, false);
        } catch (Exception e) {
            // Preserve V34's fail-open read as empty.
        }

        if (opponentPowerAtDestination > 0) {
            return contestDestination(
                    gameState, game, cardToMove,
                    playerId, destination, opponentPowerAtDestination,
                    jediDetector, uncontestedDestinationObserver);
        }

        return emptyDestination(
                gameState, game, source,
                playerId, opponentId, destination,
                opponentPowerAtDestination);
    }

    private static DestinationContest contestDestination(
            GameState gameState, SwccgGame game,
            PhysicalCard cardToMove,
            String playerId, PhysicalCard destination,
            float opponentPowerAtDestination,
            Predicate<String> jediDetector,
            Consumer<PhysicalCard> uncontestedDestinationObserver) {
        float ourPowerAtDestination = 0;
        try {
            ourPowerAtDestination = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(
                            gameState, destination, playerId,
                            false, false);
        } catch (Exception e) {
            // Preserve V36's fail-open read as uncontested.
        }

        boolean destinationWasUncontested = ourPowerAtDestination == 0;
        float contestBonus = 250.0f;
        if (destinationWasUncontested) {
            contestBonus += 150.0f;
            uncontestedDestinationObserver.accept(destination);
        }

        boolean moverArmed = false;
        if (cardToMove != null) {
            try {
                List<PhysicalCard> attachments =
                        gameState.getAttachedCards(cardToMove);
                if (attachments != null) {
                    for (PhysicalCard attachment : attachments) {
                        if (attachment != null
                                && attachment.getBlueprint() != null
                                && attachment.getBlueprint().getCardCategory()
                                        == CardCategory.WEAPON) {
                            moverArmed = true;
                            contestBonus += 100.0f;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // Preserve V34's fail-open weapon read.
            }
        }

        boolean jediAtDestination = false;
        try {
            for (PhysicalCard card :
                    gameState.getCardsAtLocation(destination)) {
                if (card == null || playerId.equals(card.getOwner())) {
                    continue;
                }
                String cardTitle = card.getTitle() != null
                        ? card.getTitle().toLowerCase(Locale.ROOT) : "";
                if (jediDetector.test(cardTitle)) {
                    jediAtDestination = true;
                    break;
                }
            }
        } catch (Exception e) {
            // Preserve V35's fail-open destination scan.
        }

        if (jediAtDestination && cardToMove != null
                && cardToMove.getTitle() != null
                && cardToMove.getTitle().toLowerCase(Locale.ROOT)
                        .contains("vader")) {
            contestBonus += 150.0f;
        }

        Contribution contest = new Contribution(
                true,
                String.format(
                        "V34 CONTEST: Moving to %s where opponents have power %.0f%s — block their drain and fight!",
                        destination.getTitle(), opponentPowerAtDestination,
                        jediAtDestination ? " [JEDI!]" : ""),
                contestBonus);
        return new DestinationContest(
                destination, contest, opponentPowerAtDestination,
                ourPowerAtDestination, destinationWasUncontested,
                moverArmed, jediAtDestination,
                Contribution.none(), false, null, null,
                false, null);
    }

    private static DestinationContest emptyDestination(
            GameState gameState, SwccgGame game,
            PhysicalCard source,
            String playerId, String opponentId,
            PhysicalCard destination,
            float opponentPowerAtDestination) {
        boolean opponentsUncontested = false;
        String opponentUncontestedLocation = null;
        float opponentUncontestedPower = 0;
        try {
            for (PhysicalCard otherLocation :
                    gameState.getLocationsInOrder()) {
                if (otherLocation == null || otherLocation == source
                        || otherLocation == destination) {
                    continue;
                }
                float opponentPower = game.getModifiersQuerying()
                        .getTotalPowerAtLocation(
                                gameState, otherLocation, opponentId,
                                false, false);
                float ourPower = game.getModifiersQuerying()
                        .getTotalPowerAtLocation(
                                gameState, otherLocation, playerId,
                                false, false);
                if (opponentPower > 0 && ourPower == 0) {
                    opponentsUncontested = true;
                    if (opponentPower > opponentUncontestedPower) {
                        opponentUncontestedPower = opponentPower;
                        opponentUncontestedLocation =
                                otherLocation.getTitle();
                    }
                }
            }
        } catch (Exception e) {
            // Preserve the partial V38.3 scan result.
        }

        Contribution battlegroundAdvance = Contribution.none();
        boolean wrongDirectionVeto = false;
        String wrongDirectionReason = null;
        if (opponentsUncontested) {
            boolean currentNonBattleground = false;
            boolean destinationBattleground = false;
            try {
                currentNonBattleground = !game.getModifiersQuerying()
                        .isBattleground(gameState, source, null);
                destinationBattleground = game.getModifiersQuerying()
                        .isBattleground(gameState, destination, null);
            } catch (Exception e) {
                // Preserve V111's partial fail-open reads.
            }

            if (currentNonBattleground && destinationBattleground) {
                battlegroundAdvance = new Contribution(
                        true,
                        String.format(
                                "V111 BG ADVANCE: Moving from non-battleground %s to battleground %s — establish drain position!",
                                source.getTitle(), destination.getTitle()),
                        400.0f);
            } else {
                wrongDirectionVeto = true;
                wrongDirectionReason = String.format(
                        "V38.3 WRONG DIRECTION: Moving to empty %s while opponents at %s",
                        destination.getTitle(),
                        opponentUncontestedLocation);
            }
        }

        boolean castleVeto = false;
        String castleVetoReason = null;
        String destinationTitle = destination.getTitle() != null
                ? destination.getTitle().toLowerCase(Locale.ROOT) : "";
        if (destinationTitle.contains("mustafar")
                && destinationTitle.contains("castle")) {
            boolean anyOpponentsOnBoard = false;
            try {
                for (PhysicalCard otherLocation :
                        gameState.getLocationsInOrder()) {
                    if (otherLocation == null) {
                        continue;
                    }
                    float opponentPower = game.getModifiersQuerying()
                            .getTotalPowerAtLocation(
                                    gameState, otherLocation, opponentId,
                                    false, false);
                    if (opponentPower > 0) {
                        anyOpponentsOnBoard = true;
                        break;
                    }
                }
            } catch (Exception e) {
                // Preserve the partial Castle scan result.
            }
            if (anyOpponentsOnBoard) {
                castleVeto = true;
                castleVetoReason = "V38.3 CASTLE RETREAT: NEVER retreat to Castle while opponents exist!";
            }
        }

        return new DestinationContest(
                destination, Contribution.none(),
                opponentPowerAtDestination, 0.0f, false,
                false, false, battlegroundAdvance,
                wrongDirectionVeto, wrongDirectionReason,
                opponentUncontestedLocation,
                castleVeto, castleVetoReason);
    }

    private static DestinationContest noneDestination() {
        return new DestinationContest(
                null, Contribution.none(), 0.0f, 0.0f,
                false, false, false, Contribution.none(),
                false, null, null, false, null);
    }
}
