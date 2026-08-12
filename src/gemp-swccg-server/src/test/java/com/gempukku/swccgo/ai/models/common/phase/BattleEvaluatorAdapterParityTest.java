package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Phase;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BattleEvaluatorAdapterParityTest {

    @Test
    public void bothBotsReplaySharedContributionsExactly() {
        com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext =
            new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                null, "bot", "CARD_ACTION_CHOICE", "Choose battle action", "1", Phase.BATTLE);
        randoContext.setActionIds(List.of("fire"));
        randoContext.setActionTexts(List.of("Fire weapon at unique character"));
        randoContext.setCardIds(List.of(""));

        com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext =
            new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                null, "bot", "CARD_ACTION_CHOICE", "Choose battle action", "1", Phase.BATTLE);
        chosenContext.setActionIds(List.of("fire"));
        chosenContext.setActionTexts(List.of("Fire weapon at unique character"));
        chosenContext.setCardIds(List.of(""));

        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction rando =
            new com.gempukku.swccgo.ai.models.rando.evaluators.BattleEvaluator()
                .evaluate(randoContext).get(0);
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction chosen =
            new com.gempukku.swccgo.ai.models.chosenone.evaluators.BattleEvaluator()
                .evaluate(chosenContext).get(0);

        assertEquals(Float.floatToRawIntBits(220.0f), Float.floatToRawIntBits(rando.getScore()));
        assertEquals(Float.floatToRawIntBits(rando.getScore()), Float.floatToRawIntBits(chosen.getScore()));
        assertEquals(rando.getReasoningString(), chosen.getReasoningString());
        assertEquals(rando.isHardVetoed(), chosen.isHardVetoed());
    }

    @Test
    public void bothAdaptersCopyRetentionTelemetryFields() throws IOException {
        for (String bot : List.of("rando", "chosenone")) {
            String source = Files.readString(mainJavaRoot()
                    .resolve("com/gempukku/swccgo/ai/models")
                    .resolve(bot)
                    .resolve("evaluators/BattleEvaluator.java"));
            assertEquals(1, occurrences(
                    source, "outcome.expectedMyBattleDestiny"));
            assertEquals(1, occurrences(
                    source, "outcome.expectedOpponentBattleDestiny"));
        }
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length())
                / needle.length();
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve(
                    "src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repoLayout)) {
                return repoLayout;
            }
            Path moduleLayout = cursor.resolve("src/main/java");
            if (Files.isDirectory(moduleLayout.resolve(
                    "com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError(
                "Could not locate gemp-swccg-server main/java");
    }
}
