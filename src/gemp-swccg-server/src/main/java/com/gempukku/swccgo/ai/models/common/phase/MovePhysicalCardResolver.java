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
        if (cards == null || playerId == null
                || (blueprintId == null && cardId == null)) {
            return null;
        }

        ResolvedMover resolved = null;
        for (PhysicalCard card : cards) {
            if (card == null || !playerId.equals(card.getOwner())) {
                continue;
            }
            if (blueprintId != null
                    && !blueprintId.equals(
                        card.getBlueprintId(true))) {
                continue;
            }
            if (cardId != null
                    && card.getCardId() != cardId) {
                continue;
            }
            PhysicalCard origin = physicalOrigin(card);
            if (origin != null) {
                if (resolved != null) {
                    // A blueprint hint alone cannot identify which physical
                    // copy the engine is moving. Unknown provenance must stay
                    // neutral instead of borrowing the first copy's state.
                    return null;
                }
                resolved = new ResolvedMover(card, origin);
            }
        }
        return resolved;
    }

    private static PhysicalCard physicalOrigin(
            PhysicalCard card) {
        try {
            PhysicalCard origin = card.getAtLocation();
            if (origin != null) {
                return origin;
            }
            PhysicalCard root =
                    card.getCardAttachedToAtLocation();
            return root != null && root != card
                    ? root.getAtLocation() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public record ResolvedMover(PhysicalCard card, PhysicalCard origin) {
    }
}
