package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployShipBoardingSourceParityTest {

    @Test
    public void shipBoardingAdaptersRemainExactMirrors() throws IOException {
        assertEquals(normalize(boardingSlice(evaluatorSource("rando"))),
                normalize(boardingSlice(evaluatorSource("chosenone"))));
    }

    @Test
    public void sharedOwnerReplacesScoresButKeepsCargoControl() throws IOException {
        String source = boardingSlice(evaluatorSource("rando"));
        assertTrue(source.contains("DeployPilotShipPolicy.evaluateShipBoarding("));
        assertTrue(source.contains("applyDeployPilotPolicy(action, boardingEvaluation.result())"));

        for (String retired : new String[]{
                "action.addReasoning(\"V29 SHIP-REF:",
                "action.addReasoning(\"V29 ABOARD SHIP:",
                "action.addReasoning(\"V29 CHARACTER ABOARD EXECUTOR:",
                "action.addReasoning(\"V29 CHARACTER ABOARD SHIP:",
                "action.addReasoning(\"⚠️ DEPLOY TO CARGO BAY = 0 POWER!"}) {
            assertFalse(retired, source.contains(retired));
        }

        assertEquals(1, count(source, "actions.add(action);"));
        assertEquals(1, count(source, "continue;"));
        assertTrue(source.contains(
                "== DeployPilotShipPolicy.AdapterStep.CONTINUE_CANDIDATE"));
    }

    @Test
    public void engineReadsAndFirstAcceptedShipMatchStayInAdapterOrder()
            throws IOException {
        String source = boardingSlice(evaluatorSource("rando"));
        int blueprint = source.indexOf(
                "getBlueprintFromId(context, deployingBlueprintId)");
        int gameText = source.indexOf("charBp.getGameText()", blueprint);
        int shipLoop = source.indexOf(
                "for (String shipName : UNIQUE_SHIP_NAMES)", gameText);
        int match = source.indexOf(
                "gameTextContainsShipName(charGameText, shipName)", shipLoop);
        int firstBreak = source.indexOf("break;", match);
        int subtype = source.indexOf("blueprint.getCardSubtype()", firstBreak);
        int policy = source.indexOf(
                "DeployPilotShipPolicy.evaluateShipBoarding(", subtype);
        int apply = source.indexOf(
                "applyDeployPilotPolicy(action, boardingEvaluation.result())", policy);
        int log = source.indexOf("logger.warn", apply);
        int control = source.indexOf("boardingEvaluation.adapterStep()", log);
        int add = source.indexOf("actions.add(action);", control);
        int next = source.indexOf("continue;", add);

        assertTrue(blueprint >= 0 && gameText > blueprint
                && shipLoop > gameText && match > shipLoop
                && firstBreak > match && subtype > firstBreak
                && policy > subtype && apply > policy && log > apply
                && control > log && add > control && next > add);
    }

    @Test
    public void tempDestinationRouteStillEndsBeforePhysicalCardBoarding()
            throws IOException {
        String source = evaluatorSource("rando");
        int tempRoute = source.indexOf("cardId.startsWith(\"temp\")");
        int physicalLookup = source.indexOf(
                "gameState.findCardById(Integer.parseInt(cardId))", tempRoute);
        int boarding = source.indexOf(
                "DeployPilotShipPolicy.evaluateShipBoarding(", physicalLookup);
        assertTrue(tempRoute >= 0 && physicalLookup > tempRoute
                && boarding > physicalLookup);
    }

    private static String boardingSlice(String source) {
        return slice(source,
                "// CRITICAL: Check if target is a STARSHIP",
                "// CRITICAL: WEAPON DEPLOYMENT");
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
