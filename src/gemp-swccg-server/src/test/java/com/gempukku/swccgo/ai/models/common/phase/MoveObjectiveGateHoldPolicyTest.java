package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.FlipGateFormationRole;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoveObjectiveGateHoldPolicyTest {

    @Test
    public void soleControlSourceAtExactRequiredLocationIsHeld() {
        MoveObjectiveGateHoldPolicy.Evaluation result =
                MoveObjectiveGateHoldPolicy.evaluateRequiredControl(
                        true, true, true, true);

        assertEquals(MoveObjectiveGateHoldPolicy.Branch.HOLD_LAST_CONTROL_SOURCE,
                result.branch());
        assertTrue(result.hardVeto());
        assertEquals(
                "MOVE.OBJECTIVE.REQUIRED_CONTROL_HOLD: keep the sole control source at the required location",
                result.reason());
    }

    @Test
    public void requiredControlHoldFailsClosedWithoutEveryProvenFact() {
        assertNeutralRequiredControl(false, true, true, true);
        assertNeutralRequiredControl(true, false, true, true);
        assertNeutralRequiredControl(true, true, false, true);
        assertNeutralRequiredControl(true, true, true, false);
    }

    @Test
    public void countedFormationKeepsLastActorAndBuddyWhenUnopposed() {
        MoveObjectiveGateHoldPolicy.Evaluation actor =
                MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                        true, FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                        5.0f, 0.0f);
        MoveObjectiveGateHoldPolicy.Evaluation buddy =
                MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                        true, FlipGateFormationRole.LAST_REQUIRED_BUDDY,
                        5.0f, 0.0f);

        assertEquals(MoveObjectiveGateHoldPolicy.Branch.HOLD_LAST_ACTOR,
                actor.branch());
        assertEquals(MoveObjectiveGateHoldPolicy.Branch.HOLD_LAST_BUDDY,
                buddy.branch());
        assertTrue(actor.hardVeto());
        assertTrue(buddy.hardVeto());
    }

    @Test
    public void countedFormationHoldsAtSixPowerDeficitButAllowsRetreatBeyondIt() {
        MoveObjectiveGateHoldPolicy.Evaluation actor =
                MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                        true, FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                        8.0f, 14.0f);
        MoveObjectiveGateHoldPolicy.Evaluation buddy =
                MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                        true, FlipGateFormationRole.LAST_REQUIRED_BUDDY,
                        8.0f, 15.0f);

        assertEquals(
                MoveObjectiveGateHoldPolicy.Branch.HOLD_DEFENSIBLE_CONTEST,
                actor.branch());
        assertTrue(actor.hardVeto());
        assertEquals(MoveObjectiveGateHoldPolicy.Branch.NONE,
                buddy.branch());
        assertFalse(buddy.hardVeto());
    }

    @Test
    public void countedFormationFailsClosedWithoutAnActiveProvenRole() {
        assertNeutralCountedFormation(
                false, FlipGateFormationRole.LAST_REQUIRED_ACTOR);
        assertNeutralCountedFormation(true, FlipGateFormationRole.NONE);
        assertNeutralCountedFormation(true, null);
    }

    @Test
    public void replayFormationHoldsAtTenPowerAgainstTwelve() {
        MoveObjectiveGateHoldPolicy.Evaluation result = evaluate(
                true, true, true, true, 1, 4, 10.0f, 12.0f);

        assertEquals(MoveObjectiveGateHoldPolicy.Branch.HOLD_DEFENSIBLE_CONTEST,
                result.branch());
        assertTrue(result.hardVeto());
    }

    @Test
    public void sixPowerDeficitStillHoldsWhileSevenAllowsRetreat() {
        assertTrue(evaluate(true, true, true, false,
                1, 3, 10.0f, 16.0f).hardVeto());
        assertFalse(evaluate(true, true, true, false,
                1, 3, 10.0f, 17.0f).hardVeto());
    }

    @Test
    public void uncontestedGateKeepsLastActorAndLastBuddy() {
        MoveObjectiveGateHoldPolicy.Evaluation actor = evaluate(
                true, true, true, true, 1, 3, 10.0f, 0.0f);
        MoveObjectiveGateHoldPolicy.Evaluation buddy = evaluate(
                true, true, true, false, 1, 2, 8.0f, 0.0f);

        assertEquals(MoveObjectiveGateHoldPolicy.Branch.HOLD_LAST_ACTOR,
                actor.branch());
        assertEquals(MoveObjectiveGateHoldPolicy.Branch.HOLD_LAST_BUDDY,
                buddy.branch());
        assertTrue(actor.hardVeto());
        assertTrue(buddy.hardVeto());
    }

    @Test
    public void uncontestedSurplusBuddyMayLeave() {
        MoveObjectiveGateHoldPolicy.Evaluation result = evaluate(
                true, true, true, false, 1, 3, 10.0f, 0.0f);

        assertEquals(MoveObjectiveGateHoldPolicy.Branch.NONE, result.branch());
        assertFalse(result.hardVeto());
    }

    @Test
    public void unrelatedFlippedAndActorlessMovesStayNeutral() {
        assertFalse(evaluate(false, true, true, true,
                1, 2, 5.0f, 0.0f).hardVeto());
        assertFalse(evaluate(true, true, false, true,
                1, 2, 5.0f, 0.0f).hardVeto());
        assertFalse(evaluate(true, true, true, false,
                0, 4, 10.0f, 12.0f).hardVeto());
    }

    private static MoveObjectiveGateHoldPolicy.Evaluation evaluate(
            boolean activePreFlipActorGate,
            boolean moverIsCharacter,
            boolean moverAtExactGate,
            boolean moverIsRequiredActor,
            int actorsAtGate,
            int friendlyCharactersAtGate,
            float friendlyPowerAtGate,
            float opponentPowerAtGate) {
        return MoveObjectiveGateHoldPolicy.evaluate(
                activePreFlipActorGate,
                moverIsCharacter,
                moverAtExactGate,
                moverIsRequiredActor,
                actorsAtGate,
                friendlyCharactersAtGate,
                friendlyPowerAtGate,
                opponentPowerAtGate);
    }

    private static void assertNeutralRequiredControl(
            boolean activePreFlipRequiredControl,
            boolean moverAtExactRequiredLocation,
            boolean currentlyControlsLocation,
            boolean soleControlSourceProven) {
        MoveObjectiveGateHoldPolicy.Evaluation result =
                MoveObjectiveGateHoldPolicy.evaluateRequiredControl(
                        activePreFlipRequiredControl,
                        moverAtExactRequiredLocation,
                        currentlyControlsLocation,
                        soleControlSourceProven);

        assertEquals(MoveObjectiveGateHoldPolicy.Branch.NONE, result.branch());
        assertFalse(result.hardVeto());
        assertEquals(null, result.reason());
    }

    private static void assertNeutralCountedFormation(
            boolean activePreFlipCountedFormation,
            FlipGateFormationRole formationRole) {
        MoveObjectiveGateHoldPolicy.Evaluation result =
                MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                        activePreFlipCountedFormation,
                        formationRole,
                        8.0f,
                        10.0f);

        assertEquals(MoveObjectiveGateHoldPolicy.Branch.NONE, result.branch());
        assertFalse(result.hardVeto());
        assertEquals(null, result.reason());
    }
}
