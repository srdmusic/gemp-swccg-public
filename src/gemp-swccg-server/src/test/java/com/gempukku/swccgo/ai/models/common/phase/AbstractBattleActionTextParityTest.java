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

    @Test
    public void battleTwoSimpleRoutesRetainLegacyScores() {
        assertScore("I Have You Now", 100.0f, "V29.9 IHYN");
        assertScore("Far More Frightening Than Death: add battle destiny",
                100.0f, "V35 FMFTD LOST");
        assertScore("You Are Beaten", 150.0f, "V35.4 YOU ARE BEATEN");
        assertScore("Stunning Leader", -200.0f, "V37.2 STUNNING LEADER");
        assertScore("+1 to battle destiny", 50.0f,
                "+1 to battle destiny - always use!");
        assertScore("substitute destiny", 30.0f,
                "Substituting destiny is good");
        assertScore("Make Missing Character lost", 0.0f,
                "V175: make-lost target not found on table — unknown");
        assertScore("cancel weapon target", 50.0f,
                "Cancel weapon targeting - protect our characters!");
        assertScore("immune to attrition", 50.0f,
                "Make character immune to attrition - valuable protection!");
        assertScore("protect forfeit", 40.0f,
                "Protect forfeit value during battle");
        assertScore("retarget weapon", 50.0f,
                "Re-target weapon at enemy - turn their weapon against them!");
    }

    private void assertScore(String actionText, float expected, String reason) {
        Score score = evaluate(actionText);
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(score.value()));
        assertTrue(score.reasoning().contains(reason));
        assertFalse(score.hardVeto());
    }
}
