package com.gempukku.swccgo.ai.models.common.trace;

import java.util.Objects;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Finalization record" item 5): one DecisionSafety correction with typed reason and the
 * exact before/after response. Recorded at the existing DecisionSafety.ensureValidResponse
 * branches (observation only — the correction itself is unchanged legacy behavior).
 */
public record TraceCorrection(Kind kind, String beforeResponse, String afterResponse, String detail) {

    /** Typed correction reason, one constant per live DecisionSafety correction branch. */
    public enum Kind {
        /** Empty response but mustChoose: random valid option forced. */
        SAFETY_FORCED_CHOICE,
        /** Must choose but zero options existed: last-resort "0". */
        SAFETY_CRITICAL_NO_OPTIONS,
        /** SAFETY LAYER 2b: non-selectable/unknown card ids clamped to selectable ids. */
        SELECTABLE_CLAMP
    }

    public TraceCorrection {
        Objects.requireNonNull(kind, "kind");
    }
}
