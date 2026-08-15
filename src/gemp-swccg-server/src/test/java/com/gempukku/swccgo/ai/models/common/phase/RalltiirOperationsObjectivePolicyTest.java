package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RalltiirOperationsObjectivePolicyTest {

    @Test
    public void exactFrontParentOwnsItsPriorityBand() {
        var front = RalltiirOperationsObjectivePolicy
                .scoreFrontRoute("front", true, false);
        assertEquals(300.0f,
                front.operations().getFirst().delta(), 0.0f);
        assertEquals("OBJECTIVE.RALLTIIR.FRONT_ROUTE",
                front.operations().getFirst().ruleArmId().id());
    }

    @Test
    public void exhaustedFrontRouteIsCategoricallyVetoed() {
        var exhausted = RalltiirOperationsObjectivePolicy
                .scoreFrontRoute("front", false, true);
        assertEquals(PolicyOperationKind.HARD_VETO,
                exhausted.operations().getFirst().kind());
        assertEquals("OBJECTIVE.RALLTIIR.ROUTE_EXHAUSTED",
                exhausted.operations().getFirst().ruleArmId().id());

        assertTrue(RalltiirOperationsObjectivePolicy
                .scoreFrontRoute("front", false, false)
                .operations().isEmpty());
    }

    @Test
    public void siteAndImperialStagesShareTheObjectiveCeiling() {
        var site = RalltiirOperationsObjectivePolicy
                .scoreFrontPullCandidate("site", 3);
        var imperial = RalltiirOperationsObjectivePolicy
                .scoreFrontPullCandidate("imperial", 2);
        assertEquals(300.0f,
                site.operations().getFirst().delta(), 0.0f);
        assertEquals(300.0f,
                imperial.operations().getFirst().delta(), 0.0f);
        assertEquals(site.operations().getFirst().delta(),
                imperial.operations().getFirst().delta(), 0.0f);
    }

    @Test
    public void exactBackTutorOwnsTheFrontRoutePriorityBand() {
        var tutor = RalltiirOperationsObjectivePolicy
                .scoreBackAnyCardTutor("tutor", true);
        assertEquals(300.0f,
                tutor.operations().getFirst().delta(), 0.0f);
        assertEquals("OBJECTIVE.RALLTIIR.BACK_ANY_CARD_TUTOR",
                tutor.operations().getFirst().ruleArmId().id());
        assertTrue(RalltiirOperationsObjectivePolicy
                .scoreBackAnyCardTutor("other", false)
                .operations().isEmpty());
    }

    @Test
    public void urgentBackHoldCandidateGetsBoundedPreference() {
        var reinforcement = RalltiirOperationsObjectivePolicy
                .scoreBackTutorCandidate("imperial", true);
        assertEquals(300.0f,
                reinforcement.operations().getFirst().delta(), 0.0f);
        assertEquals("OBJECTIVE.RALLTIIR.BACK_HOLD_REINFORCEMENT",
                reinforcement.operations().getFirst().ruleArmId().id());
        assertTrue(RalltiirOperationsObjectivePolicy
                .scoreBackTutorCandidate("location", false)
                .operations().isEmpty());
    }

    @Test
    public void selectedImperialGetsBoundedPenaltyForWrongDestination() {
        var wrong = RalltiirOperationsObjectivePolicy
                .preserveFrontProgressDeployDestination(
                    "jungle", true, false);
        assertEquals(PolicyOperationKind.ADD,
                wrong.operations().getFirst().kind());
        assertEquals(-300.0f,
                wrong.operations().getFirst().delta(), 0.0f);
        assertEquals("OBJECTIVE.RALLTIIR.WRONG_DEPLOY_DESTINATION",
                wrong.operations().getFirst().ruleArmId().id());

        assertTrue(RalltiirOperationsObjectivePolicy
                .preserveFrontProgressDeployDestination(
                    "forest", true, true)
                .operations().isEmpty());
        assertTrue(RalltiirOperationsObjectivePolicy
                .preserveFrontProgressDeployDestination(
                    "jungle", false, false)
                .operations().isEmpty());
    }

    @Test
    public void ordinaryDeployGetsBoundedCurrentRoutePenalty() {
        var penalized = RalltiirOperationsObjectivePolicy
                .preserveRouteForceForOrdinaryDeploy(
                    "deploy", true, 2, 2, 1, 1);
        assertEquals(PolicyOperationKind.ADD,
                penalized.operations().getFirst().kind());
        assertEquals(-300.0f,
                penalized.operations().getFirst().delta(), 0.0f);
        assertEquals("OBJECTIVE.RALLTIIR.ROUTE_FORCE_RESERVE",
                penalized.operations().getFirst().ruleArmId().id());

        assertTrue(RalltiirOperationsObjectivePolicy
                .preserveRouteForceForOrdinaryDeploy(
                    "deploy", true, 2, 3, 1, 1)
                .operations().isEmpty());
        assertTrue(RalltiirOperationsObjectivePolicy
                .preserveRouteForceForOrdinaryDeploy(
                    "deploy", false, 2, 2, 1, 1)
                .operations().isEmpty());
    }

}
