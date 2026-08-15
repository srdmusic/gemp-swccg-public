package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Shared V22.5 pre-flip and V22.2 post-flip MOVE consolidation.
 * Adapters retain objective gating, score, ladder, and log ownership.
 */
public final class MoveObjectiveConsolidationPolicy {
    public enum Branch {
        NONE,
        PRE_FLIP_LONE_OUTGUNNED,
        PRE_FLIP_SMALL_GROUP_OUTGUNNED,
        POST_FLIP_STRONG_CAN_MOVE,
        POST_FLIP_STAY,
        POST_FLIP_LONE_REINFORCE,
        POST_FLIP_SEVERE_REINFORCE
    }

    public record Contribution(
            boolean applies, String reason, float delta,
            boolean claimDoctrineRank) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f, false);
        }
    }

    public record Evaluation(
            Branch branch,
            Contribution contribution,
            String currentLocationName,
            int ownPowerCardCount,
            float ownPower,
            float opponentPowerAtCurrentLocation,
            String bestAllyLocation,
            float opponentTotalPower,
            boolean atProtectionLocation,
            float worstProtectionDeficit,
            String weakestProtectionLocation) {
        private static Evaluation none(
                String currentLocationName,
                int ownPowerCardCount, float ownPower,
                float opponentPowerAtCurrentLocation,
                String bestAllyLocation,
                float opponentTotalPower,
                boolean atProtectionLocation,
                float worstProtectionDeficit,
                String weakestProtectionLocation) {
            return new Evaluation(
                    Branch.NONE, Contribution.none(),
                    currentLocationName, ownPowerCardCount, ownPower,
                    opponentPowerAtCurrentLocation, bestAllyLocation,
                    opponentTotalPower, atProtectionLocation,
                    worstProtectionDeficit, weakestProtectionLocation);
        }
    }

    private MoveObjectiveConsolidationPolicy() {
    }

    public static Contribution cloudCityDestination(
            boolean bespinPresenceObjective,
            boolean objectiveRelevantDestination,
            float opponentPower,
            float ownPower) {
        if (!bespinPresenceObjective
                || !objectiveRelevantDestination
                || opponentPower != 0.0f) {
            return Contribution.none();
        }
        if (ownPower == 0.0f) {
            return new Contribution(
                    true,
                    "V24.9: Unoccupied CC site — free force drain if we move here!",
                    200.0f,
                    false);
        }
        if (ownPower > 0.0f) {
            return new Contribution(
                    true,
                    "V24.9: CC site with only our presence — already draining",
                    20.0f,
                    false);
        }
        return Contribution.none();
    }

    public static Contribution hiddenPathSplit(
            boolean hiddenPathPreFlip,
            boolean nonMapuzoDestination,
            boolean advancesDistinctHoldSite,
            int friendlyJediAtDestination,
            String destinationTitle) {
        if (!hiddenPathPreFlip
                || !nonMapuzoDestination) {
            return Contribution.none();
        }
        if (friendlyJediAtDestination >= 1) {
            return new Contribution(
                    true,
                    "V62 SPLIT SITE: Already have " + friendlyJediAtDestination
                            + " Jedi at " + destinationTitle
                            + ": move 2nd Jedi to a DIFFERENT non-Mapuzo site to flip Hidden Path!",
                    -300.0f,
                    false);
        }
        if (!advancesDistinctHoldSite) {
            return Contribution.none();
        }
        return new Contribution(
                true,
                "V62 SPLIT SITE: No friendly Jedi at " + destinationTitle
                        + " yet, great non-Mapuzo split-site target for Hidden Path flip!",
                200.0f,
                false);
    }

    public static Evaluation preFlip(
            GameState gameState, SwccgGame game,
            PhysicalCard currentLocation, String playerId) {
        String currentLocationName = currentLocation.getTitle();
        String opponentId = game.getOpponent(playerId);
        int ownPowerCardCount = 0;
        float ownPower = 0.0f;
        for (PhysicalCard card :
                gameState.getCardsAtLocation(currentLocation)) {
            if (card != null && playerId.equals(card.getOwner())
                    && card.getBlueprint() != null
                    && card.getBlueprint().hasPowerAttribute()) {
                ownPowerCardCount++;
                Float power = card.getBlueprint().getPower();
                ownPower += power != null ? power : 0.0f;
            }
        }

        float opponentPower = 0.0f;
        try {
            opponentPower = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(
                            gameState, currentLocation, opponentId,
                            false, false);
        } catch (Exception e) {
            // Preserve V22.5's fail-open opponent-power read.
        }

        if (ownPowerCardCount == 1
                && opponentPower > ownPower * 2.0f
                && opponentPower > 6.0f) {
            String bestAllyLocation = null;
            float bestAllyPower = 0.0f;
            try {
                for (PhysicalCard location :
                        gameState.getLocationsInOrder()) {
                    if (location == null || location == currentLocation) {
                        continue;
                    }
                    float allyPower = game.getModifiersQuerying()
                            .getTotalPowerAtLocation(
                                    gameState, location, playerId,
                                    false, false);
                    if (allyPower > bestAllyPower) {
                        bestAllyPower = allyPower;
                        bestAllyLocation = location.getTitle();
                    }
                }
            } catch (Exception e) {
                // Preserve V22.5's partial-result ally scan.
            }

            float bonus = opponentPower > ownPower * 3.0f
                    ? 160.0f : 100.0f;
            String reason = "V22.5 PRE-FLIP: LONE & OUTGUNNED ("
                    + (int) ownPower + " vs " + (int) opponentPower
                    + ") - move to join allies"
                    + (bestAllyLocation != null
                            ? " at " + bestAllyLocation : "");
            return new Evaluation(
                    Branch.PRE_FLIP_LONE_OUTGUNNED,
                    new Contribution(true, reason, bonus, true),
                    currentLocationName, ownPowerCardCount, ownPower,
                    opponentPower, bestAllyLocation,
                    0.0f, false, 0.0f, null);
        }

        if (ownPowerCardCount <= 2
                && opponentPower > ownPower * 1.5f
                && opponentPower > 8.0f) {
            String reason = "V22.5 PRE-FLIP: Outgunned at "
                    + currentLocationName + " (" + (int) ownPower
                    + " vs " + (int) opponentPower + ")";
            return new Evaluation(
                    Branch.PRE_FLIP_SMALL_GROUP_OUTGUNNED,
                    new Contribution(true, reason, 60.0f, false),
                    currentLocationName, ownPowerCardCount, ownPower,
                    opponentPower, null,
                    0.0f, false, 0.0f, null);
        }

        return Evaluation.none(
                currentLocationName, ownPowerCardCount, ownPower,
                opponentPower, null, 0.0f, false, 0.0f, null);
    }

    public static Evaluation postFlip(
            GameState gameState, SwccgGame game,
            PhysicalCard currentLocation, String playerId,
            Predicate<String> protectionLocation,
            Consumer<Exception> opponentPowerFailureObserver,
            Consumer<Exception> protectionFailureObserver) {
        return postFlipPhysical(
                gameState, game, currentLocation, playerId,
                location -> location != null
                        && protectionLocation.test(location.getTitle()),
                opponentPowerFailureObserver,
                protectionFailureObserver);
    }

    public static Evaluation postFlipPhysical(
            GameState gameState, SwccgGame game,
            PhysicalCard currentLocation, String playerId,
            Predicate<PhysicalCard> protectionLocation,
            Consumer<Exception> opponentPowerFailureObserver,
            Consumer<Exception> protectionFailureObserver) {
        String currentLocationName = currentLocation.getTitle();
        boolean atProtectionLocation =
                protectionLocation.test(currentLocation);
        String opponentId = game.getOpponent(playerId);

        int ownPowerCardCount = 0;
        float ownPower = 0.0f;
        for (PhysicalCard card :
                gameState.getCardsAtLocation(currentLocation)) {
            if (card != null && playerId.equals(card.getOwner())
                    && card.getBlueprint() != null
                    && card.getBlueprint().hasPowerAttribute()) {
                ownPowerCardCount++;
                Float power = card.getBlueprint().getPower();
                ownPower += power != null ? power : 0.0f;
            }
        }

        float opponentTotalPower = 0.0f;
        try {
            for (PhysicalCard location :
                    gameState.getLocationsInOrder()) {
                if (location != null) {
                    opponentTotalPower += game.getModifiersQuerying()
                            .getTotalPowerAtLocation(
                                    gameState, location, opponentId,
                                    false, false);
                }
            }
        } catch (Exception e) {
            opponentPowerFailureObserver.accept(e);
        }

        float worstProtectionDeficit = 0.0f;
        String weakestProtectionLocation = null;
        try {
            for (PhysicalCard location :
                    gameState.getLocationsInOrder()) {
                if (location == null || location.getTitle() == null) {
                    continue;
                }
                if (!protectionLocation.test(location)) {
                    continue;
                }
                float ourPowerAtLocation = game.getModifiersQuerying()
                        .getTotalPowerAtLocation(
                                gameState, location, playerId,
                                false, false);
                float theirPowerAtLocation = game.getModifiersQuerying()
                        .getTotalPowerAtLocation(
                                gameState, location, opponentId,
                                false, false);
                float deficit = (theirPowerAtLocation + 4.0f)
                        - ourPowerAtLocation;
                if (deficit > worstProtectionDeficit) {
                    worstProtectionDeficit = deficit;
                    weakestProtectionLocation = location.getTitle();
                }
            }
        } catch (Exception e) {
            protectionFailureObserver.accept(e);
        }

        if (atProtectionLocation) {
            if (ownPowerCardCount >= 3 && ownPower > 12.0f) {
                return postFlipEvaluation(
                        Branch.POST_FLIP_STRONG_CAN_MOVE,
                        "V22.2 POST-FLIP: Strong at protection loc - can move",
                        -30.0f, false,
                        currentLocationName, ownPowerCardCount, ownPower,
                        opponentTotalPower, true,
                        worstProtectionDeficit,
                        weakestProtectionLocation);
            }

            float stayPenalty = -80.0f;
            if (opponentTotalPower > 15.0f) {
                stayPenalty = -120.0f;
            }
            if (opponentTotalPower > 25.0f) {
                stayPenalty = -160.0f;
            }
            return postFlipEvaluation(
                    Branch.POST_FLIP_STAY,
                    "V22.2 POST-FLIP: STAY at protection location! Opponent power="
                            + (int) opponentTotalPower,
                    stayPenalty, false,
                    currentLocationName, ownPowerCardCount, ownPower,
                    opponentTotalPower, true,
                    worstProtectionDeficit,
                    weakestProtectionLocation);
        }

        if (ownPowerCardCount == 1) {
            float moveBonus = 80.0f;
            if (worstProtectionDeficit > 4.0f) {
                moveBonus = 120.0f;
            }
            if (worstProtectionDeficit > 8.0f) {
                moveBonus = 160.0f;
            }
            return postFlipEvaluation(
                    Branch.POST_FLIP_LONE_REINFORCE,
                    "V22.2 POST-FLIP: Lone char should reinforce "
                            + (weakestProtectionLocation != null
                                    ? weakestProtectionLocation
                                    : "protection locs"),
                    moveBonus, true,
                    currentLocationName, ownPowerCardCount, ownPower,
                    opponentTotalPower, false,
                    worstProtectionDeficit,
                    weakestProtectionLocation);
        }

        if (worstProtectionDeficit > 6.0f) {
            return postFlipEvaluation(
                    Branch.POST_FLIP_SEVERE_REINFORCE,
                    "V22.2 POST-FLIP: Protection locations severely under-guarded!",
                    60.0f, false,
                    currentLocationName, ownPowerCardCount, ownPower,
                    opponentTotalPower, false,
                    worstProtectionDeficit,
                    weakestProtectionLocation);
        }

        return Evaluation.none(
                currentLocationName, ownPowerCardCount, ownPower,
                0.0f, null, opponentTotalPower, false,
                worstProtectionDeficit, weakestProtectionLocation);
    }

    private static Evaluation postFlipEvaluation(
            Branch branch, String reason, float delta,
            boolean claimDoctrineRank,
            String currentLocationName,
            int ownPowerCardCount, float ownPower,
            float opponentTotalPower,
            boolean atProtectionLocation,
            float worstProtectionDeficit,
            String weakestProtectionLocation) {
        return new Evaluation(
                branch,
                new Contribution(
                        true, reason, delta, claimDoctrineRank),
                currentLocationName, ownPowerCardCount, ownPower,
                0.0f, null, opponentTotalPower,
                atProtectionLocation, worstProtectionDeficit,
                weakestProtectionLocation);
    }
}
