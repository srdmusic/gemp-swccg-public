package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;

/** Resolves an on-table physical mover represented by a stock decision hint. */
public final class MovePhysicalCardResolver {

    public static final String MOVER_CARD_ID_EXTRA =
            "movePhysicalCardId";

    private MovePhysicalCardResolver() {
    }

    public static ResolvedMover resolveOnTable(
            Iterable<PhysicalCard> cards,
            String playerId,
            String blueprintId) {
        return resolveOnTable(
                cards, playerId, blueprintId, null);
    }

    public static ResolvedMover resolveOnTable(
            Iterable<PhysicalCard> cards,
            String playerId,
            String blueprintId,
            Integer cardId) {
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
            if (cardId != null
                    && card.getCardId() != cardId) {
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
