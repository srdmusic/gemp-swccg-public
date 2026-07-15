package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;

/** Resolves the first on-table physical mover represented by a stock blueprint hint. */
public final class MovePhysicalCardResolver {

    private MovePhysicalCardResolver() {
    }

    public static ResolvedMover resolveOnTable(
            Iterable<PhysicalCard> cards,
            String playerId,
            String blueprintId) {
        if (cards == null || playerId == null || blueprintId == null) {
            return null;
        }

        for (PhysicalCard card : cards) {
            if (card == null || !playerId.equals(card.getOwner())) {
                continue;
            }
            if (!blueprintId.equals(card.getBlueprintId(true))) {
                continue;
            }
            PhysicalCard origin = card.getAtLocation();
            if (origin != null) {
                return new ResolvedMover(card, origin);
            }
        }
        return null;
    }

    public record ResolvedMover(PhysicalCard card, PhysicalCard origin) {
    }
}
