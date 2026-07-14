package com.gempukku.swccgo.common;

import java.util.Objects;

/** Exact destination identity exposed by a deploy action without retaining mutable cards. */
public sealed interface DeployDestinationRef {
    record Card(DeployPhysicalCardRef card) implements DeployDestinationRef {
        public Card {
            Objects.requireNonNull(card, "card");
        }
    }

    record ZoneDestination(Zone zone) implements DeployDestinationRef {
        public ZoneDestination {
            Objects.requireNonNull(zone, "zone");
        }
    }
}
