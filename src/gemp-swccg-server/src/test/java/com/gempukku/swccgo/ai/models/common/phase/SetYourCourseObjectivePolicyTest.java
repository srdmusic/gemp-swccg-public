package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SetYourCourseObjectivePolicyTest {

    @Test
    public void supportedPackageWaitsForLaserThenRoutesZeroOneTwoAndOrbitsAlderaan() {
        assertEquals(SetYourCourseObjectivePolicy.Stage.WAITING_FOR_SUPERLASER,
                SetYourCourseObjectivePolicy.classify(facts(0, null, true, false)));
        assertEquals(SetYourCourseObjectivePolicy.Stage.READY_AT_ZERO,
                SetYourCourseObjectivePolicy.classify(facts(0, null, false, true)));
        assertEquals(SetYourCourseObjectivePolicy.Stage.READY_AT_ONE,
                SetYourCourseObjectivePolicy.classify(facts(1, null, false, true)));
        assertEquals(SetYourCourseObjectivePolicy.Stage.RECOVER_AT_TWO_DEEP_SPACE,
                SetYourCourseObjectivePolicy.classify(facts(2, null, false, true)));
        assertEquals(SetYourCourseObjectivePolicy.Stage.ORBITING_ALDERAAN,
                SetYourCourseObjectivePolicy.classify(facts(2, "Alderaan", false, true)));

        var delayedMove = SetYourCourseObjectivePolicy.scoreMoveParent(
                SetYourCourseObjectivePolicy.Stage.WAITING_FOR_SUPERLASER,
                true);
        assertFalse(delayedMove.hardVeto());
        assertEquals(-300.0f, delayedMove.delta(), 0.0f);

        var armedMove = SetYourCourseObjectivePolicy.scoreMoveParent(
                SetYourCourseObjectivePolicy.Stage.READY_AT_ZERO, true);
        assertTrue(armedMove.mandatory());
        assertEquals(300.0f, armedMove.delta(), 0.0f);

        assertTrue(SetYourCourseObjectivePolicy.scoreParsecChoice(
                SetYourCourseObjectivePolicy.Stage.READY_AT_ZERO, 1).mandatory());
        assertTrue(SetYourCourseObjectivePolicy.scoreParsecChoice(
                SetYourCourseObjectivePolicy.Stage.READY_AT_ONE, 2).mandatory());
        assertTrue(SetYourCourseObjectivePolicy.scoreDestinationChoice(
                SetYourCourseObjectivePolicy.Stage.RECOVER_AT_TWO_DEEP_SPACE,
                "Orbit a system").mandatory());
        assertTrue(SetYourCourseObjectivePolicy.scoreDestinationChoice(
                SetYourCourseObjectivePolicy.Stage.READY_AT_ONE,
                "Choose destination for Death Star at parsec 2",
                "Orbit a system").mandatory());
        assertFalse(SetYourCourseObjectivePolicy.scoreDestinationChoice(
                SetYourCourseObjectivePolicy.Stage.READY_AT_ONE,
                "Choose destination for Death Star at parsec 1",
                "Orbit a system").applies());
        assertTrue(SetYourCourseObjectivePolicy.scoreOrbitSystemChoice(
                SetYourCourseObjectivePolicy.Stage.READY_AT_ONE,
                true).mandatory());
        var orbitHold = SetYourCourseObjectivePolicy.scoreMoveParent(
                SetYourCourseObjectivePolicy.Stage.ORBITING_ALDERAAN,
                true);
        assertFalse(orbitHold.hardVeto());
        assertEquals(-300.0f, orbitHold.delta(), 0.0f);
    }

    @Test
    public void classicPackageReservesExactNextPaymentAndRejectsNearMissPackages() {
        var waiting = facts(0, null, true, false);
        var armed = facts(0, null, false, true);
        var orbiting = facts(2, "Alderaan", false, true);
        assertEquals(1, SetYourCourseObjectivePolicy.nextRouteForceReserve(waiting));
        assertEquals(1, SetYourCourseObjectivePolicy.nextRouteForceReserve(armed));
        assertEquals(0, SetYourCourseObjectivePolicy.nextRouteForceReserve(orbiting));

        var noClassicCpi = new SetYourCourseObjectivePolicy.RouteFacts(
                true, true, true, 0, null,
                true, false, true, true, false, true);
        var noClassicLaser = new SetYourCourseObjectivePolicy.RouteFacts(
                true, true, true, 0, null,
                false, false, false, false, true, true);
        assertEquals(SetYourCourseObjectivePolicy.Stage.INACTIVE_OR_UNSUPPORTED,
                SetYourCourseObjectivePolicy.classify(noClassicCpi));
        assertEquals(SetYourCourseObjectivePolicy.Stage.INACTIVE_OR_UNSUPPORTED,
                SetYourCourseObjectivePolicy.classify(noClassicLaser));
    }

    @Test
    public void virtualSuperlaserCanRecoverAwayFromParsecZero() {
        var deployableAtOne = new SetYourCourseObjectivePolicy.RouteFacts(
                true, true, true, 1, null,
                true, true, false, true, true, true);
        var classicStrandedAtOne = new SetYourCourseObjectivePolicy.RouteFacts(
                true, true, true, 1, null,
                true, true, false, false, true, true);

        assertEquals(SetYourCourseObjectivePolicy.Stage.WAITING_FOR_SUPERLASER,
                SetYourCourseObjectivePolicy.classify(deployableAtOne));
        assertEquals(SetYourCourseObjectivePolicy.Stage.BROKEN_OR_UNSUPPORTED,
                SetYourCourseObjectivePolicy.classify(classicStrandedAtOne));
    }

    @Test
    public void onlyExactRouteAssetsAndChoicesReceiveBoundedScores() {
        var waiting = SetYourCourseObjectivePolicy.Stage.WAITING_FOR_SUPERLASER;
        var orbiting = SetYourCourseObjectivePolicy.Stage.ORBITING_ALDERAAN;
        assertTrue(SetYourCourseObjectivePolicy.scoreSuperlaserDeploy(waiting, true).mandatory());
        assertFalse(SetYourCourseObjectivePolicy.scoreSuperlaserDeploy(waiting, false).applies());
        assertTrue(SetYourCourseObjectivePolicy.scoreCpiAction(orbiting, true,
                "Attempt to 'blow away' Alderaan").mandatory());
        assertFalse(SetYourCourseObjectivePolicy.scoreCpiAction(orbiting, false,
                "Attempt to 'blow away' site").applies());
        assertEquals(-300.0f, SetYourCourseObjectivePolicy.scoreParsecChoice(
                SetYourCourseObjectivePolicy.Stage.READY_AT_ZERO, 0).delta(), 0.0f);
        assertEquals(-300.0f, SetYourCourseObjectivePolicy.scoreDestinationChoice(
                SetYourCourseObjectivePolicy.Stage.RECOVER_AT_TWO_DEEP_SPACE,
                "Deep Space").delta(), 0.0f);
        assertEquals(-300.0f, SetYourCourseObjectivePolicy.scoreOrbitSystemChoice(
                SetYourCourseObjectivePolicy.Stage.RECOVER_AT_TWO_DEEP_SPACE,
                false).delta(), 0.0f);
    }

    @Test
    public void controlSpendGetsBoundedNextHyperspeedPenalty() {
        var penalty = SetYourCourseObjectivePolicy
                .preserveRouteForceDuringControl(1, 3, 3.0f);
        assertFalse(penalty.hardVeto());
        assertEquals(-300.0f, penalty.delta(), 0.0f);
        assertFalse(SetYourCourseObjectivePolicy
                .preserveRouteForceDuringControl(1, 4, 3.0f)
                .applies());
        assertFalse(SetYourCourseObjectivePolicy
                .preserveRouteForceDuringControl(0, 3, 3.0f)
                .applies());
    }

    private SetYourCourseObjectivePolicy.RouteFacts facts(
            int parsec, String orbit, boolean laserInHand,
            boolean laserAttached) {
        return new SetYourCourseObjectivePolicy.RouteFacts(
                true, true, true, parsec, orbit,
                true, laserInHand, laserAttached, true, true, true);
    }
}
