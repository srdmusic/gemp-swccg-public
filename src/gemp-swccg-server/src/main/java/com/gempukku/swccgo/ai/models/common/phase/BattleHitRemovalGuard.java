package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;

/** Runtime facts for the optional Sniper/Dark Strike hit-removal mode. */
public final class BattleHitRemovalGuard {
    private BattleHitRemovalGuard() {
    }

    public static boolean preservesLastOpponent(
            GameState gameState, String playerId,
            String sourceBlueprintId, String targetTitle) {
        if (gameState == null || playerId == null
                || !("10_47".equals(sourceBlueprintId)
                    || "5_135".equals(sourceBlueprintId)
                    || "10_23".equals(sourceBlueprintId)
                    || "5_35".equals(sourceBlueprintId))) {
            return false;
        }
        BattleState battle = gameState.getBattleState();
        if (battle == null || battle.isCanceled() || battle.isReachedDamageSegment()) {
            return false;
        }

        PhysicalCard lastOpponent = null;
        for (PhysicalCard participant : battle.getAllCardsParticipating()) {
            if (playerId.equals(participant.getOwner())) {
                continue;
            }
            if (participant.getBlueprint() == null) {
                return false;
            }
            CardCategory category = participant.getBlueprint().getCardCategory();
            if (category == CardCategory.CHARACTER
                    || category == CardCategory.STARSHIP
                    || category == CardCategory.VEHICLE) {
                if (lastOpponent != null) {
                    return false;
                }
                lastOpponent = participant;
            }
        }
        return lastOpponent != null
                && lastOpponent.getBlueprint().getCardCategory() == CardCategory.CHARACTER
                && lastOpponent.isHit()
                && GameUtils.getFullName(lastOpponent).equalsIgnoreCase(targetTitle);
    }
}
