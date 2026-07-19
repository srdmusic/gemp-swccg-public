package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PullEvaluatorSourceParityTest {
    @Test
    public void actionTextEvaluatorSourcesStayNormalizedMirrors() throws IOException {
        assertNormalizedMirror("ActionTextEvaluator.java");
    }

    @Test
    public void deployEvaluatorSourcesStayNormalizedMirrors() throws IOException {
        assertNormalizedMirror("DeployEvaluator.java");
    }

    @Test
    public void cardSelectionEvaluatorSourcesStayNormalizedMirrors() throws IOException {
        assertNormalizedMirror("CardSelectionEvaluator.java");
    }

    @Test
    public void pullPolicyAdaptersStayNormalizedMirrors() throws IOException {
        assertNormalizedMirror("PullPolicyAdapter.java");
    }

    @Test
    public void pullScorerConstantsAndExternalContributionOrderStayVisible() throws IOException {
        String evaluator = evaluatorSource("rando", "ActionTextEvaluator.java");
        String deploy = evaluatorSource("rando", "DeployEvaluator.java");
        String policy = commonPhaseSource("PullActionPolicy.java");
        int pullRegion = evaluator.indexOf("// === REGION: PULL ===");
        int externalContribution = policy.indexOf("\"V67ak-pull\"");
        int singleEmit = policy.indexOf("\"V192\"");
        int hardBlockGate = policy.indexOf("boolean hardBlocked = false;");
        int base = policy.indexOf("float base = activateBase ? 5500.0f : 150.0f;");

        assertTrue(pullRegion >= 0);
        assertTrue(evaluator.indexOf("PullActionPolicy.evaluateParent", pullRegion) >= 0);
        assertTrue(base >= 0);
        assertTrue(policy.contains("activateBase ? 7100.0f : 1750.0f"));
        assertTrue(externalContribution >= 0);
        assertTrue(externalContribution < singleEmit);
        assertTrue(hardBlockGate >= 0);
        assertTrue(hardBlockGate < base);
        assertTrue(base < singleEmit);
        int parent = policy.indexOf("public static Evaluation evaluateParent(");
        int sharedWeaponOrder = policy.indexOf(
                "WeaponOrderEvaluation weaponOrder = evaluateWeaponOrder(",
                parent);
        assertTrue(parent >= 0);
        assertTrue(sharedWeaponOrder > parent);
        assertTrue(deploy.contains("PullActionPolicy.evaluateWeaponOrder("));
        assertFalse(deploy.contains(
                "action.addReasoning(\"V67ao ORDER GATE:"));
        assertFalse(deploy.contains(
                "action.addReasoning(\"V149 NO LIGHTSABER WIELDER:"));
    }

    @Test
    public void absorbedDeployBaselineAndExistingTakeRoutesStayInPlace() throws IOException {
        String deploySource = activeLines(evaluatorSource("rando", "DeployEvaluator.java"));
        String actionTextSource = evaluatorSource("rando", "ActionTextEvaluator.java");
        String cardSelectionSource = evaluatorSource("rando", "CardSelectionEvaluator.java");

        assertEquals(0, occurrences(deploySource,
                "action.addReasoning(\"V60 RESERVE PULL:"));
        assertTrue(deploySource.contains(
                "V60 RESERVE PULL guards passed for '{}'"));

        int nonReserveTakeRoute = actionTextSource.indexOf(
                "&& !textLower.contains(\"reserve deck\") && !textLower.contains(\"[upload]\")");
        int pullRoute = actionTextSource.indexOf("// === REGION: PULL ===");
        assertTrue(nonReserveTakeRoute >= 0);
        assertTrue(nonReserveTakeRoute < pullRoute);

        int takeChildRoute = cardSelectionSource.indexOf(
                "textLower.contains(\"card to take into hand\")");
        int takeChildMethod = cardSelectionSource.indexOf(
                "private List<EvaluatedAction> evaluateTakeIntoHand");
        int takeChildSort = cardSelectionSource.indexOf(
                "actions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()))",
                takeChildMethod);
        assertTrue(takeChildRoute >= 0);
        assertTrue(takeChildRoute < takeChildMethod);
        assertTrue(takeChildMethod < takeChildSort);
        assertEquals(2, occurrences(cardSelectionSource,
                "PullDeployCandidatePolicy.evaluate("));
    }

    private static void assertNormalizedMirror(String fileName) throws IOException {
        assertEquals(normalize(evaluatorSource("rando", fileName)),
                normalize(evaluatorSource("chosenone", fileName)));
    }

    private static String evaluatorSource(String bot, String fileName) throws IOException {
        Path path = mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot)
                .resolve("evaluators")
                .resolve(fileName);
        return Files.readString(path);
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

    private static String activeLines(String source) {
        return source.lines()
                .filter(line -> !line.stripLeading().startsWith("//"))
                .collect(Collectors.joining("\n"));
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
}
