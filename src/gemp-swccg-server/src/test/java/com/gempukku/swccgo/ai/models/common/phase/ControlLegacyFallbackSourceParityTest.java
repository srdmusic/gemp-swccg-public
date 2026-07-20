package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ControlLegacyFallbackSourceParityTest {

    @Test
    public void randoAndChosenOneControlFallbackAdaptersStayExactMirrors()
            throws IOException {
        assertEquals(controlMethod(source("rando")),
                controlMethod(source("chosenone")));
    }

    @Test
    public void adapterRetainsControlGateAndBoardReadOrder()
            throws IOException {
        String method = controlMethod(source("rando"));

        assertEquals(1, occurrences(method,
                "ControlDrainAssessment.scoreLegacyFallback("));
        assertFalse(method.contains("score += RandoConfig.SCORE_FORCE_DRAIN"));
        assertFalse(method.contains("score += 20 * controlled.size()"));

        int actionGate = method.indexOf(
                "if (actionText.contains(\"force drain\"))");
        int count = method.indexOf("int controlledBattlegrounds = 0", actionGate);
        int gameGuard = method.indexOf(
                "if (currentGame != null && context != null && mySide != null)", count);
        int boardRead = method.indexOf(
                "AiBoardAnalyzer.getControlledBattlegrounds(", gameGuard);
        int nonEmpty = method.indexOf("if (!controlled.isEmpty())", boardRead);
        int sizeRead = method.indexOf(
                "controlledBattlegrounds = controlled.size()", nonEmpty);
        int policy = method.indexOf(
                "ControlDrainAssessment.scoreLegacyFallback(", sizeRead);
        int methodReturn = method.indexOf("return score;", policy);

        assertTrue(actionGate >= 0);
        assertTrue(count > actionGate);
        assertTrue(gameGuard > count);
        assertTrue(boardRead > gameGuard);
        assertTrue(nonEmpty > boardRead);
        assertTrue(sizeRead > nonEmpty);
        assertTrue(policy > sizeRead);
        assertTrue(methodReturn > policy);
    }

    @Test
    public void outerFallbackPhaseOrderRemainsControlThenBattleThenPriority()
            throws IOException {
        String method = actionContextMethod(source("rando"));

        int controlGate = method.indexOf("if (phase == Phase.CONTROL)");
        int controlCall = method.indexOf(
                "score += scoreControlAction(actionLower, decisionText)", controlGate);
        int battleGate = method.indexOf("if (phase == Phase.BATTLE)", controlCall);
        int battleCall = method.indexOf(
                "score += scoreBattleAction(actionLower, decisionText)", battleGate);
        int priority = method.indexOf(
                "score += ResponsePolicy.scorePriorityCards(", battleCall);

        assertTrue(controlGate >= 0);
        assertTrue(controlCall > controlGate);
        assertTrue(battleGate > controlCall);
        assertTrue(battleCall > battleGate);
        assertTrue(priority > battleCall);
    }

    private static String source(String bot) throws IOException {
        String file = bot.equals("rando")
                ? "RandoCalAi.java" : "TheChosenOneAi.java";
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve(file));
    }

    private static String controlMethod(String source) {
        return slice(source,
                "private int scoreControlAction(",
                "private int scoreBattleAction(");
    }

    private static String actionContextMethod(String source) {
        return slice(source,
                "protected int scoreActionContext(",
                "// =========================================================================\n"
                        + "    // Phase-Specific Scoring");
    }

    private static String slice(String source, String startMarker,
                                String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return source.substring(start, end);
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

    private static int occurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
