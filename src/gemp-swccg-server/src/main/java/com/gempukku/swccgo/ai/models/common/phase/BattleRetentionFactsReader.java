package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;

/**
 * Public BATTLE-1 adapter for Batch 3 retention telemetry.
 *
 * The initiation decision does not expose exact battle-state attrition,
 * forfeiture, immunity, dependency, weapon, or response facts. This reader
 * therefore never claims exact knowledge and never unlocks live scoring.
 */
public final class BattleRetentionFactsReader {

    private BattleRetentionFactsReader() {
    }

    public static BattleRetentionPolicy.Facts read(
            String actionId,
            PhysicalCard target,
            BattleDecisionPolicy.PredictionGate predictionGate) {
        if (actionId == null || target == null || predictionGate == null
                || predictionGate.prediction() == null) {
            return unknown(actionId, target, null);
        }

        BattleDecisionPolicy.Prediction prediction =
                predictionGate.prediction();
        BattleRetentionPolicy.PredictionTelemetry telemetry =
                new BattleRetentionPolicy.PredictionTelemetry(
                        prediction.winProbability,
                        prediction.expectedDamageDealt,
                        prediction.expectedDamageTaken,
                        prediction.expectedMyBattleDestiny,
                        prediction.expectedOpponentBattleDestiny);
        if (!telemetry.complete()) {
            return unknown(actionId, target, telemetry);
        }

        return new BattleRetentionPolicy.Facts(
                actionId,
                BattleRetentionPolicy.Knowledge.RAW_PREDICTOR_ONLY,
                telemetry,
                provenance(target));
    }

    private static BattleRetentionPolicy.Facts unknown(
            String actionId,
            PhysicalCard target,
            BattleRetentionPolicy.PredictionTelemetry telemetry) {
        return new BattleRetentionPolicy.Facts(
                actionId == null ? "" : actionId,
                BattleRetentionPolicy.Knowledge.UNKNOWN,
                telemetry,
                provenance(target));
    }

    private static String provenance(PhysicalCard target) {
        if (target == null) {
            return "public-battle-initiation:no-named-target";
        }
        String title = target.getTitle();
        return "public-battle-initiation:"
                + (title == null || title.isBlank()
                    ? "named-target"
                    : title);
    }
}
