package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MoveLandoStayPolicyTest {
    @Test
    public void landoTitleClassificationPreservesSubstringMatching() {
        assertFalse(MoveLandoStayPolicy.titleMarksLando(null));
        assertFalse(MoveLandoStayPolicy.titleMarksLando("Han Solo"));
        assertTrue(MoveLandoStayPolicy.titleMarksLando("Lando Calrissian"));
        assertTrue(MoveLandoStayPolicy.titleMarksLando("Orlando Calrissian"));
    }

    @Test
    public void cloudCityClassificationPreservesLegacyFragments() {
        for (String title : new String[]{
                "Cloud City: East Platform (Docking Bay)",
                "Dining Room",
                "Upper Walkway",
                "Carbonite Chamber",
                "Security Tower",
                "Lower Corridor"}) {
            assertTrue(title, MoveLandoStayPolicy.isCloudCitySite(title));
        }
        assertFalse(MoveLandoStayPolicy.isCloudCitySite(null));
        assertFalse(MoveLandoStayPolicy.isCloudCitySite(
                "Endor: Landing Platform (Docking Bay)"));
        assertFalse(MoveLandoStayPolicy.isCloudCitySite(
                "Coruscant: Private Platform"));
    }

    @Test
    public void objectiveAndSurvivabilityMustBothPass() {
        assertNone(MoveLandoStayPolicy.evaluate(
                "Cloud City: East Platform (Docking Bay)", false, true));
        assertNone(MoveLandoStayPolicy.evaluate(
                "Cloud City: East Platform (Docking Bay)", true, false));
        assertNone(MoveLandoStayPolicy.evaluate(
                "Cloud City: East Platform (Docking Bay)", false, false));
    }

    @Test
    public void passingGatesProduceExactHardVetoReason() {
        MoveLandoStayPolicy.Evaluation result =
                MoveLandoStayPolicy.evaluate(
                        "Cloud City: East Platform (Docking Bay)", true, true);

        assertTrue(result.hardVeto());
        assertEquals(
                "V47 LANDO STAY: Lando at Cloud City: East Platform (Docking Bay)"
                        + " — stay for occupation! Don't move!",
                result.reason());
    }

    @Test
    public void destinationResidualsPreserveIndependentStacking() {
        MoveLandoStayPolicy.DestinationEvaluation support =
                MoveLandoStayPolicy.destination(
                        true, 1, false, false);
        MoveLandoStayPolicy.DestinationEvaluation stay =
                MoveLandoStayPolicy.destination(
                        false, 0, true, true);
        MoveLandoStayPolicy.DestinationEvaluation both =
                MoveLandoStayPolicy.destination(
                        true, 1, true, true);

        assertEquals(250.0f, support.totalDelta(), 0.0f);
        assertEquals(-9999.0f, stay.totalDelta(), 0.0f);
        assertEquals(-9749.0f, both.totalDelta(), 0.0f);
        assertTrue(both.support().applies());
        assertTrue(both.stay().applies());
    }

    private static void assertNone(MoveLandoStayPolicy.Evaluation result) {
        assertFalse(result.hardVeto());
        assertNull(result.reason());
    }
}
