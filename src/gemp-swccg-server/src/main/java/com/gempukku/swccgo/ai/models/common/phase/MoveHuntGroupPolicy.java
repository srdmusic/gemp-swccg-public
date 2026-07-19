package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Shared V29.13 Hunt Down MOVE cohesion analysis.
 * Adapters retain objective gating, score, ladder, catch, and log ownership.
 */
public final class MoveHuntGroupPolicy {
    public enum Branch {
        NONE,
        HUNTER_TOWARD_ALLIES,
        HUNTER_AWAY_FROM_ALLIES,
        ALLY_AWAY_FROM_HUNTER,
        ALLY_TOWARD_HUNTER,
        ALLY_ELSEWHERE
    }

    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    public record Evaluation(
            Branch branch,
            Contribution contribution,
            PhysicalCard anchorLocation,
            int totalAllyCharacters,
            float bestAllyPower,
            boolean huntingOpponents) {
        private static Evaluation none() {
            return new Evaluation(
                    Branch.NONE, Contribution.none(), null,
                    0, 0.0f, false);
        }
    }

    private MoveHuntGroupPolicy() {
    }

    public static Evaluation evaluate(
            GameState gameState, SwccgGame game,
            PhysicalCard currentLocation, PhysicalCard cardToMove,
            String playerId, Supplier<String> actionTextSupplier,
            Predicate<PhysicalCard> darkJediClassifier) {
        String movingCardTitle = cardToMove.getTitle()
                .toLowerCase(Locale.ROOT);
        boolean movingCardIsHunter = titleMarksHunter(movingCardTitle);
        if (!movingCardIsHunter) {
            try {
                movingCardIsHunter = darkJediClassifier.test(cardToMove);
            } catch (Exception e) {
                // Preserve V137b's fail-open hunter classification.
            }
        }

        String actionText = actionTextSupplier.get();
        String moveActionLower = actionText != null
                ? actionText.toLowerCase(Locale.ROOT) : "";

        if (movingCardIsHunter) {
            return evaluateHunter(
                    gameState, game, currentLocation, cardToMove,
                    playerId, moveActionLower);
        }
        return evaluateAlly(
                gameState, currentLocation, cardToMove,
                playerId, moveActionLower, darkJediClassifier);
    }

    private static Evaluation evaluateHunter(
            GameState gameState, SwccgGame game,
            PhysicalCard currentLocation, PhysicalCard cardToMove,
            String playerId, String moveActionLower) {
        PhysicalCard bestAllyLocation = null;
        float bestAllyPower = 0.0f;
        int totalAllyCharacters = 0;
        for (PhysicalCard location : gameState.getTopLocations()) {
            if (location == null || location == currentLocation) {
                continue;
            }
            float allyPowerHere = 0.0f;
            int allyCountHere = 0;
            for (PhysicalCard card : gameState.getCardsAtLocation(location)) {
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
                allyCountHere++;
                Float power = card.getBlueprint().getPower();
                allyPowerHere += power != null ? power : 0.0f;
            }
            totalAllyCharacters += allyCountHere;
            if (allyPowerHere > bestAllyPower) {
                bestAllyPower = allyPowerHere;
                bestAllyLocation = location;
            }
        }

        if (totalAllyCharacters <= 0 || bestAllyLocation == null) {
            return Evaluation.none();
        }

        String bestLocationTitle = bestAllyLocation.getTitle() != null
                ? bestAllyLocation.getTitle().toLowerCase(Locale.ROOT) : "";
        boolean movingTowardAllies = !bestLocationTitle.isEmpty()
                && moveActionLower.contains(bestLocationTitle);
        if (movingTowardAllies) {
            float groupBonus = 200.0f;
            if (bestAllyPower >= 8.0f) {
                groupBonus += 50.0f;
            }
            return new Evaluation(
                    Branch.HUNTER_TOWARD_ALLIES,
                    new Contribution(
                            true,
                            String.format(
                                    "V29.13 HUNT GROUP MOVE: Vader moving TOWARD %d allies at %s (power %.0f) — group up!",
                                    totalAllyCharacters,
                                    bestAllyLocation.getTitle(),
                                    bestAllyPower),
                            groupBonus),
                    bestAllyLocation, totalAllyCharacters,
                    bestAllyPower, false);
        }

        boolean huntingOpponents = false;
        String opponentId = game.getOpponent(playerId);
        for (PhysicalCard location : gameState.getTopLocations()) {
            if (location == null || location.getTitle() == null) {
                continue;
            }
            String locationTitle = location.getTitle()
                    .toLowerCase(Locale.ROOT);
            if (!locationTitle.isEmpty()
                    && moveActionLower.contains(locationTitle)) {
                float opponentPower = 0.0f;
                try {
                    opponentPower = game.getModifiersQuerying()
                            .getTotalPowerAtLocation(
                                    gameState, location, opponentId,
                                    false, false);
                } catch (Exception e) {
                    // Preserve the first textual destination's failed read.
                }
                if (opponentPower > 0.0f) {
                    huntingOpponents = true;
                }
                break;
            }
        }
        if (huntingOpponents) {
            return new Evaluation(
                    Branch.NONE, Contribution.none(), bestAllyLocation,
                    totalAllyCharacters, bestAllyPower, true);
        }

        return new Evaluation(
                Branch.HUNTER_AWAY_FROM_ALLIES,
                new Contribution(
                        true,
                        String.format(
                                "V29.13 HUNT GROUP: Vader moving AWAY from %d allies — stay together!",
                                totalAllyCharacters),
                        -200.0f),
                bestAllyLocation, totalAllyCharacters,
                bestAllyPower, false);
    }

    private static Evaluation evaluateAlly(
            GameState gameState, PhysicalCard currentLocation,
            PhysicalCard cardToMove, String playerId,
            String moveActionLower,
            Predicate<PhysicalCard> darkJediClassifier) {
        PhysicalCard hunterLocation = null;
        for (PhysicalCard tableCard : gameState.getAllPermanentCards()) {
            if (tableCard == null
                    || !playerId.equals(tableCard.getOwner())) {
                continue;
            }
            Zone zone = tableCard.getZone();
            if (zone == null || !zone.isInPlay()) {
                continue;
            }
            if (tableCard.getBlueprint() == null
                    || tableCard.getBlueprint().getCardCategory()
                            != CardCategory.CHARACTER) {
                continue;
            }
            String title = tableCard.getTitle() != null
                    ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
            boolean hunterAnchor = titleMarksHunter(title);
            if (!hunterAnchor) {
                try {
                    hunterAnchor = darkJediClassifier.test(tableCard);
                } catch (Exception e) {
                    // Preserve V137b's fail-open anchor classification.
                }
            }
            if (hunterAnchor) {
                hunterLocation = tableCard.getAtLocation();
                break;
            }
        }

        if (hunterLocation == null || hunterLocation.getTitle() == null) {
            return Evaluation.none();
        }

        String hunterLocationTitle = hunterLocation.getTitle()
                .toLowerCase(Locale.ROOT);
        boolean currentlyWithHunter = currentLocation == hunterLocation;
        boolean movingToHunter = !hunterLocationTitle.isEmpty()
                && moveActionLower.contains(hunterLocationTitle);

        if (currentlyWithHunter && !movingToHunter) {
            return new Evaluation(
                    Branch.ALLY_AWAY_FROM_HUNTER,
                    new Contribution(
                            true,
                            String.format(
                                    "V29.13 HUNT GROUP: %s moving AWAY from Vader at %s — stay together!",
                                    cardToMove.getTitle(),
                                    hunterLocation.getTitle()),
                            -250.0f),
                    hunterLocation, 0, 0.0f, false);
        }
        if (!currentlyWithHunter && movingToHunter) {
            return new Evaluation(
                    Branch.ALLY_TOWARD_HUNTER,
                    new Contribution(
                            true,
                            String.format(
                                    "V29.13 HUNT GROUP MOVE: %s moving TOWARD Vader at %s — group up!",
                                    cardToMove.getTitle(),
                                    hunterLocation.getTitle()),
                            250.0f),
                    hunterLocation, 0, 0.0f, false);
        }
        if (!currentlyWithHunter) {
            return new Evaluation(
                    Branch.ALLY_ELSEWHERE,
                    new Contribution(
                            true,
                            String.format(
                                    "V29.13 HUNT GROUP: %s moving but NOT toward Vader at %s — group up instead!",
                                    cardToMove.getTitle(),
                                    hunterLocation.getTitle()),
                            -100.0f),
                    hunterLocation, 0, 0.0f, false);
        }
        return Evaluation.none();
    }

    private static boolean titleMarksHunter(String titleLower) {
        return titleLower.contains("vader")
                || titleLower.contains("tyranus")
                || titleLower.contains("dooku");
    }
}
