package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.FlipGateFormationRole;

/**
 * Keeps a pre-flip actor-and-buddy formation intact at its exact objective gate.
 * The adapters own board reads and ladder veto application.
 */
public final class MoveObjectiveGateHoldPolicy {
    public enum Branch {
        NONE,
        HOLD_DEFENSIBLE_CONTEST,
        HOLD_LAST_ACTOR,
        HOLD_LAST_BUDDY,
        HOLD_LAST_CONTROL_SOURCE
    }

    public record Evaluation(Branch branch, boolean hardVeto, String reason) {
        private static Evaluation none() {
            return new Evaluation(Branch.NONE, false, null);
        }
    }

    private static final float RETREATABLE_POWER_GAP = 6.0f;

    private MoveObjectiveGateHoldPolicy() {
    }

    /**
     * Keeps the sole proven control source at an exact structured pre-flip
     * requirement. Every input is positive evidence; an unknown or missing fact
     * must be passed as false and leaves the move neutral.
     */
    public static Evaluation evaluateRequiredControl(
            boolean activePreFlipRequiredControl,
            boolean moverAtExactRequiredLocation,
            boolean currentlyControlsLocation,
            boolean soleControlSourceProven) {
        if (!activePreFlipRequiredControl
                || !moverAtExactRequiredLocation
                || !currentlyControlsLocation
                || !soleControlSourceProven) {
            return Evaluation.none();
        }

        return new Evaluation(
                Branch.HOLD_LAST_CONTROL_SOURCE,
                true,
                "MOVE.OBJECTIVE.REQUIRED_CONTROL_HOLD: keep the sole control source at the required location");
    }

    /**
     * Keeps a proven piece of a counted pre-flip formation in place while the
     * location is unopposed or defensibly contested. A deficit greater than six
     * permits the ordinary retreat policies to take over.
     */
    public static Evaluation evaluateCountedFormation(
            boolean activePreFlipCountedFormation,
            FlipGateFormationRole formationRole,
            float friendlyPowerAtLocation,
            float opponentPowerAtLocation) {
        if (!activePreFlipCountedFormation
                || formationRole == null
                || formationRole == FlipGateFormationRole.NONE) {
            return Evaluation.none();
        }

        boolean contested = opponentPowerAtLocation > 0.0f;
        boolean retreatable = opponentPowerAtLocation
                > friendlyPowerAtLocation + RETREATABLE_POWER_GAP;
        if (retreatable) {
            return Evaluation.none();
        }
        if (contested) {
            return new Evaluation(
                    Branch.HOLD_DEFENSIBLE_CONTEST,
                    true,
                    "MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD: keep the defensible counted formation together");
        }

        if (formationRole == FlipGateFormationRole.LAST_REQUIRED_ACTOR) {
            return new Evaluation(
                    Branch.HOLD_LAST_ACTOR,
                    true,
                    "MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD: the last required actor must remain");
        }
        return new Evaluation(
                Branch.HOLD_LAST_BUDDY,
                true,
                "MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD: preserve the required actor's last buddy");
    }

    public static Evaluation evaluate(
            boolean activePreFlipActorGate,
            boolean moverIsCharacter,
            boolean moverAtExactGate,
            boolean moverIsRequiredActor,
            int actorsAtGate,
            int friendlyCharactersAtGate,
            float friendlyPowerAtGate,
            float opponentPowerAtGate) {
        if (!activePreFlipActorGate || !moverIsCharacter
                || !moverAtExactGate || actorsAtGate < 1) {
            return Evaluation.none();
        }

        boolean contested = opponentPowerAtGate > 0.0f;
        boolean retreatable = opponentPowerAtGate
                > friendlyPowerAtGate + RETREATABLE_POWER_GAP;
        if (contested && !retreatable) {
            return new Evaluation(
                    Branch.HOLD_DEFENSIBLE_CONTEST,
                    true,
                    "V297 OBJECTIVE GATE HOLD: keep the defensible actor formation together");
        }

        if (contested) {
            return Evaluation.none();
        }

        if (moverIsRequiredActor && actorsAtGate == 1) {
            return new Evaluation(
                    Branch.HOLD_LAST_ACTOR,
                    true,
                    "V297 OBJECTIVE GATE HOLD: the last required actor must remain at the gate");
        }

        int remainingCharacters = friendlyCharactersAtGate - 1;
        int remainingActors = actorsAtGate - (moverIsRequiredActor ? 1 : 0);
        if (remainingActors >= 1 && remainingCharacters < 2) {
            return new Evaluation(
                    Branch.HOLD_LAST_BUDDY,
                    true,
                    "V297 OBJECTIVE GATE HOLD: preserve the required actor's last buddy");
        }

        return Evaluation.none();
    }
}
