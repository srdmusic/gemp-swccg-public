package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.Locale;

/**
 * Shared V53 undercover-spy MOVE positioning.
 * Adapters retain undercover gating, score, ladder, catch, and log ownership.
 */
public final class MoveSpyFollowPolicy {
    public enum Branch {
        NONE,
        FOLLOW,
        STAY,
        REPOSITION
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
            float opponentPowerAtSource,
            boolean destinationHasOpponent,
            PhysicalCard destination) {
        private static Evaluation none(
                float opponentPowerAtSource,
                boolean destinationHasOpponent,
                PhysicalCard destination) {
            return new Evaluation(
                    Branch.NONE, Contribution.none(),
                    opponentPowerAtSource,
                    destinationHasOpponent, destination);
        }
    }

    private MoveSpyFollowPolicy() {
    }

    public static Evaluation evaluate(
            GameState gameState, SwccgGame game,
            PhysicalCard cardToMove, String playerId,
            String actionLower) {
        String opponentId = game.getOpponent(playerId);
        PhysicalCard sourceLocation = cardToMove.getAtLocation();

        float opponentPowerAtSource = 0.0f;
        if (sourceLocation != null) {
            opponentPowerAtSource = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(
                            gameState, sourceLocation, opponentId,
                            false, false);
        }

        boolean destinationHasOpponent = false;
        PhysicalCard destination = null;
        for (PhysicalCard location : gameState.getTopLocations()) {
            if (location == null || location.getTitle() == null) {
                continue;
            }
            String destinationTitle = location.getTitle()
                    .toLowerCase(Locale.ROOT);
            if (!actionLower.contains(destinationTitle)) {
                continue;
            }
            float opponentPowerAtDestination =
                    game.getModifiersQuerying().getTotalPowerAtLocation(
                            gameState, location, opponentId,
                            false, false);
            if (opponentPowerAtDestination > 0.0f) {
                destinationHasOpponent = true;
            }
            destination = location;
            break;
        }

        if (opponentPowerAtSource == 0.0f
                && destinationHasOpponent) {
            return new Evaluation(
                    Branch.FOLLOW,
                    new Contribution(
                            true,
                            "V53 SPY FOLLOW: Opponent moved away — follow them to keep reducing drain!",
                            500.0f, true),
                    opponentPowerAtSource,
                    destinationHasOpponent, destination);
        } else if (opponentPowerAtSource > 0.0f
                && !destinationHasOpponent) {
            return new Evaluation(
                    Branch.STAY,
                    new Contribution(
                            true,
                            "V53 SPY STAY: Opponent is HERE — don't leave, keep reducing their drain!",
                            -300.0f, false),
                    opponentPowerAtSource,
                    destinationHasOpponent, destination);
        } else if (destinationHasOpponent
                && opponentPowerAtSource == 0.0f) {
            // Historical V53 arm is unreachable behind the identical FOLLOW condition.
            return new Evaluation(
                    Branch.REPOSITION,
                    new Contribution(
                            true,
                            "V53 SPY REPOSITION: Move spy to opponent location — start reducing drain!",
                            400.0f, true),
                    opponentPowerAtSource,
                    destinationHasOpponent, destination);
        }

        return Evaluation.none(
                opponentPowerAtSource,
                destinationHasOpponent, destination);
    }
}
