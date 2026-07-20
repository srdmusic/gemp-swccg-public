package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleResidualSourceOwnershipTest {

    @Test
    public void actionTextResidualArithmeticBelongsToSharedBattlePolicies()
            throws IOException {
        String rando = evaluatorSource("rando", "ActionTextEvaluator.java");
        String chosenOne = evaluatorSource("chosenone", "ActionTextEvaluator.java");
        String weapons = policySource("BattleWeaponsPolicy.java");
        String actionText = policySource("BattleActionTextPolicy.java");

        assertEquals(normalize(rando), normalize(chosenOne));
        for (String evaluator : new String[] {rando, chosenOne}) {
            assertTrue(evaluator.contains("BattleWeaponsPolicy.scoreForceLightning("));
            assertTrue(evaluator.contains("BattleWeaponsPolicy.scoreBlasterRack("));
            assertTrue(evaluator.contains("BattleActionTextPolicy.scoreRaceDestiny("));
            assertFalse(evaluator.contains(
                    "V67bi FORCE LIGHTNING BLOCK: no opponent character in play — never self-target!"));
            assertFalse(evaluator.contains(
                    "action.addReasoning(\"Race destiny always high priority\""));
            assertFalse(evaluator.contains(
                    "action.addReasoning(\"V35.2 RACK: Character in battle"));
            assertFalse(evaluator.contains(
                    "action.addReasoning(\"V35.2 RACK: Character NOT in this battle"));
            assertFalse(evaluator.contains(
                    "action.addReasoning(\"V29.6 BLASTER RACK:"));

            assertTrue(evaluator.contains("v67biSource.getTitle().toLowerCase"));
            assertTrue(evaluator.contains("for (PhysicalCard pc : v67biGs.getAllPermanentCards())"));
            assertTrue(evaluator.contains("catch (NumberFormatException nfe) { /* ignore */ }"));
            assertTrue(evaluator.contains("logger.debug(\"V67bi check error:"));
            assertTrue(evaluator.contains("rackGs.isDuringBattle()"));
            assertTrue(evaluator.contains("weaponCharAtBattle = true; // Default to allow if check fails"));
            assertTrue(evaluator.contains("action.setActionType(ActionType.RACE_DESTINY)"));
        }

        assertTrue(weapons.contains("V67bi FORCE LIGHTNING BLOCK:"));
        assertTrue(weapons.contains("V35.2 RACK: Character in battle"));
        assertTrue(weapons.contains("V35.2 RACK: Character NOT in this battle"));
        assertTrue(weapons.contains("V29.6 BLASTER RACK:"));
        assertTrue(actionText.contains("Race destiny always high priority"));
        assertPolicyIsAiPure(weapons);
        assertPolicyIsAiPure(actionText);
    }

    @Test
    public void forfeitResidualAdaptersKeepReadsAndPriorityBeforeV48()
            throws IOException {
        String rando = evaluatorSource("rando", "CardSelectionEvaluator.java");
        String chosenOne = evaluatorSource("chosenone", "CardSelectionEvaluator.java");

        assertEquals(normalize(rando), normalize(chosenOne));
        for (String evaluator : new String[] {rando, chosenOne}) {
            assertTrue(evaluator.contains("AiCardHelper.isDeadCard(card, game, playerId)"));
            assertTrue(evaluator.contains("PhysicalCard attachedTo = card.getAttachedTo()"));
            assertTrue(evaluator.contains("BattleForfeitPolicy.scoreStandalonePriority("));
            assertTrue(evaluator.indexOf("scoreStandalonePriority(")
                    < evaluator.indexOf("scoreStandaloneShipWithCrew("));
            assertTrue(evaluator.indexOf("scoreStandaloneShipWithCrew(")
                    < evaluator.lastIndexOf("scoreStandaloneResidual("));
        }
    }

    private static String evaluatorSource(String bot, String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(file));
    }

    private static String policySource(String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve(file));
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

    private static void assertPolicyIsAiPure(String policy) {
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }
}
