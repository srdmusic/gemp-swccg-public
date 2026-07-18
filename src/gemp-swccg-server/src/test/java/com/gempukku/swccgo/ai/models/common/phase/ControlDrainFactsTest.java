package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ControlDrainFactsTest {
    @Test
    public void absentStockGameObjectsFailClosedWithoutInventingFacts() {
        ControlDrainFacts facts = new ControlDrainFacts(
                null, null, "player", null, 4, () -> true, () -> true);

        assertNull(facts.primary());
        assertFalse(facts.simpleTricksBlocks());
        ControlDrainAssessment.Economy economy = facts.economy();
        assertTrue(economy.underBattleOrder());
        assertEquals(0, economy.forceAvailable());
        assertFalse(economy.hasDeployableCard());
        assertFalse(facts.battleOrderCostWaived());
        assertNull(facts.battleOrderDrainValue());
        assertNull(facts.multiDrain());
        ControlDrainAssessment.HuntDown huntDown = facts.huntDown();
        assertTrue(huntDown.active());
        assertEquals(0, huntDown.opponentIcons());
    }
}
