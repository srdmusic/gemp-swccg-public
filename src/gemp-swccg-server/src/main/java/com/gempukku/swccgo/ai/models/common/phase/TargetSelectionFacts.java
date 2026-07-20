package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Pure adapter-supplied facts for shared target-selection scoring. */
public final class TargetSelectionFacts {

    public enum Intent {
        BENEFICIAL,
        HARMFUL
    }

    public enum Ownership {
        OWN,
        OPPONENT
    }

    public record OwnershipFacts(String actionId,
                                 Intent intent,
                                 Ownership ownership) {
        public OwnershipFacts {
            actionId = requireNonBlank(actionId, "actionId");
            Objects.requireNonNull(intent, "intent");
            Objects.requireNonNull(ownership, "ownership");
        }
    }

    public record UndercoverFacts(String actionId,
                                  Intent intent,
                                  Ownership ownership,
                                  boolean undercover) {
        public UndercoverFacts {
            actionId = requireNonBlank(actionId, "actionId");
            Objects.requireNonNull(intent, "intent");
            Objects.requireNonNull(ownership, "ownership");
        }
    }

    public record ValueFacts(String actionId,
                             Intent intent,
                             Ownership ownership,
                             boolean battleMode,
                             boolean highPower,
                             boolean unique) {
        public ValueFacts {
            actionId = requireNonBlank(actionId, "actionId");
            Objects.requireNonNull(intent, "intent");
            Objects.requireNonNull(ownership, "ownership");
        }
    }

    private TargetSelectionFacts() {
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
        return value;
    }
}
