package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployParentPolicySourceParityTest {

    @Test
    public void deployAdaptersRemainExactNormalizedMirrors() throws IOException {
        String rando = adapterSource("rando").replace("models.rando", "models.MIRROR");
        String chosen = adapterSource("chosenone")
                .replace("models.chosenone", "models.MIRROR");
        assertEquals(rando, chosen);
    }

    @Test
    public void migratedBranchesHaveOnlyPurePolicyOwners() throws IOException {
        String[] policyCalls = {
                "DeployActionEnvelopePolicy.evaluateParent(",
                "DeployActionEnvelopePolicy.evaluateTitleGate(",
                "DeployCardValuePolicy.scoreBase(",
                "DeployCardValuePolicy.scoreElite(",
                "DeployCardValuePolicy.scoreType(",
                "DeployCardValuePolicy.scoreStrategic(",
                "DeployActionEnvelopePolicy.evaluateUnknown("};
        String[] retired = {
                "blockedAct.addReasoning(\"CANCEL-LOOP BLOCK:",
                "prAction.addReasoning(\"V38.4 PERSONA REPLACE:",
                "action.addReasoning(\"BLOCKED: Do not deploy this Effect",
                "action.addReasoning(String.format(\"Excellent value",
                "action.addReasoning(String.format(\"High destiny",
                "action.addReasoning(\"V40 ELITE:",
                "action.addReasoning(\"High-ability character",
                "action.addReasoning(\"Need to reinforce board",
                "action.addReasoning(\"Critical life force",
                "action.addReasoning(\"V29: Location deploy",
                "action.addReasoning(\"V40: Unknown card during",
                "action.addReasoning(\"V40: Unknown card (deploy from reserve?)"};

        for (String bot : new String[] {"rando", "chosenone"}) {
            String adapter = adapterSource(bot);
            for (String call : policyCalls) {
                assertTrue(bot + ": " + call, adapter.contains(call));
            }
            for (String emission : retired) {
                assertFalse(bot + ": " + emission, adapter.contains(emission));
            }
        }
    }

    @Test
    public void callsRemainAtOriginalInterleavedPositions() throws IOException {
        String source = adapterSource("rando");
        int parent = source.indexOf("DeployActionEnvelopePolicy.evaluateParent(");
        int pull = source.indexOf("PullDeployPolicy.evaluate(");
        int title = source.indexOf("DeployActionEnvelopePolicy.evaluateTitleGate(");
        int plan = source.indexOf("DeployPlanPolicy.evaluate(");
        int base = source.indexOf("DeployCardValuePolicy.scoreBase(");
        int elite = source.indexOf("DeployCardValuePolicy.scoreElite(");
        int type = source.indexOf("DeployCardValuePolicy.scoreType(");
        int asset = source.indexOf("DeployPilotShipPolicy.evaluateAssetTail(");
        int strategic = source.indexOf("DeployCardValuePolicy.scoreStrategic(");
        int unknown = source.indexOf("DeployActionEnvelopePolicy.evaluateUnknown(");
        assertTrue(parent < pull);
        assertTrue(pull < title);
        assertTrue(title < plan);
        assertTrue(plan < base);
        assertTrue(base < elite);
        assertTrue(elite < type);
        assertTrue(type < asset);
        assertTrue(asset < strategic);
        assertTrue(strategic < unknown);
    }

    @Test
    public void adapterReadsRetainOriginalShortCircuitAndExceptionBoundaries()
            throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String source = adapterSource(bot);

            int characterTry = source.indexOf(
                    "if (category == CardCategory.CHARACTER && gameState != null && game != null) {");
            int tryBlock = source.indexOf("try {", characterTry);
            int opponentRead = source.indexOf(
                    "String v40Oid = gameState.getOpponent(v40Pid);", tryBlock);
            int actionTextRead = source.indexOf(
                    "String v40ActionLower = actionText.toLowerCase(Locale.ROOT);", opponentRead);
            int elite = source.indexOf("DeployCardValuePolicy.scoreElite(", actionTextRead);
            int locationLoop = source.indexOf(
                    "for (PhysicalCard v40Loc : gameState.getTopLocations())", elite);
            assertTrue(bot + ": elite scoring must remain inside the V40 try",
                    characterTry >= 0 && tryBlock > characterTry
                            && opponentRead > tryBlock && actionTextRead > opponentRead
                            && elite > actionTextRead && locationLoop > elite);

            int unknownGuard = source.indexOf(
                    "if (!earlyCardIsLocation && plan != null");
            int strategyRead = source.indexOf(
                    "plan.getStrategy() == DeployStrategy.DEPLOY_LOCATIONS",
                    unknownGuard);
            int turnRead = source.indexOf(
                    "unknownTurn = context.getTurnNumber();", strategyRead);
            int guardClose = source.indexOf("}\n                if (earlyCardIsLocation)", turnRead);
            assertTrue(bot + ": unknown plan and turn reads must remain guarded",
                    unknownGuard >= 0 && strategyRead > unknownGuard
                            && turnRead > strategyRead && guardClose > turnRead);
        }
    }

    @Test
    public void pureOwnersHaveNoEngineOrForbiddenMetadataDependencies()
            throws IOException {
        String sources = phaseSource("DeployActionEnvelopeFacts.java")
                + phaseSource("DeployActionEnvelopePolicy.java")
                + phaseSource("DeployCardValueFacts.java")
                + phaseSource("DeployCardValuePolicy.java");
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying", "DeckOracle",
                "ObjectiveAnalyzer", "DecisionOrigin", "DecisionActionSemantic",
                "DecisionWire", "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata", "hardVeto(", "defer("}) {
            assertFalse(forbidden, sources.contains(forbidden));
        }
    }

    private static String adapterSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/DeployEvaluator.java"));
    }

    private static String phaseSource(String file) throws IOException {
        return Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase").resolve(file));
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
        throw new AssertionError("Could not locate gemp-swccg-server main/java");
    }
}
