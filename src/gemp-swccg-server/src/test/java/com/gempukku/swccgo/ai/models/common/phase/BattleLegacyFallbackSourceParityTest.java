package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleLegacyFallbackSourceParityTest {

    @Test
    public void randoAndChosenOneBattleFallbackAdaptersStayExactMirrors()
            throws IOException {
        assertEquals(battleMethod(source("rando")),
                battleMethod(source("chosenone")));
    }

    @Test
    public void adapterRetainsBattleReadsFallbackAndWeaponOrder()
            throws IOException {
        String method = battleMethod(source("rando"));

        assertEquals(1, occurrences(method,
                "BattleActionTextPolicy.scoreLegacyFallbackLocation("));
        assertEquals(1, occurrences(method,
                "BattleActionTextPolicy.scoreLegacyFallbackBoard("));
        assertEquals(1, occurrences(method,
                "BattleWeaponsPolicy.scoreLegacyFallbackFireWeapon("));
        assertFalse(method.contains("score += RandoConfig.SCORE_INITIATE_BATTLE"));
        assertFalse(method.contains("score -= 60"));
        assertFalse(method.contains("score += 50"));

        int initiateGate = method.indexOf(
                "if (actionText.contains(\"initiate battle\"))");
        int gameGuard = method.indexOf(
                "if (currentGame != null && context != null && mySide != null)",
                initiateGate);
        int locationRead = method.indexOf(
                "AiBoardAnalyzer.analyzeAllLocations(", gameGuard);
        int titleRead = method.indexOf("loc.location.getTitle()", locationRead);
        int titleGate = method.indexOf("if (locName == null) continue", titleRead);
        int actionMatch = method.indexOf(
                "if (!actionText.contains(locName.toLowerCase(Locale.ROOT)))",
                titleGate);
        int powerRead = method.indexOf("loc.getPowerAdvantage()", actionMatch);
        int locationPolicy = method.indexOf(
                "BattleActionTextPolicy.scoreLegacyFallbackLocation(", powerRead);
        int battlegroundRead = method.indexOf("loc.isBattleground", locationPolicy);
        int contestedRead = method.indexOf("loc.isContested()", battlegroundRead);
        int statusRead = method.indexOf(
                "loc.status == ContestStatus.WINNING", contestedRead);
        int firstMatchBreak = method.indexOf("break;", statusRead);
        int fallbackGate = method.indexOf("if (score == 0)", firstMatchBreak);
        int boardRead = method.indexOf(
                "AiBoardAnalyzer.calculateBoardAdvantage(", fallbackGate);
        int boardPolicy = method.indexOf(
                "BattleActionTextPolicy.scoreLegacyFallbackBoard(", boardRead);
        int weaponGate = method.indexOf(
                "if (actionText.contains(\"fire\")"
                        + " && actionText.contains(\"weapon\"))",
                boardPolicy);
        int weaponPolicy = method.indexOf(
                "BattleWeaponsPolicy.scoreLegacyFallbackFireWeapon(", weaponGate);
        int methodReturn = method.indexOf("return score;", weaponPolicy);

        assertTrue(initiateGate >= 0);
        assertTrue(gameGuard > initiateGate);
        assertTrue(locationRead > gameGuard);
        assertTrue(titleRead > locationRead);
        assertTrue(titleGate > titleRead);
        assertTrue(actionMatch > titleGate);
        assertTrue(powerRead > actionMatch);
        assertTrue(locationPolicy > powerRead);
        assertTrue(battlegroundRead > locationPolicy);
        assertTrue(contestedRead > battlegroundRead);
        assertTrue(statusRead > contestedRead);
        assertTrue(firstMatchBreak > statusRead);
        assertTrue(fallbackGate > firstMatchBreak);
        assertTrue(boardRead > fallbackGate);
        assertTrue(boardPolicy > boardRead);
        assertTrue(weaponGate > boardPolicy);
        assertTrue(weaponPolicy > weaponGate);
        assertTrue(methodReturn > weaponPolicy);
    }

    @Test
    public void outerFallbackKeepsBattleBeforePriorityAndSituationalScores()
            throws IOException {
        String method = actionContextMethod(source("rando"));

        int battleGate = method.indexOf("if (phase == Phase.BATTLE)");
        int battleCall = method.indexOf(
                "score += scoreBattleAction(actionLower, decisionText)", battleGate);
        int priority = method.indexOf(
                "score += ResponsePolicy.scorePriorityCards(", battleCall);
        int behindLife = method.indexOf("if (context.behindOnLifeForce())", priority);
        int aheadBoard = method.indexOf("if (context.aheadOnBoard())", behindLife);
        int behindBoard = method.indexOf("if (context.behindOnBoard())", aheadBoard);

        assertTrue(battleGate >= 0);
        assertTrue(battleCall > battleGate);
        assertTrue(priority > battleCall);
        assertTrue(behindLife > priority);
        assertTrue(aheadBoard > behindLife);
        assertTrue(behindBoard > aheadBoard);
    }

    @Test
    public void sharedOwnersContainNoGameOrBotAdapterTypes()
            throws IOException {
        for (String file : new String[]{
                "BattleActionTextPolicy.java", "BattleWeaponsPolicy.java"}) {
            String policy = Files.readString(mainJavaRoot().resolve(
                    "com/gempukku/swccgo/ai/models/common/phase").resolve(file));
            for (String forbidden : new String[]{
                    "AiBoardAnalyzer", "LocationAnalysis", "ContestStatus",
                    "SwccgGame", "GameState", "PhysicalCard", "RandoConfig"}) {
                assertFalse(file + ": " + forbidden,
                        policy.contains(forbidden));
            }
        }
    }

    private static String source(String bot) throws IOException {
        String file = bot.equals("rando")
                ? "RandoCalAi.java" : "TheChosenOneAi.java";
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve(file));
    }

    private static String battleMethod(String source) {
        return slice(source,
                "private int scoreBattleAction(",
                "// =========================================================================\n"
                        + "    // Weight Implementations");
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
