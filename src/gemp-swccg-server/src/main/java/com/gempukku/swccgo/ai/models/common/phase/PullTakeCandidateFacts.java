package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;

import java.util.Objects;

/** Immutable facts for a stock "take card into hand" child decision. */
public record PullTakeCandidateFacts(
        String actionId,
        String cardTitle,
        String blueprintId,
        Float destiny,
        Float power,
        Float ability,
        CardCategory category,
        int turnNumber,
        Integer priorityScoreByTitle,
        Integer priorityScoreByBlueprint,
        boolean admiralPull,
        boolean commanderPull,
        boolean objectiveNeedsBespinPresence,
        boolean friendlyAtCloudCity,
        boolean handBuddy,
        int availableForce,
        boolean downloadLocationEnabler) {
    public PullTakeCandidateFacts {
        Objects.requireNonNull(actionId, "actionId");
        cardTitle = cardTitle == null ? "" : cardTitle;
        blueprintId = blueprintId == null ? "" : blueprintId;
    }
}
