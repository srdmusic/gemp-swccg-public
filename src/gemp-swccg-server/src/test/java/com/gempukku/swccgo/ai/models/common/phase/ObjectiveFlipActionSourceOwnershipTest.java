package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObjectiveFlipActionSourceOwnershipTest {

    @Test
    public void sharedPolicyOwnsTheExactRuleAndRemainsPure()
            throws IOException {
        String policy = policySource();

        assertTrue(policy.contains(
                "OBJECTIVE.MWYHL.FLIP"));
        assertTrue(policy.contains(
                "Deploy Effect from Reserve Deck"));
        assertTrue(policy.contains(
                "Deploy Dagobah location from Reserve Deck"));
        assertFalse(policy.contains(
                "Deploy Bespin location from Reserve Deck"));
        assertTrue(policy.contains(
                "TraceDomainId.OBJECTIVE_INTENT"));
        assertTrue(policy.contains(
                "TraceOutputKind.BANDED"));
        assertTrue(policy.contains("300.0f"));
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying",
                "Luke", "react", "-3", "loaderEnabled",
                "DeployPhasePlanner", "CharacterDeploySiteEvaluator"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    @Test
    public void mirroredAdaptersDelegateOnceAfterLoopVetoesAndContinueDispatch()
            throws IOException {
        String rando = evaluatorSource("rando");
        String chosen = evaluatorSource("chosenone");
        assertEquals(normalize(rando), normalize(chosen));

        for (String evaluator : new String[] {rando, chosen}) {
            assertEquals(1, occurrences(evaluator,
                    "ObjectiveFlipActionPolicy.score("));
            assertEquals(2, occurrences(evaluator,
                    "hasUsefulMwyhlFrontSetupAction("));
            assertEquals(1, occurrences(evaluator,
                    ".classifyPriorityFrontSetupAction("));
            assertFalse(evaluator.contains("OBJECTIVE.MWYHL.FLIP"));
            assertFalse(evaluator.contains(
                    "Deploy Effect from Reserve Deck"));
            assertFalse(evaluator.contains(
                    "Deploy Dagobah location from Reserve Deck"));
            assertFalse(evaluator.contains(
                    "Deploy Bespin location from Reserve Deck"));
            assertTrue(evaluator.contains(
                    "\"225_53\".equals(objectiveFlipSourceBlueprintBase)"));
            assertTrue(evaluator.contains(
                    "&& objectiveFlipSourceOwned"));
            assertTrue(evaluator.contains(
                    "&& objectiveFlipSourceInPlay"));
            assertTrue(evaluator.contains(
                    "&& !objectiveFlipSourceFlipped"));
            assertTrue(evaluator.contains(
                    "&& \"Flip\".equals(actionText.trim())"));
            assertTrue(evaluator.contains(
                    "exactMwyhlFrontFlip\n"
                            + "                                    && hasUsefulMwyhlFrontSetupAction("));
            assertTrue(evaluator.contains(
                    "sourceCardId.equals(sourceCardIds.get(i))"));
            assertTrue(evaluator.contains("getReserveDeck("));
            assertTrue(evaluator.contains("Filters.deployable("));
            assertTrue(evaluator.contains("Filters.Wise_Advice"));
            assertTrue(evaluator.contains("Filters.Yodas_Hope"));
            assertTrue(evaluator.contains("Filters.Dagobah_location"));

            int blockedVeto = evaluator.indexOf(
                    "Blocked action (V163 hard veto)");
            int exactGuard = evaluator.indexOf(
                    "boolean exactMwyhlFrontFlip");
            int flip = evaluator.indexOf(
                    "ObjectiveFlipActionPolicy.score(");
            int genericObjective = evaluator.indexOf(
                    "if (exactAgentsOfBlackSunBountyMove)");
            assertTrue(blockedVeto < flip);
            assertTrue(blockedVeto < exactGuard);
            assertTrue(exactGuard < flip);
            assertTrue(flip < genericObjective);

            String seam = evaluator.substring(flip, genericObjective);
            assertEquals(1, occurrences(seam,
                    "PolicyOperationAdapter.apply("));
            assertEquals(0, occurrences(seam,
                    "actions.add(action);"));
            assertEquals(0, occurrences(seam, "continue;"));
        }
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot)
                .resolve("evaluators/ActionTextEvaluator.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/ObjectiveFlipActionPolicy.java"));
    }

    private static Path mainJavaRoot() {
        return Path.of("src", "main", "java");
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

    private static String normalize(String source) {
        return source.replace("ai.models.rando", "ai.models.BOT")
                .replace("ai.models.chosenone", "ai.models.BOT")
                .replace("Rando", "Robot")
                .replace("ChosenOne", "Robot");
    }
}
