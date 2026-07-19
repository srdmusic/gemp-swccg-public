package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Objects;

/** Immutable adapter facts for shared DEPLOY parent and fallback routing. */
public final class DeployActionEnvelopeFacts {

    private DeployActionEnvelopeFacts() {
    }

    public record ParentAction(
            String actionId,
            boolean blockedResponse,
            boolean personaReplace) {

        public ParentAction {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record TitleGate(
            String actionId,
            boolean blockTurnOneEffect) {

        public TitleGate {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record UnknownAction(
            String actionId,
            boolean earlyCardIsLocation,
            boolean deployLocationsPlanActive,
            int turnNumber) {

        public UnknownAction {
            Objects.requireNonNull(actionId, "actionId");
        }
    }
}
