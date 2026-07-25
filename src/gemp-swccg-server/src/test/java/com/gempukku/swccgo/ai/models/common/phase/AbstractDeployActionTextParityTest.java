package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Shared DEPLOY action-text adapter assertions for Rando and Chosen One. */
public abstract class AbstractDeployActionTextParityTest {

    public record Score(float value, boolean hardVeto, String reasoning) {
    }

    protected abstract Score evaluate(String actionText);

    @Test
    public void simpleDeployRoutesRetainLegacyScoresAndReasons() {
        assertScore("Use game text: Deploy docking bay", 200.0f,
                "V29.7 FIRST DOCKING BAY");
        assertScore("Deploy Vader from Reserve Deck", 150.0f,
                "V192 PULL SCORER");
        assertScore("Cloud City: Dining Room: Deploy Lando from Reserve Deck",
                -20.0f, "V29.6 Dining Room: Lando alone");
        assertScore("Reveal pilot or Star Destroyer from hand", 300.0f,
                "V22.5 CRITICAL: Deploy ship to Bespin");
        assertScore("Use game text to deploy pilot and ship simultaneously",
                120.0f,
                "V22.5: Deploy pilot+ship combo");
    }

    @Test
    public void genericDeployAndPlayRoutesRetainLegacyScoresAndReasons() {
        assertScore("Deploy on location from hand", 30.0f,
                "Deploy on location/table");
        assertScore("Deploy on side of table Projection from hand", -50.0f,
                "Never put projection on side of table");
        assertScore("Deploy unique card from hand", 30.0f,
                "Special battleground deploy");
        assertScore("Play a card", -50.0f,
                "No Force available - can't play cards!");
    }

    private void assertScore(String actionText, float expected, String reason) {
        Score score = evaluate(actionText);
        assertEquals(Float.floatToRawIntBits(expected),
                Float.floatToRawIntBits(score.value()));
        assertTrue(score.reasoning().contains(reason));
        assertFalse(score.hardVeto());
    }
}
