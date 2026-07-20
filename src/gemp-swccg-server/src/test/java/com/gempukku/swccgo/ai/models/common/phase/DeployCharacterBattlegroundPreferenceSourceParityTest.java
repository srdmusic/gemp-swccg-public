package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeployCharacterBattlegroundPreferenceSourceParityTest {

    private static final String SLICE_START =
            "// === V29.7: BATTLEGROUND PREFERENCE FOR CHARACTER DEPLOYMENT ===";
    private static final String SLICE_END =
            "// === V24.3B: DR. EVAZAN WEAPON COMBO — DEPLOY LOCATION PREFERENCE ===";

    @Test
    public void randoAndChosenOneAdaptersStayExactMirrors() throws IOException {
        assertEquals(normalize(slice(source("rando"))),
                normalize(slice(source("chosenone"))));
    }

    @Test
    public void adaptersRetainCandidateControlReadsCatchesAndOrder()
            throws IOException {
        String adapter = slice(source("rando"));

        assertTrue(adapter.contains(
                "if (isCharacter && location != null && game != null && gameState != null)"));
        assertEquals(1, count(adapter,
                ".scoreCharacterBattlegroundPreference("));
        assertEquals(1, count(adapter, "break;"));
        assertEquals(3, count(adapter, "catch (Exception"));
        assertFalse(adapter.contains("action.addReasoning("));

        assertOrdered(adapter,
                "isBattleground(gameState, location, null)",
                "boolean anyBattlegroundExists = false;",
                "int v67ahOppIcons = 0;",
                "if (!isBattlegroundSite)",
                "gameState.getTopLocations()",
                "if (bgLoc != null)",
                "isBattleground(gameState, bgLoc, null)",
                "anyBattlegroundExists = true;",
                "break;",
                "catch (Exception bgE) { /* ignore */ }",
                "if (anyBattlegroundExists)",
                "context.getSide()",
                "location.getBlueprint()",
                "getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE)",
                "getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE)",
                "catch (Exception e) { /* ignore */ }",
                "new DeployFormationSitingPolicy",
                ".CharacterBattlegroundPreferenceFacts(",
                "applyDeploySitingPolicy(action,",
                ".scoreCharacterBattlegroundPreference(",
                "catch (Exception e)",
                "logger.debug(\"V29.7 BATTLEGROUND: Error checking battleground status: {}\"");
    }

    @Test
    public void sharedPolicyContainsNoGempObservationTypes() throws IOException {
        String policy = Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/DeployFormationSitingPolicy.java"));
        for (String forbidden : new String[]{
                "DecisionContext", "EvaluatedAction", "PhysicalCard", "SwccgGame",
                "GameState", "ModifiersQuerying", "SwccgCardBlueprint",
                "com.gempukku.swccgo.common.Side",
                "com.gempukku.swccgo.common.Icon"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String source(String bot) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators/CardSelectionEvaluator.java"));
    }

    private static String slice(String source) {
        int start = source.indexOf(SLICE_START);
        int end = source.indexOf(SLICE_END, start);
        assertTrue(SLICE_START, start >= 0);
        assertTrue(SLICE_END, end > start);
        return source.substring(start, end);
    }

    private static void assertOrdered(String source, String... needles) {
        int cursor = -1;
        for (String needle : needles) {
            int next = source.indexOf(needle, cursor + 1);
            assertTrue(needle, next > cursor);
            cursor = next;
        }
    }

    private static int count(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
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
}
