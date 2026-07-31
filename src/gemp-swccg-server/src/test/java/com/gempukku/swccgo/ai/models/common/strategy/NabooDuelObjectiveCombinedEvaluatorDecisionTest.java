package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Production evaluator proof for the mirrored Naboo duel objective actions. */
public class NabooDuelObjectiveCombinedEvaluatorDecisionTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";
    private static final int OBJECTIVE_ID = 100;
    private static final int WRONG_SOURCE_ID = 101;
    private static final int TACTICAL_LOCATION_ID = 102;
    private static final int TACTICAL_MOVER_ID = 103;
    private static final int DUELIST_ONE_ID = 200;
    private static final int DUELIST_TWO_ID = 201;
    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    private static final List<Family> FAMILIES = List.of(
            new Family("We'll Handle This", "13_46", "13_46_BACK",
                    Side.LIGHT, "1_21"),
            new Family("Let Them Make The First Move", "13_73", "13_73_BACK",
                    Side.DARK, "1_168"));

    @Test
    public void exactFrontTargetActionBeatsWrongSourceAndPass() {
        assertExactActionWins(false, "Target character",
                "NABOO DUEL FRONT");
    }

    @Test
    public void exactBackLightsaberCombatActionBeatsWrongSourceAndPass() {
        assertExactActionWins(true, "Initiate lightsaber combat",
                "NABOO DUEL BACK");
    }

    @Test
    public void exactFrontTargetActionBeatsForceDrainInControlPhase() {
        assertExactActionBeatsOrdinaryTactic(
                false, Phase.CONTROL,
                "Target character", "NABOO DUEL FRONT",
                "Force drain", TACTICAL_LOCATION_ID);
    }

    @Test
    public void exactBackLightsaberCombatBeatsOrdinaryMoveInMovePhase() {
        assertExactActionBeatsOrdinaryTactic(
                true, Phase.MOVE,
                "Initiate lightsaber combat", "NABOO DUEL BACK",
                "Move using landspeed", TACTICAL_MOVER_ID);
    }

    private static void assertExactActionWins(
            boolean flipped, String actionText, String marker) {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new java.util.ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, flipped);
                Decision decision = Decision.actions(
                        flipped ? Phase.MOVE : Phase.CONTROL,
                        List.of("exact", "wrong", "pass"),
                        List.of(actionText, actionText, "Pass"),
                        List.of(String.valueOf(OBJECTIVE_ID),
                                String.valueOf(WRONG_SOURCE_ID), ""));

                List<Outcome> direct = actionText(bot, fixture, decision);
                Outcome exact = only(direct, "exact");
                Outcome wrong = only(direct, "wrong");
                assertContains(exact, marker);
                assertTrue(label(bot, family), exact.score > wrong.score);

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family), "exact", winner.actionId);
                parity.add(winner);
            }
            assertParity(parity);
        }
    }

    private static void assertExactActionBeatsOrdinaryTactic(
            boolean flipped, Phase phase,
            String exactText, String marker,
            String ordinaryText, int ordinarySourceId) {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new java.util.ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, flipped);
                Decision decision = Decision.actions(
                        phase,
                        List.of("exact", "ordinary", "pass"),
                        List.of(exactText, ordinaryText, "Pass"),
                        List.of(String.valueOf(OBJECTIVE_ID),
                                String.valueOf(ordinarySourceId), ""));

                Outcome exact = only(
                        actionText(bot, fixture, decision), "exact");
                assertContains(exact, marker);

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family), "exact", winner.actionId);
                assertContains(winner, marker);
                parity.add(winner);
            }
            assertParity(parity);
        }
    }

    @Test
    public void forceLossProtectsExactlyOneUndeployedDuelist() {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new java.util.ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, false);
                for (Decision decision : List.of(
                        Decision.forceLoss(
                                List.of(String.valueOf(DUELIST_ONE_ID),
                                        String.valueOf(DUELIST_TWO_ID))),
                        Decision.combinedForceLoss(
                                List.of(String.valueOf(DUELIST_ONE_ID),
                                        String.valueOf(DUELIST_TWO_ID))))) {
                    List<Outcome> direct = cardSelection(
                            bot, fixture, decision);
                    Outcome protectedCopy = only(
                            direct, String.valueOf(DUELIST_ONE_ID));
                    Outcome expendableCopy = only(
                            direct, String.valueOf(DUELIST_TWO_ID));
                    assertContains(protectedCopy,
                            "NABOO DUEL: retain exactly one");
                    assertTrue(label(bot, family),
                            expendableCopy.score > protectedCopy.score);

                    Outcome winner = combined(bot, fixture, decision);
                    assertEquals(label(bot, family),
                            String.valueOf(DUELIST_TWO_ID),
                            winner.actionId);
                    parity.add(winner);
                }
            }
            assertParity(List.of(parity.get(0), parity.get(2)));
            assertParity(List.of(parity.get(1), parity.get(3)));
        }
    }

    @Test
    public void deployedDuelistDisablesHandCopyRetention() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, true);
                PhysicalCard deployed = handCard(
                        family.ownDuelist, 300);
                when(deployed.getZone()).thenReturn(Zone.AT_LOCATION);
                ModifiersQuerying modifiers =
                        fixture.game.getModifiersQuerying();
                float deployedAbility =
                        deployed.getBlueprint().getAbility();
                when(modifiers.getAbility(
                        fixture.gameState, deployed)).thenReturn(
                            deployedAbility);
                when(fixture.gameState.isCardInPlayActive(
                        deployed,
                        true, false, false, false,
                        false, false, false, false)).thenReturn(true);
                List<PhysicalCard> permanents = new java.util.ArrayList<>(
                        fixture.gameState.getAllPermanentCards());
                permanents.add(deployed);
                when(fixture.gameState.getAllPermanentCards())
                        .thenReturn(permanents);

                List<Outcome> direct = cardSelection(
                        bot, fixture, Decision.forceLoss(
                                List.of(String.valueOf(DUELIST_ONE_ID),
                                        String.valueOf(DUELIST_TWO_ID))));
                assertNoReason(direct, "NABOO DUEL: retain exactly one");
            }
        }
    }

    private static Fixture fixture(
            Bot bot, Family family, boolean flipped) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = objective(family, flipped);
        PhysicalCard wrongSource = mock(PhysicalCard.class);
        SwccgCardBlueprint wrongBlueprint = mock(SwccgCardBlueprint.class);
        when(wrongSource.getOwner()).thenReturn(PLAYER);
        when(wrongSource.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(wrongSource.getBlueprint()).thenReturn(wrongBlueprint);
        when(wrongSource.getCardId()).thenReturn(WRONG_SOURCE_ID);
        when(wrongSource.getPermanentCardId()).thenReturn(WRONG_SOURCE_ID);
        when(wrongBlueprint.getCardCategory()).thenReturn(CardCategory.EFFECT);
        PhysicalCard tacticalLocation = tacticalLocation();
        PhysicalCard tacticalMover = tacticalMover(tacticalLocation);
        PhysicalCard duelistOne = handCard(
                family.ownDuelist, DUELIST_ONE_ID);
        PhysicalCard duelistTwo = handCard(
                family.ownDuelist, DUELIST_TWO_ID);
        float duelistOneAbility = duelistOne.getBlueprint().getAbility();
        float duelistTwoAbility = duelistTwo.getBlueprint().getAbility();
        when(modifiers.getAbility(
                gameState, duelistOne)).thenReturn(
                    duelistOneAbility);
        when(modifiers.getAbility(
                gameState, duelistTwo)).thenReturn(
                    duelistTwoAbility);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getSide(PLAYER)).thenReturn(family.side);
        when(gameState.getSide(OPPONENT)).thenReturn(
                family.side == Side.LIGHT ? Side.DARK : Side.LIGHT);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(3);
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(20);
        when(gameState.getForcePileSize(PLAYER)).thenReturn(10);
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getHand(PLAYER)).thenReturn(
                List.of(duelistOne, duelistTwo));
        when(gameState.getAllStackedCards()).thenReturn(List.of());
        when(gameState.getLocationsInOrder()).thenReturn(List.of());
        when(gameState.getTopLocations()).thenReturn(
                List.of(tacticalLocation));
        when(gameState.getCardsAtLocation(tacticalLocation)).thenReturn(
                List.of(tacticalMover));
        when(gameState.getAllPermanentCards())
                .thenReturn(List.of(objective, wrongSource));
        when(gameState.findCardById(anyInt())).thenAnswer(invocation -> {
            int id = invocation.getArgument(0, Integer.class);
            if (id == OBJECTIVE_ID) return objective;
            if (id == WRONG_SOURCE_ID) return wrongSource;
            if (id == TACTICAL_LOCATION_ID) return tacticalLocation;
            if (id == TACTICAL_MOVER_ID) return tacticalMover;
            if (id == DUELIST_ONE_ID) return duelistOne;
            if (id == DUELIST_TWO_ID) return duelistTwo;
            return null;
        });
        when(gameState.findCardByPermanentId(anyInt())).thenAnswer(invocation -> {
            int id = invocation.getArgument(0, Integer.class);
            if (id == OBJECTIVE_ID) return objective;
            if (id == WRONG_SOURCE_ID) return wrongSource;
            if (id == TACTICAL_LOCATION_ID) return tacticalLocation;
            if (id == TACTICAL_MOVER_ID) return tacticalMover;
            if (id == DUELIST_ONE_ID) return duelistOne;
            if (id == DUELIST_TWO_ID) return duelistTwo;
            return null;
        });
        when(modifiers.getForceAvailableToUse(gameState, PLAYER)).thenReturn(10);
        when(modifiers.getForceDrainAmount(
                gameState, tacticalLocation, PLAYER)).thenReturn(2.0f);
        when(modifiers.getInitiateForceDrainCost(
                gameState, tacticalLocation, PLAYER)).thenReturn(0.0f);
        when(modifiers.isBattleground(
                gameState, tacticalLocation, null)).thenReturn(true);
        when(modifiers.getTotalPowerAtLocation(
                gameState, tacticalLocation,
                PLAYER, false, false)).thenReturn(4.0f);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, tacticalMover)).thenReturn(tacticalLocation);

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, family.side);
        return new Fixture(
                family, analyzer, game, gameState,
                duelistOne, duelistTwo);
    }

    private static PhysicalCard handCard(
            String blueprintId, int cardId) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.HAND);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getCardId()).thenReturn(cardId);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        return card;
    }

    private static PhysicalCard tacticalLocation() {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.LOCATIONS);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getTitle()).thenReturn("Ordinary battleground site");
        when(card.getTitles()).thenReturn(
                List.of("Ordinary battleground site"));
        when(card.getCardId()).thenReturn(TACTICAL_LOCATION_ID);
        when(card.getPermanentCardId()).thenReturn(TACTICAL_LOCATION_ID);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getTitle()).thenReturn("Ordinary battleground site");
        return card;
    }

    private static PhysicalCard tacticalMover(PhysicalCard location) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.AT_LOCATION);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getAtLocation()).thenReturn(location);
        when(card.getTitle()).thenReturn("Ordinary character");
        when(card.getTitles()).thenReturn(List.of("Ordinary character"));
        when(card.getCardId()).thenReturn(TACTICAL_MOVER_ID);
        when(card.getPermanentCardId()).thenReturn(TACTICAL_MOVER_ID);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.getTitle()).thenReturn("Ordinary character");
        when(blueprint.getAbility()).thenReturn(1.0f);
        return card;
    }

    private static PhysicalCard objective(Family family, boolean flipped) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front = blueprint(family.front);
        SwccgCardBlueprint back = blueprint(family.back);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint()).thenReturn(flipped ? back : front);
        when(card.getOtherSideBlueprint()).thenReturn(flipped ? front : back);
        when(card.getBlueprintId(true)).thenReturn(family.front);
        when(card.getBlueprintId(false)).thenReturn(
                flipped ? family.back : family.front);
        when(card.getTitle()).thenReturn(
                (flipped ? back : front).getTitle());
        when(card.getTitles()).thenReturn(List.of(front.getTitle(), back.getTitle()));
        when(card.getCardId()).thenReturn(OBJECTIVE_ID);
        when(card.getPermanentCardId()).thenReturn(OBJECTIVE_ID);
        when(card.isFlipped()).thenReturn(flipped);
        return card;
    }

    private static SwccgCardBlueprint blueprint(String id) {
        SwccgCardBlueprint blueprint = CARDS.getSwccgoCardBlueprint(id);
        assertNotNull("Missing real blueprint " + id, blueprint);
        return blueprint;
    }

    private static List<Outcome> actionText(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .ActionTextEvaluator().evaluate(
                            randoContext(fixture, decision)).stream()
                    .map(NabooDuelObjectiveCombinedEvaluatorDecisionTest
                            ::outcome).toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator().evaluate(
                        chosenContext(fixture, decision)).stream()
                .map(NabooDuelObjectiveCombinedEvaluatorDecisionTest
                        ::outcome).toList();
    }

    private static Outcome combined(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return outcome(new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoContext(fixture, decision)));
        }
        return outcome(new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CombinedEvaluator()
                .evaluateDecision(chosenContext(fixture, decision)));
    }

    private static List<Outcome> cardSelection(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator().evaluate(
                            randoContext(fixture, decision)).stream()
                    .map(NabooDuelObjectiveCombinedEvaluatorDecisionTest
                            ::outcome).toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CardSelectionEvaluator().evaluate(
                        chosenContext(fixture, decision)).stream()
                .map(NabooDuelObjectiveCombinedEvaluatorDecisionTest
                        ::outcome).toList();
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture, Decision decision) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators
                .DecisionContext(
                        fixture.gameState, PLAYER, decision.type,
                        decision.text, "naboo-duel-decision", decision.phase);
        context.setGame(fixture.game);
        context.setSide(fixture.family.side);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    Fixture fixture, Decision decision) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .DecisionContext(
                        fixture.gameState, PLAYER, decision.type,
                        decision.text, "naboo-duel-decision", decision.phase);
        context.setGame(fixture.game);
        context.setSide(fixture.family.side);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds);
        context.setActionTexts(decision.actionTexts);
        context.setCardIds(decision.cardIds);
        context.setBlueprints(List.of());
        context.setTestingTexts(List.of());
        context.setSelectable(decision.cardIds.stream()
                .map(ignored -> true).toList());
        context.setNoPass(false);
        context.setMin(0);
        context.setMax(1);
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds);
        context.setActionTexts(decision.actionTexts);
        context.setCardIds(decision.cardIds);
        context.setBlueprints(List.of());
        context.setTestingTexts(List.of());
        context.setSelectable(decision.cardIds.stream()
                .map(ignored -> true).toList());
        context.setNoPass(false);
        context.setMin(0);
        context.setMax(1);
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()));
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()));
    }

    private static Outcome only(List<Outcome> outcomes, String actionId) {
        return outcomes.stream()
                .filter(outcome -> actionId.equals(outcome.actionId))
                .findFirst().orElseThrow();
    }

    private static void assertContains(Outcome outcome, String marker) {
        assertTrue("Expected '" + marker + "' in " + outcome.reasoning,
                outcome.reasoning.stream()
                        .anyMatch(reason -> reason.contains(marker)));
    }

    private static void assertNoReason(
            List<Outcome> outcomes, String marker) {
        assertTrue(outcomes.toString(), outcomes.stream()
                .flatMap(outcome -> outcome.reasoning.stream())
                .noneMatch(reason -> reason.contains(marker)));
    }

    private static void assertParity(List<Outcome> outcomes) {
        assertEquals(2, outcomes.size());
        assertEquals(outcomes.get(0).actionId, outcomes.get(1).actionId);
        assertEquals(Float.floatToRawIntBits(outcomes.get(0).score),
                Float.floatToRawIntBits(outcomes.get(1).score));
        assertEquals(outcomes.get(0).reasoning, outcomes.get(1).reasoning);
    }

    private static String label(Bot bot, Family family) {
        return bot + " " + family.frontTitle;
    }

    private enum Bot {
        RANDO,
        CHOSEN_ONE
    }

    private record Family(
            String frontTitle, String front, String back,
            Side side, String ownDuelist) {
    }

    private record Fixture(
            Family family, ObjectiveAnalyzer analyzer,
            SwccgGame game, GameState gameState,
            PhysicalCard duelistOne,
            PhysicalCard duelistTwo) {
    }

    private record Decision(
            String type,
            String text,
            Phase phase,
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds) {

        private static Decision actions(
                Phase phase,
                List<String> actionIds,
                List<String> actionTexts,
                List<String> cardIds) {
            return new Decision(
                    "ACTION_CHOICE", "Choose action", phase,
                    actionIds, actionTexts, cardIds);
        }

        private static Decision forceLoss(
                List<String> cardIds) {
            return new Decision(
                    "CARD_SELECTION", "Choose Force to lose",
                    Phase.CONTROL, List.of(), List.of(), cardIds);
        }

        private static Decision combinedForceLoss(
                List<String> cardIds) {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose Force to lose or card from battle to forfeit",
                    Phase.BATTLE, List.of(), List.of(), cardIds);
        }
    }

    private record Outcome(
            String actionId, float score, List<String> reasoning) {
    }
}
