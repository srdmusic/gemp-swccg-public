package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.Set;

/** Shared, bot-neutral ObjectiveFacts inputs for model and later adapter parity tests. */
final class ObjectiveFactsFixtures {

    static final int OBJECTIVE_PERMANENT_CARD_ID = 9001;
    static final int OBJECTIVE_CURRENT_CARD_ID = 501;
    static final String FRONT_BLUEPRINT_ID = "226_28";
    static final String BACK_BLUEPRINT_ID = "226_28_BACK";
    static final String FRONT_TITLE = "The Hidden Path";
    static final String BACK_TITLE = "Gather Allies And Train";
    static final String FRONT_GAME_TEXT =
            "Deploy Mining Village, Safehouse, Underground Corridor, and Fallen Order. "
                    + "For remainder of game, you may not deploy <> locations, Anakin, or Jedi "
                    + "(except Jedi survivors) or play A Jedi's Resilience. Weapon Levitation may "
                    + "not steal weapons. Once per turn, may [download] a Jabiim location. While "
                    + "this side up, you may not play Nabrun Leids. Your Force drains at Mapuzo "
                    + "sites are -1. Once per turn, may [download] a holocron. Flip this card if "
                    + "Jedi occupy two non-Mapuzo sites.";
    static final String BACK_GAME_TEXT =
            "While this side up, Jedi survivors are deploy -1. If your holocron is about to leave "
                    + "table, place it in Used Pile. Opponent's total battle destiny where they have "
                    + "a character of ability > 4 is -1. During your move phase, may use 2 Force to "
                    + "relocate a Jedi between a Jabiim site and a battleground site as a regular "
                    + "move. At the end of opponent's turn, if Jedi occupy two battleground sites, "
                    + "opponent loses 1 Force. Flip this card if Jedi do not occupy two non-Mapuzo "
                    + "sites.";

    static final Set<Integer> SENATOR_CARD_IDS = Set.of(101, 102);
    static final Set<Integer> JEDI_SURVIVOR_CARD_IDS = Set.of(201, 202);
    static final Set<Integer> INQUISITOR_CARD_IDS = Set.of(301, 302);
    static final Set<Integer> INQUISITOR_WITH_HATRED_CARD_IDS = Set.of(302);
    static final Set<Integer> GALACTIC_SENATE_LOCATION_IDS = Set.of(401);
    static final Set<Integer> OBJECTIVE_RELEVANT_LOCATION_IDS = Set.of(401, 402);
    static final Set<Integer> FLIP_BACK_PROTECTION_LOCATION_IDS = Set.of(403);

    private ObjectiveFactsFixtures() {
    }

    static ObjectiveFacts.Identity identity(boolean flipped) {
        return identity(
                flipped,
                flipped ? BACK_BLUEPRINT_ID : FRONT_BLUEPRINT_ID,
                flipped ? FRONT_BLUEPRINT_ID : BACK_BLUEPRINT_ID,
                flipped ? BACK_TITLE : FRONT_TITLE,
                flipped ? FRONT_TITLE : BACK_TITLE,
                flipped ? BACK_GAME_TEXT : FRONT_GAME_TEXT,
                flipped ? FRONT_GAME_TEXT : BACK_GAME_TEXT);
    }

    static ObjectiveFacts.Identity identity(boolean flipped,
                                             String currentBlueprintId,
                                             String oppositeBlueprintId,
                                             String currentTitle,
                                             String oppositeTitle,
                                             String currentGameText,
                                             String oppositeGameText) {
        return new ObjectiveFacts.Identity(
                OBJECTIVE_PERMANENT_CARD_ID,
                OBJECTIVE_CURRENT_CARD_ID,
                FRONT_BLUEPRINT_ID,
                BACK_BLUEPRINT_ID,
                currentBlueprintId,
                oppositeBlueprintId,
                FRONT_TITLE,
                BACK_TITLE,
                currentTitle,
                oppositeTitle,
                FRONT_GAME_TEXT,
                BACK_GAME_TEXT,
                currentGameText,
                oppositeGameText,
                flipped);
    }

    static ObjectiveFacts.ProfileResolution profileResolution(
            ObjectiveFacts.ProfileResolution.MatchKind matchKind) {
        String label = matchKind == ObjectiveFacts.ProfileResolution.MatchKind.NONE
                ? ""
                : matchKind.name().toLowerCase();
        return new ObjectiveFacts.ProfileResolution(matchKind, label, false, false, false);
    }

    static ObjectiveFacts facts(boolean flipped) {
        return new ObjectiveFacts(
                known(identity(flipped), "physical objective orientation"),
                known(profileResolution(ObjectiveFacts.ProfileResolution.MatchKind.BLUEPRINT_ID),
                        "blueprint-first objective profile"),
                known(strategyFacts(), "typed objective strategy"),
                known(typedBoardFacts(), "typed objective board"));
    }

    static ObjectiveFacts factsFromMutableCollections(Set<String> strings, Set<Integer> cardIds) {
        ObjectiveFacts.StartingRefs startingRefs = new ObjectiveFacts.StartingRefs(
                strings, strings, strings, strings, strings, strings);
        ObjectiveFacts.StrategyFacts strategy = new ObjectiveFacts.StrategyFacts(
                new ObjectiveFacts.ObjectiveKind(false, false, false, true, false, false),
                strings,
                strings,
                strings,
                strings,
                startingRefs,
                strings,
                known("site", "mutable fixture site"),
                known("card", "mutable fixture card"),
                known("front", "mutable fixture front text"),
                known("back", "mutable fixture back text"),
                true,
                false,
                true,
                false,
                strings);
        ObjectiveFacts.TypedBoardFacts board = new ObjectiveFacts.TypedBoardFacts(
                cardIds,
                cardIds,
                cardIds,
                cardIds,
                cardIds,
                cardIds,
                cardIds,
                true,
                2,
                true,
                false,
                false,
                false);
        return new ObjectiveFacts(
                known(identity(false), "physical objective orientation"),
                known(profileResolution(ObjectiveFacts.ProfileResolution.MatchKind.BLUEPRINT_ID),
                        "blueprint-first objective profile"),
                known(strategy, "typed objective strategy"),
                known(board, "typed objective board"));
    }

    static ObjectiveFacts.StrategyFacts strategyFacts() {
        return new ObjectiveFacts.StrategyFacts(
                new ObjectiveFacts.ObjectiveKind(false, false, false, true, false, false),
                Set.of("non-mapuzo site"),
                Set.of("non-mapuzo site"),
                Set.of(),
                Set.of("jabiim location", "holocron"),
                new ObjectiveFacts.StartingRefs(
                        Set.of("226_21", "226_22", "226_23"),
                        Set.of("mining village", "safehouse", "underground corridor"),
                        Set.of("226_14"),
                        Set.of("fallen order"),
                        Set.of(),
                        Set.of()),
                Set.of(),
                unknown("flip-critical control site", "The Hidden Path has no control gate"),
                unknown("flip-critical control card", "The Hidden Path has no control gate"),
                known("Jedi occupy two non-Mapuzo sites", "front-side flip condition"),
                known("Jedi do not occupy two non-Mapuzo sites", "back-side flip condition"),
                true,
                false,
                true,
                false,
                Set.of("jedi survivor"));
    }

    static ObjectiveFacts.TypedBoardFacts typedBoardFacts() {
        return boardWithHiddenPathCount(2, true);
    }

    static ObjectiveFacts.TypedBoardFacts boardWithHiddenPathCount(int count, boolean conditionMet) {
        return new ObjectiveFacts.TypedBoardFacts(
                SENATOR_CARD_IDS,
                JEDI_SURVIVOR_CARD_IDS,
                INQUISITOR_CARD_IDS,
                INQUISITOR_WITH_HATRED_CARD_IDS,
                GALACTIC_SENATE_LOCATION_IDS,
                OBJECTIVE_RELEVANT_LOCATION_IDS,
                FLIP_BACK_PROTECTION_LOCATION_IDS,
                true,
                count,
                conditionMet,
                false,
                false,
                false);
    }

    static TraceSnapshots.Input traceInput(ObjectiveFacts objectiveFacts) {
        TraceSnapshots.Input input = new TraceSnapshots.Input();
        input.producerId = "objective-facts-fixture";
        input.decisionId = "objective-facts-decision";
        input.decisionTypeName = AwaitingDecisionType.INTEGER.name();
        input.decisionText = "Choose an amount";
        input.phase = Phase.DEPLOY;
        input.turn = 3;
        input.currentPlayer = "light-player";
        input.side = Side.LIGHT;
        input.noPassParam = false;
        input.minParam = 0;
        input.maxParam = 1;
        input.objectiveFacts = objectiveFacts;
        return input;
    }

    private static <T> FactValue<T> known(T value, String provenance) {
        return FactValue.known(value, ObjectiveFacts.PRODUCER, provenance);
    }

    private static <T> FactValue<T> unknown(String provenance, String reason) {
        return FactValue.unknown(ObjectiveFacts.PRODUCER, provenance, reason);
    }
}
