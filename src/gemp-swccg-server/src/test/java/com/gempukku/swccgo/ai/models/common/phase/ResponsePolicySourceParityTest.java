package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResponsePolicySourceParityTest {
    @Test
    public void actionTextAdaptersStayNormalizedMirrors()
            throws IOException {
        assertEquals(normalizeBotSource(actionTextSource("rando")),
                normalizeBotSource(actionTextSource("chosenone")));
    }

    @Test
    public void directResponseAdaptersStayMirrored() throws IOException {
        String rando = directResponseBlock(
                botSource("rando", "RandoCalAi.java"));
        String chosen = directResponseBlock(
                botSource("chosenone", "TheChosenOneAi.java"));
        assertEquals(rando, chosen);
    }

    @Test
    public void recognizedResponseShapesHaveOneSharedOwner()
            throws IOException {
        String rando = botSource("rando", "RandoCalAi.java");
        String chosen = botSource("chosenone", "TheChosenOneAi.java");
        String policy = policySource();

        for (String source : new String[]{rando, chosen}) {
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.classify("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.revertApproval("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.yesNoIndexes("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.shouldDeployUndercover("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.scorePriorityCards("));
            assertFalse(source.contains("int yesIndex = 0"));
            assertFalse(source.contains("int v170YesIdx = 0"));
            assertFalse(source.contains("private int scorePriorityCards("));
        }

        assertTrue(policy.contains("enum Route"));
        assertTrue(policy.contains("return Route.LEGACY"));
        assertTrue(policy.contains("private static final int DAMAGE_CANCEL_SCORE = 100"));
        assertTrue(policy.contains("private static final int BARRIER_SCORE = 80"));
        assertTrue(policy.contains("private static final int SENSE_SCORE = 70"));
    }

    @Test
    public void fixedActionTextArmsHaveOneSharedPolicyOwner()
            throws IOException {
        String policy = policySource();

        for (String bot : new String[]{"rando", "chosenone"}) {
            String source = actionTextSource(bot);
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.scoreWhenDeployedFreeTrigger("));
            assertEquals(2, countOccurrences(source,
                    "ResponsePolicy.scoreSenseRedraw("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.scoreSaveJedi("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.scoreReact("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.scoreCancelOwn("));
            assertEquals(1, countOccurrences(source,
                    "ResponsePolicy.scoreRemainingBattleDamageCancel("));
            assertFalse(source.contains(
                    "action.addReasoning(\"Avoid reacts (bot doesn't understand timing)\""));
            assertFalse(source.contains(
                    "action.addReasoning(\"Never cancel own cards\""));
            assertFalse(source.contains(
                    "action.addReasoning(\"Cancel battle damage - valuable survival card\""));
        }

        for (String ruleId : new String[]{
                "V184-when-deployed-trigger",
                "V29.8-sense-redraw-hand",
                "V29.8-sense-mutual-redraw",
                "V53b-save-jedi",
                "RESPONSE-react",
                "RESPONSE-cancel-own",
                "RESPONSE-houjix-ghhhk"}) {
            assertEquals(ruleId, 1, countOccurrences(policy, ruleId));
        }
        assertTrue(policy.contains("TraceDomainId.RESPONSE_ROUTING"));
        assertTrue(policy.contains("TraceOutputKind.BANDED"));
    }

    @Test
    public void actionTextControlOrderMatchesLegacy()
            throws IOException {
        String source = actionTextSource("rando");

        assertInOrder(source,
                "PullActionPolicy.evaluateEarlySearch(",
                "ResponsePolicy.scoreWhenDeployedFreeTrigger(",
                "MoveTransitPolicy.capacitySlotSwap(",
                "ResponsePolicy.scoreSenseRedraw(actionId, true, false)",
                "ResponsePolicy.scoreSenseRedraw(actionId, false, true)",
                "// ========== V29.7: UNIVERSAL RESERVE DECK PULL VALIDATION");
        assertInOrder(source,
                "if (actionText.equals(\"Activate Force\"))",
                "ResponsePolicy.scoreSaveJedi(actionId)",
                "// ========== V53: BLOCK WOKLING EFFECT SEARCH");
        assertInOrder(source,
                "ResponsePolicy.scoreReact(actionId)",
                "else if (textLower.contains(\"steal\"))",
                "else if (textLower.contains(\"play sabacc\"))",
                "ResponsePolicy.scoreCancelOwn(actionId)",
                "evaluateSenseCancel(action, context, actionText, controlLedger)",
                "else if (textLower.contains(\"cancel\") && textLower.contains(\"redraw\") && textLower.contains(\"destiny\"))",
                "else if (actionText.contains(\"Cancel all remaining battle damage\"))",
                "evaluateHoujixGhhhk(action, context)");
    }

    @Test
    public void adaptersRetainObservationsTypesLogsAndFailureBoundaries()
            throws IOException {
        String source = actionTextSource("rando");
        String v184 = block(source,
                "// === V184", "// === V87");
        String sense = block(source,
                "// ========== V29.8: SENSE & UNCERTAIN",
                "// ========== V29.7: UNIVERSAL RESERVE DECK PULL VALIDATION");
        String react = block(source,
                "// ========== React", "// ========== Steal");
        String cancelOwn = block(source,
                "// ========== Cancel Own Cards",
                "// ========== Cancel Opponent's Interrupt");
        String damageCancel = block(source,
                "// ========== Cancel Battle Damage",
                "// ========== V67af");

        assertTrue(v184.contains(
                "if (gameState != null && context.getPlayerId() != null)"));
        assertTrue(v184.contains("if (v184Reveal)"));
        assertTrue(v184.contains("else if (v184Retrieve)"));
        assertTrue(v184.contains("gameState.getReserveDeckSize(v184Pid) > 0"));
        assertTrue(v184.contains("gameState.getLostPile(v184Pid)"));
        assertTrue(v184.contains("&& !textLower.contains(\"use \")"));
        assertEquals(3, countOccurrences(v184, "catch (Exception"));
        assertInOrder(v184,
                "if (v184Reveal)",
                "else if (v184Retrieve)",
                "if (v184Fire)",
                "ResponsePolicy.scoreWhenDeployedFreeTrigger(",
                "V184 WHEN-DEPLOYED TRIGGER: '{}' → +300 ({})",
                "V184 error: {}");

        assertTrue(sense.contains(
                "if (textLower.contains(\"redraw\") && textLower.contains(\"hand\"))"));
        assertTrue(sense.contains(
                "if (textLower.contains(\"each player\") && (textLower.contains(\"redraw\") || textLower.contains(\"shuffle\")))"));
        assertInOrder(sense,
                "ResponsePolicy.scoreSenseRedraw(actionId, true, false)",
                "V29.8 SENSE REDRAW BLOCKED: Attempted to redraw hand",
                "ResponsePolicy.scoreSenseRedraw(actionId, false, true)",
                "V29.8 SENSE UNCERTAIN BLOCKED: Attempted mutual redraw");

        assertInOrder(react,
                "action.setActionType(ActionType.REACT)",
                "ResponsePolicy.scoreReact(actionId)");
        assertInOrder(cancelOwn,
                "action.setActionType(ActionType.CANCEL)",
                "ResponsePolicy.scoreCancelOwn(actionId)");
        assertTrue(damageCancel.contains(
                "else if (actionText.contains(\"Cancel all remaining battle damage\"))"));
        assertFalse(damageCancel.contains(
                "textLower.contains(\"cancel all remaining battle damage\")"));
        assertInOrder(damageCancel,
                "action.setActionType(ActionType.CANCEL_DAMAGE)",
                "evaluateHoujixGhhhk(action, context)");
    }

    @Test
    public void retiredBotConfigScoresAreGone() throws IOException {
        for (String bot : new String[]{"rando", "chosenone"}) {
            String config = botSource(bot, "RandoConfig.java");
            assertFalse(config.contains("SCORE_DAMAGE_CANCEL"));
            assertFalse(config.contains("SCORE_BARRIER_USE"));
            assertFalse(config.contains("SCORE_SENSE_USE"));
        }
    }

    @Test
    public void pureResponsePolicyHasNoEngineOrGameStateSurface()
            throws IOException {
        String policy = policySource();
        for (String forbidden : new String[]{
                "AwaitingDecision", "DecisionContext", "GameState",
                "SwccgGame", "PhysicalCard", "EvaluatedAction",
                "DecisionOrigin", "DecisionActionSemantic", "DecisionWire",
                "PullDeployRef", "PullPhysicalCardRef",
                "DeployDestinationRef", "DeployPhysicalCardRef",
                "DeployActionMetadata"}) {
            assertFalse(forbidden, policy.contains(forbidden));
        }
    }

    private static String directResponseBlock(String source) {
        int start = source.indexOf(
                "ResponsePolicy.Route responseRoute");
        int end = source.indexOf(
                "// V61 EPIC EVENT SAGA CHOICE", start);
        if (start < 0 || end < 0) {
            throw new AssertionError(
                    "Could not locate direct response adapter block");
        }
        return source.substring(start, end);
    }

    private static String botSource(String bot, String file)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve(file));
    }

    private static String actionTextSource(String bot)
            throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators")
                .resolve("ActionTextEvaluator.java"));
    }

    private static String policySource() throws IOException {
        return Files.readString(mainJavaRoot().resolve(
                "com/gempukku/swccgo/ai/models/common/phase/ResponsePolicy.java"));
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
        throw new AssertionError(
                "Could not locate gemp-swccg-server main/java");
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }

    private static String normalizeBotSource(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static String block(
            String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        if (start < 0 || end < 0) {
            throw new AssertionError(
                    "Could not locate source block from " + startMarker
                            + " to " + endMarker);
        }
        return source.substring(start, end);
    }

    private static void assertInOrder(String source, String... needles) {
        int cursor = 0;
        for (String needle : needles) {
            int next = source.indexOf(needle, cursor);
            assertTrue("Missing or out of order: " + needle, next >= cursor);
            cursor = next + needle.length();
        }
    }
}
