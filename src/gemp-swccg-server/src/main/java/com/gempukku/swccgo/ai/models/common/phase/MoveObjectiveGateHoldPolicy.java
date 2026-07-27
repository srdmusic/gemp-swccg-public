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
        HOLD_LAST_CONTROL_SOURCE,
        HOLD_FLIP_BACK_BLOCKER,
        HOLD_POST_FLIP_SURVIVAL_ACTOR,
        HOLD_POST_FLIP_ANCHOR_CONCENTRATION,
        HOLD_REQUIRED_CARD_RETENTION_DEFENDER,
        HOLD_HARD_LOSS_DEFENDER
    }

    public record Evaluation(Branch branch, boolean hardVeto, String reason) {
        private static Evaluation none() {
            return new Evaluation(Branch.NONE, false, null);
        }
    }

    // Hoth repair #3 (2026-07-27): public so the anchor-concentration
    // adapters and ObjectiveAnalyzer's safe-site count reuse the exact same
    // retreatability threshold as evaluatePostFlipSurvivalActor.
    public static final float RETREATABLE_POWER_GAP = 6.0f;

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
     * Keeps control of a location that enables deployment of a still-missing
     * required objective card. This is intentionally distinct from the
     * objective's actual flip condition.
     */
    public static Evaluation evaluateRequiredCardControlEnabler(
            boolean activePreFlipControlEnabler,
            boolean moverAtExactEnablerLocation,
            boolean currentlyControlsLocation,
            boolean soleControlSourceProven) {
        if (!activePreFlipControlEnabler
                || !moverAtExactEnablerLocation
                || !currentlyControlsLocation
                || !soleControlSourceProven) {
            return Evaluation.none();
        }
        return new Evaluation(
                Branch.HOLD_LAST_CONTROL_SOURCE,
                true,
                "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_HOLD: keep the sole control source until the required card deploys");
    }

    public static Evaluation evaluateHardLossLocationDefender(
            FlipGateFormationRole formationRole,
            float friendlyPowerAtLocation,
            float opponentPowerAtLocation) {
        if (formationRole
                != FlipGateFormationRole.HARD_LOSS_LOCATION_DEFENDER
                || opponentPowerAtLocation
                    > friendlyPowerAtLocation + RETREATABLE_POWER_GAP) {
            return Evaluation.none();
        }
        return new Evaluation(
                Branch.HOLD_HARD_LOSS_DEFENDER,
                true,
                "MOVE.OBJECTIVE.HARD_LOSS_LOCATION_HOLD: keep the sole presence source defending a terminal-loss location");
    }

    public static Evaluation evaluateRequiredCardRetentionDefender(
            FlipGateFormationRole formationRole,
            float friendlyPowerAtLocation,
            float opponentPowerAtLocation) {
        return evaluateRequiredCardRetentionDefender(
                formationRole, friendlyPowerAtLocation,
                opponentPowerAtLocation, false);
    }

    public static Evaluation evaluateRequiredCardRetentionDefender(
            FlipGateFormationRole formationRole,
            float friendlyPowerAtLocation,
            float opponentPowerAtLocation,
            boolean safeQualifyingRelocation) {
        if (formationRole
                != FlipGateFormationRole
                    .REQUIRED_CARD_RETENTION_DEFENDER
                || safeQualifyingRelocation
                || opponentPowerAtLocation
                    > friendlyPowerAtLocation
                        + RETREATABLE_POWER_GAP) {
            return Evaluation.none();
        }
        return new Evaluation(
                Branch.HOLD_REQUIRED_CARD_RETENTION_DEFENDER,
                true,
                "MOVE.OBJECTIVE.REQUIRED_CARD_RETENTION_HOLD: keep the sole defender preventing an active required card from removing itself");
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
        return evaluateCountedFormation(
                activePreFlipCountedFormation,
                formationRole,
                friendlyPowerAtLocation,
                opponentPowerAtLocation,
                false);
    }

    public static Evaluation evaluateCountedFormation(
            boolean activePreFlipCountedFormation,
            FlipGateFormationRole formationRole,
            float friendlyPowerAtLocation,
            float opponentPowerAtLocation,
            boolean hasSafeQualifyingRelocation) {
        if (!activePreFlipCountedFormation
                || formationRole == null
                || hasSafeQualifyingRelocation
                || (formationRole != FlipGateFormationRole.LAST_REQUIRED_ACTOR
                    && formationRole
                        != FlipGateFormationRole.LAST_REQUIRED_BUDDY)) {
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

    /**
     * Keeps the sole active actor satisfying a runtime-location flip leg in
     * place, even when another unsatisfied alternative keeps the full rule
     * unflipped. A power deficit greater than six releases the retreat.
     */
    public static Evaluation evaluateRuntimeActorFormation(
            boolean activePreFlipRuntimeActor,
            FlipGateFormationRole formationRole,
            float friendlyPowerAtLocation,
            float opponentPowerAtLocation) {
        return evaluateRuntimeActorFormation(
                activePreFlipRuntimeActor, formationRole,
                friendlyPowerAtLocation, opponentPowerAtLocation,
                false);
    }

    /**
     * A legal, formation-safe relocation to another qualifying runtime
     * location preserves the actor leg and must not be mistaken for an
     * evacuation.
     */
    public static Evaluation evaluateRuntimeActorFormation(
            boolean activePreFlipRuntimeActor,
            FlipGateFormationRole formationRole,
            float friendlyPowerAtLocation,
            float opponentPowerAtLocation,
            boolean hasSafeQualifyingRelocation) {
        if (!activePreFlipRuntimeActor
                || formationRole
                    != FlipGateFormationRole.LAST_REQUIRED_ACTOR
                || hasSafeQualifyingRelocation
                || opponentPowerAtLocation
                    > friendlyPowerAtLocation + RETREATABLE_POWER_GAP) {
            return Evaluation.none();
        }
        return new Evaluation(
                Branch.HOLD_LAST_ACTOR,
                true,
                "MOVE.OBJECTIVE.RUNTIME_ACTOR_HOLD: keep the sole required actor at a qualifying location");
    }

    /** Holds Castle's reverse move when every legal mover is the sole actor. */
    public static Evaluation evaluateVaderCastleReturn(
            boolean activeRuntimeActorRule,
            boolean everyLegalMoverRequiresHold) {
        if (!activeRuntimeActorRule
                || !everyLegalMoverRequiresHold) {
            return Evaluation.none();
        }
        return new Evaluation(
                Branch.HOLD_LAST_ACTOR,
                true,
                "MOVE.OBJECTIVE.RUNTIME_ACTOR_HOLD: Castle return would evacuate every legal sole battleground Vader");
    }

    /**
     * Keeps a sole post-flip presence source in place when its departure would
     * immediately satisfy the objective's flip-back predicate. A deficit
     * greater than six permits ordinary retreat policy to take over.
     */
    public static Evaluation evaluatePostFlipBlocker(
            boolean departureTriggersFlipBack,
            float friendlyPowerAtLocation,
            float opponentPowerAtLocation) {
        if (!departureTriggersFlipBack
                || opponentPowerAtLocation
                    > friendlyPowerAtLocation + RETREATABLE_POWER_GAP) {
            return Evaluation.none();
        }
        return new Evaluation(
                Branch.HOLD_FLIP_BACK_BLOCKER,
                true,
                "MOVE.OBJECTIVE.FLIP_BACK_BLOCKER_HOLD: keep the sole blocker preventing immediate flip-back");
    }

    /**
     * Keeps the last actor satisfying a post-flip survival law. The terminal
     * result can be removal from play rather than a flip back.
     */
    public static Evaluation evaluatePostFlipSurvivalActor(
            boolean departureTriggersObjectiveLoss,
            float friendlyPowerAtLocation,
            float opponentPowerAtLocation,
            boolean safeRelocation) {
        if (!departureTriggersObjectiveLoss
                || safeRelocation
                || opponentPowerAtLocation
                    > friendlyPowerAtLocation + RETREATABLE_POWER_GAP) {
            return Evaluation.none();
        }
        return new Evaluation(
                Branch.HOLD_POST_FLIP_SURVIVAL_ACTOR,
                true,
                "MOVE.OBJECTIVE.POST_FLIP_SURVIVAL_HOLD: keep the last actor preventing immediate objective loss");
    }

    /**
     * Hoth repair #3 (2026-07-27): blocks a move that would concentrate at
     * the ONLY stay-flipped anchor site while that site is unsafe and no
     * other safe anchor site exists. The 93r5wrrnbo3q91j0 loss mode was not
     * departure but concentration: every mover piled onto the sole anchor,
     * which died in one battle and took the flipped objective out of play.
     * An anchor fleeing a hopeless site is unaffected (its destination does
     * not hold the last anchor), so this never fights the survival release.
     */
    public static Evaluation evaluatePostFlipAnchorConcentration(
            boolean stayFlippedRuleActive,
            boolean destinationHoldsLastAnchor,
            boolean destinationUnsafe,
            int safeAnchorSitesAfterMove) {
        if (!stayFlippedRuleActive
                || !destinationHoldsLastAnchor
                || !destinationUnsafe
                || safeAnchorSitesAfterMove != 0) {
            return Evaluation.none();
        }
        return new Evaluation(
                Branch.HOLD_POST_FLIP_ANCHOR_CONCENTRATION,
                true,
                "MOVE.OBJECTIVE.POST_FLIP_ANCHOR_CONCENTRATION: do not stack every stay-flipped anchor at one unsafe site");
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
