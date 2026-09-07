package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;

/** Keeps a table source from masquerading as the card a deploy action offers. */
public final class DeployCandidateLookupPolicy {

    private DeployCandidateLookupPolicy() {
    }

    public static boolean isDirectHandCandidate(
            PhysicalCard card, String playerId) {
        return card != null && playerId != null
                && playerId.equals(card.getOwner())
                && card.getZone() == Zone.HAND;
    }
}
