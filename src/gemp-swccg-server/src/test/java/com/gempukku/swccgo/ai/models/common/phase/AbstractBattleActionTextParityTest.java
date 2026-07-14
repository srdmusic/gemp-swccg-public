package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Shared BATTLE tactic repair assertions for both mirrored ActionText evaluators. */
public abstract class AbstractBattleActionTextParityTest {

    public record Score(float value, boolean hardVeto, String reasoning) {
    }

    protected abstract Score evaluate(String actionText);

    @Test
    public void cancelRedrawPrecedesGenericOpponentCancel() {
        Score low = evaluate(
                "Cancel opponent's Interrupt destiny and redraw destiny as a 1");
        Score high = evaluate(
                "Cancel opponent's Interrupt destiny and redraw destiny as a 6");

        assertEquals(Float.floatToRawIntBits(100f),
                Float.floatToRawIntBits(low.value()));
        assertEquals(Float.floatToRawIntBits(-300f),
                Float.floatToRawIntBits(high.value()));
        assertTrue(low.reasoning().contains("V37 REDRAW"));
        assertTrue(high.reasoning().contains("V37 DON'T REDRAW"));
    }

    @Test
    public void forcePushBattleAndExchangeModesStaySeparate() {
        Score battle = evaluate("Force Push: Exclude character from battle");
        Score exchange = evaluate(
                "Force Push: Exchange cards with card in Force Pile");

        assertEquals(Float.floatToRawIntBits(80f),
                Float.floatToRawIntBits(battle.value()));
        assertEquals(Float.floatToRawIntBits(-500f),
                Float.floatToRawIntBits(exchange.value()));
        assertFalse(battle.hardVeto());
        assertFalse(exchange.hardVeto());
    }
}
