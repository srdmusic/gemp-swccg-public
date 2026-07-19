package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable adapter facts for the shared BATTLE action-text policy. */
public final class BattleActionTextFacts {

    private BattleActionTextFacts() {
    }

    public record InitiationFacts(
            String actionId,
            boolean locationResolved,
            String locationTitle,
            float ourPower,
            float theirPower,
            float ourAbility,
            float theirAbility,
            int reserveDeckSize) {

        public InitiationFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }
}
