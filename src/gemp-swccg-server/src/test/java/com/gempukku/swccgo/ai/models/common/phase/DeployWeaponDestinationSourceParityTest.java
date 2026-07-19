package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployWeaponDestinationSourceParityTest {

    @Test
    public void weaponDestinationAdaptersRemainExactMirrors()
            throws IOException {
        assertEquals(normalize(weaponSlice(evaluatorSource("rando"))),
                normalize(weaponSlice(evaluatorSource("chosenone"))));
    }

    @Test
    public void sharedOwnerReplacesAllDestinationScoreBodies()
            throws IOException {
        String source = weaponSlice(evaluatorSource("rando"));
        assertTrue(source.contains("DeployWeaponPolicy.evaluateDestinationSlot("));
        assertEquals(2, count(source,
                "DeployWeaponPolicy.evaluateLightsaberDestination("));

        for (String retired : new String[]{
                "action.addReasoning(\"⚠️ CHARACTER ALREADY HAS WEAPON:",
                "action.addReasoning(\"Character needs weapon\"",
                "action.addReasoning(\"V25 HUNT DOWN: Target ALREADY HAS lightsaber",
                "action.addReasoning(\"V25 HUNT DOWN: DEPLOYING LIGHTSABER"}) {
            assertFalse(retired, source.contains(retired));
        }
        assertFalse("V274 must not add candidate short-circuiting",
                source.contains("continue;"));
    }

    @Test
    public void attachmentScansAndLazyObjectiveReadStayInLegacyOrder()
            throws IOException {
        String source = weaponSlice(evaluatorSource("rando"));
        int attached = source.indexOf("gameState.getAttachedCards(targetCharacter)");
        int attachedLoop = source.indexOf(
                "for (PhysicalCard attached : attachedCards)", attached);
        int attachedCategory = source.indexOf(
                "attached.getBlueprint().getCardCategory()", attachedLoop);
        int firstBreak = source.indexOf("break;", attachedCategory);
        int slotPolicy = source.indexOf(
                "DeployWeaponPolicy.evaluateDestinationSlot(", firstBreak);
        int slotLog = source.indexOf("logger.warn", slotPolicy);

        int deployingBlueprint = source.indexOf(
                "getBlueprintFromId(context, deployingBlueprintId)", slotLog);
        int deployingTitle = source.indexOf("lsDeployBp.getTitle()", deployingBlueprint);
        int lightsaberGuard = source.indexOf(
                "lsDeployTitle.contains(\"lightsaber\")", deployingTitle);
        int targetAttached = source.indexOf(
                "gameState.getAttachedCards(targetChar)", lightsaberGuard);
        int targetLoop = source.indexOf(
                "for (PhysicalCard att : targetAttached)", targetAttached);
        int targetCategory = source.indexOf(
                "att.getBlueprint().getCardCategory()", targetLoop);
        int targetTitle = source.indexOf("att.getTitle()", targetCategory);
        int secondBreak = source.indexOf("break;", targetTitle);
        int blockedPolicy = source.indexOf(
                "DeployWeaponPolicy.evaluateLightsaberDestination(", secondBreak);
        int blockedLog = source.indexOf("logger.warn", blockedPolicy);
        int objective = source.indexOf("context.getObjectiveAnalyzer()", blockedLog);
        int analyzed = source.indexOf("lsDeployOA.isAnalyzed()", objective);
        int huntDown = source.indexOf("lsDeployOA.isHuntDownV()", analyzed);
        int priorityPolicy = source.indexOf(
                "DeployWeaponPolicy.evaluateLightsaberDestination(",
                blockedPolicy + 1);
        int priorityLog = source.indexOf("logger.warn", priorityPolicy);

        assertTrue(attached >= 0 && attachedLoop > attached
                && attachedCategory > attachedLoop && firstBreak > attachedCategory
                && slotPolicy > firstBreak && slotLog > slotPolicy
                && deployingBlueprint > slotLog && deployingTitle > deployingBlueprint
                && lightsaberGuard > deployingTitle && targetAttached > lightsaberGuard
                && targetLoop > targetAttached && targetCategory > targetLoop
                && targetTitle > targetCategory && secondBreak > targetTitle
                && blockedPolicy > secondBreak && blockedLog > blockedPolicy
                && objective > blockedLog && analyzed > objective
                && huntDown > analyzed && priorityPolicy > huntDown
                && priorityLog > priorityPolicy);
    }

    @Test
    public void bothLegacySecondLightsaberContributionsStayReachable()
            throws IOException {
        String source = weaponSlice(evaluatorSource("rando"));
        int slotPolicy = source.indexOf(
                "DeployWeaponPolicy.evaluateDestinationSlot(");
        int lightsaberPolicy = source.indexOf(
                "DeployWeaponPolicy.evaluateLightsaberDestination(", slotPolicy);
        assertTrue(slotPolicy >= 0 && lightsaberPolicy > slotPolicy);

        String policy = Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/DeployWeaponPolicy.java"));
        for (String forbidden : new String[]{
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying", "DeckOracle",
                "ObjectiveAnalyzer", "DecisionOrigin", "DecisionActionSemantic",
                "DecisionWire", "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String weaponSlice(String source) {
        return slice(source,
                "// CRITICAL: WEAPON DEPLOYMENT - check if target already has weapon",
                "// CRITICAL: Detect location type");
    }

    private static String slice(String source, String startMarker,
                                String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue(startMarker, start >= 0);
        assertTrue(endMarker, end > start);
        return source.substring(start, end);
    }

    private static int count(String source, String needle) {
        int occurrences = 0;
        int cursor = 0;
        while ((cursor = source.indexOf(needle, cursor)) >= 0) {
            occurrences++;
            cursor += needle.length();
        }
        return occurrences;
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
