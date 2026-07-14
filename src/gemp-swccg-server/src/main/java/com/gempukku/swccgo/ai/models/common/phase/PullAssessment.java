package com.gempukku.swccgo.ai.models.common.phase;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Pure PULL verdict with a closed, ordered evidence vocabulary. */
public record PullAssessment(PullRoute route, Verdict verdict, List<Evidence> evidence) {

    public enum Verdict {
        ALLOW,
        DEFER,
        BLOCK
    }

    public enum Evidence {
        EXACT_PERMANENT_AND_CURRENT_IDENTITY,
        ACCEPTED_PARENT_TRANSACTION_ID,
        SOURCE_FILTER_UNKNOWN,
        LEGACY_COMPATIBILITY_WIRE,
        FAILED_VERIFY_EMPTY_SELECTION,
        POLICY_ALLOW,
        POLICY_DEFER,
        POLICY_BLOCK
    }

    public PullAssessment {
        Objects.requireNonNull(route, "route");
        if (route == PullRoute.LEGACY_UNOWNED) {
            throw new IllegalArgumentException("LEGACY_UNOWNED has no typed PullAssessment");
        }
        Objects.requireNonNull(verdict, "verdict");
        evidence = List.copyOf(evidence);
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("typed assessment evidence must not be empty");
        }
    }

    public static PullAssessment allow(PullRoute route, Evidence... evidence) {
        return new PullAssessment(route, Verdict.ALLOW, Arrays.asList(evidence));
    }

    public static PullAssessment defer(PullRoute route, Evidence... evidence) {
        return new PullAssessment(route, Verdict.DEFER, Arrays.asList(evidence));
    }

    public static PullAssessment block(PullRoute route, Evidence... evidence) {
        return new PullAssessment(route, Verdict.BLOCK, Arrays.asList(evidence));
    }

    /** Current compatibility assessment. It adds no replacement scoring policy. */
    public static PullAssessment compatibility(PullFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.route() == PullRoute.PULL_FAILED_VERIFY) {
            return allow(facts.route(),
                    Evidence.ACCEPTED_PARENT_TRANSACTION_ID,
                    Evidence.EXACT_PERMANENT_AND_CURRENT_IDENTITY,
                    Evidence.SOURCE_FILTER_UNKNOWN,
                    Evidence.FAILED_VERIFY_EMPTY_SELECTION);
        }
        if (facts.route() == PullRoute.PULL_PARENT) {
            return defer(facts.route(),
                    Evidence.EXACT_PERMANENT_AND_CURRENT_IDENTITY,
                    Evidence.SOURCE_FILTER_UNKNOWN,
                    Evidence.LEGACY_COMPATIBILITY_WIRE,
                    Evidence.POLICY_DEFER);
        }
        return defer(facts.route(),
                Evidence.ACCEPTED_PARENT_TRANSACTION_ID,
                Evidence.EXACT_PERMANENT_AND_CURRENT_IDENTITY,
                Evidence.SOURCE_FILTER_UNKNOWN,
                Evidence.LEGACY_COMPATIBILITY_WIRE,
                Evidence.POLICY_DEFER);
    }
}
