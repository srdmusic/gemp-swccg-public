package com.gempukku.swccgo.ai.models.common.phase;

/** Closed ownership result for one stage of a standard card-pile pull. */
public enum PullRoute {
    LEGACY_UNOWNED,
    PULL_PARENT,
    PULL_DEPLOY_CHILD,
    PULL_TAKE_CHILD,
    PULL_DESTINATION,
    PULL_FAILED_VERIFY
}
