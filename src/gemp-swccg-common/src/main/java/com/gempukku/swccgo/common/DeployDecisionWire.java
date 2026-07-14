package com.gempukku.swccgo.common;

/** Closed wire keys for engine-owned DEPLOY transaction identity. */
public final class DeployDecisionWire {
    public static final String ATTEMPT_ID = "deployAttemptId";
    public static final String PARENT_DECISION_ID = "deployParentDecisionId";
    public static final String PARENT_ACTION_ORDINAL = "deployParentActionOrdinal";
    public static final String PLAYER_ID = "deployPlayerId";
    public static final String SOURCE_CARD_ID = "deploySourceCardId";
    public static final String SOURCE_PERMANENT_CARD_ID = "deploySourcePermanentCardId";
    public static final String SOURCE_ZONE = "deploySourceZone";
    public static final String DESTINATION_LEGALITY_KNOWN = "deployDestinationLegalityKnown";
    public static final String LEGAL_DESTINATIONS = "deployLegalDestinations";
    public static final String LEGAL_BUDDIES = "deployLegalBuddies";
    public static final String SELECTED_BUDDY = "deploySelectedBuddy";
    public static final String DESTINATION_CARD_ID = "deployDestinationCardId";
    public static final String DESTINATION_PERMANENT_CARD_ID = "deployDestinationPermanentCardId";
    public static final String BUDDY_CARD_ID = "deployBuddyCardId";
    public static final String BUDDY_PERMANENT_CARD_ID = "deployBuddyPermanentCardId";
    public static final String SELECTED_BUDDY_CARD_ID = "deploySelectedBuddyCardId";
    public static final String SELECTED_BUDDY_PERMANENT_CARD_ID =
            "deploySelectedBuddyPermanentCardId";
    public static final String FORCED_DESTINATION = "deployForcedDestination";

    private DeployDecisionWire() {
    }
}
