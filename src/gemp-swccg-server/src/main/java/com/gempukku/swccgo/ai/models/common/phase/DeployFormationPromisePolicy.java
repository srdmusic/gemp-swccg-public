package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.List;

/**
 * Shared non-additive owner for a funded same-site formation promise.
 *
 * A weak first body may deploy only because the phase plan names an exact,
 * affordable companion. Once that first body reaches the promised site, a
 * mid-phase replan must not strand it before the companion follows.
 */
public final class DeployFormationPromisePolicy {

    private DeployFormationPromisePolicy() {
    }

    public record PlannedInstruction(int permanentCardId,
                                     int currentCardId,
                                     String targetLocationId) {
    }

    public record Promise(int anchorPermanentCardId,
                          int anchorCurrentCardId,
                          int companionPermanentCardId,
                          int companionCurrentCardId,
                          String targetLocationId,
                          String companionName,
                          int companionCost) {
    }

    /** Detect an exact deployed anchor plus its cheapest exact planned companion. */
    public static Promise detect(SwccgGame game,
                                 String playerId,
                                 int forceAvailable,
                                 List<PlannedInstruction> instructions) {
        if (game == null || playerId == null || instructions == null
                || instructions.size() < 2 || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        GameState gameState = game.getGameState();
        for (PlannedInstruction anchorInstruction : instructions) {
            PhysicalCard anchor = exactCard(
                    gameState, anchorInstruction.permanentCardId(),
                    anchorInstruction.currentCardId());
            PhysicalCard target = exactTarget(
                    gameState, anchorInstruction.targetLocationId());
            if (!isExactAnchorAtSite(
                    game, playerId, anchor, target,
                    anchorInstruction.targetLocationId())) {
                continue;
            }

            Promise best = null;
            for (PlannedInstruction companionInstruction : instructions) {
                if (companionInstruction == anchorInstruction
                        || !anchorInstruction.targetLocationId().equals(
                                companionInstruction.targetLocationId())) {
                    continue;
                }
                PhysicalCard companion = exactCard(
                        gameState, companionInstruction.permanentCardId(),
                        companionInstruction.currentCardId());
                Integer cost = exactAffordableCompanionCost(
                        game, playerId, companion, target, forceAvailable);
                if (cost == null) continue;
                if (best == null || cost < best.companionCost()) {
                    best = new Promise(
                            anchorInstruction.permanentCardId(),
                            anchorInstruction.currentCardId(),
                            companionInstruction.permanentCardId(),
                            companionInstruction.currentCardId(),
                            anchorInstruction.targetLocationId(),
                            companion.getTitle(), cost);
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** Revalidate a promise on every later decision in the same deploy phase. */
    public static boolean shouldContinue(SwccgGame game,
                                         String playerId,
                                         int forceAvailable,
                                         Promise promise) {
        if (game == null || playerId == null || promise == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        GameState gameState = game.getGameState();
        PhysicalCard anchor = exactCard(
                gameState, promise.anchorPermanentCardId(),
                promise.anchorCurrentCardId());
        PhysicalCard target = exactTarget(
                gameState, promise.targetLocationId());
        if (!isExactAnchorAtSite(
                game, playerId, anchor, target,
                promise.targetLocationId())) {
            return false;
        }
        PhysicalCard companion = exactCard(
                gameState, promise.companionPermanentCardId(),
                promise.companionCurrentCardId());
        return exactAffordableCompanionCost(
                game, playerId, companion, target, forceAvailable) != null;
    }

    private static boolean isExactAnchorAtSite(
            SwccgGame game, String playerId, PhysicalCard anchor,
            PhysicalCard target, String targetLocationId) {
        if (anchor == null || target == null || targetLocationId == null
                || anchor.getBlueprint() == null
                || target.getBlueprint() == null
                || anchor.getBlueprint().getCardCategory()
                        != CardCategory.CHARACTER
                || target.getBlueprint().getCardSubtype()
                        != CardSubtype.SITE
                || !playerId.equals(anchor.getOwner())
                || anchor.isUndercover()
                || anchor.getZone() == null
                || !anchor.getZone().isInPlay()) {
            return false;
        }
        try {
            GameState gameState = game.getGameState();
            PhysicalCard actualLocation = game.getModifiersQuerying()
                    .getLocationThatCardIsAt(gameState, anchor);
            if (actualLocation == null
                    || actualLocation.getCardId() != target.getCardId()
                    || FormationSafety.countFriendlyNonUndercoverCharacters(
                            gameState.getCardsAtLocation(target), playerId) != 1
                    || game.getModifiersQuerying().getTotalAbilityAtLocation(
                            gameState, playerId, target)
                            >= FormationSafety.DESTINY_ABILITY_THRESHOLD) {
                return false;
            }
            return Integer.toString(target.getCardId())
                    .equals(targetLocationId);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Integer exactAffordableCompanionCost(
            SwccgGame game, String playerId, PhysicalCard companion,
            PhysicalCard target, int forceAvailable) {
        if (companion == null || target == null
                || companion.getBlueprint() == null
                || companion.getBlueprint().getCardCategory()
                        != CardCategory.CHARACTER
                || !playerId.equals(companion.getOwner())
                || companion.getZone() != Zone.HAND
                || companion.isUndercover()) {
            return null;
        }
        try {
            GameState gameState = game.getGameState();
            if (!Filters.deployableToLocation(
                    companion, Filters.sameCardId(target), false, 0.0f)
                    .accepts(gameState, game.getModifiersQuerying(),
                            companion)) {
                return null;
            }
            float exactCost = game.getModifiersQuerying().getDeployCost(
                    gameState, companion, companion, target,
                    false, null, false, 0.0f, null, true);
            if (!Float.isFinite(exactCost)) return null;
            int payable = (int) Math.ceil(Math.max(0.0f, exactCost));
            return payable <= forceAvailable ? payable : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PhysicalCard exactCard(
            GameState gameState, int permanentCardId, int currentCardId) {
        PhysicalCard card = gameState.findCardByPermanentId(permanentCardId);
        return card != null && card.getCardId() == currentCardId
                ? card : null;
    }

    private static PhysicalCard exactTarget(
            GameState gameState, String targetLocationId) {
        if (targetLocationId == null) return null;
        try {
            return gameState.findCardById(
                    Integer.parseInt(targetLocationId));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
