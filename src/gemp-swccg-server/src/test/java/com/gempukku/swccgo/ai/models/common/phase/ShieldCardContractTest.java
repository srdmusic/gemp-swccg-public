package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.cards.set13.dark.Card13_084;
import com.gempukku.swccgo.cards.set13.light.Card13_044;
import com.gempukku.swccgo.cards.set200.dark.Card200_110;
import com.gempukku.swccgo.cards.set200.light.Card200_035;
import com.gempukku.swccgo.cards.set601.dark.Card601_001;
import com.gempukku.swccgo.cards.set601.light.Card601_039;
import com.gempukku.swccgo.common.Title;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShieldCardContractTest {

    @Test
    public void resistanceAndUltimatumAliasesMatchTheirPrintedDrainCapCondition() {
        Card13_084 resistance = new Card13_084();
        Card13_044 ultimatum = new Card13_044();

        assertEquals(Title.Resistance, resistance.getTitle());
        assertEquals(Title.Ultimatum, ultimatum.getTitle());
        assertDrainCapCondition(resistance.getGameText());
        assertDrainCapCondition(ultimatum.getGameText());
    }

    @Test
    public void stackedPileSourceAliasesMatchTheActualStartingEffects() {
        Card200_110 knowledgeAndDefense = new Card200_110();
        Card200_035 angerFearAggression = new Card200_035();
        Card601_001 legacyKnowledgeAndDefense = new Card601_001();
        Card601_039 legacyAngerFearAggression = new Card601_039();

        assertTrue(ShieldPolicy.isStackedPileShieldSource(
                knowledgeAndDefense.getTitle()));
        assertTrue(ShieldPolicy.isStackedPileShieldSource(
                angerFearAggression.getTitle()));
        assertTrue(ShieldPolicy.isStackedPileShieldSource(
                legacyKnowledgeAndDefense.getTitle()));
        assertTrue(ShieldPolicy.isStackedPileShieldSource(
                legacyAngerFearAggression.getTitle()));
        assertTrue(knowledgeAndDefense.getGameText().contains("may play a card from here"));
        assertTrue(angerFearAggression.getGameText().contains("may play a card from here"));
        assertTrue(legacyKnowledgeAndDefense.getGameText().contains("may play a card from here"));
        assertTrue(legacyAngerFearAggression.getGameText().contains("may play a card from here"));
    }

    private static void assertDrainCapCondition(String gameText) {
        String lower = gameText.toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("occupy at least 3 battlegrounds"));
        assertTrue(lower.contains("opponent occupies no battlegrounds"));
        assertTrue(lower.contains("no more than 2 force"));
    }
}
