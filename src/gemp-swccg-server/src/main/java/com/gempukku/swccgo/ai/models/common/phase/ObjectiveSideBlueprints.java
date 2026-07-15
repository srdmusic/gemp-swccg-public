package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;

/** V200: Stable objective sides independent of the physical card's current flipped state. */
public record ObjectiveSideBlueprints(
        SwccgCardBlueprint front,
        SwccgCardBlueprint back) {

    public static ObjectiveSideBlueprints resolve(PhysicalCard objectiveCard) {
        if (objectiveCard == null) {
            return null;
        }

        SwccgCardBlueprint current = objectiveCard.getBlueprint();
        if (current == null) {
            return null;
        }

        SwccgCardBlueprint opposite = objectiveCard.getOtherSideBlueprint();
        if (opposite == null) {
            return new ObjectiveSideBlueprints(current, null);
        }

        return objectiveCard.isFlipped()
                ? new ObjectiveSideBlueprints(opposite, current)
                : new ObjectiveSideBlueprints(current, opposite);
    }
}
