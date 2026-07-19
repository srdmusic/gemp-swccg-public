package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployPhaseSourceParityTest {
    @Test
    public void deployEvaluatorsStayNormalizedMirrors() throws IOException {
        assertNormalizedMirror("DeployEvaluator.java");
    }

    @Test
    public void actionTextEvaluatorsStayNormalizedMirrors() throws IOException {
        assertNormalizedMirror("ActionTextEvaluator.java");
    }

    @Test
    public void sequencingAndBudgetScoresHaveOneSharedOwner() throws IOException {
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        String actionText = evaluatorSource("rando", "ActionTextEvaluator.java");

        assertTrue(deploy.contains("DeploySequencingPolicy.phaseEnvelope("));
        assertTrue(deploy.contains("DeployPlanPolicy.evaluate("));
        assertTrue(deploy.contains("DeployBudgetPolicy.newMaintenanceCard("));
        assertTrue(deploy.contains("DeployBudgetPolicy.futureObligations("));
        assertTrue(deploy.contains("DeploySequencingPolicy.locationFromHand("));
        assertTrue(deploy.contains("DeploySequencingPolicy.tailScripts("));
        assertTrue(actionText.contains("DeploySequencingPolicy.woklingEarlySearch("));
        assertTrue(actionText.contains("DeploySequencingPolicy.locationsFirstNonDeploy("));

        assertFalse(deploy.contains("V52 MOMENTUM: Already deployed"));
        assertFalse(deploy.contains("V162 HOLD LOCATION: life force"));
        assertFalse(deploy.contains("V48 VADER MOVE RESERVE: Deploy costs"));
        assertFalse(deploy.contains("IN DEPLOYMENT PLAN: \" + plan"));
        assertFalse(actionText.contains("action.addReasoning(\"V53c BLOCK WOKLING"));
        assertFalse(actionText.contains("action.addReasoning(\"V24.4 LOCATIONS FIRST"));
    }

    @Test
    public void aiOnlyDeployFilesContainNoForbiddenEngineMetadata() throws IOException {
        String combined = Files.readString(commonPhaseRoot().resolve("DeploySequencingPolicy.java"))
                + Files.readString(commonPhaseRoot().resolve("DeployBudgetPolicy.java"))
                + Files.readString(commonPhaseRoot().resolve("DeployPlanPolicy.java"));
        for (String forbidden : new String[]{
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef", "DeployDestinationRef",
                "DeployPhysicalCardRef", "DeployActionMetadata"}) {
            assertFalse(forbidden, combined.contains(forbidden));
        }
    }

    @Test
    public void v169AndV176FactReadsKeepIndependentFailureBoundaries() throws IOException {
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        int endangeredRead = deploy.indexOf("DeploySequencingFactsReader.firstEndangeredLocation(");
        int endangeredCatch = deploy.indexOf("catch (Exception sequencingError)", endangeredRead);
        int battleRead = deploy.indexOf("DeploySequencingFactsReader.firstWinnableBattle(", endangeredRead);
        int battleCatch = deploy.indexOf("catch (Exception sequencingError)", battleRead);

        assertTrue(endangeredRead >= 0);
        assertTrue(endangeredCatch > endangeredRead);
        assertTrue(battleRead > endangeredCatch);
        assertTrue(battleCatch > battleRead);
    }

    @Test
    public void skywalkerSagaReadKeepsTurnGuardAndFailOpenBoundary() throws IOException {
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        int adapter = deploy.indexOf("boolean skywalkerSaga = false;");
        int turnGuard = deploy.indexOf("if (context.getTurnNumber() <= 3)", adapter);
        int pyreRead = deploy.indexOf("DeploySequencingFactsReader.hasAnakinsFuneralPyre(", adapter);
        int failOpenCatch = deploy.indexOf("catch (Exception ignored)", pyreRead);

        assertTrue(adapter >= 0);
        assertTrue(turnGuard > adapter);
        assertTrue(pyreRead > turnGuard);
        assertTrue(failOpenCatch > pyreRead);
    }

    @Test
    public void emptyPhysicalTitleStillFallsBackToDecisionTitle() throws IOException {
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        int titleRead = deploy.indexOf("String sequencingCardTitle =");
        int emptyFallback = deploy.indexOf(
                "if (sequencingCardTitle.isEmpty() && cardTitleFromGemp != null)",
                titleRead);

        assertTrue(titleRead >= 0);
        assertTrue(emptyFallback > titleRead);
    }

    private static void assertNormalizedMirror(String fileName) throws IOException {
        assertEquals(normalize(evaluatorSource("rando", fileName)),
                normalize(evaluatorSource("chosenone", fileName)));
    }

    private static String evaluatorSource(String bot, String fileName) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(fileName));
    }

    private static Path commonPhaseRoot() {
        return mainJavaRoot().resolve("com/gempukku/swccgo/ai/models/common/phase");
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

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }
}
