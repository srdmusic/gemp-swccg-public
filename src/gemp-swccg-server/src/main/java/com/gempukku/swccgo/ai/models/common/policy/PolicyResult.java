package com.gempukku.swccgo.ai.models.common.policy;

import java.util.List;
import java.util.Objects;

/** Immutable ordered operations emitted by one policy producer. */
public record PolicyResult(String producerId, List<PolicyOperation> operations) {

    public PolicyResult {
        Objects.requireNonNull(producerId, "producerId");
        Objects.requireNonNull(operations, "operations");
        if (producerId.isBlank()) {
            throw new IllegalArgumentException("producerId must be nonblank");
        }
        operations = List.copyOf(operations);
    }

}
