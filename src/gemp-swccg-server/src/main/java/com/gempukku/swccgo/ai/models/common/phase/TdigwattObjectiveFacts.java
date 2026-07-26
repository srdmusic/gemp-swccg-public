package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DestinyType;

import java.util.Objects;

/**
 * Immutable, source-backed facts for the two mechanically different
 * This Deal Is Getting Worse All The Time printings.
 *
 * <p>The shared title is deliberately not accepted as identity. Adapters must
 * supply the exact physical objective card id and the canonical front
 * blueprint id. The side currently up is a separate fact because engine
 * callers may expose either the front or active-side blueprint.</p>
 */
public final class TdigwattObjectiveFacts {
    public static final String CLASSIC_BLUEPRINT_ID = "109_12";
    public static final String VIRTUAL_BLUEPRINT_ID = "226_12";

    public enum Printing {
        CLASSIC,
        VIRTUAL
    }

    public enum PullTarget {
        BESPIN_SYSTEM,
        BESPIN_CLOUD_CITY,
        DARK_DEAL,
        CLOUD_CITY_OCCUPATION,
        VADERS_BOUNTY
    }

    /**
     * Identity of the exact objective permanent that owns the plan.
     */
    public record ObjectiveIdentity(
            int physicalCardId,
            String frontBlueprintId,
            boolean backSideUp) {
        public ObjectiveIdentity {
            if (physicalCardId <= 0) {
                throw new IllegalArgumentException(
                        "physicalCardId must be positive");
            }
            Objects.requireNonNull(
                    frontBlueprintId, "frontBlueprintId");
            if (!CLASSIC_BLUEPRINT_ID.equals(frontBlueprintId)
                    && !VIRTUAL_BLUEPRINT_ID.equals(
                            frontBlueprintId)) {
                throw new IllegalArgumentException(
                        "unsupported TDIGWATT objective blueprint: "
                                + frontBlueprintId);
            }
        }

        public Printing printing() {
            return CLASSIC_BLUEPRINT_ID.equals(frontBlueprintId)
                    ? Printing.CLASSIC : Printing.VIRTUAL;
        }

        public boolean isSamePhysicalObjective(
                ObjectiveIdentity other) {
            return other != null
                    && physicalCardId == other.physicalCardId
                    && frontBlueprintId.equals(
                            other.frontBlueprintId);
        }
    }

    /**
     * Classic 109_12 law. Occupation is recorded directly and is never
     * inferred from uncontested control.
     */
    public record ClassicState(
            ObjectiveIdentity objective,
            boolean darkDealOnTable,
            boolean darkOccupiesBespinSystem,
            boolean darkOccupiesBespinCloudCity,
            boolean darkDealJustCanceled,
            boolean opponentControlsBespinSystem,
            boolean bespinJustBlownAway) {
        public ClassicState {
            requirePrinting(objective, Printing.CLASSIC);
        }
    }

    /**
     * Virtual 226_12 law. Counts are exact controlled Bespin locations, not
     * occupied locations and not a combined total.
     */
    public record VirtualState(
            ObjectiveIdentity objective,
            int darkControlledBespinLocations,
            int lightControlledBespinLocations) {
        public VirtualState {
            requirePrinting(objective, Printing.VIRTUAL);
            if (darkControlledBespinLocations < 0
                    || lightControlledBespinLocations < 0) {
                throw new IllegalArgumentException(
                        "controlled-location counts must be nonnegative");
            }
        }
    }

    /**
     * One exact Reserve Deck candidate offered by the objective's own action.
     * The adapter establishes target semantics from card source or filters;
     * the policy never recognizes a candidate by shared title text.
     */
    public record PullFacts(
            ObjectiveIdentity objective,
            int actionSourcePhysicalCardId,
            int candidatePhysicalCardId,
            String candidateBlueprintId,
            PullTarget target,
            boolean specialEditionPrint,
            boolean candidateInReserve) {
        public PullFacts {
            Objects.requireNonNull(objective, "objective");
            if (actionSourcePhysicalCardId <= 0
                    || candidatePhysicalCardId <= 0) {
                throw new IllegalArgumentException(
                        "physical card ids must be positive");
            }
            Objects.requireNonNull(
                    candidateBlueprintId, "candidateBlueprintId");
            if (candidateBlueprintId.isBlank()) {
                throw new IllegalArgumentException(
                        "candidateBlueprintId must be nonblank");
            }
            Objects.requireNonNull(target, "target");
        }
    }

    /**
     * The virtual objective grants this regular move on both sides. The
     * adapter supplies the current engine-computed movement cost because
     * ordinary move-cost modifiers and collapsed sites still apply.
     */
    public record LandoMoveFacts(
            ObjectiveIdentity objective,
            int actionSourcePhysicalCardId,
            boolean sourceActionAvailable,
            boolean exactRouteKnown,
            boolean legalDestinationExists,
            boolean advancesOrProtectsObjective,
            boolean formationSafe,
            int requiredForceCost) {
        public LandoMoveFacts {
            Objects.requireNonNull(objective, "objective");
            if (actionSourcePhysicalCardId <= 0) {
                throw new IllegalArgumentException(
                        "actionSourcePhysicalCardId must be positive");
            }
            if (requiredForceCost < 0) {
                throw new IllegalArgumentException(
                        "requiredForceCost must be nonnegative");
            }
        }
    }

    /**
     * Battle facts used by the back-side payoff. "Any Lobot" intentionally
     * has no ownership qualifier, but must be participating in that battle.
     */
    public record BattleFacts(
            ObjectiveIdentity objective,
            boolean yourAlienInBattle,
            boolean yourImperialInBattle,
            boolean yourUgnaughtInBattle,
            boolean yourLandoInBattle,
            boolean anyLobotParticipating) {
        public BattleFacts {
            Objects.requireNonNull(objective, "objective");
        }
    }

    public enum DestinyDrawOwner {
        YOURS,
        OPPONENTS
    }

    /**
     * Exact live facts for the virtual back-side Lando destiny adjustment.
     * The engine remains the authority for per-battle usage; the count here
     * records the source-defined one-use or Lobot-enabled two-use ceiling.
     */
    public record DestinyAdjustmentFacts(
            ObjectiveIdentity objective,
            int actionSourcePhysicalCardId,
            BattleFacts battle,
            DestinyDrawOwner drawOwner,
            DestinyType destinyType,
            int usesPerBattle) {
        public DestinyAdjustmentFacts {
            requirePrinting(objective, Printing.VIRTUAL);
            if (!objective.backSideUp()) {
                throw new IllegalArgumentException(
                        "destiny adjustment requires the virtual back side");
            }
            if (actionSourcePhysicalCardId <= 0) {
                throw new IllegalArgumentException(
                        "actionSourcePhysicalCardId must be positive");
            }
            Objects.requireNonNull(battle, "battle");
            Objects.requireNonNull(drawOwner, "drawOwner");
            Objects.requireNonNull(destinyType, "destinyType");
            if (!objective.isSamePhysicalObjective(
                        battle.objective())
                    || !battle.yourLandoInBattle()
                    || (usesPerBattle != 1
                        && usesPerBattle != 2)) {
                throw new IllegalArgumentException(
                        "invalid virtual Lando destiny facts");
            }
        }
    }

    private TdigwattObjectiveFacts() {
    }

    private static void requirePrinting(
            ObjectiveIdentity objective,
            Printing expected) {
        Objects.requireNonNull(objective, "objective");
        if (objective.printing() != expected) {
            throw new IllegalArgumentException(
                    "expected " + expected
                            + " objective facts, got "
                            + objective.printing());
        }
    }
}
