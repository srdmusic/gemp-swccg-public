package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LegacyLaneCloseoutSourceParityTest {

    @Test
    public void coordinatorAdaptersStayExactMirrors() throws IOException {
        assertEquals(actionContextMethod(source("rando")),
                actionContextMethod(source("chosenone")));
        assertEquals(deployMethod(source("rando")),
                deployMethod(source("chosenone")));
    }

    @Test
    public void actionContextRetainsPhasePriorityAndPostureOrder()
            throws IOException {
        String method = actionContextMethod(source("rando"));

        int deploy = method.indexOf("if (phase == Phase.DEPLOY)");
        int control = method.indexOf("if (phase == Phase.CONTROL)", deploy);
        int battle = method.indexOf("if (phase == Phase.BATTLE)", control);
        int priority = method.indexOf(
                "ResponsePolicy.scorePriorityCards(", battle);
        int posture = method.indexOf(
                "CoordinatorPosturePolicy.score(", priority);
        int methodReturn = method.indexOf("return score;", posture);

        assertTrue(deploy >= 0);
        assertTrue(control > deploy);
        assertTrue(battle > control);
        assertTrue(priority > battle);
        assertTrue(posture > priority);
        assertTrue(methodReturn > posture);
        assertFalse(method.contains("if (context.behindOnLifeForce())"));
        assertFalse(method.contains("if (context.aheadOnBoard())"));
        assertFalse(method.contains("if (context.behindOnBoard())"));
    }

    @Test
    public void deployAdapterRetainsReadsLoopsAndFirstMatchOrder()
            throws IOException {
        String method = deployMethod(source("rando"));

        int locationGate = method.indexOf(
                "if (actionText.contains(\"deploy\") && actionText.contains(\"location\"))");
        int locationPolicy = method.indexOf(
                "DeployActionTextPolicy.scoreLegacyFallbackDeployLocation(",
                locationGate);
        int gameGuard = method.indexOf(
                "if (currentGame != null && context != null && mySide != null)",
                locationPolicy);
        int losingRead = method.indexOf(
                "AiBoardAnalyzer.getLosingLocations(", gameGuard);
        int powerRead = method.indexOf("loc.getPowerAdvantage()", losingRead);
        int reinforcePolicy = method.indexOf(
                "DeployActionTextPolicy.scoreLegacyFallbackReinforce(", powerRead);
        int losingBreak = method.indexOf("break;", reinforcePolicy);
        int opponentRead = method.indexOf(
                "AiBoardAnalyzer.getOpponentOnlyLocations(", losingBreak);
        int gainPolicy = method.indexOf(
                "DeployActionTextPolicy.scoreLegacyFallbackGainGround(", opponentRead);
        int opponentBreak = method.indexOf("break;", gainPolicy);
        int allRead = method.indexOf(
                "AiBoardAnalyzer.analyzeAllLocations(", opponentBreak);
        int domainBranch = method.indexOf(
                "boolean matchingDomain = false", allRead);
        int domainPolicy = method.indexOf(
                "DeployActionTextPolicy.scoreLegacyFallbackDomainMatch(",
                domainBranch);
        int domainBreak = method.indexOf("break;", domainPolicy);
        int pilotGate = method.indexOf(
                "if (actionText.contains(\"pilot\") && actionText.contains(\"matching\"))",
                domainBreak);
        int pilotPolicy = method.indexOf(
                "DeployActionTextPolicy.scoreLegacyFallbackMatchingPilot(",
                pilotGate);
        int methodReturn = method.indexOf("return score;", pilotPolicy);

        assertTrue(locationGate >= 0);
        assertTrue(locationPolicy > locationGate);
        assertTrue(gameGuard > locationPolicy);
        assertTrue(losingRead > gameGuard);
        assertTrue(powerRead > losingRead);
        assertTrue(reinforcePolicy > powerRead);
        assertTrue(losingBreak > reinforcePolicy);
        assertTrue(opponentRead > losingBreak);
        assertTrue(gainPolicy > opponentRead);
        assertTrue(opponentBreak > gainPolicy);
        assertTrue(allRead > opponentBreak);
        assertTrue(domainBranch > allRead);
        assertTrue(domainPolicy > domainBranch);
        assertTrue(domainBreak > domainPolicy);
        assertTrue(pilotGate > domainBreak);
        assertTrue(pilotPolicy > pilotGate);
        assertTrue(methodReturn > pilotPolicy);
        assertFalse(method.contains("score += RandoConfig."));
        assertFalse(method.contains("score += 15"));
        assertFalse(method.contains("score += 10"));
        assertFalse(method.contains("score -= 10"));
        assertFalse(method.contains("score += 5"));
        assertFalse(method.contains("score -= 20"));
    }

    @Test
    public void sharedOwnersContainNoGameOrBotAdapterTypes()
            throws IOException {
        for (String file : new String[]{
                "CoordinatorPosturePolicy.java",
                "DeployActionTextPolicy.java", "ResponsePolicy.java"}) {
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

    private static String actionContextMethod(String source) {
        return slice(source, "protected int scoreActionContext(",
                "// =========================================================================\n"
                        + "    // Phase-Specific Scoring");
    }

    private static String deployMethod(String source) {
        return slice(source, "private int scoreDeployAction(",
                "private int scoreControlAction(");
    }

    private static String slice(
            String source, String startMarker, String endMarker) {
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
}
