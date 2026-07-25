package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MoveObjectiveGateHoldSourceParityTest {

    @Test
    public void bothMoveAdaptersUseTheSharedGateHoldOnce() throws IOException {
        String rando = evaluatorSource("rando");
        String chosen = evaluatorSource("chosenone");

        assertEquals(1, countOccurrences(
                rando, "MoveObjectiveGateHoldPolicy.evaluate("));
        assertEquals(1, countOccurrences(
                chosen, "MoveObjectiveGateHoldPolicy.evaluate("));
        assertEquals(1, countOccurrences(
                rando, "MoveObjectiveGateHoldPolicy.evaluateRequiredControl("));
        assertEquals(1, countOccurrences(
                chosen, "MoveObjectiveGateHoldPolicy.evaluateRequiredControl("));
        assertTrue(rando.contains(
                "isPreFlipPlainControlRequirementLocation("));
        assertTrue(chosen.contains(
                "isPreFlipPlainControlRequirementLocation("));
        assertTrue(rando.contains(
                "isSoleControlSourceAtRequiredLocation("));
        assertTrue(chosen.contains(
                "isSoleControlSourceAtRequiredLocation("));
        assertTrue(rando.contains("ladderVetoHard = true;"));
        assertTrue(chosen.contains("ladderVetoHard = true;"));
        assertTrue(rando.contains("+ oppWeaponBonusAt("));
        assertTrue(chosen.contains("+ oppWeaponBonusAt("));
    }

    @Test
    public void gateHoldRunsBeforeGenericMoveScoring() throws IOException {
        String move = evaluatorSource("rando");
        int gateHold = move.indexOf("MoveObjectiveGateHoldPolicy.evaluate(");
        int requiredControlHold = move.indexOf(
                "MoveObjectiveGateHoldPolicy.evaluateRequiredControl(");
        int genericMoveScoring = move.indexOf(
                "rankMoveFromLocation(action", gateHold);

        assertTrue(gateHold >= 0);
        assertTrue(requiredControlHold >= 0);
        assertTrue(genericMoveScoring > gateHold);
        assertTrue(genericMoveScoring > requiredControlHold);
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/MoveEvaluator.java"));
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve(
                    "src/gemp-swccg-server/src/main/java");
            if (Files.isDirectory(repoLayout)) return repoLayout;
            Path moduleLayout = cursor.resolve("src/main/java");
            if (Files.isDirectory(moduleLayout.resolve(
                    "com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
