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
                    "DeployObjectiveSequencingPolicy.isEarlyLocationCandidate("));
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
            int locationCard = source.indexOf("PhysicalCard earlyLocationCard = null;");
            int titleNull = source.indexOf("if (cardTitleFromGemp == null)", locationCard);
            int legacyEarlyCard = source.indexOf("earlyCard = earlyLocationCard;", titleNull);
            int classifyEarly = source.indexOf(
                    "DeployObjectiveSequencingPolicy.isEarlyLocationCandidate(");
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
            int bespinTerminal = source.indexOf(
                    "== DeployObjectiveSequencingPolicy.AdapterStep.CONTINUE_ACTION", evaluate);
            int addPenalized = source.indexOf("actions.add(action);", bespinTerminal);
            int continuePenalized = source.indexOf("continue;", addPenalized);
            assertTrue(bot + ": migrated calls must retain order",
                    locationCard >= 0 && titleNull > locationCard
                            && legacyEarlyCard > titleNull
                            && classifyEarly > legacyEarlyCard && early > classifyEarly
                            && earlyApply > early && earlyContinue > earlyApply
                            && classify > earlyContinue && candidate > classify
                            && objectiveForbid > candidate && oracleGuard > objectiveForbid
                            && oracleRead > oracleGuard && evaluate > oracleRead
                            && bespinTerminal > evaluate && addPenalized > bespinTerminal
                            && continuePenalized > addPenalized);
        }
    }

    @Test
    public void v29UsesResolvedCategoryButUnknownEnvelopeRetainsLegacyCard() throws IOException {
        for (String bot : new String[] {"rando", "chosenone"}) {
            String source = adapterSource(bot);
            int v29 = source.indexOf("// === V29: TDIGWATT BESPIN-FIRST GUARD");
            int mainLookup = source.indexOf("// === Look up the card using multiple methods", v29);
            assertTrue(bot + ": V29 block bounds", v29 >= 0 && mainLookup > v29);
            String v29Block = source.substring(v29, mainLookup);
            assertTrue(bot + ": V29 must use canonical objective identity",
                    v29Block.contains("bespinFirstAnalyzer.isTdigwatt()"));
            assertFalse(bot + ": V29 must not use broad Bespin objective inference",
                    v29Block.contains("bespinFirstAnalyzer.needsBespinSystemPresence()"));
            assertTrue(bot + ": V29 must use the resolved classification card",
                    v29Block.contains("if (earlyLocationCard != null && earlyLocationCard.getBlueprint() != null)"));

            int unknown = source.indexOf("boolean earlyCardIsLocation = earlyCard != null", mainLookup);
            int unknownEnd = source.indexOf("// === V67bk", unknown);
            assertTrue(bot + ": unknown envelope bounds", unknown > mainLookup && unknownEnd > unknown);
            String unknownBlock = source.substring(unknown, unknownEnd);
            assertFalse(bot + ": V29 classification card leaked into unknown envelope",
                    unknownBlock.contains("earlyLocationCard"));
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
