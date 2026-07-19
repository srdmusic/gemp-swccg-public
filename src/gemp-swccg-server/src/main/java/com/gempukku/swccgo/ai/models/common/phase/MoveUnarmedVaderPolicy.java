package com.gempukku.swccgo.ai.models.common.phase;

/**
 * Shared V29.9 unarmed-Vader readiness scoring.
 * Adapters retain all card and hand reads, score application, and logging.
 */
public final class MoveUnarmedVaderPolicy {
    public enum Branch {
        NONE,
        EQUIP_FIRST,
        UNARMED
    }

    public record Evaluation(
            Branch branch,
            boolean applies,
            String reason,
            float delta) {
        private static Evaluation none() {
            return new Evaluation(Branch.NONE, false, null, 0.0f);
        }
    }

    private MoveUnarmedVaderPolicy() {
    }

    public static Evaluation evaluate(
            boolean titleMarksVader,
            boolean hasAttachedWeapon,
            boolean lightsaberInHand) {
        if (!titleMarksVader || hasAttachedWeapon) {
            return Evaluation.none();
        }
        if (lightsaberInHand) {
            return new Evaluation(
                    Branch.EQUIP_FIRST,
                    true,
                    "V29.9 UNARMED VADER: Lightsaber in hand — EQUIP FIRST before attacking!",
                    -250.0f);
        }
        return new Evaluation(
                Branch.UNARMED,
                true,
                "V29.9 UNARMED VADER: No weapon — vulnerable without lightsaber!",
                -100.0f);
    }
}
