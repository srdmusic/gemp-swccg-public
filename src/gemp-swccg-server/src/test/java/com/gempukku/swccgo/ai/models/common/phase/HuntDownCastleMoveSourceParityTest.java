package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HuntDownCastleMoveSourceParityTest {
    private static final String OUTBOUND_ACTION =
            "Move from here to other battleground site";

    @Test
    public void castleSourceDefinesTheRealActionPromptsAndOneForceMove()
            throws IOException {
        String castle = Files.readString(repoRoot().resolve(
                "src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/"
                        + "cards/set209/dark/Card209_050.java"));
        String action = Files.readString(repoRoot().resolve(
                "src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/"
                        + "cards/actions/MoveUsingLocationTextAction.java"));
        String conditions = Files.readString(repoRoot().resolve(
                "src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/"
                        + "cards/GameConditions.java"));
        String filters = Files.readString(repoRoot().resolve(
                "src/gemp-swccg-logic/src/main/java/com/gempukku/swccgo/"
                        + "filters/Filters.java"));

        assertTrue(castle.contains(
                "Filter otherBattlegroundSites = Filters.and("
                        + "Filters.other(self), Filters.battleground_site);"));
        assertTrue(castle.contains("Filters.Vader, self, "
                + "otherBattlegroundSites, false"));
        assertTrue(castle.contains(
                "action.setText(\"" + OUTBOUND_ACTION + "\");"));
        assertTrue(castle.contains(
                "action.setText(\"Move from other battleground site to here\");"));

        int origin = action.indexOf("\"Choose card to move from\"");
        int destination = action.indexOf("\"Choose card to move to\"", origin);
        int mover = action.indexOf(
                "\"Choose card to move to \" + GameUtils.getCardLink("
                        + "_destination)",
                destination);
        assertTrue(origin >= 0);
        assertTrue(destination > origin);
        assertTrue(mover > destination);

        assertTrue("The convenience overload fixes the base cost at one Force",
                action.contains("cardToMoveFilter, fromCardFilter, "
                        + "toCardFilter, forFree, 1);"));
        assertTrue("The action pays the modifier-adjusted engine cost",
                action.contains("new PayMoveUsingLocationTextCostEffect("
                        + "this, getPerformingPlayer(), _cardToMove, "
                        + "_destination, _baseCost, 0)"));
        assertTrue("The action is offered only with a legal funded move",
                conditions.contains("Filters.canMoveToUsingLocationText("
                        + "cardToMove, forFree, baseCost, 0)"));
        assertTrue("The engine checks available Force against the exact cost",
                filters.contains("getForceAvailableToUse(gameState, "
                        + "cardToMove.getOwner())"));
        assertTrue(filters.contains("getMoveUsingLocationTextCost("
                + "gameState, cardToMove, currentLocation, "
                + "destinationLocation, baseCost, changeInCost)"));
    }

    @Test
    public void bothBotsKeepTheCastleParentAndDestinationAdaptersMirrored()
            throws IOException {
        assertEquals(normalize(evaluatorSource("rando",
                        "MoveEvaluator.java")),
                normalize(evaluatorSource("chosenone",
                        "MoveEvaluator.java")));
        assertEquals(normalize(evaluatorSource("rando",
                        "ActionTextEvaluator.java")),
                normalize(evaluatorSource("chosenone",
                        "ActionTextEvaluator.java")));
        assertEquals(normalize(evaluatorSource("rando",
                        "CardSelectionEvaluator.java")),
                normalize(evaluatorSource("chosenone",
                        "CardSelectionEvaluator.java")));
    }

    @Test
    public void parentMoveUsesCastleSourceAndTheSafeLocationTextResolver()
            throws IOException {
        String actionTextEvaluator = evaluatorSource(
                "rando", "ActionTextEvaluator.java");
        String analyzer = Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/"
                        + "ai/models/common/strategy/ObjectiveAnalyzer.java"));

        int actionText = actionTextEvaluator.indexOf(
                "\"" + OUTBOUND_ACTION.toLowerCase() + "\"");
        int sourceCard = actionTextEvaluator.indexOf(
                "gameState.findCardById(", actionText);
        int sourceBlueprint = actionTextEvaluator.indexOf(
                "\"209_50\"", sourceCard);
        int allRouteHold = actionTextEvaluator.indexOf(
                "mustHoldVaderCastleRoutes(",
                sourceBlueprint);
        int resolver = actionTextEvaluator.indexOf(
                "hasSafeVaderCastleOutboundRoute(",
                allRouteHold);
        int objectiveScore = actionTextEvaluator.indexOf(
                "objectiveActorLocationStart(", resolver);

        assertTrue("Use the exact source action text", actionText >= 0);
        assertTrue("Resolve CARD_ACTION_CHOICE cardId as the source card",
                sourceCard > actionText);
        assertTrue("Prove the action source is Vader's Castle",
                sourceBlueprint > sourceCard);
        assertTrue("Fail closed when every exact route is inadmissible",
                allRouteHold > sourceBlueprint);
        assertTrue("Resolve a safe exact route instead of treating Castle as Vader",
                resolver > allRouteHold);
        assertTrue("Score only after the resolver proves a route",
                objectiveScore > resolver);

        String helper = methodSlice(analyzer,
                "public List<VaderCastleRouteAssessment> "
                        + "assessVaderCastleRoutes(",
                "private PhysicalCard findActiveVaderCastle(");
        int actors = helper.indexOf("getAllPermanentCards()");
        int origin = helper.indexOf(
                "getLocationThatCardIsPresentAt(", actors);
        int atSource = helper.indexOf(
                "samePhysicalLocation(origin, castle)", origin);
        int engineLegality = helper.indexOf(
                "Filters.canMoveToUsingLocationText(", atSource);
        int destinationSafety = helper.indexOf(
                "FormationSafety.vetoMoveDestination(",
                engineLegality);
        int originSafety = helper.indexOf(
                "FormationSafety.vetoMoveOrigin(",
                destinationSafety);
        int objectiveAdvance = helper.indexOf(
                "advancesPreFlipActorAtRuntimeLocation(",
                originSafety);

        assertTrue("Resolve the real actor on table", actors >= 0);
        assertTrue("Resolve the actor's actual origin", origin > actors);
        assertTrue("Require that origin to be the action source",
                atSource > origin);
        assertTrue("Use the engine's location-text move legality",
                engineLegality > atSource);
        assertTrue("Keep the destination safety veto",
                destinationSafety > engineLegality);
        assertTrue("Keep the origin safety veto",
                originSafety > destinationSafety);
        assertTrue("Require structured objective progress",
                objectiveAdvance > originSafety);
    }

    @Test
    public void destinationReadsTheLiveCastleActionBeforeScoring()
            throws IOException {
        String destination = methodSlice(
                evaluatorSource("rando", "CardSelectionEvaluator.java"),
                "private boolean isHuntDownCastleMoveDecision(",
                "private PhysicalCard resolveCastleFinalDestination(");

        int actionState = destination.indexOf(
                "getTopGameTextActionState()");
        int gameTextAction = destination.indexOf(
                "getGameTextAction()", actionState);
        int actionSource = destination.indexOf(
                "getActionSource()", gameTextAction);
        int sourceBlueprint = destination.indexOf(
                "\"209_50\"", actionSource);
        int exactAction = destination.indexOf(
                "\"" + OUTBOUND_ACTION + "\"", actionSource);
        if (exactAction < 0) {
            exactAction = destination.indexOf(
                    "\"" + OUTBOUND_ACTION.toLowerCase() + "\"",
                    actionSource);
        }
        int routeAssessment = destination.indexOf(
                "assessVaderCastleRoutes(",
                exactAction);
        int destinationSafety = destination.indexOf(
                "route.admissible()", routeAssessment);
        int objectiveAdvance = destination.indexOf(
                "route.objectiveSafe()", routeAssessment);
        int objectiveScore = destination.indexOf(
                "OBJECTIVE.HUNT_DOWN.CASTLE_ROUTE",
                objectiveAdvance);

        assertTrue("Read the active engine game-text action",
                actionState >= 0);
        assertTrue(gameTextAction > actionState);
        assertTrue("Read its real source card", actionSource > gameTextAction);
        assertTrue("Require Vader's Castle provenance",
                sourceBlueprint > actionSource);
        assertTrue("Require the outbound Castle action, not the retreat",
                exactAction > actionSource);
        assertTrue("Assess every exact physical Castle route",
                routeAssessment > exactAction);
        assertTrue("Reject an inadmissible selected route",
                destinationSafety > routeAssessment);
        assertTrue("Require the selected route to be objective-safe",
                objectiveAdvance > routeAssessment);
        assertTrue("Apply the semantic score only after those proofs",
                objectiveScore > objectiveAdvance);
    }

    private static String evaluatorSource(String bot, String file)
            throws IOException {
        return Files.readString(repoRoot().resolve(
                "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/"
                        + "ai/models/" + bot + "/evaluators/" + file));
    }

    private static String methodSlice(
            String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start);
        assertTrue(start >= 0);
        assertTrue(end > start);
        return source.substring(start, end);
    }

    private static Path repoRoot() {
        Path cursor = Paths.get("").toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve(
                    "src/gemp-swccg-server/src/main/java"))
                    && Files.isDirectory(cursor.resolve(
                    "src/gemp-swccg-cards/src/main/java"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError("Could not locate repository root");
    }

    private static String normalize(String source) {
        return source.replace("models.rando", "models.BOT")
                .replace("models.chosenone", "models.BOT")
                .lines()
                .map(line -> line.stripLeading().startsWith("//")
                        ? line.stripLeading() : line)
                .collect(Collectors.joining("\n"));
    }
}
