package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleForfeitSourceOwnershipTest {

    @Test
    public void standaloneResidualBelongsToSharedPolicyAndMirrorsStayNormalized() throws IOException {
        String rando = evaluatorSource("rando");
        String chosenOne = evaluatorSource("chosenone");
        String policy = policySource();

        assertEquals(normalize(rando), normalize(chosenOne));
        for (String evaluator : new String[] {rando, chosenOne}) {
            assertTrue(evaluator.contains("scoreStandaloneShipWithCrew("));
            assertTrue(evaluator.contains("BattleForfeitPolicy.scoreStandalonePriority("));
            assertTrue(evaluator.contains("BattleForfeitPolicy.scoreStandaloneResidual("));
            assertFalse(evaluator.contains("V48 SHIP WITH CREW:"));
            assertFalse(evaluator.contains(
                    "action.addReasoning(\"☠️ DEAD CARD (persona on table) - forfeit!\""));
            assertFalse(evaluator.contains(
                    "action.addReasoning(\"PILOT ON SHIP - forfeit first!\""));
            assertFalse(evaluator.contains("float forfeitScore = Math.max"));
            assertFalse(evaluator.contains("V139 High power - prefer keeping for battle"));
            assertFalse(evaluator.contains("V139 VALUABLE UNIQUE - never forfeit unless forced"));
            assertFalse(evaluator.contains("OBJECTIVE CRITICAL - NEVER FORFEIT!"));
            assertTrue(evaluator.contains(
                    "Float forfeit = null;\n"
                            + "                        Float power = null;"));
            assertTrue(evaluator.contains(
                    "// TODO: Check for destiny draw bonuses when API available\n"
                            + "                        }\n\n"
                            + "                        // V21: OBJECTIVE-CRITICAL CARD PROTECTION"));
        }
        assertTrue(policy.contains("scoreStandaloneShipWithCrew"));
        assertTrue(policy.contains("scoreStandalonePriority"));
        assertTrue(policy.contains("scoreStandaloneResidual"));
        assertTrue(policy.contains("V48 SHIP WITH CREW:"));
        assertTrue(policy.contains("☠️ DEAD CARD (persona on table) - forfeit!"));
        assertTrue(policy.contains("PILOT ON SHIP - forfeit first!"));
        assertTrue(policy.contains("V139 High power - prefer keeping for battle"));
        assertTrue(policy.contains("V139 VALUABLE UNIQUE - never forfeit unless forced"));
        assertTrue(policy.contains(
                "OBJECTIVE CRITICAL: prefer to retain (-300 objective preference)"));
        assertTrue(policy.contains("PolicyOperation.hardVeto("));
        assertFalse(policy.contains("defer("));
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    @Test
    public void directInterceptorEarlyContinueAndCombinedNoOpRemainOwned()
            throws IOException {
        String policy = policySource();
        assertTrue(policy.contains("V154-hit-host"));
        assertTrue(policy.contains("AdapterStep.CONTINUE_CANDIDATE"));
        assertTrue(policy.contains("scoreCombinedShipWithCrew"));

        for (String evaluator : new String[] {
                evaluatorSource("rando"), evaluatorSource("chosenone")}) {
            assertTrue(evaluator.contains("return evaluateForceLossOrForfeit(context)"));
            assertTrue(evaluator.contains(
                    "V67y deliberately not applied in this method"));
            assertTrue(evaluator.contains(
                    "BattleForfeitFacts.countAboardCharacters("));
            assertTrue(evaluator.contains(
                    "scoreCombinedShipWithCrew("));
            assertFalse(evaluator.contains(
                    "gameState.getAttachedCards(battleCandidate)"));
        }

        assertTrue(aiSource("rando", "RandoCalAi.java").contains(
                "V45 IMMUNE FORFEIT: All cards immune"));
        assertTrue(aiSource("chosenone", "TheChosenOneAi.java").contains(
                "V45 IMMUNE FORFEIT: All cards immune"));
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/CardSelectionEvaluator.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase/BattleForfeitPolicy.java"));
    }

    private static String aiSource(String bot, String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve(file));
    }

    private static Path mainJavaRoot() {
        return Path.of("src", "main", "java");
    }

    private static String normalize(String source) {
        return source.replace("ai.models.rando", "ai.models.BOT")
                .replace("ai.models.chosenone", "ai.models.BOT")
                .replace("Rando", "Robot")
                .replace("ChosenOne", "Robot");
    }
}
