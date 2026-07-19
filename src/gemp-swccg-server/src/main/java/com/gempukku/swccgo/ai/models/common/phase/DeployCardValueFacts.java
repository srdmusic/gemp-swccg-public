package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable adapter facts for generic DEPLOY card-value scoring. */
public final class DeployCardValueFacts {

    private DeployCardValueFacts() {
    }

    public record BaseValue(
            String actionId,
            int power,
            int ability,
            int deployCost,
            float destiny) {

        public BaseValue {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record EliteValue(
            String actionId,
            boolean eliteCharacter) {

        public EliteValue {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record Strategic(
            String actionId,
            boolean needsReinforcement,
            boolean criticalLifeForce) {

        public Strategic {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record TypeValue(
            String actionId,
            boolean highAbilityCharacter) {

        public TypeValue {
            Objects.requireNonNull(actionId, "actionId");
        }
    }
}
