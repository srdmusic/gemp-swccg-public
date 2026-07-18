package com.gempukku.swccgo.ai.models.common.playbook;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ObjectiveProgressAssessmentTest {
    @Test
    public void noObjectiveCarriesNoInventedFacts() {
        ObjectiveProgressAssessment assessment = ObjectiveProgressAssessment.noObjective();
        assertEquals(ObjectiveProgressAssessment.Outcome.NO_OBJECTIVE, assessment.outcome());
        assertEquals(null, assessment.objectiveBlueprintId());
        assertFalse(assessment.flipped());
        assertEquals(Set.of(), assessment.missingRequirements());
    }

    @Test
    public void defensivelyCopiesRequirementSets() {
        Set<String> missing = new HashSet<>(Set.of("four-isb-agents"));
        Set<String> advanced = new HashSet<>(Set.of("four-isb-agents"));
        ObjectiveProgressAssessment assessment = new ObjectiveProgressAssessment(
                "7_299", false, ObjectiveProgressAssessment.Outcome.COMPLETES_FLIP_NOW,
                Set.of(), missing, advanced, "Fourth ISB agent would deploy");

        missing.clear();
        advanced.clear();

        assertEquals(Set.of("four-isb-agents"), assessment.missingRequirements());
        assertEquals(Set.of("four-isb-agents"), assessment.advancedRequirements());
    }

    @Test
    public void rejectsUnprovenAdvancementAndPrematureFlipBackProtection() {
        try {
            new ObjectiveProgressAssessment("7_299", false,
                    ObjectiveProgressAssessment.Outcome.ADVANCES_MISSING_REQUIREMENT,
                    Set.of(), Set.of("four-isb-agents"), Set.of(), "No requirement advanced");
            fail("expected empty advancement rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            new ObjectiveProgressAssessment("7_299", false,
                    ObjectiveProgressAssessment.Outcome.PROTECTS_FLIP_BACK,
                    Set.of(), Set.of(), Set.of(), "Objective is not flipped");
            fail("expected unflipped protection rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void rejectsContradictoryAndAlreadyFlippedFrontSideFacts() {
        try {
            new ObjectiveProgressAssessment("7_299", false,
                    ObjectiveProgressAssessment.Outcome.NEUTRAL,
                    Set.of("four-isb-agents"), Set.of("four-isb-agents"), Set.of(),
                    "Contradictory requirement state");
            fail("expected satisfied/missing contradiction rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        try {
            new ObjectiveProgressAssessment("7_299", true,
                    ObjectiveProgressAssessment.Outcome.COMPLETES_FLIP_NOW,
                    Set.of(), Set.of("four-isb-agents"), Set.of("four-isb-agents"),
                    "Already flipped");
            fail("expected already-flipped completion rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
