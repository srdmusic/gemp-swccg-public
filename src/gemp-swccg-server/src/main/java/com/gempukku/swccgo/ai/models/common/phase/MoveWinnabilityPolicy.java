package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Locale;

/**
 * Shared V137 contested-move winnability and anti-solo scoring.
 * Adapters retain board, power, ability, forfeit, predicate, score, and logging reads.
 */
public final class MoveWinnabilityPolicy {
    public enum Branch {
        NONE,
        UNWINNABLE,
        ANTI_SOLO_BATTLEGROUND
    }

    public record Evaluation(
            Branch branch,
            boolean applies,
            boolean canWinVeto,
            String vetoReason,
            String reason,
            float delta,
            int projectedCharactersAtDestination) {
        private static Evaluation none() {
            return new Evaluation(
                    Branch.NONE, false, false,
                    null, null, 0.0f, 0);
        }
    }

    private MoveWinnabilityPolicy() {
    }

    public static boolean actionTargetsLocation(
            String actionLower,
            String locationTitle) {
        return actionLower.contains(
                locationTitle.toLowerCase(Locale.ROOT));
    }

    public static Evaluation contested(
            String moverTitle,
            String destinationTitle,
            float projectedFriendlyPower,
            float projectedFriendlyAbility,
            float opponentPower,
            boolean canWin) {
        if (canWin) {
            return Evaluation.none();
        }
        float penalty = opponentPower - projectedFriendlyPower >= 6.0f
                ? -1500.0f : -800.0f;
        return new Evaluation(
                Branch.UNWINNABLE,
                true,
                true,
                String.format(
                        "V137 UNWINNABLE MOVE: %s → %s contested"
                                + " — even the full group (%.0f pwr/%.0f abil)"
                                + " loses to opp %.0f pwr (shared canWinAt false)",
                        moverTitle,
                        destinationTitle,
                        projectedFriendlyPower,
                        projectedFriendlyAbility,
                        opponentPower),
                String.format(
                        "V137 UNWINNABLE MOVE: %s → %s contested"
                                + " — even the full group (%.0f pwr/%.0f abil)"
                                + " loses to opp %.0f pwr; don't waste move force",
                        moverTitle,
                        destinationTitle,
                        projectedFriendlyPower,
                        projectedFriendlyAbility,
                        opponentPower),
                penalty,
                0);
    }

    public static Evaluation uncontestedBattleground(
            String moverTitle,
            String destinationTitle,
            boolean destinationBattleground,
            int friendlyCharactersAtDestination,
            int movingFriendlyCharacters) {
        if (!destinationBattleground) {
            return Evaluation.none();
        }
        int projected = friendlyCharactersAtDestination
                + movingFriendlyCharacters;
        if (projected > 1) {
            return Evaluation.none();
        }
        return new Evaluation(
                Branch.ANTI_SOLO_BATTLEGROUND,
                true,
                false,
                null,
                String.format(
                        "V137 ANTI-SOLO BG: %s → %s would be SOLO at a battleground"
                                + " (uncontested now, opp can reinforce/attack next turn)"
                                + " — don't park alone",
                        moverTitle,
                        destinationTitle),
                -500.0f,
                projected);
    }
}
