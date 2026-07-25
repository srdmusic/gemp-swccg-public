package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Shared V31 post-flip objective-location consolidation scoring.
 * Adapters retain objective, location, power, score, and logging reads.
 */
public final class MovePostFlipConsolidationPolicy {
    public record Evaluation(
            boolean applies,
            String weakestLocationTitle,
            float weakestPower,
            String reason,
            float delta,
            boolean claimDoctrine) {
        private static Evaluation none() {
            return new Evaluation(false, null, 0.0f, null, 0.0f, false);
        }
    }

    private MovePostFlipConsolidationPolicy() {
    }

    public static boolean isObjectiveLocation(
            String locationTitle,
            Set<String> objectiveFragments) {
        if (locationTitle == null) {
            return false;
        }
        String lower = locationTitle.toLowerCase(Locale.ROOT);
        for (String fragment : objectiveFragments) {
            if (lower.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static Evaluation evaluate(
            String currentLocationTitle,
            boolean atObjectiveLocation,
            Map<String, Float> occupiedObjectivePower) {
        return evaluate(
                currentLocationTitle,
                atObjectiveLocation,
                false,
                occupiedObjectivePower);
    }

    public static Evaluation evaluate(
            String currentLocationTitle,
            boolean atObjectiveLocation,
            boolean currentLocationMustBeHeld,
            Map<String, Float> occupiedObjectivePower) {
        if (currentLocationMustBeHeld
                || occupiedObjectivePower.size() < 3
                || !atObjectiveLocation) {
            return Evaluation.none();
        }

        String weakestLocation = null;
        float weakestPower = Float.MAX_VALUE;
        for (Map.Entry<String, Float> entry
                : occupiedObjectivePower.entrySet()) {
            if (entry.getValue() < weakestPower) {
                weakestPower = entry.getValue();
                weakestLocation = entry.getKey();
            }
        }
        if (weakestLocation == null
                || !currentLocationTitle.equals(weakestLocation)) {
            return Evaluation.none();
        }

        return new Evaluation(
                true,
                weakestLocation,
                weakestPower,
                String.format(
                        "V31 POST-FLIP CONSOLIDATE: At weakest obj loc %s"
                                + " (power %.0f) — move to reinforce stronger position!",
                        weakestLocation,
                        weakestPower),
                200.0f,
                true);
    }
}
