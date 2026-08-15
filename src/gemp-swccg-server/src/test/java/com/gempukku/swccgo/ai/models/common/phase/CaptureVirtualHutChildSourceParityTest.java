package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CaptureVirtualHutChildSourceParityTest {

    @Test
    public void cardSourceDefinesTheExactFreeThreeChildRoute()
            throws IOException {
        String hut = cardSource(
                "set214/light/Card214_019.java");
        String action = cardSource(
                "actions/MoveUsingLocationTextAction.java");
        String objective = cardSource(
                "set9/light/Card9_061.java");

        assertTrue(hut.contains(
                "Filter character = Filters.Luke;"));
        assertTrue(hut.contains(
                "Filter destination = Filters.Landing_Platform;"));
        assertTrue(hut.contains(
                "GameConditions.isOpponentsTurn(game, self)"));
        assertTrue(hut.contains(
                "character, self, destination, true);"));
        assertTrue(hut.contains(
                "action.setText(\"Move Luke to Landing Platform\");"));

        int origin = action.indexOf(
                "\"Choose card to move from\"");
        int destination = action.indexOf(
                "\"Choose card to move to\"", origin);
        int mover = action.indexOf(
                "\"Choose card to move to \" "
                    + "+ GameUtils.getCardLink(_destination)",
                destination);
        assertTrue(origin >= 0);
        assertTrue(destination > origin);
        assertTrue(mover > destination);

        assertTrue(objective.contains(
                "Filters.and(Filters.Luke, "
                    + "Filters.at(Filters.site))"));
        assertTrue(objective.contains(
                "Filters.Imperial, "
                    + "Filters.atSameSite(luke), "
                    + "Filters.canEscortCaptive(luke, true)"));
    }

    @Test
    public void bothBotsKeepTheChildAdapterMirrored()
            throws IOException {
        assertEquals(
                normalize(botSource("rando")),
                normalize(botSource("chosenone")));
    }

    @Test
    public void adapterBindsEveryPhysicalLegBeforeScoring()
            throws IOException {
        String source = methodSlice(
                botSource("rando"),
                "private boolean "
                    + "isCaptureVirtualHutMoveDecision(",
                "private boolean "
                    + "isHuntDownCastleMoveDecision(");

        assertOrdered(source,
                "getTopGameTextActionState()",
                "getGameTextAction()",
                "getActionSource()",
                "CaptureObjectiveFacts.isOwnedExactSource(",
                "\"214_19\"",
                "\"move luke to landing platform\"");
        assertOrdered(source,
                ".isVirtualHutOrigin(hut)",
                "samePhysicalCard(hut, targetOrigin)",
                ".virtualHutActionGuaranteesCapture(");
        assertTrue(source.contains(
                ".isGuaranteedVirtualHutDestination("));
        assertTrue(source.contains(
                ".isExactObjectiveTarget("));
        assertTrue(source.contains(
                "resolveCastleFinalDestination("));
        assertOrdered(source,
                "isCardSelectable(context, i)",
                "CaptureVirtualHutChoicePolicy.choose(",
                "CaptureObjectivePolicy.scoreCaptureRoute(");
        assertTrue(source.contains(
                "Choice.HARD_VETO"));
        int boundedAlternative = source.indexOf(
                "Choice.HARD_VETO");
        int branchEnd = source.indexOf(
                "actions.add(action);", boundedAlternative);
        String alternativeBranch = source.substring(
                boundedAlternative, branchEnd);
        assertTrue(alternativeBranch.contains(
                "addObjectiveContribution(action,"));
        assertTrue(alternativeBranch.contains("-300.0f"));
        assertTrue(alternativeBranch.contains(
                "MOVE.OBJECTIVE.TIGIH.VIRTUAL_HUT_CHILD_HOLD"));
        assertTrue(!alternativeBranch.contains("action.hardVeto("));
    }

    @Test
    public void dedicatedRoutePrecedesGenericMoveRouting()
            throws IOException {
        String source = botSource("rando");
        int dedicated = source.indexOf(
                "isCaptureVirtualHutMoveDecision(context)");
        int castle = source.indexOf(
                "isHuntDownCastleMoveDecision(context)");
        int generic = source.indexOf(
                "return evaluateMoveDestination(context);",
                dedicated);

        assertTrue(dedicated >= 0);
        assertTrue(castle > dedicated);
        assertTrue(generic > dedicated);
    }

    private static String cardSource(String relative)
            throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-cards/src/main/java/"
                    + "com/gempukku/swccgo/cards/")
                .resolve(relative));
    }

    private static String botSource(String bot)
            throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/"
                    + "com/gempukku/swccgo/ai/models/")
                .resolve(bot)
                .resolve("evaluators/CardSelectionEvaluator.java"));
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
