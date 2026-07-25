package com.gempukku.swccgo.ai.models.common.strategy;

import java.util.Locale;

/**
 * Narrow Endor Operations sequencing and post-flip formation policy.
 *
 * <p>The objective temporarily rewards spreading to satisfy its flip
 * requirements. Once Imperial Outpost is active, ordinary reinforcement and
 * attack plans must outrank establishing another unsupported Endor site.</p>
 */
public final class EndorOperationsTacticalPolicy {
    public static final float ENDOR_SHIELD_BOOTSTRAP_BONUS = 1500.0f;
    public static final float POST_FLIP_REINFORCE_BONUS = 500.0f;
    public static final float POST_FLIP_SPREAD_PENALTY = -250.0f;

    private EndorOperationsTacticalPolicy() {
    }

    public static boolean isEndorOperations(
            String objectiveBlueprintId,
            String objectiveTitle) {
        if ("8_167".equals(objectiveBlueprintId)) {
            return true;
        }
        String lower = lower(objectiveTitle);
        return lower.contains("endor operations")
                || lower.contains("imperial outpost");
    }

    public static float postFlipPlanAdjustment(
            boolean endorOperations,
            boolean flipped,
            boolean reinforceOrAttack,
            boolean targetsEndorSite,
            boolean establishesEmptyEndorSite) {
        if (!endorOperations || !flipped || !targetsEndorSite) {
            return 0.0f;
        }
        if (reinforceOrAttack) {
            return POST_FLIP_REINFORCE_BONUS;
        }
        if (establishesEmptyEndorSite) {
            return POST_FLIP_SPREAD_PENALTY;
        }
        return 0.0f;
    }

    public static float endorShieldBootstrapAdjustment(
            String sourceTitle,
            String actionText) {
        return "endor: bunker".equals(lower(sourceTitle))
                && lower(actionText)
                    .contains("deploy endor shield from reserve deck")
                ? ENDOR_SHIELD_BOOTSTRAP_BONUS
                : 0.0f;
    }

    public static boolean shouldReserveShieldSlotForBattleOrder(
            boolean endorOperations,
            boolean battleOrderAlreadyPlayed,
            boolean occupiesBattlegroundSite,
            boolean occupiesBattlegroundSystem,
            boolean battleOrderRouteStillAvailable) {
        return endorOperations
                && !battleOrderAlreadyPlayed
                && occupiesBattlegroundSite
                && !occupiesBattlegroundSystem
                && battleOrderRouteStillAvailable;
    }

    public static boolean shouldPursueEndorSystem(
            boolean endorOperations,
            boolean flipped,
            boolean endorSystemPresent,
            boolean controlsEndorSystem,
            float opponentPowerAtEndor) {
        return endorOperations
                && flipped
                && endorSystemPresent
                && !controlsEndorSystem
                && opponentPowerAtEndor <= 0.0f;
    }

    public static boolean shouldSuppressEmptyEndorGroundEstablish(
            boolean endorSystemOccupationPending,
            boolean targetsEmptyEndorSite) {
        return endorSystemOccupationPending && targetsEmptyEndorSite;
    }

    public static float bunkerGarrisonAdjustment(
            boolean endorOperations,
            boolean flipped,
            boolean bunker,
            boolean imperialAdmiral,
            int deployCost,
            boolean highValueMobileCharacter) {
        if (!endorOperations || flipped || !bunker) {
            return 0.0f;
        }
        if (imperialAdmiral && deployCost <= 2) {
            return 1200.0f;
        }
        if (highValueMobileCharacter) {
            return -1200.0f;
        }
        return 0.0f;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
