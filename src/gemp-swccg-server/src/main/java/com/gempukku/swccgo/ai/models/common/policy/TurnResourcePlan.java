package com.gempukku.swccgo.ai.models.common.policy;

/** Immutable Force budget facts for the remainder of the current turn. */
public record TurnResourcePlan(
        int currentForce,
        int movementObligation,
        int battleObligation,
        int objectiveDefenseObligation,
        int protectedReserve,
        int drawReserve) {

    public TurnResourcePlan {
        requireNonNegative(currentForce, "currentForce");
        requireNonNegative(movementObligation, "movementObligation");
        requireNonNegative(battleObligation, "battleObligation");
        requireNonNegative(objectiveDefenseObligation, "objectiveDefenseObligation");
        requireNonNegative(protectedReserve, "protectedReserve");
        requireNonNegative(drawReserve, "drawReserve");
        committedForce(movementObligation, battleObligation, objectiveDefenseObligation,
                protectedReserve, drawReserve);
    }

    public int committedForce() {
        return committedForce(movementObligation, battleObligation, objectiveDefenseObligation,
                protectedReserve, drawReserve);
    }

    public int spendableNow() {
        return Math.max(0, currentForce - committedForce());
    }

    public int shortfall() {
        return Math.max(0, committedForce() - currentForce);
    }

    private static int committedForce(int... obligations) {
        int total = 0;
        for (int obligation : obligations) {
            total = Math.addExact(total, obligation);
        }
        return total;
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0, was " + value);
        }
    }
}
