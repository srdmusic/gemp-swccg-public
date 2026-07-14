package com.gempukku.swccgo.ai.models.common.objective;

import java.util.Objects;

/** One ordered, phase-local objective score contribution for a candidate. */
public record ObjectiveContribution(
        Rule rule,
        Channel channel,
        int candidateOrdinal,
        float value) {

    public enum Rule {
        MY_LORD_V83,
        MY_LORD_V88,
        MY_LORD_V108,
        MY_LORD_V110,
        OBJECTIVE_SITE,
        V193_PARENT,
        V193_CHILD,
        V192_PULL_PARENT,
        V29_9_HUNT_DOWN,
        V35_VADER_EXPENDABLE,
        V35_HUNT_DESTINY
    }

    public enum Channel {
        DEPLOY_PARENT,
        DEPLOY_CHILD,
        PULL_PARENT,
        BATTLE_INITIATE
    }

    public ObjectiveContribution {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(channel, "channel");
        if (candidateOrdinal < 0) {
            throw new IllegalArgumentException("candidateOrdinal must be >= 0");
        }
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite");
        }
    }
}
