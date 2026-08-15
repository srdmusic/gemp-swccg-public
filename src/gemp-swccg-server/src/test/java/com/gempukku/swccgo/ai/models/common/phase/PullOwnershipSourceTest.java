package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PullOwnershipSourceTest {

    @Test
    public void actionAndSelectionAdaptersStayNormalizedMirrors()
            throws IOException {
        assertEquals(normalize(evaluator("rando", "ActionTextEvaluator.java")),
                normalize(evaluator("chosenone", "ActionTextEvaluator.java")));
        assertEquals(normalize(evaluator("rando", "CardSelectionEvaluator.java")),
                normalize(evaluator("chosenone", "CardSelectionEvaluator.java")));
    }

    @Test
    public void v144SearchOwnerRunsBeforeBattleFreezeOwner()
            throws IOException {
        String adapter = evaluator("rando", "ActionTextEvaluator.java");
        int search = adapter.indexOf("PullSpecificActionPolicy.scoreYouAreBeatenSearch(");
        int freeze = adapter.indexOf("BattleActionTextPolicy.scoreYouAreBeatenMode(");

        assertTrue(search >= 0);
        assertTrue(freeze > search);
    }

    @Test
    public void policyAdapterStepsOwnEmptyPileAndExhaustedSearchExits()
            throws IOException {
        String adapter = evaluator("rando", "ActionTextEvaluator.java");
        String pileRegion = between(adapter,
                "// Lost Pile searches", "// ========== V256: DEPLOY ACTION-TEXT — AMSD");
        String tdigwattRegion = between(adapter,
                "// ========== V24: TDIGWATT EXHAUSTED SEARCH GUARD ==========",
                "// ========== V24.6B: I'M SORRY LOCATION PULL");

        assertEquals(2, occurrences(pileRegion, "pileSearch.adapterStep()"));
        assertFalse(pileRegion.contains("if (lostSize == 0)"));
        assertFalse(pileRegion.contains("if (usedSize == 0)"));
        assertTrue(tdigwattRegion.contains("tdigwatt.adapterStep()"));
        assertFalse(tdigwattRegion.contains("if (!anyTargetInReserve)"));
    }

    @Test
    public void inChainPullArithmeticHasOneSharedOwner() throws IOException {
        String adapter = evaluator("rando", "ActionTextEvaluator.java");
        String policy = phase("PullSpecificActionPolicy.java");

        assertTrue(adapter.contains("PullSpecificActionPolicy.scoreEffectSearch("));
        assertTrue(adapter.contains("PullSpecificActionPolicy.scoreAdmiralGeneralPull("));
        for (String movedReason : new String[] {
                "V53 BLOCK WOKLING: Don't waste 3 force searching for effects!",
                "Search for Effect from Reserve Deck",
                "V29.7 NO TARGET: No admirals/generals in Reserve Deck — skip!",
                "OBJECTIVE: prefer an admiral pilot for the Executor-to-Bespin route (+300 bounded preference)",
                "V29.7 PULL FIRST: Retrieve admiral/general into hand before deploying!"}) {
            assertFalse(movedReason, adapter.contains(movedReason));
            assertTrue(movedReason, policy.contains(movedReason));
        }
    }

    @Test
    public void parentTakeIntoHandRulesHaveOnePullActionOwner()
            throws IOException {
        String adapter = evaluator("rando", "ActionTextEvaluator.java");
        String parentPolicy = phase("PullActionPolicy.java");
        String specificPolicy = phase("PullSpecificActionPolicy.java");

        assertTrue(adapter.contains("PullActionPolicy.scoreTakeIntoHand("));
        assertFalse(adapter.contains("V29.7 BOUNCE: Return own card from table to hand"));
        assertFalse(adapter.contains("Take card into hand from Lost Pile"));
        assertTrue(parentPolicy.contains("V29.7 BOUNCE: Return own card from table to hand"));
        assertTrue(parentPolicy.contains("Take card into hand from Lost Pile"));
        assertFalse(specificPolicy.contains("scoreTakeIntoHand"));
        assertFalse(phase("PullTakeCandidatePolicy.java").contains("V29.7 BOUNCE:"));
    }

    @Test
    public void cardSelectionPullArithmeticHasSharedOwners()
            throws IOException {
        String adapter = evaluator("rando", "CardSelectionEvaluator.java");
        String policy = phase("PullSelectionCandidatePolicy.java");

        assertTrue(adapter.contains("PullSelectionCandidatePolicy.scoreUnknownPull("));
        assertTrue(adapter.contains("PullSelectionCandidatePolicy.scoreBlueprintPull("));
        assertTrue(adapter.contains("PullSelectionCandidatePolicy.evaluateAmsdPilot("));
        assertTrue(adapter.contains("PullSelectionCandidatePolicy.scoreIwtmLocation("));
        assertFalse(adapter.contains("action.addReasoning(\"V26 OBJECTIVE:"));
        assertFalse(adapter.contains("action.addReasoning(\"V24.10 AMSD SAFETY NET:"));
        assertTrue(policy.contains("V26 OBJECTIVE: Upper Walkway is EXTERIOR"));
        assertTrue(policy.contains("V24.10 AMSD SAFETY NET:"));
    }

    @Test
    public void pureOwnersHaveNoGameEngineOrForbiddenMetadataDependencies()
            throws IOException {
        String sources = phase("PullActionPolicy.java")
                + phase("PullSpecificActionFacts.java")
                + phase("PullSpecificActionPolicy.java")
                + phase("PullSelectionCandidateFacts.java")
                + phase("PullSelectionCandidatePolicy.java");

        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, sources.contains(forbidden));
        }
    }

    private static String evaluator(String bot, String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(file));
    }

    private static String phase(String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve(file));
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        assertTrue("missing start marker " + start, from >= 0);
        assertTrue("missing end marker " + end, to > from);
        return source.substring(from, to);
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
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
            if (Files.isDirectory(moduleLayout.resolve("com/gempukku/swccgo/ai/models"))) {
                return moduleLayout;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }
}
