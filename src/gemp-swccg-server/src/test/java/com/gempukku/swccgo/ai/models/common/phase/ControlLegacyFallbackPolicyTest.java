package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ControlLegacyFallbackPolicyTest {

    @Test
    public void forceDrainBaseAndControlledBattlegroundBonusStayAdditive() {
        assertEquals(120, ControlDrainAssessment.scoreLegacyFallback(120, 0));
        assertEquals(140, ControlDrainAssessment.scoreLegacyFallback(120, 1));
        assertEquals(180, ControlDrainAssessment.scoreLegacyFallback(120, 3));
    }

    @Test
    public void adapterSuppliedBaseScoreRemainsAnExplicitInput() {
        assertEquals(117, ControlDrainAssessment.scoreLegacyFallback(77, 2));
    }
}
