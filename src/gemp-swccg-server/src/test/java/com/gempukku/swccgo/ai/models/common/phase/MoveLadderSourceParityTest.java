package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoveLadderSourceParityTest {
    @Test
    public void moveEvaluatorsStayNormalizedMirrors() throws IOException {
        assertEquals(normalize(evaluatorSource("rando")),
                normalize(evaluatorSource("chosenone")));
    }

    @Test
    public void ladderThresholdsAndFinalizerHaveOneSharedOwner()
            throws IOException {
        String move = evaluatorSource("rando");
        String policy = policySource();

        for (String retired : new String[]{
                "private static final float RANK_R4",
                "private static final float RANK_R3",
                "private static final float RANK_R2",
                "private static final float LADDER_VETO",
                "private static final float FINE_CLAMP",
                "private static final float R2_CLAIM_MIN_FINE",
                "private static final float R2_CLAIM_MIN_DRAIN_DELTA",
                "if (ladderVetoHard) {",
                "if (ladderWrongDirVeto) {",
                "if (ladderCanWinVeto) {",
                "if (fines > FINE_CLAMP)"}) {
            assertFalse(retired, move.contains(retired));
        }

        assertTrue(policy.contains("private static final float RANK_R4_SCORE = 20000.0f"));
        assertTrue(policy.contains("private static final float RANK_R3_SCORE = 12000.0f"));
        assertTrue(policy.contains("private static final float RANK_R2_SCORE = 1000.0f"));
        assertTrue(policy.contains("private static final float LADDER_VETO = -100000.0f"));
        assertTrue(policy.contains("private static final float FINE_CLAMP = 2800.0f"));
        assertTrue(policy.contains("public static RankTwoClaim claimR2("));
        assertTrue(policy.contains("public static BandIntegrity bandIntegrity()"));
        assertTrue(policy.contains("public static Finalization finalizeAction("));
    }

    @Test
    public void adaptersRetainClaimsLogsMutationAndOneTimeBandGate()
            throws IOException {
        String move = evaluatorSource("rando");

        assertTrue(move.contains("private static boolean ladderBandsChecked = false"));
        assertFalse(move.contains("MoveLadderPolicy.claimR4(ladderRank)"));
        assertTrue(move.contains("MoveLadderPolicy.claimR3(ladderRank)"));
        assertTrue(move.contains("MoveLadderPolicy.claimR2("));
        assertTrue(move.contains("MoveLadderPolicy.bandIntegrity()"));
        assertFalse(move.contains("LADDER BAND INVERSION"));
        assertTrue(move.contains("LADDER R2 TACTICAL RANGE"));
        assertTrue(move.contains("MoveLadderPolicy.finalizeAction("));
        assertTrue(move.contains("action.addReasoning(step.reasoning(), step.delta())"));
        assertTrue(move.contains("logger.warn("));
        assertFalse(move.contains("ladderClaimR4Transit("));
        assertTrue(move.contains("ladderClaimR3(\"THREAT RETREAT\")"));
        assertTrue(move.contains("ladderClaimR2(\"ATTACK\""));
        assertTrue(move.contains("ladderVetoHard = true"));
        assertTrue(move.contains("ladderWrongDirVeto = true"));
        assertTrue(move.contains("ladderCanWinVeto = v137Decision.canWinVeto()"));
    }

    @Test
    public void finalizerRemainsOncePerActionBeforeAdd() throws IOException {
        String move = evaluatorSource("rando");
        int loop = move.indexOf("for (int i = 0; i < actionIds.size(); i++)");
        int reset = move.indexOf("ladderResetForAction()", loop);
        int finalizer = move.indexOf("ladderFinalize(action)", reset);
        int add = move.indexOf("actions.add(action)", finalizer);

        assertTrue(loop >= 0);
        assertTrue(reset > loop);
        assertTrue(finalizer > reset);
        assertTrue(add > finalizer);
        assertEquals(1, countOccurrences(move, "ladderFinalize(action)"));
    }

    @Test
    public void policyContainsNoGameOrEngineDependencies() throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/MoveEvaluator.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("MoveLadderPolicy.java"));
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

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
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
