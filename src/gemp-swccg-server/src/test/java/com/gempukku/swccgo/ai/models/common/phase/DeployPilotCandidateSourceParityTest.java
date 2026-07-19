package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployPilotCandidateSourceParityTest {

    @Test
    public void pilotSelectionAdaptersRemainExactNormalizedMirrors()
            throws IOException {
        String rando = evaluatorSource("rando");
        String chosen = evaluatorSource("chosenone");

        assertEquals(normalize(pilotSelectionSlice(rando)),
                normalize(pilotSelectionSlice(chosen)));
        assertEquals(normalize(simultaneousPilotSlice(rando)),
                normalize(simultaneousPilotSlice(chosen)));
    }

    @Test
    public void sharedPolicyOwnsPilotCandidateScoreEmissions()
            throws IOException {
        for (String bot : new String[]{"rando", "chosenone"}) {
            String source = evaluatorSource(bot);
            String pilot = pilotSelectionSlice(source);
            String simultaneous = simultaneousPilotSlice(source);

            assertTrue(bot, pilot.contains(
                    "DeployPilotShipPolicy.evaluatePilotCandidate("));
            assertTrue(bot, simultaneous.contains(
                    "DeployPilotShipPolicy.evaluateSimultaneousPilotGuard("));
            assertTrue(bot, simultaneous.contains(
                    "DeployPilotShipPolicy.evaluateSimultaneousPilotChoice("));

            for (String retired : new String[]{
                    "action.addReasoning(\"Ability ",
                    "action.addReasoning(\"Good power bonus (",
                    "action.addReasoning(\"Deploy cost ",
                    "action.addReasoning(\"SD BLOCKED:",
                    "action.addReasoning(\"SD: Imperial/First Order pilot",
                    "action.addReasoning(\"PLANNED pilot for ",
                    "action.addReasoning(\"Matching pilot for "}) {
                assertFalse(bot + ": " + retired,
                        pilot.contains(retired) || simultaneous.contains(retired));
            }
        }
    }

    @Test
    public void genericPilotReadsRemainInLegacyOrder() throws IOException {
        String source = pilotSelectionSlice(evaluatorSource("rando"));
        int ability = source.indexOf("pilotAbility = blueprint.getAbility();");
        int power = source.indexOf("pilotPower = blueprint.getPower();");
        int cost = source.indexOf("pilotDeployCost = blueprint.getDeployCost();");
        int policy = source.indexOf(
                "DeployPilotShipPolicy.evaluatePilotCandidate(");

        assertTrue(ability >= 0 && power > ability && cost > power
                && policy > cost);
    }

    @Test
    public void simultaneousGuardShortCircuitsBeforePlanAndQualityReads()
            throws IOException {
        String source = simultaneousPilotSlice(evaluatorSource("rando"));
        int starDestroyerGuard = source.indexOf("if (isStarDestroyerDeploy) {");
        int imperialRead = source.indexOf("blueprint.hasIcon(Icon.IMPERIAL)");
        int firstOrderRead = source.indexOf("blueprint.hasIcon(Icon.FIRST_ORDER)");
        int guardPolicy = source.indexOf(
                "DeployPilotShipPolicy.evaluateSimultaneousPilotGuard(");
        int reset = source.indexOf("action.setScore(guardEvaluation.resetScore())");
        int apply = source.indexOf(
                "applyDeployPilotPolicy(action, guardEvaluation.result())");
        int adapterStep = source.indexOf(
                "DeployPilotShipPolicy.AdapterStep.CONTINUE_CANDIDATE");
        int candidateContinue = source.indexOf("continue;", adapterStep);
        int plannedRead = source.indexOf(
                "boolean plannedPilot = plannedPilotBlueprintId != null");
        int qualityGuard = source.indexOf("if (!plannedPilot) {", plannedRead);
        int costRead = source.indexOf(
                "pilotDeployCost = blueprint.getDeployCost();", qualityGuard);
        int abilityRead = source.indexOf(
                "pilotAbility = blueprint.getAbility();", costRead);
        int matchingRead = source.indexOf(
                "matchingPilot = titleLower.contains(shipNameLower)", abilityRead);
        int choicePolicy = source.indexOf(
                "DeployPilotShipPolicy.evaluateSimultaneousPilotChoice(", matchingRead);

        assertTrue(starDestroyerGuard >= 0
                && imperialRead > starDestroyerGuard
                && firstOrderRead > imperialRead
                && guardPolicy > firstOrderRead);
        assertTrue(guardPolicy < reset && reset < apply && apply < adapterStep
                && adapterStep < candidateContinue && candidateContinue < plannedRead);
        assertTrue(plannedRead < qualityGuard && qualityGuard < costRead
                && costRead < abilityRead && abilityRead < matchingRead
                && matchingRead < choicePolicy);
    }

    @Test
    public void pureOwnerHasNoEngineOrForbiddenMetadataDependencies()
            throws IOException {
        String policy = Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/DeployPilotShipPolicy.java"));
        for (String forbidden : new String[]{
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying", "DeckOracle",
                "ObjectiveAnalyzer", "DecisionOrigin", "DecisionActionSemantic",
                "DecisionWire", "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String pilotSelectionSlice(String source) {
        return slice(source,
                "private List<EvaluatedAction> evaluatePilotSelection(",
                "private List<EvaluatedAction> evaluateSimultaneousPilotSelection(");
    }

    private static String simultaneousPilotSlice(String source) {
        return slice(source,
                "private List<EvaluatedAction> evaluateSimultaneousPilotSelection(",
                "private List<EvaluatedAction> evaluateMoveDestination(");
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
