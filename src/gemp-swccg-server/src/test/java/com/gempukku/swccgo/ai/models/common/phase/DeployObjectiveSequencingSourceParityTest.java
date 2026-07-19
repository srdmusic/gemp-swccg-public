package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployObjectiveSequencingSourceParityTest {

    @Test
    public void deployAdaptersRemainExactNormalizedMirrors() throws IOException {
        String rando = adapterSource("rando").replace("models.rando", "models.MIRROR");
        String chosen = adapterSource("chosenone")
                .replace("models.chosenone", "models.MIRROR");
        assertEquals(rando, chosen);
    }

    @Test
    public void migratedScoresHaveOnlyThePureSharedOwner() throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String source = adapterSource(bot);
            assertTrue(source.contains(
                    "DeployObjectiveSequencingPolicy.evaluateEarlyLocation("));
            assertTrue(source.contains(
                    "DeployObjectiveSequencingPolicy.classifyBespinFirst("));
            assertTrue(source.contains(
                    "DeployObjectiveSequencingPolicy.evaluateBespinFirst("));
            for (String retired : new String[] {
                    "action.addReasoning(\"LOCATION - deploy first!\"",
                    "action.addReasoning(\"V24.10 PIETT MISSING:",
                    "action.addReasoning(\"V24.15 BESPIN PRIORITY:",
                    "action.addReasoning(\n                                    \"V29 BESPIN-FIRST:"}) {
                assertFalse(bot + ": " + retired, source.contains(retired));
            }
        }
    }

    @Test
    public void policyCallsRetainTerminalAndLazyReadOrder() throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String source = adapterSource(bot);
            int early = source.indexOf(
                    "DeployObjectiveSequencingPolicy.evaluateEarlyLocation(");
            int earlyApply = source.indexOf(
                    "PolicyOperationAdapter.apply(action, earlyLocationLedger);", early);
            int earlyContinue = source.indexOf(
                    "== DeployObjectiveSequencingPolicy.AdapterStep.CONTINUE_ACTION", earlyApply);
            int classify = source.indexOf(
                    "DeployObjectiveSequencingPolicy.classifyBespinFirst(", earlyContinue);
            int candidate = source.indexOf(
                    "== DeployObjectiveSequencingPolicy.BespinFirstRoute.CANDIDATE", classify);
            int objectiveForbid = source.indexOf(
                    "bespinFirstAnalyzer.objectiveForbidsDeployingExecutor();", candidate);
            int oracleGuard = source.indexOf(
                    "if (!objectiveForbidsExecutor)", objectiveForbid);
            int oracleRead = source.indexOf("context.getDeckOracle();", oracleGuard);
            int evaluate = source.indexOf(
                    "DeployObjectiveSequencingPolicy.evaluateBespinFirst(", oracleRead);
            assertTrue(bot + ": migrated calls must retain order",
                    early >= 0 && earlyApply > early && earlyContinue > earlyApply
                            && classify > earlyContinue && candidate > classify
                            && objectiveForbid > candidate && oracleGuard > objectiveForbid
                            && oracleRead > oracleGuard && evaluate > oracleRead);
        }
    }

    @Test
    public void pureOwnerHasNoEngineOrForbiddenMetadataDependencies()
            throws IOException {
        String source = phaseSource("DeployObjectiveSequencingFacts.java")
                + phaseSource("DeployObjectiveSequencingPolicy.java");
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying", "DeckOracle",
                "ObjectiveAnalyzer", "DecisionOrigin", "DecisionActionSemantic",
                "DecisionWire", "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata", "hardVeto(", "defer("}) {
            assertFalse(forbidden, source.contains(forbidden));
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
