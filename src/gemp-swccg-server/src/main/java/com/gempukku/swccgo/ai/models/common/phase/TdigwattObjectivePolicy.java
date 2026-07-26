package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/**
 * Pure source-law and objective-intent decisions for classic and virtual
 * TDIGWATT. This layer does not read engine state and does not score legacy
 * actions. Mirrored adapters may use these decisions to dominate old rules
 * without deleting them.
 */
public final class TdigwattObjectivePolicy {
    public enum DeployPriority {
        NONE,
        PROTECT_STABLE_BACK,
        ADVANCE_FRONT,
        COMPLETE_FRONT
    }

    public record BattlePayoff(
            int totalBattleDestinyBonus,
            int landoDestinyAdjustments) {
        public BattlePayoff {
            if (totalBattleDestinyBonus < 0
                    || landoDestinyAdjustments < 0) {
                throw new IllegalArgumentException(
                        "battle payoffs must be nonnegative");
            }
        }

        public boolean eligible() {
            return totalBattleDestinyBonus > 0
                    || landoDestinyAdjustments > 0;
        }
    }

    /**
     * The positive amount by which losing one candidate erodes the objective
     * plan. The two terminal flags identify the edges that need stronger
     * protection than ordinary progress.
     */
    public record RetentionMargin(
            int frontProgressLost,
            int stableBackCushionLost,
            boolean preventsReadyFrontFlip,
            boolean breaksStableBack) {
        public RetentionMargin {
            if (frontProgressLost < 0
                    || stableBackCushionLost < 0) {
                throw new IllegalArgumentException(
                        "retention losses must be nonnegative");
            }
        }

        public boolean protectionRequired() {
            return frontProgressLost > 0
                    || stableBackCushionLost > 0
                    || preventsReadyFrontFlip
                    || breaksStableBack;
        }
    }

    public record LossForfeitRetention(
            RetentionMargin forceLoss,
            RetentionMargin forfeit) {
        public LossForfeitRetention {
            Objects.requireNonNull(forceLoss, "forceLoss");
            Objects.requireNonNull(forfeit, "forfeit");
        }
    }

    private static final BattlePayoff NO_BATTLE_PAYOFF =
            new BattlePayoff(0, 0);
    private static final RetentionMargin NO_RETENTION =
            new RetentionMargin(0, 0, false, false);

    private TdigwattObjectivePolicy() {
    }

    public static boolean classicFlipReady(
            TdigwattObjectiveFacts.ClassicState facts) {
        Objects.requireNonNull(facts, "facts");
        return !facts.objective().backSideUp()
                && facts.darkDealOnTable()
                && facts.darkOccupiesBespinSystem()
                && facts.darkOccupiesBespinCloudCity();
    }

    /**
     * Classic back-side stability fails only on the three triggers named by
     * Card109_012_BACK. Dark Deal merely leaving table and loss of occupation
     * are intentionally absent.
     */
    public static boolean classicStableBack(
            TdigwattObjectiveFacts.ClassicState facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.objective().backSideUp()
                && !facts.darkDealJustCanceled()
                && !facts.opponentControlsBespinSystem()
                && !facts.bespinJustBlownAway();
    }

    public static boolean virtualFlipReady(
            TdigwattObjectiveFacts.VirtualState facts) {
        Objects.requireNonNull(facts, "facts");
        return !facts.objective().backSideUp()
                && facts.darkControlledBespinLocations() >= 3
                && facts.lightControlledBespinLocations() < 3;
    }

    /**
     * Card226_012_BACK remains stable at every tie. It flips only when Light
     * controls strictly more Bespin locations than Dark.
     */
    public static boolean virtualStableBack(
            TdigwattObjectiveFacts.VirtualState facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.objective().backSideUp()
                && virtualStableBackCushion(facts) >= 0;
    }

    public static int virtualStableBackCushion(
            TdigwattObjectiveFacts.VirtualState facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.darkControlledBespinLocations()
                - facts.lightControlledBespinLocations();
    }

    /**
     * Keeps the two source-defined pull lists separate. Classic may take any
     * Bespin system print. Virtual may take Bespin only when the candidate has
     * the Special Edition icon.
     */
    public static boolean sourceLegalPull(
            TdigwattObjectiveFacts.PullFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.candidateInReserve()
                || facts.actionSourcePhysicalCardId()
                    != facts.objective().physicalCardId()) {
            return false;
        }

        if (facts.objective().printing()
                == TdigwattObjectiveFacts.Printing.CLASSIC) {
            if (facts.objective().backSideUp()) {
                return false;
            }
            return switch (facts.target()) {
                case BESPIN_SYSTEM, BESPIN_CLOUD_CITY,
                        DARK_DEAL, CLOUD_CITY_OCCUPATION -> true;
                case VADERS_BOUNTY -> false;
            };
        }

        return switch (facts.target()) {
            case DARK_DEAL, VADERS_BOUNTY -> true;
            case BESPIN_SYSTEM -> facts.specialEditionPrint();
            case BESPIN_CLOUD_CITY, CLOUD_CITY_OCCUPATION -> false;
        };
    }

    public static DeployPriority deployPriority(
            TdigwattObjectiveFacts.ClassicState before,
            TdigwattObjectiveFacts.ClassicState after) {
        requireSameObjective(
                before.objective(), after.objective());
        if (before.objective().backSideUp()
                != after.objective().backSideUp()) {
            throw new IllegalArgumentException(
                    "deploy projection must remain on the same side");
        }
        if (before.objective().backSideUp()
                || classicFlipReady(before)) {
            return DeployPriority.NONE;
        }
        if (classicFlipReady(after)) {
            return DeployPriority.COMPLETE_FRONT;
        }
        return classicFrontProgress(after)
                    > classicFrontProgress(before)
                ? DeployPriority.ADVANCE_FRONT
                : DeployPriority.NONE;
    }

    public static DeployPriority deployPriority(
            TdigwattObjectiveFacts.VirtualState before,
            TdigwattObjectiveFacts.VirtualState after) {
        requireSameObjective(
                before.objective(), after.objective());
        if (before.objective().backSideUp()
                != after.objective().backSideUp()) {
            throw new IllegalArgumentException(
                    "deploy projection must remain on the same side");
        }

        if (!before.objective().backSideUp()) {
            if (virtualFlipReady(before)) {
                return DeployPriority.NONE;
            }
            if (virtualFlipReady(after)) {
                return DeployPriority.COMPLETE_FRONT;
            }
            return virtualFrontProgress(after)
                        > virtualFrontProgress(before)
                    ? DeployPriority.ADVANCE_FRONT
                    : DeployPriority.NONE;
        }

        int beforeCushion = virtualStableBackCushion(before);
        int afterCushion = virtualStableBackCushion(after);
        return beforeCushion >= 0
                && afterCushion > beforeCushion
                ? DeployPriority.PROTECT_STABLE_BACK
                : DeployPriority.NONE;
    }

    /**
     * Returns the engine-computed Force floor needed to preserve a useful
     * source-granted virtual Lando move. Classic has no such source action.
     */
    public static int virtualLandoMoveForceReserve(
            TdigwattObjectiveFacts.LandoMoveFacts facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.objective().printing()
                    == TdigwattObjectiveFacts.Printing.VIRTUAL
                && facts.actionSourcePhysicalCardId()
                    == facts.objective().physicalCardId()
                && facts.sourceActionAvailable()
                && facts.exactRouteKnown()
                && facts.legalDestinationExists()
                && facts.advancesOrProtectsObjective()
                && facts.formationSafe()
                ? facts.requiredForceCost() : 0;
    }

    public static BattlePayoff battlePayoff(
            TdigwattObjectiveFacts.BattleFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.objective().backSideUp()) {
            return NO_BATTLE_PAYOFF;
        }

        boolean pair = facts.yourAlienInBattle()
                && facts.yourImperialInBattle();
        if (facts.objective().printing()
                == TdigwattObjectiveFacts.Printing.CLASSIC) {
            return pair
                    ? new BattlePayoff(
                            facts.yourUgnaughtInBattle() ? 4 : 2,
                            0)
                    : NO_BATTLE_PAYOFF;
        }

        int destinyBonus = pair ? 2 : 0;
        int landoAdjustments = facts.yourLandoInBattle()
                ? facts.anyLobotParticipating() ? 2 : 1
                : 0;
        return destinyBonus == 0 && landoAdjustments == 0
                ? NO_BATTLE_PAYOFF
                : new BattlePayoff(
                        destinyBonus, landoAdjustments);
    }

    public static LossForfeitRetention retentionMargins(
            TdigwattObjectiveFacts.ClassicState before,
            TdigwattObjectiveFacts.ClassicState afterForceLoss,
            TdigwattObjectiveFacts.ClassicState afterForfeit) {
        requireSameProjection(
                before.objective(),
                afterForceLoss.objective(),
                afterForfeit.objective());
        return new LossForfeitRetention(
                classicRetention(before, afterForceLoss),
                classicRetention(before, afterForfeit));
    }

    public static LossForfeitRetention retentionMargins(
            TdigwattObjectiveFacts.VirtualState before,
            TdigwattObjectiveFacts.VirtualState afterForceLoss,
            TdigwattObjectiveFacts.VirtualState afterForfeit) {
        requireSameProjection(
                before.objective(),
                afterForceLoss.objective(),
                afterForfeit.objective());
        return new LossForfeitRetention(
                virtualRetention(before, afterForceLoss),
                virtualRetention(before, afterForfeit));
    }

    private static RetentionMargin classicRetention(
            TdigwattObjectiveFacts.ClassicState before,
            TdigwattObjectiveFacts.ClassicState after) {
        requireSameSide(
                before.objective(), after.objective());
        if (!before.objective().backSideUp()) {
            int progressLost = Math.max(
                    0, classicFrontProgress(before)
                            - classicFrontProgress(after));
            boolean preventsReadyFlip = classicFlipReady(before)
                    && !classicFlipReady(after);
            return progressLost == 0 && !preventsReadyFlip
                    ? NO_RETENTION
                    : new RetentionMargin(
                            progressLost, 0,
                            preventsReadyFlip, false);
        }

        boolean stableBefore = classicStableBack(before);
        boolean stableAfter = classicStableBack(after);
        return stableBefore && !stableAfter
                ? new RetentionMargin(0, 1, false, true)
                : NO_RETENTION;
    }

    private static RetentionMargin virtualRetention(
            TdigwattObjectiveFacts.VirtualState before,
            TdigwattObjectiveFacts.VirtualState after) {
        requireSameSide(
                before.objective(), after.objective());
        if (!before.objective().backSideUp()) {
            int progressLost = Math.max(
                    0, virtualFrontProgress(before)
                            - virtualFrontProgress(after));
            boolean preventsReadyFlip = virtualFlipReady(before)
                    && !virtualFlipReady(after);
            return progressLost == 0 && !preventsReadyFlip
                    ? NO_RETENTION
                    : new RetentionMargin(
                            progressLost, 0,
                            preventsReadyFlip, false);
        }

        int beforeCushion = virtualStableBackCushion(before);
        int afterCushion = virtualStableBackCushion(after);
        int cushionLost = Math.max(
                0, beforeCushion - afterCushion);
        boolean breaksStable = beforeCushion >= 0
                && afterCushion < 0;
        return cushionLost == 0 && !breaksStable
                ? NO_RETENTION
                : new RetentionMargin(
                        0, cushionLost, false, breaksStable);
    }

    private static int classicFrontProgress(
            TdigwattObjectiveFacts.ClassicState facts) {
        int progress = facts.darkDealOnTable() ? 1 : 0;
        if (facts.darkOccupiesBespinSystem()) progress++;
        if (facts.darkOccupiesBespinCloudCity()) progress++;
        return progress;
    }

    private static int virtualFrontProgress(
            TdigwattObjectiveFacts.VirtualState facts) {
        return Math.min(
                3, facts.darkControlledBespinLocations())
                + (facts.lightControlledBespinLocations() < 3
                    ? 1 : 0);
    }

    private static void requireSameProjection(
            TdigwattObjectiveFacts.ObjectiveIdentity before,
            TdigwattObjectiveFacts.ObjectiveIdentity afterForceLoss,
            TdigwattObjectiveFacts.ObjectiveIdentity afterForfeit) {
        requireSameObjective(before, afterForceLoss);
        requireSameObjective(before, afterForfeit);
        requireSameSide(before, afterForceLoss);
        requireSameSide(before, afterForfeit);
    }

    private static void requireSameObjective(
            TdigwattObjectiveFacts.ObjectiveIdentity before,
            TdigwattObjectiveFacts.ObjectiveIdentity after) {
        Objects.requireNonNull(before, "before objective");
        Objects.requireNonNull(after, "after objective");
        if (!before.isSamePhysicalObjective(after)) {
            throw new IllegalArgumentException(
                    "projection must use the same physical objective");
        }
    }

    private static void requireSameSide(
            TdigwattObjectiveFacts.ObjectiveIdentity before,
            TdigwattObjectiveFacts.ObjectiveIdentity after) {
        if (before.backSideUp() != after.backSideUp()) {
            throw new IllegalArgumentException(
                    "projection must remain on the same side");
        }
    }
}
