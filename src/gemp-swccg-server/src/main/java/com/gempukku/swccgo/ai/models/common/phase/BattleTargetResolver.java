package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;

import java.util.List;
import java.util.Locale;

/** Resolves a battle action's selected location from the stock, ordinally aligned card id. */
public final class BattleTargetResolver {

    private BattleTargetResolver() {
    }

    public static PhysicalCard resolve(
            List<PhysicalCard> locations,
            String alignedCardId,
            String actionText) {
        if (locations == null || locations.isEmpty()) {
            return null;
        }

        if (alignedCardId != null && !alignedCardId.isBlank()) {
            String normalizedCardId = alignedCardId.trim();
            for (PhysicalCard location : locations) {
                if (location != null
                        && normalizedCardId.equals(String.valueOf(location.getCardId()))) {
                    return location;
                }
            }
        }

        String actionLower = actionText != null
                ? actionText.toLowerCase(Locale.ROOT) : "";
        for (PhysicalCard location : locations) {
            String title = location != null ? location.getTitle() : null;
            if (title != null && actionLower.contains(title.toLowerCase(Locale.ROOT))) {
                return location;
            }
        }
        return null;
    }
}
