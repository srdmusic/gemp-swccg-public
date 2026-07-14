package com.gempukku.swccgo.ai.models.common.phase;

/** One frozen result from the legacy battle predictor. */
public record BattlePredictionAssessment(
        boolean known,
        float winProbability,
        float expectedDamageDealt,
        float expectedDamageTaken) {

    public BattlePredictionAssessment {
        if (known && (!Float.isFinite(winProbability)
                || !Float.isFinite(expectedDamageDealt)
                || !Float.isFinite(expectedDamageTaken))) {
            throw new IllegalArgumentException("known prediction values must be finite");
        }
    }

    public static BattlePredictionAssessment unknown() {
        return new BattlePredictionAssessment(false, 0f, 0f, 0f);
    }
}
