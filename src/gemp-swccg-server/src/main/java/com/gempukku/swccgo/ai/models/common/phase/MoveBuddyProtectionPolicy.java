package com.gempukku.swccgo.ai.models.common.phase;

/**
 * Shared V27/V59 buddy-protection and doomed-position scoring.
 * Adapters retain card collection, blueprint, power, score, and logging reads.
 */
public final class MoveBuddyProtectionPolicy {
    public enum Branch {
        NONE,
        DOOMED_ESCAPE,
        BUDDY_PROTECT
    }

    public record Evaluation(
            Branch branch,
            boolean applies,
            String reason,
            float delta,
            boolean claimSurvival,
            boolean enemyThreat) {
        private static Evaluation none() {
            return new Evaluation(
                    Branch.NONE, false, null, 0.0f, false, false);
        }
    }

    private MoveBuddyProtectionPolicy() {
    }

    public static boolean hasBuddyPair(
            int ourCharacterCount,
            boolean includesMover) {
        return ourCharacterCount == 2 && includesMover;
    }

    public static boolean needsPowerAnalysis(
            int allyPower,
            int minimumSoloPower,
            float theirPowerHere) {
        return allyPower < minimumSoloPower || theirPowerHere > 0.0f;
    }

    public static Evaluation evaluate(
            String locationTitle,
            String remainingAllyTitle,
            int allyPower,
            int minimumSoloPower,
            float ourPowerHere,
            float theirPowerHere) {
        boolean enemyThreat = theirPowerHere > 0.0f;
        if (!needsPowerAnalysis(
                allyPower, minimumSoloPower, theirPowerHere)) {
            return Evaluation.none();
        }

        boolean doomed = enemyThreat
                && (theirPowerHere >= ourPowerHere * 2.0f
                || (theirPowerHere - ourPowerHere) >= 10.0f);
        if (doomed) {
            return new Evaluation(
                    Branch.DOOMED_ESCAPE,
                    true,
                    String.format(
                            "V59 DOOMED: %s is a lost position (us %d vs enemy %d)"
                                    + " — ESCAPE the valuable character!",
                            locationTitle,
                            (int) ourPowerHere,
                            (int) theirPowerHere),
                    200.0f,
                    true,
                    true);
        }

        float buddyPenalty = -150.0f;
        if (enemyThreat && allyPower < theirPowerHere) {
            buddyPenalty = -400.0f;
        } else if (enemyThreat) {
            buddyPenalty = -250.0f;
        }
        return new Evaluation(
                Branch.BUDDY_PROTECT,
                true,
                String.format(
                        "V27 BUDDY PROTECT: Moving away leaves %s (power %d)"
                                + " ALONE at %s!%s",
                        remainingAllyTitle,
                        allyPower,
                        locationTitle,
                        enemyThreat
                                ? " ENEMY POWER=" + (int) theirPowerHere + "!"
                                : ""),
                buddyPenalty,
                false,
                enemyThreat);
    }
}
