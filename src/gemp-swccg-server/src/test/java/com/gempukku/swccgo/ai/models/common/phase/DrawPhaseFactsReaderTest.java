package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrawPhaseFactsReaderTest {

    @Test
    public void missingGamePreservesBaseOneForceGeneration() {
        assertEquals(1, DrawPhaseFactsReader.calculateForceGeneration(
                null, null, null, LogManager.getLogger(getClass())));
    }

    @Test
    public void expensiveCardReaderPreservesInterruptSkipAndAffordability() {
        List<PhysicalCard> hand = List.of(
                card(CardCategory.CHARACTER, 4, 8, true),
                card(CardCategory.CHARACTER, 2, 3, true),
                card(CardCategory.INTERRUPT, 0, 12, false));

        DrawPhaseFactsReader.ExpensiveCards facts =
                DrawPhaseFactsReader.inspectExpensiveCards(hand, 4);

        assertEquals(3, facts.handCardCount());
        assertEquals(8, facts.maxDeployableCost());
        assertEquals(1, facts.affordableCardsCount());
        assertTrue(facts.expensiveCardInHand());
    }

    @Test
    public void forceStarvedReaderKeepsEfficiencySortAndGreedyCost() {
        List<PhysicalCard> hand = List.of(
                card(CardCategory.CHARACTER, 4, 4, true),
                card(CardCategory.CHARACTER, 2, 1, true),
                card(CardCategory.EFFECT, 0, 1, false));

        DrawPhaseFactsReader.ForceStarved facts =
                DrawPhaseFactsReader.inspectForceStarved(hand);

        assertEquals(6, facts.deployablePower());
        assertEquals(5, facts.minCostForThresholdPower());
    }

    @Test
    public void emptyHandProducesNeutralFactRecords() {
        DrawPhaseFactsReader.ExpensiveCards expensive =
                DrawPhaseFactsReader.inspectExpensiveCards(List.of(), 4);
        DrawPhaseFactsReader.ForceStarved starved =
                DrawPhaseFactsReader.inspectForceStarved(List.of());

        assertEquals(0, expensive.handCardCount());
        assertFalse(expensive.expensiveCardInHand());
        assertEquals(0, starved.deployablePower());
        assertEquals(999, starved.minCostForThresholdPower());
    }

    private static PhysicalCard card(CardCategory category, float power,
                                     float deployCost, boolean hasPower) {
        SwccgCardBlueprint blueprint = (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgCardBlueprint.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCardCategory" -> category;
                    case "hasPowerAttribute" -> hasPower;
                    case "getPower" -> power;
                    case "getDeployCost" -> deployCost;
                    default -> throw new UnsupportedOperationException(
                            "Unexpected blueprint call: " + method.getName());
                });
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    if ("getBlueprint".equals(method.getName())) {
                        return blueprint;
                    }
                    throw new UnsupportedOperationException(
                            "Unexpected PhysicalCard call: " + method.getName());
                });
    }
}
