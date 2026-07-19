package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployScalarAssetDestinationSourceParityTest {

    @Test
    public void scalarDestinationAdaptersRemainExactMirrors()
            throws IOException {
        assertEquals(normalize(packetSlices(evaluatorSource("rando"))),
                normalize(packetSlices(evaluatorSource("chosenone"))));
    }

    @Test
    public void sharedOwnersReplaceEveryDirectScoreBody() throws IOException {
        String source = packetSlices(evaluatorSource("rando"));
        assertTrue(source.contains(
                "DeployPilotShipPolicy.evaluateExecutorDestination("));
        assertTrue(source.contains(
                "DeploySitingPolicy.evaluateOpponentForceIcons("));
        assertTrue(source.contains(
                "DeployCardValuePolicy.scoreDestinationAbility("));

        for (String retired : new String[]{
                "action.addReasoning(\"V24.10 EXECUTOR TO BESPIN:",
                "action.addReasoning(\"V24.10 EXECUTOR WRONG SYSTEM:",
                "action.addReasoning(\"V23 FORCE DRAIN:",
                "action.addReasoning(\"V29.7 HIGH ABILITY:",
                "action.addReasoning(\"V29.7 ABILITY:",
                "action.addReasoning(\"V29.7 LOW ABILITY:"}) {
            assertFalse(retired, source.contains(retired));
        }
        assertFalse("V272 must not add candidate short-circuiting",
                source.contains("continue;"));
    }

    @Test
    public void engineReadsRemainInLegacyAdapterOrder() throws IOException {
        String source = evaluatorSource("rando");

        String executor = executorSlice(source);
        int name = executor.indexOf("deployingCardName.toLowerCase");
        int location = executor.indexOf("title.toLowerCase", name);
        int policy = executor.indexOf(
                "DeployPilotShipPolicy.evaluateExecutorDestination(", location);
        int log = executor.indexOf("logger.warn", policy);
        assertTrue(name >= 0 && location > name && policy > location && log > policy);

        String icons = iconSlice(source);
        int side = icons.indexOf("context.getSide()");
        int light = icons.indexOf("getIconCount(Icon.LIGHT_FORCE)", side);
        int dark = icons.indexOf("getIconCount(Icon.DARK_FORCE)", light);
        int iconPolicy = icons.indexOf(
                "DeploySitingPolicy.evaluateOpponentForceIcons(", dark);
        int iconLog = icons.indexOf("logger.info", iconPolicy);
        assertTrue(side >= 0 && light > side && dark > light
                && iconPolicy > dark && iconLog > iconPolicy);

        String ability = abilitySlice(source);
        int blueprint = ability.indexOf(
                "getBlueprintFromId(context, deployingBlueprintId)");
        int hasAbility = ability.indexOf("hasAbilityAttribute()", blueprint);
        int readAbility = ability.indexOf("getAbility()", hasAbility);
        int abilityPolicy = ability.indexOf(
                "DeployCardValuePolicy.scoreDestinationAbility(", readAbility);
        int catchBlock = ability.indexOf("} catch (Exception e) {", abilityPolicy);
        assertTrue(blueprint >= 0 && hasAbility > blueprint
                && readAbility > hasAbility && abilityPolicy > readAbility
                && catchBlock > abilityPolicy);
    }

    @Test
    public void pureOwnersHaveNoEngineOrForbiddenMetadataDependencies()
            throws IOException {
        for (String owner : new String[]{
                "DeployPilotShipPolicy.java",
                "DeploySitingPolicy.java",
                "DeployCardValuePolicy.java"}) {
            String policy = Files.readString(mainJavaRoot().resolve(
                    "com/gempukku/swccgo/ai/models/common/phase/" + owner));
            for (String forbidden : new String[]{
                    "DecisionContext", "EvaluatedAction", "PhysicalCard",
                    "SwccgGame", "GameState", "ModifiersQuerying", "DeckOracle",
                    "ObjectiveAnalyzer", "DecisionOrigin", "DecisionActionSemantic",
                    "DecisionWire", "PullDeployRef", "PullPhysicalCardRef",
                    "DeployDestinationRef", "DeployPhysicalCardRef",
                    "DeployActionMetadata"}) {
                assertFalse(owner + ": " + forbidden, policy.contains(forbidden));
            }
        }
    }

    private static String packetSlices(String source) {
        return executorSlice(source) + iconSlice(source) + abilitySlice(source);
    }

    private static String executorSlice(String source) {
        return slice(source,
                "// === V24.10: EXECUTOR MUST DEPLOY TO BESPIN ===",
                "// === V22.7: OBJECTIVE-CRITICAL LOCATION CONTESTATION ===");
    }

    private static String iconSlice(String source) {
        return slice(source,
                "// === V23: OPPONENT FORCE ICON PREFERENCE (ALL OBJECTIVES) ===",
                "// === V24.15: AVOID DEPLOYING CHARACTERS TO WORTHLESS-DRAIN LOCATIONS ===");
    }

    private static String abilitySlice(String source) {
        return slice(source,
                "// === V29.7: ABILITY-BASED CHARACTER SCORING ===",
                "// === V25: HUNT DOWN V — VADER PRIORITY DEPLOYMENT ===");
    }

    private static String slice(String source, String startMarker,
                                String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(startMarker, start >= 0);
        assertTrue(endMarker, end > start);
        return source.substring(start, end);
    }

    private static String evaluatorSource(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/CardSelectionEvaluator.java"));
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

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }
}
