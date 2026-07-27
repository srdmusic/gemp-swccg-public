package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ShieldStrategy;
import com.gempukku.swccgo.cards.set13.light.Card13_008;
import com.gempukku.swccgo.cards.set13.dark.Card13_054;
import com.gempukku.swccgo.cards.set13.dark.Card13_061;
import com.gempukku.swccgo.cards.set13.dark.Card13_095;
import com.gempukku.swccgo.cards.set200.dark.Card200_110;
import com.gempukku.swccgo.cards.set200.light.Card200_035;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShieldCardSelectionPolicyParityTest {

    @Test
    public void mixedMenuAppliesV112ThenV117ExactlyOnceInBothBots() {
        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(7, card(new Card13_054(), "13_54"));
        candidates.put(8, card(new Card200_110(), "200_110"));
        candidates.put(9, card(new Card200_035(), "200_35"));
        GameState gameState = gameState(candidates, threeShieldsOnTable(), 1);

        EvaluationPair pair = evaluate(gameState, null, "CARD_SELECTION", "Pick one",
                List.of("7", "8", "9"), List.of(), List.of(),
                new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));

        var battleOrder = action(pair.rando(), "7");
        assertReasonOnce(battleOrder.getReasoning(), "V112 BATTLE ORDER GATE");
        assertReasonOnce(battleOrder.getReasoning(), "V117 4TH SHIELD HOLD");
        assertTrue(reasonIndex(battleOrder.getReasoning(), "V112 BATTLE ORDER GATE")
                < reasonIndex(battleOrder.getReasoning(), "V117 4TH SHIELD HOLD"));
    }

    @Test
    public void reserveNonShieldKeepsBaseFiftyWithoutShieldCrossTalk() {
        GameState gameState = gameState(Map.of(), List.of(), 1);
        EvaluationPair pair = evaluate(gameState, null, "CARD_SELECTION",
                "Deploy from Reserve Deck", List.of(), List.of("200_110"),
                List.of("13_54"), new ShieldStrategy(Side.DARK),
                new ShieldStrategy(Side.DARK));

        assertBits(50.0f, pair.rando().get(0).getScore());
        assertNoReasonContaining(pair.rando().get(0).getReasoning(), "Shield");
        assertNoReasonContaining(pair.rando().get(0).getReasoning(), "V51");
        assertNoReasonContaining(pair.rando().get(0).getReasoning(), "V53");
    }

    @Test
    public void unknownActualShieldUsesCanonicalTitleAndAddsFiftyInBothRoutes() {
        Map<Integer, PhysicalCard> candidates = Map.of(
                7, card(new Card13_095(), "13_95"));
        GameState gameState = gameState(candidates, List.of(), 1);

        EvaluationPair dedicated = evaluate(gameState, null, "CARD_SELECTION",
                "Choose a defensive shield", List.of("7"), List.of(), List.of(),
                new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));
        assertBits(50.0f, dedicated.rando().get(0).getScore());
        assertReasonOnce(dedicated.rando().get(0).getReasoning(), "Shield: Unknown shield");

        EvaluationPair reserve = evaluate(gameState, null, "CARD_SELECTION",
                "Deploy from Reserve Deck", List.of(), List.of("13_95"),
                List.of("13_54"), new ShieldStrategy(Side.DARK),
                new ShieldStrategy(Side.DARK));
        assertBits(100.0f, reserve.rando().get(0).getScore());
        assertReasonOnce(reserve.rando().get(0).getReasoning(), "Shield scoring (+50.0)");
        assertNoReasonContaining(reserve.rando().get(0).getReasoning(), "V51");
    }

    @Test
    public void reserveAppliesExactMinusFiftyAndMinusOneHundredStrategyScalars() {
        GameState gameState = gameState(Map.of(), List.of(), 1);
        ShieldStrategy randoPaced = twoPlayedShields();
        ShieldStrategy chosenPaced = twoPlayedShields();
        EvaluationPair paced = evaluate(gameState, null, "CARD_SELECTION",
                "Deploy from Reserve Deck", List.of(), List.of("13_52"), List.of(),
                randoPaced, chosenPaced);
        assertBits(0.0f, paced.rando().get(0).getScore());
        assertReasonOnce(paced.rando().get(0).getReasoning(), "Shield scoring (-50.0)");

        ShieldStrategy randoPlayed = new ShieldStrategy(Side.DARK);
        ShieldStrategy chosenPlayed = new ShieldStrategy(Side.DARK);
        randoPlayed.recordShieldPlayed("13_52", "Allegations Of Corruption");
        chosenPlayed.recordShieldPlayed("13_52", "Allegations Of Corruption");
        EvaluationPair duplicate = evaluate(gameState, null, "CARD_SELECTION",
                "Deploy from Reserve Deck", List.of(), List.of("13_52"), List.of(),
                randoPlayed, chosenPlayed);
        assertBits(-50.0f, duplicate.rando().get(0).getScore());
        assertReasonOnce(duplicate.rando().get(0).getReasoning(), "Shield scoring (-100.0)");
    }

    @Test
    public void authoritativeFourthSlotCountClosesDedicatedAndReserveRoutes() {
        Map<Integer, PhysicalCard> candidates = Map.of(
                7, card(new Card13_095(), "13_95"));
        GameState gameState = gameState(candidates, threeShieldsOnTable(), 3);

        EvaluationPair dedicated = evaluate(gameState, null, "CARD_SELECTION",
                "Choose a defensive shield", List.of("7"), List.of(), List.of(),
                new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));
        assertBits(-4950.0f, dedicated.rando().get(0).getScore());
        assertReasonOnce(dedicated.rando().get(0).getReasoning(),
                "V105/V107 4TH SLOT HOLD");

        EvaluationPair reserve = evaluate(gameState, null, "CARD_SELECTION",
                "Deploy from Reserve Deck", List.of(), List.of("13_95"), List.of(),
                new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));
        assertBits(-4900.0f, reserve.rando().get(0).getScore());
        assertReasonOnce(reserve.rando().get(0).getReasoning(),
                "V105/V107 4TH SLOT HOLD");
    }

    @Test
    public void fourthSlotBoostsMatchingPreferredAndRejectsEveryOtherShield() {
        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(7, card(new Card13_054(), "13_54"));
        candidates.put(8, card(new Card13_095(), "13_95"));
        GameState gameState = gameState(candidates, threeShieldsOnTable(), 1);
        SwccgGame game = gameWithBothTheaters(gameState);

        EvaluationPair pair = evaluate(gameState, game, "CARD_SELECTION",
                "Choose a defensive shield", List.of("7", "8"), List.of(), List.of(),
                new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));

        var preferred = action(pair.rando(), "7");
        // Hoth repair #2 (2026-07-27): this mock board answers every canSpot
        // true, so the OPPONENT also occupies both theaters and is self-exempt
        // from Battle Order's tax — the corrected gate is rightly dead here
        // (V51 -9999, and the V53 turn-1 exception no longer applies). The
        // live-gate path is covered in ShieldPolicyTest.
        assertBits(-12919.0f, preferred.getScore());
        assertReasonOnce(preferred.getReasoning(), "4TH SLOT BOOST");
        assertReasonOnce(preferred.getReasoning(), "V51 BATTLE ORDER GATE");
        assertReasonOnce(preferred.getReasoning(), "V53 SHIELD MIN-TURN");

        var other = action(pair.rando(), "8");
        assertBits(-4950.0f, other.getScore());
        assertReasonOnce(other.getReasoning(), "not preferred");
    }

    @Test
    public void opponentBattlePlanHardBlocksRedundantBattleOrderAtFourthSlot() {
        Map<Integer, PhysicalCard> candidates = Map.of(
                7, card(new Card13_054(), "13_54"));
        PhysicalCard battlePlan =
                card(new Card13_008(), "13_8");
        when(battlePlan.getOwner()).thenReturn("opponent");
        when(battlePlan.getZone()).thenReturn(
                Zone.SIDE_OF_TABLE);
        List<PhysicalCard> table = new java.util.ArrayList<>(
                threeShieldsOnTable());
        table.add(battlePlan);
        GameState gameState = gameState(candidates, table, 3);
        SwccgGame game = gameWithBothTheaters(gameState);

        EvaluationPair pair = evaluate(
                gameState, game, "CARD_SELECTION",
                "Choose a defensive shield",
                List.of("7"), List.of(), List.of(),
                new ShieldStrategy(Side.DARK),
                new ShieldStrategy(Side.DARK));

        var battleOrder = action(pair.rando(), "7");
        assertTrue(battleOrder.getScore() < -9000.0f);
        assertReasonOnce(battleOrder.getReasoning(),
                "BATTLE ORDER/PLAN REDUNDANT");
        assertNoReasonContaining(
                battleOrder.getReasoning(), "4TH SLOT BOOST");
    }

    @Test
    public void blueprintOnlyReserveMenuDiscoversPreferredAcrossMaximumLength() {
        GameState gameState = gameState(Map.of(), threeShieldsOnTable(), 1);
        SwccgGame game = gameWithBothTheaters(gameState);
        EvaluationPair pair = evaluate(gameState, game, "CARD_SELECTION",
                "Deploy from Reserve Deck", List.of(), List.of("13_54", "13_95"),
                List.of(), new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));

        // Hoth repair #2: corrected gate dead on this both-players-occupy
        // mock board (see fourthSlot test note); preferred discovery and
        // parity semantics unchanged.
        assertBits(-12869.0f, pair.rando().get(0).getScore());
        assertReasonOnce(pair.rando().get(0).getReasoning(), "4TH SLOT BOOST");
        assertBits(-4900.0f, pair.rando().get(1).getScore());
        assertReasonOnce(pair.rando().get(1).getReasoning(), "not preferred");
    }

    @Test
    public void turnOneBothTheatersPreservesExactDedicatedAndReserveTotals() {
        Map<Integer, PhysicalCard> candidates = Map.of(
                7, card(new Card13_054(), "13_54"));
        GameState gameState = gameState(candidates, List.of(), 1);
        SwccgGame game = gameWithBothTheaters(gameState);

        EvaluationPair dedicated = evaluate(gameState, game, "CARD_SELECTION",
                "Choose a defensive shield", List.of("7"), List.of(), List.of(),
                new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));
        // Hoth repair #2: corrected gate dead on this both-players-occupy
        // mock board (opponent self-exempt); V53 turn-1 exception is keyed on
        // the live gate and no longer waives the min-turn veto here.
        assertBits(-14919.0f, dedicated.rando().get(0).getScore());
        assertReasonOnce(dedicated.rando().get(0).getReasoning(),
                "V51 BATTLE ORDER GATE");
        assertReasonOnce(dedicated.rando().get(0).getReasoning(),
                "V53 SHIELD MIN-TURN");

        EvaluationPair reserve = evaluate(gameState, game, "CARD_SELECTION",
                "Deploy from Reserve Deck", List.of(), List.of("13_54"), List.of(),
                new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));
        assertBits(-14869.0f, reserve.rando().get(0).getScore());
        assertReasonOnce(reserve.rando().get(0).getReasoning(),
                "V51 BATTLE ORDER GATE");
        assertReasonOnce(reserve.rando().get(0).getReasoning(),
                "V53 SHIELD MIN-TURN");
    }

    @Test
    public void minimumTurnVetoAppliesOncePerRouteBeforeAnyLaterContribution() {
        Map<Integer, PhysicalCard> candidates = Map.of(
                7, card(new Card13_061(), "13_61"));
        GameState gameState = gameState(candidates, List.of(), 1);

        EvaluationPair dedicated = evaluate(gameState, null, "CARD_SELECTION",
                "Choose a defensive shield", List.of("7"), List.of(), List.of(),
                new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));
        assertBits(-4920.0f, dedicated.rando().get(0).getScore());
        assertReasonOnce(dedicated.rando().get(0).getReasoning(),
                "V53 SHIELD MIN-TURN");

        EvaluationPair reserve = evaluate(gameState, null, "CARD_SELECTION",
                "Deploy from Reserve Deck", List.of(), List.of("13_61"), List.of(),
                new ShieldStrategy(Side.DARK), new ShieldStrategy(Side.DARK));
        assertBits(-4870.0f, reserve.rando().get(0).getScore());
        assertReasonOnce(reserve.rando().get(0).getReasoning(),
                "V53 SHIELD MIN-TURN");
    }

    @Test
    public void nullStrategyDedicatedFallbackIsOneHundredBeforeFourthSlotPolicy() {
        Map<Integer, PhysicalCard> candidates = Map.of(
                7, card(new Card13_095(), "13_95"));
        GameState gameState = gameState(candidates, threeShieldsOnTable(), 3);
        EvaluationPair pair = evaluate(gameState, null, "CARD_SELECTION",
                "Choose a defensive shield", List.of("7"), List.of(), List.of(),
                null, null);

        assertBits(-4900.0f, pair.rando().get(0).getScore());
        assertReasonOnce(pair.rando().get(0).getReasoning(),
                "Defensive shield (no strategy) (+50.0)");
        assertReasonOnce(pair.rando().get(0).getReasoning(),
                "V105/V107 4TH SLOT HOLD");
        assertTrue(reasonIndex(pair.rando().get(0).getReasoning(),
                "Defensive shield (no strategy)")
                < reasonIndex(pair.rando().get(0).getReasoning(),
                "V105/V107 4TH SLOT HOLD"));
    }

    private static EvaluationPair evaluate(
            GameState gameState, SwccgGame game, String type, String text,
            List<String> cardIds, List<String> blueprints, List<String> titles,
            ShieldStrategy randoStrategy, ShieldStrategy chosenStrategy) {
        var randoContext = randoContext(gameState, type, text, cardIds, blueprints, titles);
        var chosenContext = chosenContext(gameState, type, text, cardIds, blueprints, titles);
        randoContext.setGame(game);
        chosenContext.setGame(game);
        randoContext.setSide(Side.DARK);
        chosenContext.setSide(Side.DARK);
        randoContext.setShieldStrategy(randoStrategy);
        chosenContext.setShieldStrategy(chosenStrategy);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);
        assertActionParity(rando, chosen);
        return new EvaluationPair(rando, chosen);
    }

    private static GameState gameState(Map<Integer, PhysicalCard> cards,
                                       List<PhysicalCard> cardsOnTable,
                                       int turnNumber) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(turnNumber);
        when(gameState.getCurrentPlayerId()).thenReturn("tester");
        when(gameState.getHand("tester")).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(cardsOnTable);
        when(gameState.getTopLocations()).thenReturn(List.of());
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> cards.get(invocation.getArgument(0, Integer.class)));
        return gameState;
    }

    private static SwccgGame gameWithBothTheaters(GameState gameState) {
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(mock(
                com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying.class));
        when(gameState.iterateActiveCards(any(), any(), any(), any(), any()))
                .thenReturn(true);
        return game;
    }

    private static PhysicalCard card(SwccgCardBlueprint blueprint, String blueprintId) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        return card;
    }

    private static List<PhysicalCard> threeShieldsOnTable() {
        SwccgCardBlueprint blueprint = new Card13_061();
        return List.of(shieldOnTable(blueprint), shieldOnTable(blueprint),
                shieldOnTable(blueprint));
    }

    private static PhysicalCard shieldOnTable(SwccgCardBlueprint blueprint) {
        PhysicalCard card = card(blueprint, "13_54");
        when(card.getOwner()).thenReturn("tester");
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        return card;
    }

    private static ShieldStrategy twoPlayedShields() {
        ShieldStrategy strategy = new ShieldStrategy(Side.DARK);
        strategy.recordShieldPlayed("one", "One");
        strategy.recordShieldPlayed("two", "Two");
        return strategy;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            GameState gameState, String type, String text, List<String> cardIds,
            List<String> blueprints, List<String> titles) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", type, text, "shield-selection", Phase.DEPLOY);
        context.setCardIds(cardIds);
        context.setBlueprints(blueprints);
        context.setTestingTexts(titles);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            GameState gameState, String type, String text, List<String> cardIds,
            List<String> blueprints, List<String> titles) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", type, text, "shield-selection", Phase.DEPLOY);
        context.setCardIds(cardIds);
        context.setBlueprints(blueprints);
        context.setTestingTexts(titles);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> actions,
            String id) {
        return actions.stream().filter(candidate -> id.equals(candidate.getActionId()))
                .findFirst().orElseThrow();
    }

    private static void assertActionParity(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> rando,
            List<com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction> chosen) {
        assertEquals(rando.size(), chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertBits(rando.get(i).getScore(), chosen.get(i).getScore());
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
        }
    }

    private static void assertReasonOnce(List<String> reasons, String marker) {
        assertEquals(1, reasons.stream().filter(reason -> reason.contains(marker)).count());
    }

    private static void assertNoReasonContaining(List<String> reasons, String marker) {
        assertFalse(reasons.stream().anyMatch(reason -> reason.contains(marker)));
    }

    private static int reasonIndex(List<String> reasons, String marker) {
        for (int i = 0; i < reasons.size(); i++) {
            if (reasons.get(i).contains(marker)) {
                return i;
            }
        }
        return -1;
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }

    private record EvaluationPair(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> rando,
            List<com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction> chosen) {
    }
}
