package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ControlUtilityResidualSourceOwnershipTest {

    @Test
    public void mirroredAdaptersKeepOneSharedOwnerPerUtilityArm() throws IOException {
        String rando = evaluator("rando");
        String chosen = evaluator("chosenone");

        assertEquals(normalize(rando), normalize(chosen));
        for (String source : new String[] {rando, chosen}) {
            assertEquals(1, occurrences(source,
                    "ControlActionPolicy.steal(actionId)"));
            assertEquals(1, occurrences(source,
                    "ControlActionPolicy.dangerousCard(actionId)"));
            assertFalse(source.contains("action.addReasoning(\"Stealing is good\""));
            assertFalse(source.contains("action.addReasoning(\"Known dangerous card\""));

            assertInOrder(source,
                    "else if (textLower.contains(\"steal\"))",
                    "action.setActionType(ActionType.STEAL)",
                    "ControlActionPolicy.steal(actionId)");
            assertInOrder(source,
                    "textLower.contains(\"stardust\") || textLower.contains(\"on the edge\")",
                    "ControlActionPolicy.dangerousCard(actionId)");
        }

        String policy = Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/ControlActionPolicy.java"));
        assertEquals(1, occurrences(policy, "\"Stealing is good\""));
        assertEquals(1, occurrences(policy, "\"Known dangerous card\""));
    }

    private static String evaluator(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot)
                .resolve("evaluators/ActionTextEvaluator.java"));
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
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }

    private static String normalize(String source) {
        return source.replace("ai.models.rando", "ai.models.BOT")
                .replace("ai.models.chosenone", "ai.models.BOT")
                .replace("Rando", "Robot")
                .replace("ChosenOne", "Robot");
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static void assertInOrder(String source, String... needles) {
        int previous = -1;
        for (String needle : needles) {
            int next = source.indexOf(needle, previous + 1);
            assertTrue("missing or out of order: " + needle, next > previous);
            previous = next;
        }
    }
}
