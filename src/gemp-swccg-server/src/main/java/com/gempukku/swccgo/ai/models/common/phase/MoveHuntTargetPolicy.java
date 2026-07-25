package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Shared V29.12/V35 MOVE target search for armed Hunt Down hunters.
 * Adapters retain objective, score, ladder, and log ownership.
 */
public final class MoveHuntTargetPolicy {
    public enum Branch {
        NONE,
        JEDI,
        GENERIC
    }

    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    public record Evaluation(
            Branch branch,
            Contribution contribution,
            boolean hunter,
            boolean armed,
            float opponentPowerAtCurrentLocation,
            String currentLocationName,
            String targetLocation,
            float targetPower) {
        private static Evaluation none(
                boolean hunter, boolean armed,
                float opponentPowerAtCurrentLocation) {
            return new Evaluation(
                    Branch.NONE, Contribution.none(), hunter, armed,
                    opponentPowerAtCurrentLocation, null, null, 0.0f);
        }
    }

    private MoveHuntTargetPolicy() {
    }

    public static Evaluation evaluate(
            GameState gameState, SwccgGame game,
            PhysicalCard currentLocation, PhysicalCard cardToMove,
            String playerId, BooleanSupplier huntDownGate,
            Predicate<PhysicalCard> darkJediClassifier,
            Predicate<PhysicalCard> blockerLocationClassifier,
            float jediBonus) {
        boolean hunter = false;
        if (cardToMove != null) {
            try {
                hunter = darkJediClassifier.test(cardToMove);
            } catch (Exception e) {
                // Preserve V137b's fail-open hunter classification.
            }
        }

        if (!huntDownGate.getAsBoolean() || !hunter) {
            return Evaluation.none(hunter, false, 0.0f);
        }

        String opponentId = game.getOpponent(playerId);
        float opponentPowerAtCurrentLocation = 0.0f;
        try {
            opponentPowerAtCurrentLocation = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(
                            gameState, currentLocation, opponentId,
                            false, false);
        } catch (Exception e) {
            // Preserve V29.12's fail-open current-location power read.
        }

        boolean armed = false;
        try {
            List<PhysicalCard> attachments =
                    gameState.getAttachedCards(cardToMove);
            if (attachments != null) {
                for (PhysicalCard attachment : attachments) {
                    if (attachment != null
                            && attachment.getBlueprint() != null
                            && attachment.getBlueprint().getCardCategory()
                                    == CardCategory.WEAPON) {
                        armed = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Preserve V29.12's fail-open attachment scan.
        }

        if (!armed || opponentPowerAtCurrentLocation != 0.0f) {
            return Evaluation.none(
                    hunter, armed, opponentPowerAtCurrentLocation);
        }

        boolean opponentsElsewhere = false;
        String bestTargetLocation = null;
        float bestTargetPower = 0.0f;
        String bestJediLocation = null;
        float bestJediPower = 0.0f;
        try {
            for (PhysicalCard location : gameState.getTopLocations()) {
                if (location == null || location == currentLocation) {
                    continue;
                }
                float opponentPower = game.getModifiersQuerying()
                        .getTotalPowerAtLocation(
                                gameState, location, opponentId,
                                false, false);
                if (opponentPower > 0.0f) {
                    opponentsElsewhere = true;
                    if (opponentPower > bestTargetPower) {
                        bestTargetPower = opponentPower;
                        bestTargetLocation = location.getTitle();
                    }
                    if (blockerLocationClassifier.test(location)
                            && opponentPower > bestJediPower) {
                        bestJediPower = opponentPower;
                        bestJediLocation = location.getTitle();
                    }
                }
            }
        } catch (Exception e) {
            // Preserve V35's partial-result fail-open target scan.
        }

        if (!opponentsElsewhere) {
            return Evaluation.none(
                    hunter, armed, opponentPowerAtCurrentLocation);
        }

        boolean jediTarget = bestJediLocation != null;
        String targetLocation = jediTarget
                ? bestJediLocation : bestTargetLocation;
        float targetPower = jediTarget
                ? bestJediPower : bestTargetPower;
        float bonus = jediTarget ? jediBonus : 200.0f;
        String currentLocationName = currentLocation.getTitle() != null
                ? currentLocation.getTitle() : "current location";
        String branchName = jediTarget ? "JEDI" : "DOWN";
        return new Evaluation(
                jediTarget ? Branch.JEDI : Branch.GENERIC,
                new Contribution(
                        true,
                        String.format(
                                "V35 HUNT %s: Armed Vader at %s — GO HUNT! Target: %s (power %.0f)",
                                branchName, currentLocationName,
                                targetLocation != null ? targetLocation : "?",
                                targetPower),
                        bonus),
                hunter, armed, opponentPowerAtCurrentLocation,
                currentLocationName, targetLocation, targetPower);
    }
}
