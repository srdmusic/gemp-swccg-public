package com.gempukku.swccgo.ai.models.common.policy;

/** How one migrated rule arm changes an evaluated action. */
public enum PolicyOperationKind {
    ADD,
    HARD_VETO,
    DEFER
}
