package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CaptureCriticalRetentionSourceParityTest {

    @Test
    public void cardSourcesDefineBothCriticalPackages()
            throws IOException {
        String tigih = cardSource(
                "set9/light/Card9_061.java");
        String tigihBack = cardSource(
                "set9/light/Card9_061_BACK.java");
        String bhbm = cardSource(
                "set9/dark/Card9_151.java");
        String bhbmBack = cardSource(
                "set9/dark/Card9_151_BACK.java");

        assertTrue(tigih.contains(
                "Filters.and(Filters.Luke, "
                    + "Filters.at(Filters.site))"));
        assertTrue(tigih.contains(
                "Filters.I_Feel_The_Conflict, "
                    + "true, false"));
        assertTrue(tigih.contains(
                "action.setText(\"Capture Luke\")"));
        assertTrue(tigihBack.contains(
                "Filters.or(Filters.captive, "
                    + "Filters.presentWith"));

        assertTrue(bhbm.contains(
                "Filters.Vader, "
                    + "Filters.presentWith(luke)"));
        assertTrue(bhbm.contains(
                "DeployCardFromReserveDeckEffect("
                    + "action, Filters.Emperor, -2, true)"));
        assertTrue(bhbm.contains(
                "action.setText(\"Capture Luke\")"));
        assertTrue(bhbmBack.contains(
                "DeployCardFromReserveDeckEffect("
                    + "action, Filters.Emperor, -2, true)"));
        assertTrue(bhbmBack.contains(
                "Filters.or(Filters.captive, "
                    + "Filters.presentWith"));
    }

    @Test
    public void factsProtectOnlyThePreferredPhysicalCopy()
            throws IOException {
        String facts = commonSource(
                "phase/CaptureObjectiveFacts.java");
        String preferred = methodSlice(
                facts,
                "preferredCriticalLossRole(",
                "public static int "
                    + "nextCaptureMoveForceReserve(");
        String copyChoice = methodSlice(
                facts,
                "private static boolean "
                    + "isPreferredOwnedCopy(",
                "private static int "
                    + "lossCandidateRank(");

        assertTrue(preferred.contains(
                "? Filters.Luke : Filters.Vader"));
        assertTrue(preferred.contains(
                "? Filters.I_Feel_The_Conflict "
                    + ": Filters.Emperor"));
        assertEquals(2, occurrences(
                preferred, "isPreferredOwnedCopy("));
        assertOrdered(copyChoice,
                "getAllPermanentCards()",
                "playerId.equals(card.getOwner())",
                "criticalCopyRoute(",
                "route == null",
                "lossCandidateRank(",
                "route.rank",
                "route.cost",
                "card.getPermanentCardId()",
                "samePhysicalCard(");
        assertTrue(copyChoice.contains(
                "Filters.persona(Persona.SIDIOUS)"));
        assertTrue(copyChoice.contains(
                "liveObjectiveDeployCost("));
        assertTrue(copyChoice.contains(
                "liveNormalDeployCost("));
    }

    @Test
    public void adapterCoversPreferredAndStableBackRoles()
            throws IOException {
        String source = botSource("rando");
        String adapter = methodSlice(
                source,
                "private void "
                    + "applyCaptureCriticalRetention(",
                "private ForceLossPolicy.ObjectiveFlags "
                    + "forceLossObjectiveFlags(");

        assertOrdered(adapter,
                "CaptureObjectiveFacts.objectiveKind(",
                "CaptureObjectiveFacts."
                    + "preferredCriticalLossRole(",
                "CaptureObjectiveFacts",
                ".wouldBreakStableBackIfRemoved(",
                "CriticalRole",
                ".CAPTURE_PIECE",
                "CaptureObjectivePolicy."
                    + "scoreCriticalRetention(",
                "new CaptureObjectivePolicy."
                    + "RetentionFacts(",
                "PolicyOperationAdapter.apply(");
    }

    @Test
    public void adapterPrecedesEveryLossAndForfeitExit()
            throws IOException {
        String source = botSource("rando");
        String forceLoss = methodSlice(
                source,
                "private List<EvaluatedAction> "
                    + "evaluateForceLoss(",
                "private void "
                    + "applyCaptureCriticalRetention(");
        String forfeit = methodSlice(
                source,
                "private List<EvaluatedAction> "
                    + "evaluateForfeit(",
                "private List<EvaluatedAction> "
                    + "evaluateForceLossOrForfeit(");
        String combined = methodSlice(
                source,
                "private List<EvaluatedAction> "
                    + "evaluateForceLossOrForfeit(",
                "private int extractNumberAfter(");

        assertOrdered(forceLoss,
                "findCardById(",
                "applyCaptureCriticalRetention(",
                "ForceLossFacts.readCandidate(",
                "ForceLossPolicy.score(");
        assertOrdered(forfeit,
                "applyCaptureCriticalRetention(",
                "if (isOptional "
                    + "&& optionalDamageRemaining <= 0)",
                "continue;",
                "else if (isOptional "
                    + "&& optionalDamageRemaining > 0)",
                "continue;  // Skip normal scoring");
        assertOrdered(combined,
                "card = gameState.findCardById(",
                "combinedCardsById.put(",
                "battleCandidate = combinedCardsById.get(",
                "applyCaptureCriticalRetention(",
                "BattleForfeitPolicy.evaluateCombined(",
                "AdapterStep.CONTINUE_CANDIDATE",
                "continue;");
    }

    @Test
    public void bothBotsKeepTheRetentionAdapterMirrored()
            throws IOException {
        assertEquals(
                normalize(botSource("rando")),
                normalize(botSource("chosenone")));
    }

    private static String cardSource(String relative)
            throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-cards/src/main/java/"
                    + "com/gempukku/swccgo/cards/")
                .resolve(relative));
    }

    private static String commonSource(String relative)
            throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/"
                    + "com/gempukku/swccgo/ai/models/common/")
                .resolve(relative));
    }

    private static String botSource(String bot)
            throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/"
                    + "com/gempukku/swccgo/ai/models/")
                .resolve(bot)
                .resolve(
                    "evaluators/CardSelectionEvaluator.java"));
    }

    private static String methodSlice(
            String source,
            String startToken,
            String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return source.substring(start, end);
    }

    private static void assertOrdered(
            String source,
            String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = source.indexOf(
                    token, previous + 1);
            assertTrue(
                    "Missing or out-of-order token: "
                        + token,
                    current > previous);
            previous = current;
        }
    }

    private static int occurrences(
            String source,
            String token) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(
                token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private static Path repoRoot() {
        Path cursor = Paths.get("")
                .toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve(
                    "src/gemp-swccg-server/src/main/java"))
                    && Files.isDirectory(cursor.resolve(
                        "src/gemp-swccg-cards/src/main/java"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError(
                "Could not locate repository root");
    }

    private static String normalize(String source) {
        return source
                .replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT");
    }
}
