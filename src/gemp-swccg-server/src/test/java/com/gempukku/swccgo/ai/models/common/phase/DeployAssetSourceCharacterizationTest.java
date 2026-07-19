package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployAssetSourceCharacterizationTest {
    @Test
    public void deployAssetAdaptersStayNormalizedMirrors() throws IOException {
        assertNormalizedMirror("DeployEvaluator.java");
        assertNormalizedMirror("CardSelectionEvaluator.java");
        assertNormalizedMirror("ActionTextEvaluator.java");
    }

    @Test
    public void sharedPoliciesOwnExtractedWeaponPilotAndShipScores() throws IOException {
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        String destination = evaluatorSource("rando", "CardSelectionEvaluator.java");
        String actionText = evaluatorSource("rando", "ActionTextEvaluator.java");

        for (String call : new String[]{
                "DeployWeaponPolicy.evaluateDirectEligibility(",
                "DeployWeaponPolicy.evaluateNamedPriority(",
                "DeployPilotShipPolicy.evaluateMatchingPilot(",
                "DeployPilotShipPolicy.evaluateMatchingShip(",
                "DeployPilotShipPolicy.evaluateCrew(",
                "DeployPilotShipPolicy.evaluateShipAbility(",
                "DeployPilotShipPolicy.evaluateShipThreat(",
                "DeployPilotShipPolicy.evaluateAssetTail("}) {
            assertTrue(call, deploy.contains(call));
        }
        assertTrue(destination.contains(
                "DeployPilotShipPolicy.evaluateObjectivePilotDestination("));
        assertTrue(actionText.contains("DeployWeaponPolicy.evaluateReserveTarget("));
        assertTrue(actionText.contains("DeployWeaponPolicy.evaluateReserveWielder("));
        assertTrue(actionText.contains("DeployWeaponPolicy.evaluatePullCriteria("));

        assertFalse(deploy.contains("V158 WEAPON BLOCK: \" + v158BlockWhy"));
        assertFalse(deploy.contains("VEHICLE/SHIP NEEDS PILOT: \" + reason"));
        assertFalse(deploy.contains("V30 MATCHING COMBO: %s + %s both in hand"));
        assertFalse(destination.contains(
                "V121 INVASION (CS): Neimoidian pilot must deploy aboard '"));
        assertFalse(actionText.contains(
                "V158 RESERVE-DEPLOY BLOCK: %s already armed"));
        assertFalse(actionText.contains(
                "V120 WEAPON-PULL BLOCK: '\" + v120WeaponName"));
    }

    @Test
    public void v185RemainsPullOwnedAndDeployPoliciesStayAiOnly() throws IOException {
        String deployPolicies = commonPhaseSource("DeployWeaponPolicy.java")
                + commonPhaseSource("DeployPilotShipPolicy.java");
        String pullPolicies = commonPhaseSource("PullDeployPolicy.java")
                + commonPhaseSource("PullActionPolicy.java");

        assertFalse(deployPolicies.contains("V185"));
        assertTrue(pullPolicies.contains("\"V185\""));
        assertTrue(pullPolicies.contains("\"V185-ate\""));

        for (String forbidden : new String[]{
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, deployPolicies.contains(forbidden));
        }
    }

    private static void assertNormalizedMirror(String fileName) throws IOException {
        assertEquals(normalize(evaluatorSource("rando", fileName)),
                normalize(evaluatorSource("chosenone", fileName)));
    }

    private static String evaluatorSource(String bot, String fileName)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(fileName));
    }

    private static String commonPhaseSource(String fileName) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve(fileName));
    }

    private static Path mainJavaRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            Path repoLayout = cursor.resolve("src/gemp-swccg-server/src/main/java");
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
