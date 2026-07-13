package com.gempukku.swccgo.ai.models.common.trace;

import java.util.Objects;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Error and lifecycle rules"): one typed, ordered capture failure attached to an
 * INCOMPLETE trace. Stage and error class are mandatory so a truncated record explains
 * exactly where evidence was lost instead of comparing as plausible.
 */
public record TraceCaptureFailure(Stage stage, String errorClass, String detail) {

    /** Where in the capture lifecycle the failure occurred. */
    public enum Stage {
        OPEN,
        SNAPSHOT,
        OPERATION,
        ROUTE,
        FINALIZATION,
        EVALUATOR,
        STATE_EVENT,
        CLOSE,
        SINK
    }

    public TraceCaptureFailure {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(errorClass, "errorClass");
        if (errorClass.isBlank()) {
            throw new IllegalArgumentException("errorClass must be nonblank");
        }
    }
}
