package com.gempukku.swccgo.ai.models.common.policy;

import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;

/** Shared signed ceiling for ordinary objective influence in one scored decision. */
public final class ObjectivePreferencePolicy {
    public static final float SCORE = 300.0f;

    private ObjectivePreferencePolicy() {
    }

    public static boolean isObjective(TraceDomainId domainId) {
        return domainId == TraceDomainId.OBJECTIVE_INTENT;
    }

    public static boolean isPositiveObjective(
            TraceDomainId domainId, float delta) {
        return isObjective(domainId) && delta > 0.0f;
    }

    public static float normalize(
            TraceDomainId domainId, float delta) {
        if (!isObjective(domainId)) {
            return delta;
        }
        if (delta > 0.0f) {
            return SCORE;
        }
        return Math.max(-SCORE, delta);
    }

    public static float applyWithinCeiling(
            float appliedSoFar, float requestedDelta) {
        if (requestedDelta > 0.0f
                && appliedSoFar + requestedDelta > SCORE) {
            // Positive objective signals are atomic: one normalized +300
            // preference or zero. Never leak a synthetic partial positive
            // that would be normalized back to +300 by PolicyOperation.
            return 0.0f;
        }
        float boundedTotal = Math.max(-SCORE,
                Math.min(appliedSoFar + requestedDelta, SCORE));
        return boundedTotal - appliedSoFar;
    }
}
