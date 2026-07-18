package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ShieldStrategy;
import com.gempukku.swccgo.cards.set13.dark.Card13_054;
import com.gempukku.swccgo.cards.set200.dark.Card200_110;
import com.gempukku.swccgo.cards.set200.light.Card200_035;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShieldCardSelectionPolicyParityTest {

    @Test
    public void mixedMenuAppliesV112ThenV117ExactlyOnceInBothBots() {
        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(7, card(new Card13_054()));
        candidates.put(8, card(new Card200_110()));
        candidates.put(9, card(new Card200_035()));
        GameState gameState = gameState(candidates, threeShieldsOnTable());

        var randoContext = randoContext(gameState, "CARD_SELECTION", "Pick one",
                List.of("7", "8", "9"), List.of(), List.of());
        var chosenContext = chosenContext(gameState, "CARD_SELECTION", "Pick one",
                List.of("7", "8", "9"), List.of(), List.of());
        randoContext.setShieldStrategy(new ShieldStrategy(Side.DARK));
        chosenContext.setShieldStrategy(new ShieldStrategy(Side.DARK));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertActionParity(rando, chosen);
        var battleOrder = action(rando, "7");
        assertReasonOnce(battleOrder.getReasoning(), "V112 BATTLE ORDER GATE");
        assertReasonOnce(battleOrder.getReasoning(), "V117 4TH SHIELD HOLD");
        assertTrue(reasonIndex(battleOrder.getReasoning(), "V112 BATTLE ORDER GATE")
                < reasonIndex(battleOrder.getReasoning(), "V117 4TH SHIELD HOLD"));
    }

    @Test
    public void reserveBattleOrderGateKeepsLegacyBaseCompositionInBothBots() {
        GameState gameState = gameState(Map.of(), List.of());
        var randoContext = randoContext(gameState, "CARD_SELECTION", "Deploy from Reserve Deck",
                List.of(), List.of("13_54"), List.of("Battle Order"));
        var chosenContext = chosenContext(gameState, "CARD_SELECTION", "Deploy from Reserve Deck",
                List.of(), List.of("13_54"), List.of("Battle Order"));
        randoContext.setShieldStrategy(new ShieldStrategy(Side.DARK));
        chosenContext.setShieldStrategy(new ShieldStrategy(Side.DARK));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertActionParity(rando, chosen);
        assertEquals(1, rando.size());
        assertBits(-9869.0f, rando.get(0).getScore());
        assertReasonOnce(rando.get(0).getReasoning(), "V51 BATTLE ORDER GATE");
    }

    @Test
    public void dedicatedFourthSlotKeepsV105BeforeV51AndExactBaseComposition() {
        Map<Integer, PhysicalCard> candidates = Map.of(7, card(new Card13_054()));
        GameState gameState = gameState(candidates, List.of());
        var randoContext = randoContext(gameState, "CARD_SELECTION", "Choose a defensive shield",
                List.of("7"), List.of(), List.of());
        var chosenContext = chosenContext(gameState, "CARD_SELECTION", "Choose a defensive shield",
                List.of("7"), List.of(), List.of());
        ShieldStrategy randoStrategy = threePlayedShields();
        ShieldStrategy chosenStrategy = threePlayedShields();
        randoContext.setShieldStrategy(randoStrategy);
        chosenContext.setShieldStrategy(chosenStrategy);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertActionParity(rando, chosen);
        assertEquals(1, rando.size());
        assertBits(-15049.0f, rando.get(0).getScore());
        assertReasonOnce(rando.get(0).getReasoning(), "V105/V107 4TH SLOT HOLD");
        assertReasonOnce(rando.get(0).getReasoning(), "V51 BATTLE ORDER GATE");
        assertTrue(reasonIndex(rando.get(0).getReasoning(), "V105/V107 4TH SLOT HOLD")
                < reasonIndex(rando.get(0).getReasoning(), "V51 BATTLE ORDER GATE"));
    }

    private static GameState gameState(Map<Integer, PhysicalCard> cards,
                                       List<PhysicalCard> cardsOnTable) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(1);
        when(gameState.getCurrentPlayerId()).thenReturn("tester");
        when(gameState.getHand("tester")).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(cardsOnTable);
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> cards.get(invocation.getArgument(0, Integer.class)));
        return gameState;
    }

    private static PhysicalCard card(SwccgCardBlueprint blueprint) {
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getBlueprint()).thenReturn(blueprint);
        if (blueprint instanceof Card13_054) {
            when(card.getBlueprintId(true)).thenReturn("13_54");
        }
        return card;
    }

    private static List<PhysicalCard> threeShieldsOnTable() {
        SwccgCardBlueprint blueprint = new Card13_054();
        return List.of(shieldOnTable(blueprint), shieldOnTable(blueprint),
                shieldOnTable(blueprint));
    }

    private static PhysicalCard shieldOnTable(SwccgCardBlueprint blueprint) {
        PhysicalCard card = card(blueprint);
        when(card.getOwner()).thenReturn("tester");
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        return card;
    }

    private static ShieldStrategy threePlayedShields() {
        ShieldStrategy strategy = new ShieldStrategy(Side.DARK);
        strategy.recordShieldPlayed("one", "One");
        strategy.recordShieldPlayed("two", "Two");
        strategy.recordShieldPlayed("three", "Three");
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
}
