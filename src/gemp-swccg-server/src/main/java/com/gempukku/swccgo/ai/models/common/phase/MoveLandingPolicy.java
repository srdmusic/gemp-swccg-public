package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;

import java.util.Collection;

/**
 * Shared MOVE landing classification and V49/V67f1 safety scoring.
 * Adapters retain ladder mutation and logging.
 */
public final class MoveLandingPolicy {
    public enum Route {
        HARD_VETO,
        STARFIGHTER_PENALTY,
        PASSENGER_SHIP_ALLOWED,
        GROUND_ALLOWED
    }

    public record Evaluation(Route route, String cardName,
                             boolean passengerScanRan, int actualPassengers,
                             float delta, String reason) {
    }

    private MoveLandingPolicy() {
    }

    public static Evaluation evaluate(
            String actionLower, PhysicalCard card, SwccgGame game) {
        boolean isStarfighter = false;
        boolean isStarship = false;
        boolean hasPassengers = false;
        boolean passengerScanRan = false;
        int actualOnboard = 0;
        String cardName = "unknown";

        if (card != null) {
            cardName = card.getTitle();
            SwccgCardBlueprint blueprint = card.getBlueprint();
            CardSubtype subtype = blueprint != null
                    ? blueprint.getCardSubtype() : null;
            if (subtype == CardSubtype.STARFIGHTER) {
                isStarfighter = true;
                isStarship = true;
            } else if (subtype == CardSubtype.CAPITAL
                    || subtype == CardSubtype.TRANSPORT) {
                isStarship = true;
            }

            // Preserve V67f1's ordering: only blueprint-detected non-starfighters
            // receive the actual passenger scan. Name fallbacks happen afterward.
            if (isStarship && !isStarfighter) {
                passengerScanRan = true;
                try {
                    if (game != null && card != null) {
                        Collection<PhysicalCard> aboard = Filters.filter(
                                game.getGameState().getAllPermanentCards(),
                                game,
                                Filters.and(Filters.character,
                                        Filters.aboard(card)));
                        if (aboard != null) {
                            actualOnboard = aboard.size();
                        }
                    }
                } catch (Exception e) {
                    // Preserve the legacy fail-open read as no passengers.
                }
                hasPassengers = actualOnboard > 0;
            }
        }

        // Preserve the legacy fallback order and exact keyword set.
        if (!isStarfighter && !isStarship) {
            isStarfighter = actionLower.contains("x-wing")
                    || actionLower.contains("y-wing")
                    || actionLower.contains("a-wing")
                    || actionLower.contains("b-wing")
                    || actionLower.contains("tie")
                    || actionLower.contains("starfighter");
            if (isStarfighter) {
                isStarship = true;
            }

            if (!isStarship) {
                isStarship = actionLower.contains("karrde")
                        || actionLower.contains("falcon")
                        || actionLower.contains("executor")
                        || actionLower.contains("dreadnaught")
                        || actionLower.contains("frigate")
                        || actionLower.contains("cruiser")
                        || actionLower.contains("corvette")
                        || actionLower.contains("destroyer");
            }
        }

        if (isStarship && !hasPassengers) {
            return new Evaluation(
                    Route.HARD_VETO, cardName, passengerScanRan,
                    actualOnboard, 0.0f,
                    String.format(
                            "V49 BLOCKED: Landing %s at a site with NO passengers = power 0 = instant death from overflow! NEVER land unprotected!",
                            cardName));
        } else if (isStarfighter) {
            return new Evaluation(
                    Route.STARFIGHTER_PENALTY, cardName, passengerScanRan,
                    actualOnboard, -100.0f,
                    "AVOID: Landing starfighter (" + cardName
                            + ") wastes combat power!");
        } else if (isStarship && hasPassengers) {
            return new Evaluation(
                    Route.PASSENGER_SHIP_ALLOWED, cardName,
                    passengerScanRan, actualOnboard, 10.0f,
                    String.format(
                            "V49: Landing %s with %s passengers aboard — can disembark to protect",
                            cardName, ""));
        }

        return new Evaluation(
                Route.GROUND_ALLOWED, cardName, passengerScanRan,
                actualOnboard, 10.0f, "Land (ground deployment)");
    }
}
