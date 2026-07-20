package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ShieldStrategy;
import com.gempukku.swccgo.common.Side;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShieldActivationProvenanceTest {

    @Test
    public void validatedAlignedStackedPileSelectionRecordsExactlyOneActivation() {
        for (String sourceTitle : new String[] {
                "Knowledge And Defense (V)", "Anger, Fear, Aggression"}) {
            ShieldStrategy strategy = new ShieldStrategy(Side.DARK);
            String sourceCardId = ShieldPolicy.selectedTopLevelPlayCardSourceId(
                    "ACTION_CHOICE",
                    new String[] {"deploy", "play-shield", "pass"},
                    new String[] {"Deploy", "Play a card", "Pass"},
                    new String[] {"7", "8", "9"},
                    "play-shield");

            if (sourceCardId != null
                    && ShieldPolicy.isStackedPileShieldSource(sourceTitle)) {
                strategy.recordKnDActivation(1);
            }

            assertEquals("8", sourceCardId);
            assertEquals(1, strategy.knDActivationsThisTurn(1));
        }
    }

    @Test
    public void shieldObservationRecordsTheShieldButNeverAnActivation() {
        ShieldStrategy strategy = new ShieldStrategy(Side.DARK);
        strategy.recordShieldPlayed("13_95", "Weapon Of A Sith");

        assertEquals(3, strategy.shieldsRemaining());
        assertEquals(0, strategy.knDActivationsThisTurn(1));
    }

    @Test
    public void bothBotEntryPointsKeepValidationAndObservationProvenanceMirrored()
            throws IOException {
        String rando = aiSource("rando", "RandoCalAi.java");
        String chosen = aiSource("chosenone", "TheChosenOneAi.java");

        String randoFinalization = between(rando,
                "result = validated[0];",
                "if (ActivateDecisionRouting.selectedTopLevelActivate(");
        String chosenFinalization = between(chosen,
                "result = validated[0];",
                "if (ActivateDecisionRouting.selectedTopLevelActivate(");
        assertEquals(normalize(randoFinalization), normalize(chosenFinalization));
        assertTrue(randoFinalization.indexOf("result = validated[0];")
                < randoFinalization.indexOf(
                "ShieldPolicy.selectedTopLevelPlayCardSourceId("));
        assertTrue(randoFinalization.contains(
                "ShieldPolicy.isStackedPileShieldSource(sourceCard.getTitle())"));
        assertEquals(1, occurrences(randoFinalization,
                "shieldStrategy.recordKnDActivation(activationTurn)"));

        String randoObservation = method(rando, "private void trackOwnShields(");
        String chosenObservation = method(chosen, "private void trackOwnShields(");
        assertEquals(normalize(randoObservation), normalize(chosenObservation));
        for (String observation : new String[] {randoObservation, chosenObservation}) {
            assertTrue(observation.contains("shieldStrategy.recordShieldPlayed("));
            assertFalse(observation.contains("recordKnDActivation"));
        }

        assertEquals(1, occurrences(rando, "recordKnDActivation("));
        assertEquals(1, occurrences(chosen, "recordKnDActivation("));
    }

    private static String aiSource(String bot, String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve(file));
    }

    private static Path mainJavaRoot() {
        return Path.of("src", "main", "java");
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        assertTrue(from >= 0);
        assertTrue(to > from);
        return source.substring(from, to);
    }

    private static String method(String source, String signature) {
        int from = source.indexOf(signature);
        assertTrue(from >= 0);
        int open = source.indexOf('{', from);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char current = source.charAt(i);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(from, i + 1);
            }
        }
        throw new AssertionError("Unterminated method: " + signature);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static String normalize(String source) {
        return source.replace("ai.models.rando", "ai.models.BOT")
                .replace("ai.models.chosenone", "ai.models.BOT")
                .replace("Rando", "Robot")
                .replace("ChosenOne", "Robot");
    }
}
