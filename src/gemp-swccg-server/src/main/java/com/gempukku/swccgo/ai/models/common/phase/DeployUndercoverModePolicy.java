package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;

import java.util.Locale;
import java.util.regex.Pattern;

/** Typed detection for a character deployment that can use undercover rules. */
public final class DeployUndercoverModePolicy {

    public static final String VERSION_MARKER =
            "V307_TYPED_UNDERCOVER_DEPLOY";
    private static final Pattern EXPLICIT_UNDERCOVER_DEPLOY_MODE =
            Pattern.compile("\\bdeploy\\b.*\\bas (?:an )?undercover spy\\b");

    private DeployUndercoverModePolicy() {
    }

    /** True when game rules leave no normal deployment mode for this card. */
    public static boolean mustDeployUndercover(
            SwccgGame game, PhysicalCard deployingCard,
            SwccgCardBlueprint blueprint) {
        if (game == null || deployingCard == null || blueprint == null) {
            return false;
        }
        try {
            if (blueprint.isOnlyDeploysAsUndercoverSpy(
                    game, deployingCard)) {
                return true;
            }
            return blueprint.getCardCategory() == CardCategory.CHARACTER
                    && game.getGameState() != null
                    && game.getModifiersQuerying() != null
                    && game.getModifiersQuerying()
                        .isDeploysAndMovesLikeUndercoverSpy(
                            game.getGameState(), deployingCard);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * True when a mandatory rule, the bot's optional-mode policy, or an
     * explicit deployment prompt predicts that this deployment is Undercover.
     */
    public static boolean predictsUndercoverDeploy(
            SwccgGame game, PhysicalCard deployingCard,
            SwccgCardBlueprint blueprint, String decisionText) {
        if (mustDeployUndercover(game, deployingCard, blueprint)) {
            return true;
        }
        if (game != null && deployingCard != null && blueprint != null) {
            try {
                if (blueprint.mayDeployAsUndercoverSpy(
                            game, deployingCard)
                        && predictsOptionalUndercoverChoice(
                            game, deployingCard.getOwner())) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Explicit decision text remains a safe fallback.
            }
        }
        String normalized = decisionText == null ? ""
                : decisionText.toLowerCase(Locale.ROOT);
        return EXPLICIT_UNDERCOVER_DEPLOY_MODE.matcher(normalized).find();
    }

    public static boolean isUndercoverDeploy(
            SwccgGame game, PhysicalCard deployingCard,
            SwccgCardBlueprint blueprint, String decisionText) {
        return predictsUndercoverDeploy(
                game, deployingCard, blueprint, decisionText);
    }

    private static boolean predictsOptionalUndercoverChoice(
            SwccgGame game, String playerId) {
        if (game == null || playerId == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        try {
            String opponent = game.getGameState()
                    .getOpponent(playerId);
            if (opponent == null) return false;
            int opponentDrain = 0;
            for (PhysicalCard location
                    : game.getGameState().getTopLocations()) {
                if (location == null) continue;
                boolean opponentPresent = game.getGameState()
                    .getCardsAtLocation(location).stream()
                    .anyMatch(card -> card != null
                        && opponent.equals(card.getOwner()));
                if (opponentPresent) {
                    opponentDrain += (int) game.getModifiersQuerying()
                        .getForceDrainAmount(
                            game.getGameState(), location, opponent);
                }
            }
            return ResponsePolicy.shouldDeployUndercover(
                    opponentDrain);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
