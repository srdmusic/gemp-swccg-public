package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HuntDownLegacyAdapterSourceParityTest {

    @Test
    public void huntDownLegacyCorrectionRemainsMirroredAndNarrow()
            throws IOException {
        String randoCardSelection = botSource(
                "rando", "CardSelectionEvaluator.java");
        String chosenCardSelection = botSource(
                "chosenone", "CardSelectionEvaluator.java");
        String randoDeploy = botSource("rando", "DeployEvaluator.java");
        String chosenDeploy = botSource("chosenone", "DeployEvaluator.java");

        assertEquals(normalize(randoCardSelection),
                normalize(chosenCardSelection));
        assertEquals(normalize(randoDeploy), normalize(chosenDeploy));
        assertEquals(2, countOccurrences(
                randoCardSelection,
                ".isVaderRequiredHuntDownObjective()"));
        assertEquals(2, countOccurrences(
                chosenCardSelection,
                ".isVaderRequiredHuntDownObjective()"));
        assertEquals(1, countOccurrences(
                randoDeploy,
                ".isVaderRequiredHuntDownObjective()"));
        assertEquals(1, countOccurrences(
                chosenDeploy,
                ".isVaderRequiredHuntDownObjective()"));

        String v25 = sourceSlice(
                randoCardSelection,
                "// === V25: HUNT DOWN V",
                "// === V25: CLOUD CITY");
        assertTrue(v25.contains(
                ".isVaderRequiredHuntDownObjective()"));
        assertFalse(v25.contains(".isHuntDownV()"));

        String setup = sourceSlice(
                randoCardSelection,
                "boolean huntDown = context.getObjectiveAnalyzer()",
                "applySetupContributions(action,\n"
                        + "                        SetupPolicy.startingEffectDeck");
        assertTrue(setup.contains(
                ".isVaderRequiredHuntDownObjective()"));
        assertFalse(setup.contains(".isHuntDownV()"));

        String v51 = sourceSlice(
                randoDeploy,
                "// === V51: VADER AGGRESSIVE FLIP ===",
                "// === V29.13: DEPLOY DIRECTLY TO OPPONENTS");
        assertTrue(v51.contains(
                ".isVaderRequiredHuntDownObjective()"));
        assertFalse(v51.contains(".isHuntDownV()"));
    }

    @Test
    public void directEngageKeepsFamilyBlockerAndUsesTypedPrimaryHunter()
            throws IOException {
        String direct = sourceSlice(
                botSource("rando", "DeployEvaluator.java"),
                "// V35: Check the current priority target",
                "// No opponents here");

        assertTrue(direct.contains(".isHuntDownV()"));
        assertTrue(direct.contains(
                ".isPreFlipGlobalBlockerAt("));
        assertTrue(direct.contains(
                ".qualifiesPreFlipRuntimeActorAtLocation("));
        assertTrue(direct.contains(": deploysVader;"));
        assertFalse("No title-string Galen or Vader routing is allowed",
                direct.contains("contains(\"galen\")")
                        || direct.contains("contains(\"vader\")"));
    }

    private static String botSource(String bot, String file)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(file));
    }

    private static String sourceSlice(
            String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start);
        assertTrue("Missing source start token: " + startToken,
                start >= 0);
        assertTrue("Missing source end token: " + endToken,
                end > start);
        return source.substring(start, end);
    }

    private static int countOccurrences(String source, String token) {
        int count = 0;
        int cursor = 0;
        while ((cursor = source.indexOf(token, cursor)) >= 0) {
            count++;
            cursor += token.length();
        }
        return count;
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repositoryLayout = cursor.resolve(
                    "src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repositoryLayout)) {
                return repositoryLayout;
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
