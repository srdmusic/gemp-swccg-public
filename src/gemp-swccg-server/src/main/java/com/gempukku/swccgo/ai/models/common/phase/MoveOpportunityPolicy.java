package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;

/**
 * Shared MOVE attack and spread opportunity analysis.
 * Preserves the legacy all-location scan; adapters still own score application
 * and ladder claims.
 */
public final class MoveOpportunityPolicy {
    private static final int ESTABLISH_THRESHOLD = 6;
    private static final int CONTEST_MARGIN = 4;
    private static final int ATTACK_POWER_ADVANTAGE = 4;
    private static final float ICON_BONUS = 15.0f;
    private static final float GOOD_DELTA = 10.0f;

    public record Contribution(boolean applies, String reason, float delta) {
        private static Contribution none() {
            return new Contribution(false, null, 0.0f);
        }
    }

    private MoveOpportunityPolicy() {
    }

    public static AttackAnalysis attack(
            GameState gameState, String playerId, Side mySide,
            PhysicalCard currentLocation, float ourPowerHere) {
        String opponentId = gameState.getOpponent(playerId);
        AttackAnalysis bestAttack = null;
        float bestScore = 0;

        for (PhysicalCard targetLocation : gameState.getLocationsInOrder()) {
            if (targetLocation == currentLocation) {
                continue;
            }

            float theirPower = 0;
            int theirCount = 0;
            float ourPowerThere = 0;

            List<PhysicalCard> cardsAtTarget =
                    gameState.getCardsAtLocation(targetLocation);
            for (PhysicalCard card : cardsAtTarget) {
                if (card == null) {
                    continue;
                }
                String owner = card.getOwner();
                SwccgCardBlueprint blueprint = card.getBlueprint();
                if (blueprint == null || !blueprint.hasPowerAttribute()) {
                    continue;
                }

                Float power = blueprint.getPower();
                if (power == null) {
                    power = 0f;
                }

                if (opponentId != null && opponentId.equals(owner)) {
                    // Attack ignores undercover spies; spread intentionally does not.
                    if (card.isUndercover()) {
                        continue;
                    }
                    theirPower += power;
                    theirCount++;
                } else if (playerId.equals(owner)) {
                    ourPowerThere += power;
                }
            }

            if (theirCount == 0 || theirPower == 0) {
                continue;
            }

            int theirIcons = opponentIcons(targetLocation.getBlueprint(), mySide);
            float potentialPower = ourPowerThere + ourPowerHere;
            float advantage = potentialPower - theirPower;

            if (advantage >= ATTACK_POWER_ADVANTAGE) {
                float score = 50.0f;
                if (potentialPower >= theirPower * 2) {
                    score += 25.0f;
                }
                score += theirIcons * ICON_BONUS;
                score += theirPower / 2;

                String reason = String.format(
                        "ATTACK %d enemies with %d power (+%d advantage)",
                        (int) theirPower, (int) potentialPower, (int) advantage);
                if (theirIcons > 0) {
                    reason += " - deny " + theirIcons + " icon drain!";
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestAttack = new AttackAnalysis(true, reason, score,
                            theirIcons > 0, targetLocation);
                }
            }
        }

        return bestAttack;
    }

    public static Contribution attackContribution(AttackAnalysis attack) {
        if (attack == null || !attack.viable) {
            return Contribution.none();
        }
        if (attack.hasForcedrainPotential) {
            return new Contribution(true, attack.reason, attack.score);
        }
        return new Contribution(
                true,
                "Possible attack (no drain icons)",
                15.0f);
    }

    public static SpreadAnalysis spread(
            GameState gameState, String playerId, Side mySide,
            PhysicalCard currentLocation, float ourPowerHere,
            float theirPowerHere) {
        String opponentId = gameState.getOpponent(playerId);
        float powerToRetain = Math.max(
                theirPowerHere + CONTEST_MARGIN, ESTABLISH_THRESHOLD);
        float powerWeCanSpare = ourPowerHere - powerToRetain;

        if (powerWeCanSpare < 2) {
            return new SpreadAnalysis(false, String.format(
                    "need %d power to retain control, only have %d",
                    (int) powerToRetain, (int) ourPowerHere), 0);
        }

        SpreadAnalysis bestOpportunity = null;
        float bestScore = 0;

        for (PhysicalCard targetLocation : gameState.getLocationsInOrder()) {
            if (targetLocation == currentLocation) {
                continue;
            }

            float theirPower = 0;
            float ourPowerThere = 0;

            List<PhysicalCard> cardsAtTarget =
                    gameState.getCardsAtLocation(targetLocation);
            for (PhysicalCard card : cardsAtTarget) {
                if (card == null) {
                    continue;
                }
                String owner = card.getOwner();
                SwccgCardBlueprint blueprint = card.getBlueprint();
                if (blueprint == null || !blueprint.hasPowerAttribute()) {
                    continue;
                }

                Float power = blueprint.getPower();
                if (power == null) {
                    power = 0f;
                }

                if (opponentId != null && opponentId.equals(owner)) {
                    theirPower += power;
                } else if (playerId.equals(owner)) {
                    ourPowerThere += power;
                }
            }

            if (ourPowerThere >= ESTABLISH_THRESHOLD && theirPower == 0) {
                continue;
            }

            int theirIcons = opponentIcons(targetLocation.getBlueprint(), mySide);
            // Preserve the legacy own-icon read even though only opponent icons affect score.
            int myIcons = ownIcons(targetLocation.getBlueprint(), mySide);
            float potentialPower = ourPowerThere + powerWeCanSpare;

            if (theirPower == 0) {
                if (potentialPower >= ESTABLISH_THRESHOLD) {
                    float score = GOOD_DELTA * 2;
                    score += theirIcons * ICON_BONUS;

                    String reason = "Can establish at empty location";
                    if (theirIcons > 0) {
                        reason += " - " + theirIcons
                                + " opponent icon(s) = force drain!";
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestOpportunity =
                                new SpreadAnalysis(true, reason, score);
                    }
                }
            } else {
                float powerNeeded = theirPower + CONTEST_MARGIN;
                if (potentialPower >= powerNeeded) {
                    float score = GOOD_DELTA * 3 + theirPower / 2;
                    score += theirIcons * ICON_BONUS;

                    String reason = String.format(
                            "Can contest location with %d enemies",
                            (int) theirPower);
                    if (theirIcons > 0) {
                        reason += " - " + theirIcons
                                + " opponent icon(s) = force drain!";
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestOpportunity =
                                new SpreadAnalysis(true, reason, score);
                    }
                }
            }
        }

        return bestOpportunity != null
                ? bestOpportunity
                : new SpreadAnalysis(false, "no good adjacent locations", 0);
    }

    public static Contribution spreadContribution(SpreadAnalysis spread) {
        if (spread == null) {
            return Contribution.none();
        }
        if (spread.viable) {
            return new Contribution(true, spread.reason, spread.score);
        }
        return new Contribution(
                true,
                "Can't spread: " + spread.reason,
                -10.0f);
    }

    private static int opponentIcons(SwccgCardBlueprint blueprint, Side mySide) {
        if (blueprint == null) {
            return 0;
        }
        return mySide == Side.LIGHT
                ? blueprint.getIconCount(Icon.DARK_FORCE)
                : blueprint.getIconCount(Icon.LIGHT_FORCE);
    }

    private static int ownIcons(SwccgCardBlueprint blueprint, Side mySide) {
        if (blueprint == null) {
            return 0;
        }
        return mySide == Side.LIGHT
                ? blueprint.getIconCount(Icon.LIGHT_FORCE)
                : blueprint.getIconCount(Icon.DARK_FORCE);
    }

    public static final class AttackAnalysis {
        public final boolean viable;
        public final String reason;
        public final float score;
        public final boolean hasForcedrainPotential;
        public final PhysicalCard targetLocation;

        private AttackAnalysis(boolean viable, String reason, float score,
                               boolean hasForcedrainPotential,
                               PhysicalCard targetLocation) {
            this.viable = viable;
            this.reason = reason;
            this.score = score;
            this.hasForcedrainPotential = hasForcedrainPotential;
            this.targetLocation = targetLocation;
        }
    }

    public static final class SpreadAnalysis {
        public final boolean viable;
        public final String reason;
        public final float score;

        private SpreadAnalysis(boolean viable, String reason, float score) {
            this.viable = viable;
            this.reason = reason;
            this.score = score;
        }
    }
}
