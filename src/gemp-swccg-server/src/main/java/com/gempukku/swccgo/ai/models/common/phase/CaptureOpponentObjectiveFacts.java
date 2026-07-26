package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;

/**
 * Reads capture-objective actions offered from the opponent's objective.
 *
 * <p>This deliberately does not depend on the current player's
 * {@code ObjectiveAnalyzer}. Unknown or incomplete engine state fails closed.
 */
public final class CaptureOpponentObjectiveFacts {
    private static final String TIGIH_BLUEPRINT_ID = "9_61";
    private static final String BHBM_BLUEPRINT_ID = "9_151";

    public record TigihTransferAssessment(
            boolean legal,
            boolean crossoverModifierPressureKnown,
            float crossoverModifierPressure) {
    }

    public record BhbmTargetDownloadAssessment(
            boolean legalObjectiveDownload) {
    }

    private CaptureOpponentObjectiveFacts() {
    }

    /**
     * Mirrors the exact back-side legality for I Can Save Him's
     * opponent-controlled "Transfer Luke to Vader" action.
     */
    public static boolean isLegalTigihTransferLukeToVader(
            SwccgGame game,
            String playerId,
            PhysicalCard source) {
        return assessTigihTransferLukeToVader(
                game, playerId, source).legal();
    }

    public static TigihTransferAssessment
            assessTigihTransferLukeToVader(
                SwccgGame game,
                String playerId,
                PhysicalCard source) {
        if (game == null || playerId == null || source == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return unavailable();
        }

        try {
            String opponentId = game.getOpponent(playerId);
            if (opponentId == null
                    || !opponentId.equals(source.getOwner())
                    || !TIGIH_BLUEPRINT_ID.equals(
                        normalizeBlueprintId(
                            source.getBlueprintId(true)))
                    || !source.isFlipped()
                    || !game.getGameState()
                        .isCardInPlayActive(source)) {
                return unavailable();
            }

            PhysicalCard captiveLuke = Filters.findFirstActive(
                    game,
                    source,
                    SpotOverride.INCLUDE_CAPTIVE,
                    Filters.and(
                        Filters.Luke,
                        Filters.captiveNotProhibitedFromBeingTransferred,
                        Filters.escortedBy(
                            source,
                            Filters.and(
                                Filters.Imperial,
                                Filters.except(Filters.Vader)))));
            if (captiveLuke == null) {
                return unavailable();
            }

            PhysicalCard vader = Filters.findFirstActive(
                    game,
                    source,
                    Filters.and(
                        Filters.Vader,
                        Filters.presentWith(captiveLuke),
                        Filters.canEscortCaptive(captiveLuke)));
            if (vader == null) {
                return unavailable();
            }

            float modifierPressure =
                    game.getModifiersQuerying()
                        .getCrossoverAttemptTotal(
                            game.getGameState(), vader, 0.0f);
            if (!Float.isFinite(modifierPressure)) {
                return new TigihTransferAssessment(
                        true, false, 0.0f);
            }
            return new TigihTransferAssessment(
                    true, true, modifierPressure);
        } catch (Exception ignored) {
            return unavailable();
        }
    }

    /**
     * Mirrors the opponent-owned target downloads printed on either side of
     * Bring Him Before Me. The engine remains the legality owner. This reader
     * only proves the exact active opponent objective and one of its exact
     * Luke, Leia, or Kanan download action texts.
     */
    public static BhbmTargetDownloadAssessment
            assessBhbmOpponentTargetDownload(
                SwccgGame game,
                String playerId,
                PhysicalCard source,
                String actionText) {
        if (game == null || playerId == null || source == null
                || actionText == null
                || game.getGameState() == null) {
            return unavailableBhbmDownload();
        }
        try {
            String opponentId = game.getOpponent(playerId);
            String normalizedText =
                    actionText.trim().toLowerCase(
                        java.util.Locale.ROOT);
            boolean exactDownloadText =
                    "deploy luke from reserve deck"
                        .equals(normalizedText)
                    || "deploy luke from lost pile"
                        .equals(normalizedText)
                    || "deploy leia from reserve deck"
                        .equals(normalizedText)
                    || "deploy leia from lost pile"
                        .equals(normalizedText)
                    || "deploy kanan from reserve deck"
                        .equals(normalizedText)
                    || "deploy kanan from lost pile"
                        .equals(normalizedText);
            return new BhbmTargetDownloadAssessment(
                    exactDownloadText
                    && opponentId != null
                    && opponentId.equals(source.getOwner())
                    && BHBM_BLUEPRINT_ID.equals(
                        normalizeBlueprintId(
                            source.getBlueprintId(true)))
                    && game.getGameState()
                        .isCardInPlayActive(source));
        } catch (Exception ignored) {
            return unavailableBhbmDownload();
        }
    }

    private static TigihTransferAssessment unavailable() {
        return new TigihTransferAssessment(
                false, false, 0.0f);
    }

    private static BhbmTargetDownloadAssessment
            unavailableBhbmDownload() {
        return new BhbmTargetDownloadAssessment(false);
    }

    private static String normalizeBlueprintId(
            String blueprintId) {
        if (blueprintId == null) {
            return null;
        }
        String normalized = blueprintId.endsWith("_BACK")
                ? blueprintId.substring(
                    0, blueprintId.length() - 5)
                : blueprintId;
        String[] parts = normalized.split("_");
        if (parts.length != 2) {
            return normalized;
        }
        try {
            return Integer.parseInt(parts[0])
                    + "_" + Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
            return normalized;
        }
    }
}
