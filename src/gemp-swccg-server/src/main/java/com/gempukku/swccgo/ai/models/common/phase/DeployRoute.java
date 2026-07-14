package com.gempukku.swccgo.ai.models.common.phase;

/** Closed decision routes owned by the DEPLOY transaction. */
public enum DeployRoute {
    LEGACY_UNOWNED,
    DEPLOY_PARENT,
    DEPLOY_DESTINATION,
    DEPLOY_BUDDY,
    DEPLOY_V170_UNDERCOVER,
    DEPLOY_CAPACITY,
    DEPLOY_CONFIRMATION
}
