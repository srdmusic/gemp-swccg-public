package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;

import java.util.List;
import java.util.Objects;

/**
 * Typed BATTLE-1 retention boundary.
 *
 * The public initiation decision currently has predictor averages, not exact
 * simultaneous battle-damage, attrition, forfeiture, immunity, dependency,
 * weapon, response, or lethal facts. Those averages are telemetry only. Until
 * an authoritative exact-facts producer exists, this policy is deliberately
 * score-neutral.
 */
public final class BattleRetentionPolicy {
    private static final String PRODUCER = "B3_BATTLE_RETENTION_POLICY";

    public enum Knowledge {
        RAW_PREDICTOR_ONLY,
        UNKNOWN
    }

    public enum Assessment {
        UNKNOWN
    }

    public record PredictionTelemetry(
            float winProbability,
            float expectedDamageDealt,
            float expectedDamageTaken,
            float expectedMyBattleDestiny,
            float expectedOpponentBattleDestiny) {

        public boolean complete() {
            return Float.isFinite(winProbability)
                    && Float.isFinite(expectedDamageDealt)
                    && Float.isFinite(expectedDamageTaken)
                    && Float.isFinite(expectedMyBattleDestiny)
                    && Float.isFinite(expectedOpponentBattleDestiny)
                    && winProbability >= 0.0f
                    && winProbability <= 1.0f
                    && expectedDamageDealt >= 0.0f
                    && expectedDamageTaken >= 0.0f
                    && expectedMyBattleDestiny >= 0.0f
                    && expectedOpponentBattleDestiny >= 0.0f;
        }
    }

    public record Facts(
            String actionId,
            Knowledge knowledge,
            PredictionTelemetry telemetry,
            String provenance) {

        public Facts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(knowledge, "knowledge");
            Objects.requireNonNull(provenance, "provenance");
        }
    }

    public record Evaluation(
            PolicyResult result,
            Assessment assessment) {
    }

    private BattleRetentionPolicy() {
    }

    /**
     * No predictor-only input can prove exact retained material. Returning no
     * operation makes the zero-score boundary executable rather than implied.
     */
    public static Evaluation evaluate(Facts facts) {
        Objects.requireNonNull(facts, "facts");
        return new Evaluation(
                new PolicyResult(PRODUCER, List.of()),
                Assessment.UNKNOWN);
    }
}
