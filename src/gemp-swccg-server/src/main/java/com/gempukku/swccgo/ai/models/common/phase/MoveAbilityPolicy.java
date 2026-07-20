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
        JOIN_GROUP,
        WEAK_SPLIT,
        JOIN_DESTINATION,
        ABILITY_BUDDY_BREAK,
        ABILITY_BUDDY_DOOMED_SKIP
    }

    public record Analysis(Branch branch, float abilityAfterMove) {
    }

    public record Evaluation(
            Branch branch,
            boolean applies,
            String reason,
            float delta,
            boolean claimDoctrine) {
        private static Evaluation none() {
            return new Evaluation(Branch.NONE, false, null, 0.0f, false);
        }
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

    public static Evaluation weakSplit(
            Float moverAbility,
            boolean differentDestination,
            float destinationOpponentPower,
            int destinationFriendlyCharacters,
            int remainingFriendlyCharacters,
            float maximumRemainingAbility) {
        if (moverAbility != null
                && moverAbility < 4.0f
                && differentDestination
                && destinationOpponentPower <= 0.0f
                && destinationFriendlyCharacters == 0
                && remainingFriendlyCharacters == 1
                && maximumRemainingAbility < 4.0f) {
            return new Evaluation(
                    Branch.WEAK_SPLIT,
                    true,
                    "L1/L4 SPLIT (batch1b): weak mover to empty site would create TWO weak solos",
                    -800.0f,
                    false);
        }
        return Evaluation.none();
    }

    public static Evaluation joinDestination(
            String destinationTitle,
            float destinationAbility,
            float moverAbility,
            boolean destinyCapable,
            String sourceTitle) {
        float delta = Math.min(
                450.0f,
                250.0f
                        + Math.min(100.0f, destinationAbility * 10.0f)
                        + (destinyCapable ? 150.0f : 0.0f));
        return new Evaluation(
                Branch.JOIN_DESTINATION,
                true,
                String.format(
                        "V156 JOIN-GROUP DEST: %s reaches ability %.0f%s"
                                + " — join (weak solo leaving %s)!",
                        destinationTitle,
                        destinationAbility + moverAbility,
                        destinyCapable ? " (destiny-capable)" : "",
                        sourceTitle),
                delta,
                false);
    }

    public static Evaluation abilityBuddy(
            String moverTitle,
            String locationTitle,
            int friendlyCharacterCount,
            float totalAbilityHere,
            float abilityAfterMove,
            int abilityBuddyThreshold,
            float opponentPowerGap) {
        boolean siteDoomed = opponentPowerGap >= 6.0f;
        boolean breaksBuddyThreshold = friendlyCharacterCount > 1
                && totalAbilityHere >= abilityBuddyThreshold
                && abilityAfterMove < abilityBuddyThreshold;
        if (breaksBuddyThreshold
                && abilityAfterMove >= 4.0f
                && !siteDoomed) {
            return new Evaluation(
                    Branch.ABILITY_BUDDY_BREAK,
                    true,
                    String.format(
                            "V33 BUDDY BREAK: Moving %s drops ability from %.0f"
                                    + " to %.0f (< %d) at %s",
                            moverTitle,
                            totalAbilityHere,
                            abilityAfterMove,
                            abilityBuddyThreshold,
                            locationTitle),
                    -150.0f,
                    false);
        }
        if (siteDoomed && breaksBuddyThreshold) {
            return new Evaluation(
                    Branch.ABILITY_BUDDY_DOOMED_SKIP,
                    false,
                    null,
                    0.0f,
                    false);
        }
        return Evaluation.none();
    }
}
