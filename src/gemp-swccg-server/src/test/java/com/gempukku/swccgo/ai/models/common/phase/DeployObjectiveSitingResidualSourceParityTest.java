package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployObjectiveSitingResidualSourceParityTest {

    @Test
    public void randoAndChosenOneObjectiveResidualAdaptersStayExactMirrors()
            throws IOException {
        String rando = source("rando");
        String chosenOne = source("chosenone");
        for (String[] bounds : bounds()) {
            assertEquals(bounds[0], normalize(slice(rando, bounds)),
                    normalize(slice(chosenOne, bounds)));
        }
    }

    @Test
    public void sharedPolicyOwnsEveryMigratedScoreBody() throws IOException {
        String adapter = source("rando");
        String contest = slice(adapter, bounds()[0]);
        String isb = slice(adapter, bounds()[1]);
        String huntDown = slice(adapter, bounds()[2]);
        String cloudCity = slice(adapter, bounds()[3]);
        String lando = slice(adapter, bounds()[4]);
        String objectiveTail = slice(adapter, bounds()[5]);

        assertEquals(1, count(contest, ".evaluateMustContest("));
        assertEquals(1, count(isb, ".evaluateIsbAgent("));
        assertEquals(1, count(huntDown, ".evaluateHuntDownCharacter("));
        assertEquals(1, count(cloudCity, ".evaluateCloudCitySpread("));
        assertEquals(1, count(lando, ".evaluateLandoDestination("));
        assertEquals(1, count(lando, ".evaluateLandoSafety("));
        assertEquals(1, count(objectiveTail,
                ".evaluateTdgwattOffObjective("));
        assertEquals(2, count(objectiveTail, ".evaluateObjectiveTail("));

        assertFalse(contest.contains("action.addReasoning("));
        assertFalse(isb.contains("action.addReasoning("));
        assertFalse(huntDown.contains("action.addReasoning("));
        assertFalse(cloudCity.contains("action.addReasoning("));
        assertFalse(lando.contains("action.addReasoning("));
        assertFalse(objectiveTail.contains("action.addReasoning("));
    }

    @Test
    public void adaptersRetainReadsCatchesAndOrdering() throws IOException {
        String adapter = source("rando");

        String contest = slice(adapter, bounds()[0]);
        assertOrdered(contest,
                "getTotalPowerAtLocation(", "game.getOpponent(playerId)",
                "getTotalPowerAtLocation(", ".evaluateMustContest(",
                "catch (Exception e)");

        String isb = slice(adapter, bounds()[1]);
        assertOrdered(isb,
                "getBlueprintFromId(context, deployingBlueprintId)",
                "countISBAgentsOnTable", "isBattleground(",
                ".evaluateIsbAgent(", "catch (Exception e)");

        String huntDown = slice(adapter, bounds()[2]);
        assertOrdered(huntDown,
                "isVaderOnTable", "getBlueprintFromId(context, deployingBlueprintId)",
                ".evaluateHuntDownCharacter(", "catch (Exception e)");

        String cloudCity = slice(adapter, bounds()[3]);
        assertOrdered(cloudCity,
                "getTotalAbilityAtLocation(", "getCardsAtLocation(location)",
                "getLocationsInOrder()", ".evaluateCloudCitySpread(",
                "catch (Exception e)");

        String objectiveTail = slice(adapter, bounds()[5]);
        assertOrdered(objectiveTail,
                ".evaluateTdgwattOffObjective(",
                "applyDeploySitingPolicy(action, v279Tdgwatt.result())",
                "V29 TDIGWATT: Discouraging character deploy",
                "getLocationsInOrder()",
                "if (checkLoc == null) continue;",
                ".evaluateObjectiveTail(",
                "catch (Exception e)");
    }

    @Test
    public void pureOwnerContainsNoEngineDecisionMetadata() throws IOException {
        String policy = Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/DeployObjectiveSitingPolicy.java"));
        for (String forbidden : new String[]{
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    @Test
    public void typedActorRouteStagingKeepsSharedOwnershipMirrorAndSafetyOrder()
            throws IOException {
        String rando = source("rando");
        String chosenOne = source("chosenone");
        String randoBlock = slice(rando, new String[]{
                "PhysicalCard v136DeployingCard =",
                "logger.debug(\"V136 CS error: {}\", e.getMessage());"});
        String chosenBlock = slice(chosenOne, new String[]{
                "PhysicalCard v136DeployingCard =",
                "logger.debug(\"V136 CS error: {}\", e.getMessage());"});
        String policy = Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/DeployObjectiveSitingPolicy.java"));

        assertEquals(normalize(randoBlock), normalize(chosenBlock));
        assertEquals(1, count(randoBlock, ".stagesPreFlipActorRoute("));
        assertEquals(1, count(randoBlock, ".scoreActorRouteStaging("));
        assertTrue(policy.contains(
                "public static PolicyResult scoreActorRouteStaging("));
        assertOrdered(randoBlock,
                "FormationSafety",
                "DeployConstraint.HARD_BLOCK",
                "PolicyOperationAdapter.apply(action, v212SitingCsLedger);",
                ".stagesPreFlipActorRoute(",
                ".scoreActorRouteStaging(");
    }

    private static String[][] bounds() {
        return new String[][]{
                {"// === V22.7: OBJECTIVE-CRITICAL LOCATION CONTESTATION ===",
                        "// === V23: OPPONENT FORCE ICON PREFERENCE"},
                {"// === V29.7: ISB OPERATIONS DEPLOYMENT STRATEGY",
                        "// === V29.7: ABILITY-BASED CHARACTER SCORING ==="},
                {"// === V25: HUNT DOWN V — VADER PRIORITY DEPLOYMENT ===",
                        "// === V25: CLOUD CITY ABILITY-BASED SPREAD STRATEGY"},
                {"// === V25: CLOUD CITY ABILITY-BASED SPREAD STRATEGY",
                        "// V59 UNIVERSAL SPY SCORING"},
                {"boolean v279LandoDeploy =",
                        "// V22/V22.2: Strongly prefer deploying to objective locations"},
                {"} else if (!isObjLocation && !isFlipBackLocation) {",
                        "// V67as, including nested V67br/V75/V67bj"}
        };
    }

    private static String source(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/CardSelectionEvaluator.java"));
    }

    private static String slice(String source, String[] bounds) {
        int start = source.indexOf(bounds[0]);
        int end = source.indexOf(bounds[1], start);
        assertTrue(bounds[0], start >= 0);
        assertTrue(bounds[1], end > start);
        return source.substring(start, end);
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static void assertOrdered(String source, String... needles) {
        int cursor = -1;
        for (String needle : needles) {
            int next = source.indexOf(needle, cursor + 1);
            assertTrue(needle, next > cursor);
            cursor = next;
        }
    }

    private static int count(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve("src/gemp-swccg-server/src/main/java");
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
