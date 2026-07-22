package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoveObjectiveGateHoldPolicyTest {

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
}
