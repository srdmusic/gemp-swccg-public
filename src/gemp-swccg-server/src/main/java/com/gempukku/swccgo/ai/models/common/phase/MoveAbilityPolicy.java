package com.gempukku.swccgo.ai.models.common.phase;

/**
 * Shared V32/V156 ability-protection and weak-solo join scoring.
 * Adapters retain card, objective, adjacency, power, score, and logging reads.
 */
public final class MoveAbilityPolicy {
    public enum Branch {
        NONE,
        DESTINY_DANGER,
        SOLO_ESCAPE,
        JOIN_GROUP
    }

    public record Analysis(Branch branch, float abilityAfterMove) {
    }

    public record Evaluation(
            Branch branch,
            boolean applies,
            String reason,
            float delta,
            boolean claimDoctrine) {
    }

    private MoveAbilityPolicy() {
    }

    public static Analysis analyze(
            int friendlyCharacterCount,
            float totalAbilityHere,
            float moverAbility) {
        float abilityAfterMove = totalAbilityHere - moverAbility;
        if (friendlyCharacterCount > 1
                && abilityAfterMove > 0.0f
                && abilityAfterMove < 4.0f) {
            return new Analysis(Branch.DESTINY_DANGER, abilityAfterMove);
        }
        if (friendlyCharacterCount == 1 && totalAbilityHere < 4.0f) {
            return new Analysis(Branch.SOLO_ESCAPE, abilityAfterMove);
        }
        return new Analysis(Branch.NONE, abilityAfterMove);
    }

    public static Evaluation destinyDanger(
            String moverTitle,
            String locationTitle,
            float totalAbilityHere,
            float abilityAfterMove,
            float theirPowerHere) {
        boolean enemyPresent = theirPowerHere > 0.0f;
        return new Evaluation(
                Branch.DESTINY_DANGER,
                true,
                String.format(
                        "V32 ABILITY DANGER: Moving %s away drops ability from %.0f"
                                + " to %.0f (< 4) at %s! NO BATTLE DESTINY!%s",
                        moverTitle,
                        totalAbilityHere,
                        abilityAfterMove,
                        locationTitle,
                        enemyPresent
                                ? " ENEMY POWER=" + (int) theirPowerHere
                                : ""),
                enemyPresent ? -500.0f : -300.0f,
                false);
    }

    public static Evaluation soloEscape(
            String moverTitle,
            float totalAbilityHere) {
        return new Evaluation(
                Branch.SOLO_ESCAPE,
                true,
                String.format(
                        "V32 ABILITY SOLO ESCAPE: %s alone with ability %.0f < 4"
                                + " — move to join allies!",
                        moverTitle,
                        totalAbilityHere),
                50.0f,
                false);
    }

    public static boolean isUncontested(float opponentPowerHere) {
        return opponentPowerHere == 0.0f;
    }

    public static boolean canJoinGroup(
            boolean undercover,
            boolean atReadyFlipSite) {
        return !undercover && !atReadyFlipSite;
    }

    public static Evaluation joinGroup(
            String moverTitle,
            float moverAbility,
            String currentLocationTitle,
            String destinationTitle,
            float destinationAbilityTotal) {
        return new Evaluation(
                Branch.JOIN_GROUP,
                true,
                String.format(
                        "V156 JOIN-GROUP: %s (ability %.0f) solo at uncontested %s"
                                + " — join %s (stack reaches ability %.0f)!",
                        moverTitle,
                        moverAbility,
                        currentLocationTitle,
                        destinationTitle,
                        destinationAbilityTotal),
                250.0f,
                true);
    }
}
