package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployTacticalResidualSourceParityTest {

    @Test
    public void tacticalResidualAdaptersRemainExactNormalizedMirrors()
            throws IOException {
        assertEquals(normalize(residualSlices(source("rando"))),
                normalize(residualSlices(source("chosenone"))));
    }

    @Test
    public void adapterReadsCatchesLogsAndSuppressionStayInSourceOrder()
            throws IOException {
        String adapter = source("rando");
        String v2415 = slice(adapter,
                "// === V24.15: AVOID DEPLOYING CHARACTERS TO WORTHLESS-DRAIN LOCATIONS ===",
                "// === V29.7: ISB OPERATIONS DEPLOYMENT STRATEGY");
        assertOrdered(v2415,
                "getForceDrainAmount(",
                "getInitiateForceDrainCost(",
                "gameState.getOpponent(playerId)",
                "computeNetDrainBalance(game, gameState, playerId)",
                "DeployTacticalPolicy.evaluateV2415Drain(",
                "logger.warn(\"V24.15 ZERO DRAIN:",
                "catch (Exception e)");
        assertFalse(v2415.contains("action.addReasoning(\"V24.15"));

        String v59 = slice(adapter,
                "// V59 UNIVERSAL SPY SCORING",
                "// CRITICAL: Check power at location");
        assertOrdered(v59,
                "getTotalPowerAtLocation(",
                "DeployTacticalPolicy.evaluateV59UniversalSpy(",
                "logger.warn(\"V59 SPY UNIVERSAL:",
                "spyScoringApplied = true",
                "catch (Exception e)");
        assertFalse(v59.contains("action.addReasoning(\"V59"));

        String v223 = slice(adapter,
                "// CRITICAL: Check power at location",
                "// No opponent power - uncontested");
        assertOrdered(v223,
                "getTotalPowerAtLocation(",
                "blueprint.hasPowerAttribute()",
                "locObjAnalyzer.isObjectiveRelevantLocation(title)",
                "DeployTacticalPolicy.evaluateV223Contest(",
                "logger.warn(\"V22.7 OBJ CONTEST:",
                "logger.warn(\"V22.3 CONTEST:");
        assertFalse(v223.contains("action.addReasoning(\"V22.3"));
        assertFalse(v223.contains("action.addReasoning(\"V22.7"));

        String fallback = fallbackSlice(adapter);
        assertOrdered(fallback,
                "getTotalPowerAtLocation(",
                "catch (Exception e) { /* ignore */ }",
                "game.getGameState().getCardsAtLocation(location)",
                "break;",
                "getBlueprintFromId(context, deployingBlueprintId)",
                "DeployTacticalPolicy.evaluateV2414BFallbackSpy(",
                "logger.warn(\"V24.14B SPY DOUBLED:");
        assertEquals(1, count(fallback, "break;"));
        assertFalse(fallback.contains("action.addReasoning(\"V24.14B"));

        String tail = adapter.substring(adapter.indexOf(
                "} else if (!isObjLocation && !isFlipBackLocation) {",
                adapter.indexOf("DeployTacticalPolicy.evaluateV2414BFallbackSpy(")));
        assertTrue(tail.contains(
                "DeployObjectiveSitingPolicy.evaluateTdgwattOffObjective("));
        assertFalse(tail.contains("action.addReasoning(\"V29 TDIGWATT:"));

        String v243b = slice(adapter,
                "// === V24.3B: DR. EVAZAN WEAPON COMBO — DEPLOY LOCATION PREFERENCE ===",
                "boolean v279LandoDeploy =");
        assertOrdered(v243b,
                "boolean deployingEvazan = decisionText.contains(\"evazan\")",
                "gameState.getCardsAtLocation(location)",
                "continue;",
                "break;",
                "catch (Exception e) { /* ignore */ }",
                "DeployTacticalPolicy.evaluateV243BPartner(",
                "logger.warn(\"V24.3 EVAZAN COMBO:");
        assertEquals(2, count(v243b, "break;"));
        assertFalse(v243b.contains("action.addReasoning(\"V24.3 EVAZAN COMBO:"));
    }

    @Test
    public void tacticalResidualPolicyStaysPureOfEngineAndRoutingMetadata()
            throws IOException {
        String policy = Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/DeployTacticalPolicy.java"));
        for (String forbidden : new String[]{
                "DecisionContext", "EvaluatedAction", "PhysicalCard", "SwccgGame",
                "GameState", "ModifiersQuerying", "ObjectiveAnalyzer",
                "SwccgCardBlueprint", "DecisionOrigin", "DecisionActionSemantic",
                "DecisionWire", "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String residualSlices(String source) {
        return slice(source,
                "// === V24.15: AVOID DEPLOYING CHARACTERS TO WORTHLESS-DRAIN LOCATIONS ===",
                "// === V29.7: ISB OPERATIONS DEPLOYMENT STRATEGY")
                + slice(source, "// V59 UNIVERSAL SPY SCORING",
                "// No opponent power - uncontested")
                + fallbackSlice(source)
                + slice(source,
                "// === V24.3B: DR. EVAZAN WEAPON COMBO — DEPLOY LOCATION PREFERENCE ===",
                "boolean v279LandoDeploy =");
    }

    private static String fallbackSlice(String source) {
        return slice(source, "if (isUndercoverSpy && !spyScoringApplied) {",
                "} else if (!isObjLocation && !isFlipBackLocation) {");
    }

    private static String source(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/CardSelectionEvaluator.java"));
    }

    private static String slice(String source, String startMarker,
                                String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(startMarker, start >= 0);
        assertTrue(endMarker, end > start);
        return source.substring(start, end);
    }

    private static void assertOrdered(String source, String... terms) {
        int previous = -1;
        for (String term : terms) {
            int current = source.indexOf(term, previous + 1);
            assertTrue(term, current > previous);
            previous = current;
        }
    }

    private static int count(String source, String term) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(term, from)) >= 0) {
            count++;
            from += term.length();
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
