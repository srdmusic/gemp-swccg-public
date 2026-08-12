package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BattleRetentionPolicyTest {

    @Test
    public void rawPredictorTelemetryAlwaysScoresZero() {
        BattleRetentionPolicy.Facts raw = new BattleRetentionPolicy.Facts(
                "battle",
                BattleRetentionPolicy.Knowledge.RAW_PREDICTOR_ONLY,
                new BattleRetentionPolicy.PredictionTelemetry(
                        0.10f, 0.0f, 20.0f, 1.0f, 6.0f),
                "DB72184/public-predictor");

        BattleRetentionPolicy.Evaluation result =
                BattleRetentionPolicy.evaluate(raw);
        assertTrue(result.result().operations().isEmpty());
        assertTrue(result.assessment()
                == BattleRetentionPolicy.Assessment.UNKNOWN);
    }

    @Test
    public void unknownFactsAlwaysScoreZero() {
        BattleRetentionPolicy.Facts unknown =
                new BattleRetentionPolicy.Facts(
                        "battle",
                        BattleRetentionPolicy.Knowledge.UNKNOWN,
                        null,
                        "DB72274/hindsight-not-input");

        BattleRetentionPolicy.Evaluation result =
                BattleRetentionPolicy.evaluate(unknown);
        assertTrue(result.result().operations().isEmpty());
        assertTrue(result.assessment()
                == BattleRetentionPolicy.Assessment.UNKNOWN);
    }

    @Test
    public void replayShapedPublicFactsStayUnknownAndZero() {
        for (String replay : List.of(
                "DB72184-react",
                "DB72186-lone-survivor",
                "DB72232-immune-clean",
                "DB72271-hidden-future",
                "DB72274-realized-destiny-hindsight",
                "DB72251-v61b-overpower")) {
            BattleRetentionPolicy.Facts facts =
                    new BattleRetentionPolicy.Facts(
                            "battle",
                            BattleRetentionPolicy.Knowledge
                                    .RAW_PREDICTOR_ONLY,
                            new BattleRetentionPolicy.PredictionTelemetry(
                                    0.75f, 3.0f, 1.0f, 3.0f, 3.0f),
                            replay);

            BattleRetentionPolicy.Evaluation result =
                    BattleRetentionPolicy.evaluate(facts);

            assertEquals(BattleRetentionPolicy.Assessment.UNKNOWN,
                    result.assessment());
            assertTrue(replay, result.result().operations().isEmpty());
        }
    }
}
