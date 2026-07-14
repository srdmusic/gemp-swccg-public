package com.gempukku.swccgo.common;

/** Closed wire keys for engine-owned PULL transaction identity. */
public final class PullDecisionWire {
    public static final String PARENT_DECISION_ID = "pullParentDecisionId";
    public static final String PARENT_ACTION_ORDINAL = "pullParentActionOrdinal";
    public static final String TRANSACTION_ID = "pullTransactionId";
    public static final String PLAYER_ID = "pullPlayerId";
    public static final String SOURCE_CARD_ID = "pullSourceCardId";
    public static final String SOURCE_PERMANENT_CARD_ID = "pullSourcePermanentCardId";
    public static final String GAME_TEXT_ACTION_ID = "pullGameTextActionId";
    public static final String SOURCE_ZONE = "pullSourceZone";
    public static final String SOURCE_ZONE_OWNER = "pullSourceZoneOwner";
    public static final String PHYSICAL_CARD_ID = "pullPhysicalCardId";
    public static final String PHYSICAL_PERMANENT_CARD_ID = "pullPhysicalPermanentCardId";
    public static final String SELECTED_CARD_ID = "pullSelectedCardId";
    public static final String SELECTED_PERMANENT_CARD_ID = "pullSelectedPermanentCardId";
    public static final String DESTINATION_CARD_ID = "pullDestinationCardId";
    public static final String DESTINATION_PERMANENT_CARD_ID = "pullDestinationPermanentCardId";
    public static final String FORCED_DESTINATION_CARD_ID = "pullForcedDestinationCardId";
    public static final String FORCED_DESTINATION_PERMANENT_CARD_ID = "pullForcedDestinationPermanentCardId";

    private PullDecisionWire() {
    }
}
