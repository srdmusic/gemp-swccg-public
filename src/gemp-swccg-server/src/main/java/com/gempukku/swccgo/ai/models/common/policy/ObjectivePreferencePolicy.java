package com.gempukku.swccgo.ai.models.common.policy;

import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;

/** Shared strongest-tier policy for objective influence in one scored decision. */
public final class ObjectivePreferencePolicy {
    public static final float SCORE = 300.0f;
    public static final float EXACT_PROGRESS_SCORE = 1000.0f;

    public enum Strength {
        NONE(0.0f),
        ORDINARY(SCORE),
        EXACT_PROGRESS(EXACT_PROGRESS_SCORE);

        private final float positiveCeiling;

        Strength(float positiveCeiling) {
            this.positiveCeiling = positiveCeiling;
        }

        private float positiveCeiling() {
            return positiveCeiling;
        }
    }

    public record State(float strongestPositive, float boundedNegative) {
        public static State empty() {
            return new State(0.0f, 0.0f);
        }

        public float net() {
            return strongestPositive + boundedNegative;
        }
    }

    public record Application(State state, float delta) {
    }

    private ObjectivePreferencePolicy() {
    }

    public static boolean isObjective(TraceDomainId domainId) {
        return domainId == TraceDomainId.OBJECTIVE_INTENT;
    }

    public static boolean isPositiveObjective(
            TraceDomainId domainId, float delta) {
        return isObjective(domainId) && delta > 0.0f;
    }

    public static Strength defaultStrength(TraceDomainId domainId) {
        return isObjective(domainId) ? Strength.ORDINARY : Strength.NONE;
    }

    public static float normalize(
            TraceDomainId domainId, float delta) {
        return normalize(domainId, defaultStrength(domainId), delta);
    }

    public static float normalize(
            TraceDomainId domainId, Strength strength, float delta) {
        if (!isObjective(domainId)) {
            return delta;
        }
        if (delta > 0.0f) {
            Strength effective = strength == null || strength == Strength.NONE
                    ? Strength.ORDINARY : strength;
            return effective.positiveCeiling();
        }
        return Math.max(-SCORE, delta);
    }

    public static Application apply(
            State state, Strength strength, float requestedDelta) {
        State current = state != null ? state : State.empty();
        if (requestedDelta > 0.0f) {
            Strength effective = strength == null || strength == Strength.NONE
                    ? Strength.ORDINARY : strength;
            float nextPositive = Math.max(
                    current.strongestPositive(),
                    effective.positiveCeiling());
            return new Application(
                    new State(nextPositive, current.boundedNegative()),
                    nextPositive - current.strongestPositive());
        }
        if (requestedDelta < 0.0f) {
            float nextNegative = Math.max(
                    -SCORE,
                    current.boundedNegative() + requestedDelta);
            return new Application(
                    new State(current.strongestPositive(), nextNegative),
                    nextNegative - current.boundedNegative());
        }
        return new Application(current, 0.0f);
    }

    public static State merge(State left, State right) {
        State first = left != null ? left : State.empty();
        State second = right != null ? right : State.empty();
        return new State(
                Math.max(first.strongestPositive(),
                        second.strongestPositive()),
                Math.max(-SCORE,
                        first.boundedNegative()
                                + second.boundedNegative()));
    }

}
