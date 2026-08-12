package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BattleRetentionFactsReaderTest {

    @Test
    public void publicPredictionIsRawTelemetryWithZeroScore() {
        PhysicalCard target = mock(PhysicalCard.class);
        when(target.getTitle()).thenReturn("Test Site");
        BattleDecisionPolicy.Prediction prediction =
                new BattleDecisionPolicy.Prediction(
                        0.75f, 3.0f, 1.0f, 2.5f, 3.5f);
        BattleDecisionPolicy.PredictionGate gate = gate(prediction);

        BattleRetentionPolicy.Facts facts =
                BattleRetentionFactsReader.read("battle", target, gate);

        assertEquals(BattleRetentionPolicy.Knowledge.RAW_PREDICTOR_ONLY,
                facts.knowledge());
        assertEquals(2.5f,
                facts.telemetry().expectedMyBattleDestiny(), 0.0f);
        BattleRetentionPolicy.Evaluation result =
                BattleRetentionPolicy.evaluate(facts);
        assertEquals(BattleRetentionPolicy.Assessment.UNKNOWN,
                result.assessment());
        assertTrue(result.result().operations().isEmpty());
    }

    @Test
    public void legacyThreeFieldPredictionProducesUnknownRetention() {
        PhysicalCard target = mock(PhysicalCard.class);
        when(target.getTitle()).thenReturn("Legacy Site");
        BattleDecisionPolicy.Prediction legacy =
                new BattleDecisionPolicy.Prediction(0.75f, 3.0f, 1.0f);

        BattleRetentionPolicy.Facts facts =
                BattleRetentionFactsReader.read(
                        "battle", target, gate(legacy));

        assertEquals(BattleRetentionPolicy.Knowledge.UNKNOWN,
                facts.knowledge());
        assertTrue(Float.isNaN(
                facts.telemetry().expectedMyBattleDestiny()));
    }

    @Test
    public void negativeExpectedDestinyProducesUnknownRetention() {
        PhysicalCard target = mock(PhysicalCard.class);
        when(target.getTitle()).thenReturn("Invalid Site");
        BattleDecisionPolicy.Prediction invalid =
                new BattleDecisionPolicy.Prediction(
                        0.75f, 3.0f, 1.0f, -1.0f, 3.0f);

        BattleRetentionPolicy.Facts facts =
                BattleRetentionFactsReader.read(
                        "battle", target, gate(invalid));

        assertEquals(BattleRetentionPolicy.Knowledge.UNKNOWN,
                facts.knowledge());
    }

    @Test
    public void missingNamedTargetIsUnknownAndSilent() {
        BattleRetentionPolicy.Facts facts =
                BattleRetentionFactsReader.read(
                        "battle", null,
                        gate(new BattleDecisionPolicy.Prediction(
                                0.75f, 3.0f, 1.0f, 3.0f, 3.0f)));

        assertEquals(BattleRetentionPolicy.Knowledge.UNKNOWN,
                facts.knowledge());
    }

    private static BattleDecisionPolicy.PredictionGate gate(
            BattleDecisionPolicy.Prediction prediction) {
        return new BattleDecisionPolicy.PredictionGate(
                prediction,
                BattleInitiationPolicy.prediction(
                        "Test Site",
                        prediction.winProbability,
                        prediction.expectedDamageTaken),
                10, 1, 8, 1);
    }
}
