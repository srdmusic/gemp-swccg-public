package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployObjectiveIdentitySourceParityTest {

    @Test
    public void deployAdaptersRemainExactNormalizedMirrors() throws IOException {
        String rando = adapterSource("rando").replace("models.rando", "models.MIRROR");
        String chosen = adapterSource("chosenone")
                .replace("models.chosenone", "models.MIRROR");
        assertEquals(rando, chosen);
    }

    @Test
    public void everyTdigwattGateUsesCanonicalIdentity() throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String source = adapterSource(bot);
            assertEquals(3, occurrences(source, ".isTdigwatt()"));
            assertEquals(1, occurrences(source, ".isTdigwattPreFlip()"));
            assertFalse(source.contains("tdigwattPlan = context.getObjectiveAnalyzer() != null\n"
                    + "                        && context.getObjectiveAnalyzer().isAnalyzed()\n"
                    + "                        && !context.getObjectiveAnalyzer().isHuntDownV()"));
            assertFalse(source.contains("boolean isTdigwattDeck = holdBackObjAnalyzer != null && holdBackObjAnalyzer.isAnalyzed()\n"
                    + "                    && !holdBackObjAnalyzer.isHuntDownV()"));
        }
    }

    @Test
    public void tailScriptRequiresActualPreFlipState() throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String source = adapterSource(bot);
            int start = source.indexOf("boolean tdigwattPreFlip =");
            int end = source.indexOf(";", start);
            String gate = source.substring(start, end);
            assertTrue(gate.contains("sequencingObjective.isTdigwattPreFlip()"));
            assertFalse(gate.contains("needsBespinSystemPresence()"));
            assertFalse(gate.contains("isHuntDownV()"));
            assertFalse(gate.contains("isFlipped()"));
        }
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String adapterSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/DeployEvaluator.java"));
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
}
