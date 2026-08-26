package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForceUtilityResidualSourceOwnershipTest {

    @Test
    public void actionAndSelectionAdaptersRemainNormalizedMirrors()
            throws IOException {
        assertEquals(normalize(evaluator("rando", "ActionTextEvaluator.java")),
                normalize(evaluator("chosenone", "ActionTextEvaluator.java")));
        assertEquals(normalize(evaluator("rando", "CardSelectionEvaluator.java")),
                normalize(evaluator("chosenone", "CardSelectionEvaluator.java")));
    }

    @Test
    public void actionTextFirstMatchOrderStaysExact() throws IOException {
        assertInOrder(evaluator("rando", "ActionTextEvaluator.java"),
                "ControlActionPolicy.noEscapeRetrieval(actionId)",
                "PullSpecificActionPolicy.scorePileSearch(",
                "BattleActionTextPolicy.scoreVaderRecall(",
                "BattleActionTextPolicy.scoreInquisitorRecall(",
                "PullSpecificActionPolicy.scoreAdmiralGeneralPull(",
                "evaluateSenseCancel(action, context, actionText, controlLedger)",
                "evaluateTakeIntoHand(action, context, actionText, textLower)",
                "actionText.contains(\"LOST: Reveal opponent's hand\")",
                "actionText.contains(\"Make opponent lose\")",
                "actionText.contains(\"Place in Lost Pile\")",
                "ControlActionPolicy.retrieve(",
                "actionText.startsWith(\"USED: Peek at top\")",
                "ResponsePolicy.scoreLateForceDrainCancel(",
                ".contains(\"maintenance\")",
                "ForceLossPolicy.ActionTextChoice.GENERIC_USE_UPKEEP",
                "ForceLossPolicy.ActionTextChoice.GENERIC_LOSE",
                "ForceLossPolicy.ActionTextChoice.GENERIC_SACRIFICE",
                "PullActionPolicy.evaluateParent(");
    }

    @Test
    public void opponentDrainCancellationRemainsResponseOnlyAndExactlyOnce()
            throws IOException {
        String response = phase("ResponsePolicy.java");
        String control = phase("ControlActionPolicy.java");
        String actionText = evaluator("rando", "ActionTextEvaluator.java");

        assertEquals(1, occurrences(response,
                "RESPONSE-sense-force-drain-opponent"));
        assertEquals(1, occurrences(response,
                "RESPONSE-late-force-drain-opponent"));
        assertFalse(control.contains("force-drain-opponent"));
        assertEquals(1, occurrences(actionText,
                "ResponsePolicy.scoreSenseCancel("));
        assertEquals(1, occurrences(actionText,
                "ResponsePolicy.scoreLateForceDrainCancel("));
        assertEquals(2, occurrences(actionText,
                "ControlActionPolicy.selfCancelDrain("));
    }

    @Test
    public void unknownSelectionKeepsRoutingOrderAndPolicyOperationOrder()
            throws IOException {
        String adapter = evaluator("rando", "CardSelectionEvaluator.java");
        String unknown = between(adapter,
                "private List<EvaluatedAction> evaluateUnknown(DecisionContext context)",
                "private boolean isLikelyBattleground(");
        assertInOrder(unknown,
                "if (!isCardSelectable(context, i))",
                "PullDeployCandidatePolicy.evaluate(",
                "ShieldPolicy.unknownBattleOrderGate(",
                "SetupPolicy.startingEffectBan(cardTitle)",
                "if (setupBan.terminalCandidate())",
                "ForceLossPolicy.scoreUnknownLoss(",
                "applyUnknownPullSelectionPolicy(");

        String forceLoss = phase("ForceLossPolicy.java");
        assertInOrder(forceLoss,
                "FORCE-LOSS-unknown-effect-interrupt",
                "V25-unknown-loss");
    }

    @Test
    public void residualArithmeticHasOneSharedOwner() throws IOException {
        String actionText = evaluator("rando", "ActionTextEvaluator.java");
        String selection = evaluator("rando", "CardSelectionEvaluator.java");
        String forceLoss = phase("ForceLossPolicy.java");
        String control = phase("ControlActionPolicy.java");
        String pull = phase("PullActionPolicy.java");
        String pullSpecific = phase("PullSpecificActionPolicy.java");
        String takeCandidate = phase("PullTakeCandidatePolicy.java");

        for (String reason : new String[] {
                "Avoid losing cards",
                "V74 MAINTENANCE PAY: keep the card alive!",
                "V74 MAINTENANCE SACRIFICE: place out of play is PERMANENT loss!",
                "V74 MAINTENANCE USED-PILE: lose card to used pile, keep blueprint",
                "V74 MAINTENANCE SACRIFICE: avoid",
                "V22.3 MAINTENANCE: Pay upkeep cost!",
                "'Use Force' action",
                "'Lose Force' action",
                "V22.3: Avoid sacrificing cards"}) {
            assertFalse(reason, actionText.contains(reason));
            assertTrue(reason, forceLoss.contains(reason));
        }
        for (String reason : new String[] {
                "Opponent has many cards - reveal worth it",
                "Opponent has few cards - save reveal",
                "Making opponent lose force",
                "High lost pile - retrieve worth it",
                "Low lost pile - save retrieve",
                "Peek for card advantage"}) {
            assertFalse(reason, actionText.contains(reason));
            assertTrue(reason, control.contains(reason));
        }
        for (String reason : new String[] {
                "Avoid taking Palpatine",
                "V29.7 BOUNCE: Return own card from table to hand",
                "V63 LOST PILE EMPTY: no matching target in Lost Pile",
                "Take card into hand from Lost Pile"}) {
            assertFalse(reason, actionText.contains(reason));
            assertTrue(reason, pull.contains(reason));
            assertFalse(reason, pullSpecific.contains(reason));
            assertFalse(reason, takeCandidate.contains(reason));
        }
        assertFalse(actionText.contains(
                "action.addReasoning(\"Take card into hand\""));
        assertTrue(pull.contains("\"Take card into hand\""));
        assertFalse(pullSpecific.contains("\"Take card into hand\""));
        assertFalse(takeCandidate.contains("\"Take card into hand\""));
        for (String reason : new String[] {
                "Effect/Interrupt - OK to lose",
                "Character - avoid losing",
                "Starship - avoid losing",
                "Vehicle - avoid losing",
                "Location - avoid losing",
                "V25 HUNT DOWN: PROTECT LIGHTSABER from loss!"}) {
            assertFalse(reason, selection.contains(reason));
            assertTrue(reason, forceLoss.contains(reason));
        }

        String v25Comment = "=== V25: HUNT DOWN V \u2014 LIGHTSABER PRIORITY "
                + "(evaluateUnknown path) ===";
        assertEquals(1, occurrences(selection, v25Comment));
        assertEquals(1, occurrences(
                evaluator("chosenone", "CardSelectionEvaluator.java"),
                v25Comment));

        assertTrue(pull.contains("public enum TakeIntoHandKind"));
        assertTrue(pull.contains("public static PolicyResult scoreTakeIntoHand("));
        assertFalse(pullSpecific.contains("TakeIntoHandKind"));
        assertFalse(pullSpecific.contains("scoreTakeIntoHand("));
        assertFalse(takeCandidate.contains("TakeIntoHandKind"));
        assertFalse(takeCandidate.contains("scoreTakeIntoHand("));
    }

    @Test
    public void sharedOwnersRemainPureAndAllOperationsStayAdditive()
            throws IOException {
        String pullAction = phase("PullActionPolicy.java");
        String policies = phase("ForceLossPolicy.java")
                + phase("ControlActionPolicy.java")
                + pullAction
                + phase("PullSpecificActionFacts.java")
                + phase("PullSpecificActionPolicy.java");
        for (String forbidden : new String[] {
                "DecisionContext", "EvaluatedAction", "PhysicalCard",
                "SwccgGame", "GameState", "ModifiersQuerying"}) {
            assertFalse(forbidden, policies.contains(forbidden));
        }
        String residualOwners = phase("ForceLossPolicy.java")
                + phase("ControlActionPolicy.java")
                + between(pullAction,
                "public static PolicyResult scoreTakeIntoHand(",
                "public static Evaluation evaluateParent(")
                + phase("PullSpecificActionPolicy.java");
        assertEquals(1, occurrences(
                residualOwners, "PolicyOperation.hardVeto("));
        assertTrue(residualOwners.contains(
                "TraceRuleId.of(\"V53d-wokling-location-ramp\")"));
        assertFalse(residualOwners.contains("PolicyOperation.defer("));
    }

    private static String evaluator(String bot, String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models")
                .resolve(bot).resolve("evaluators").resolve(file));
    }

    private static String phase(String file) throws IOException {
        return Files.readString(mainJavaRoot()
                .resolve("com/gempukku/swccgo/ai/models/common/phase")
                .resolve(file));
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        assertTrue("missing start " + start, from >= 0);
        assertTrue("missing end " + end, to > from);
        return source.substring(from, to);
    }

    private static void assertInOrder(String source, String... needles) {
        int previous = -1;
        for (String needle : needles) {
            int next = source.indexOf(needle, previous + 1);
            assertTrue("missing or out of order: " + needle, next > previous);
            previous = next;
        }
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
