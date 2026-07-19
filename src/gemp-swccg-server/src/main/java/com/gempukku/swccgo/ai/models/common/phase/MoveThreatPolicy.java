package com.gempukku.swccgo.ai.models.common.phase;

/**
 * Shared MOVE threat classification and raw score contribution.
 * Adapters retain board scanning, ladder mutation, and logging.
 */
public final class MoveThreatPolicy {
    public enum ThreatLevel {
        CRUSH, FAVORABLE, RISKY, DANGEROUS, RETREAT
    }

    public record Evaluation(boolean applies, ThreatLevel level,
                             String reason, float delta,
                             boolean claimSurvivalRank) {
        private static Evaluation none() {
            return new Evaluation(false, null, null, 0.0f, false);
        }
    }

    private MoveThreatPolicy() {
    }

    public static Evaluation evaluate(
            float opponentPower, float powerDiff,
            int favorableThreshold, int dangerThreshold) {
        if (!(opponentPower > 0)) {
            return Evaluation.none();
        }

        if (powerDiff >= favorableThreshold + 4) {
            return new Evaluation(
                    true, ThreatLevel.CRUSH,
                    "V37.1 STAY AND CRUSH: Power +" + (int) powerDiff
                            + " — DESTROY them!",
                    -1500.0f, false);
        }
        if (powerDiff >= favorableThreshold) {
            return new Evaluation(
                    true, ThreatLevel.FAVORABLE,
                    "V37.1 STAY AND FIGHT: Power +" + (int) powerDiff
                            + " — hold position!",
                    -1500.0f, false);
        }
        if (powerDiff >= -favorableThreshold) {
            return new Evaluation(
                    true, ThreatLevel.RISKY,
                    "V37.1 CONTESTED: Even power (" + (int) powerDiff
                            + ") — hold position!",
                    -500.0f, false);
        }
        if (powerDiff >= dangerThreshold) {
            return new Evaluation(
                    true, ThreatLevel.DANGEROUS,
                    "Dangerous location - retreat recommended ("
                            + (int) powerDiff + ")",
                    20.0f, false);
        }
        return new Evaluation(
                true, ThreatLevel.RETREAT,
                "Strategic retreat - badly outmatched ("
                        + (int) powerDiff + ")",
                150.0f, true);
    }
}
