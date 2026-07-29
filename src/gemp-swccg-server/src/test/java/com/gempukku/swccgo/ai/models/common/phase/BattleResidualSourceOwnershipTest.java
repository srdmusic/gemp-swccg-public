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
    public void forfeitResidualAdaptersApplyAboardV48BeforeEarlyOptionalRoutes()
            throws IOException {
        String rando = evaluatorSource("rando", "CardSelectionEvaluator.java");
        String chosenOne = evaluatorSource("chosenone", "CardSelectionEvaluator.java");

        assertEquals(normalize(rando), normalize(chosenOne));
        for (String evaluator : new String[] {rando, chosenOne}) {
            assertTrue(evaluator.contains("AiCardHelper.isDeadCard(card, game, playerId)"));
            assertTrue(evaluator.contains("PhysicalCard attachedTo = card.getAttachedTo()"));
            assertTrue(evaluator.contains("BattleForfeitPolicy.scoreStandalonePriority("));
            assertTrue(evaluator.contains(
                    "BattleForfeitFacts.countAboardCharacters("));
            assertTrue(evaluator.indexOf("scoreStandaloneShipWithCrew(")
                    < evaluator.indexOf("if (isOptional && optionalDamageRemaining <= 0)"));
            assertTrue(evaluator.indexOf("scoreStandaloneShipWithCrew(")
                    < evaluator.indexOf("scoreStandalonePriority("));
            assertTrue(evaluator.indexOf("scoreStandalonePriority(")
                    < evaluator.lastIndexOf("scoreStandaloneResidual("));
        }
    }

    @Test
    public void targetResidualArithmeticBelongsOnlyToTargetSelectionPolicy()
            throws IOException {
        String rando = evaluatorSource("rando", "CardSelectionEvaluator.java");
        String chosenOne = evaluatorSource("chosenone", "CardSelectionEvaluator.java");
        String randoTarget = targetMethod(rando);
        String chosenTarget = targetMethod(chosenOne);
        String targetPolicy = policySource("TargetSelectionPolicy.java");
        String targetFacts = policySource("TargetSelectionFacts.java");

        assertEquals(normalize(rando), normalize(chosenOne));
        assertEquals(normalize(randoTarget), normalize(chosenTarget));
        for (String target : new String[] {randoTarget, chosenTarget}) {
            assertFalse(target.contains("action.addReasoning("));
            assertEquals(1, occurrences(target,
                    "PolicyOperationAdapter.apply(action, targetLedger)"));
            assertEquals(1, occurrences(target,
                    "PolicyContributionLedger targetLedger"));
            assertTrue(target.indexOf("for (String cardId : context.getCardIds())")
                    < target.indexOf("PolicyContributionLedger targetLedger"));
            assertTrue(target.indexOf("PolicyContributionLedger targetLedger")
                    < target.indexOf("PolicyOperationAdapter.apply(action, targetLedger)"));
            assertTrue(target.indexOf("PolicyOperationAdapter.apply(action, targetLedger)")
                    < target.indexOf("actions.add(action)"));

            assertTrue(target.contains("isBeneficialTargetingCard(decisionText)"));
            assertTrue(target.contains(
                    "gameState.findCardById(Integer.parseInt(cardId))"));
            assertTrue(target.contains("String owner = card.getOwner()"));
            assertTrue(target.contains(
                    "SwccgCardBlueprint blueprint = card.getBlueprint()"));
            assertTrue(target.contains("if (card.isHit())"));
            assertTrue(target.contains("boolean undercover = card.isUndercover()"));
            assertTrue(target.contains(
                    "targetGame != null && gameState != null && context.getPhase() == Phase.BATTLE"));
            assertTrue(target.contains(
                    "getModifiersQuerying().getDefenseValue(gameState, card)"));
            assertTrue(target.contains("context.getDeckOracle()"));
            assertTrue(target.contains(
                    "targetLower.contains(\"padme\") || targetLower.contains(\"naberrie\")"));
            assertTrue(target.contains("isJediOrPadawan(targetLower)"));
            assertTrue(target.contains("catch (Exception e)"));
            assertTrue(target.contains("catch (NumberFormatException e)"));
            assertTrue(target.contains(
                    "logger.warn(\"V51 ALREADY HIT: Weapon targeting"));
            assertTrue(target.contains(
                    "logger.warn(\"V51 KILL SPY: Targeting spy"));
            assertTrue(target.contains(
                    "logger.warn(\"V36 WEAPON TARGET:"));
            assertTrue(target.contains(
                    "logger.debug(\"V36 WEAPON TARGET: Error calculating hit probability:"));
            assertTrue(target.contains(
                    "logger.warn(\"V38.3 SELF-TARGET BLOCKED:"));
        }

        for (String movedReason : new String[] {
                "Beneficial effect on our card",
                "High-power target for buff",
                "Unique target for buff",
                "Don't buff opponent's card!",
                "Target opponent's card",
                "V51 KILL SPY: Target is an undercover spy — eliminate it!",
                "High-power target",
                "Unique target"}) {
            assertFalse(movedReason, randoTarget.contains(movedReason));
            assertFalse(movedReason, chosenTarget.contains(movedReason));
            assertTrue(movedReason, targetPolicy.contains(movedReason));
        }

        assertTrue(randoTarget.contains("TargetSelectionPolicy.initialScore(cardId)"));
        assertTrue(randoTarget.contains("TargetSelectionPolicy.scoreOwnership("));
        assertTrue(randoTarget.contains("TargetSelectionPolicy.scoreUndercover("));
        assertEquals(2, occurrences(randoTarget,
                "TargetSelectionPolicy.scoreValue("));
        assertTrue(randoTarget.contains("BattleWeaponsPolicy.scoreTarget("));
        assertPolicyIsAiPure(targetPolicy);
        assertPolicyIsAiPure(targetFacts);
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

    private static String targetMethod(String source) {
        int start = source.indexOf(
                "private List<EvaluatedAction> evaluateTargetSelection");
        int end = source.indexOf("* Location selection", start);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return source.substring(start, end);
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
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
