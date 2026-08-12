package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BattleInitiationSourceParityTest {

    @Test
    public void initiationConditionsWeightsAndReasonsHaveOnePureOwner()
            throws IOException {
        String coordinator = coordinatorSource();
        String policy = policySource();

        for (String retired : new String[] {
                "private static final int FAVORABLE_THRESHOLD",
                "private static final int MARGINAL_THRESHOLD",
                "private static final float ABILITY_BATTLE_MAX_POWER_DEFICIT",
                "private static final int MIN_RESERVE_FOR_BATTLE",
                "No favorable battles available - don't initiate",
                "V34 MUST-FIGHT: Opponent draining from",
                "V61 RESERVE EMPTY: 0 cards in Reserve",
                "V27.1 DRAW THEIR FIRE: Opponent has DTF on table!",
                "Behind on life force - slightly more aggressive"}) {
            assertFalse(retired, coordinator.contains(retired));
            assertTrue(retired, policy.contains(retired));
        }

        for (String call : new String[] {
                "BattleInitiationPolicy.barrierRisk(",
                "BattleInitiationPolicy.huntAggression(",
                "BattleInitiationPolicy.inquisitorDestiny(",
                "BattleInitiationPolicy.prediction(",
                "BattleInitiationPolicy.specificBattle(",
                "BattleInitiationPolicy.fallbackLocation(",
                "BattleInitiationPolicy.scanOutcome(",
                "BattleInitiationPolicy.mustFight(",
                "BattleInitiationPolicy.reserve(",
                "BattleInitiationPolicy.interruptForce(",
                "BattleInitiationPolicy.lifeForce("}) {
            assertTrue(call, coordinator.contains(call));
        }
    }

    @Test
    public void coordinatorRetainsFactsPredictionSafetyLogsAndMutation()
            throws IOException {
        String coordinator = coordinatorSource();

        assertTrue(coordinator.contains("SwccgGame game = context.getGame()"));
        assertTrue(coordinator.contains("GameState gameState = context.getGameState()"));
        assertTrue(coordinator.contains("for (PhysicalCard"));
        assertTrue(coordinator.contains("context.predictBattle("));
        assertTrue(coordinator.contains("FormationSafety"));
        assertTrue(coordinator.contains("action.hardVeto("));
        assertTrue(coordinator.contains("logger.warn("));
        assertTrue(coordinator.contains("addReasoning(contribution.reason(), contribution.delta())"));
    }

    @Test
    public void purePolicyHasNoEngineOrDecisionMetadataDependencies()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying",
                "hardVeto(", "defer(",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    @Test
    public void bothBattleAdaptersUseTheSameSharedCoordinator()
            throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String evaluator = evaluatorSource(bot);
            assertTrue(evaluator.contains(
                    "BattleDecisionPolicy.canEvaluate(adapt(context))"));
            assertTrue(evaluator.contains(
                    "BattleDecisionPolicy.evaluate(adapt(context))"));
            assertTrue(evaluator.contains(
                    "for (BattleDecisionPolicy.ScoredAction result"));
        }
    }

    @Test
    public void retentionGateIsAfterReserveAndBeforeV27() throws IOException {
        String coordinator = coordinatorSource();
        int reserve = coordinator.indexOf(
                "BattleInitiationPolicy.reserve(");
        int retention = coordinator.indexOf(
                "context.readBattleRetentionFacts(");
        int v27 = coordinator.indexOf(
                "// === V27: BATTLE INTERRUPT FORCE RESERVATION ===");

        assertTrue(reserve >= 0);
        assertTrue(retention > reserve);
        assertTrue(v27 > retention);
        assertTrue(coordinator.contains(
                "BattleRetentionPolicy.evaluate(retentionFacts)"));
    }

    @Test
    public void phaseThreeConfidenceBlockIsByteStable()
            throws IOException, NoSuchAlgorithmException {
        String coordinator = coordinatorSource();
        int start = coordinator.indexOf(
                "                                    // === V76/V25 RECONCILIATION ===");
        int end = coordinator.indexOf(
                "                                    boolean exactStructuredPreFlipTarget",
                start);
        String phaseThreeWithSeparator = coordinator.substring(start, end);
        assertTrue(phaseThreeWithSeparator.endsWith("\n\n"));
        String phaseThree = phaseThreeWithSeparator.substring(
                0, phaseThreeWithSeparator.length() - 1);

        assertEquals(
                "8b97976005012b75aacff7afc109165b20c6414c98625194d2a22db71181dca8",
                sha256(phaseThree));
    }

    @Test
    public void predictorMirrorsAndRandomCallOrderRemainExact()
            throws IOException {
        String rando = predictorSource("rando")
                .replace("models.rando", "models.BOT");
        String chosen = predictorSource("chosenone")
                .replace("models.chosenone", "models.BOT");

        assertEquals(rando, chosen);
        assertEquals(2, occurrences(rando,
                "int myBattleDestiny = simulateDestiny(myDestinyDraws);"));
        assertEquals(1, occurrences(rando,
                "int opponentBattleDestiny = simulateDestiny(oppDestinyDraws);"));
        int friendly = rando.indexOf(
                "int myBattleDestiny = simulateDestiny(myDestinyDraws);");
        int opponent = rando.indexOf(
                "int opponentBattleDestiny = simulateDestiny(oppDestinyDraws);");
        assertTrue(friendly >= 0 && opponent > friendly);
    }

    @Test
    public void retentionUsesCachedPredictionAndPublicRouteCannotScore()
            throws IOException {
        String coordinator = coordinatorSource();
        String reader = Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("BattleRetentionFactsReader.java"));

        assertTrue(coordinator.contains(
                "selectedRetentionPredictionGate"));
        assertFalse(reader.contains("predictBattle("));
        assertFalse(reader.contains("Knowledge.EXACT"));
        assertTrue(reader.contains("Knowledge.RAW_PREDICTOR_ONLY"));
    }

    private static String coordinatorSource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("BattleDecisionPolicy.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve("BattleInitiationPolicy.java"));
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/BattleEvaluator.java"));
    }

    private static String predictorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/BattlePredictor.java"));
    }

    private static int occurrences(String source, String needle) {
        return (source.length() - source.replace(needle, "").length())
                / needle.length();
    }

    private static String sha256(String value)
            throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte item : digest) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
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
