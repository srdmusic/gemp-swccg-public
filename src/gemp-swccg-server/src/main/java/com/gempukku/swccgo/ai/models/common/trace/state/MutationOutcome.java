package com.gempukku.swccgo.ai.models.common.trace.state;

/**
 * TRACE STAGE 4A1 (Handoffs/CODEX_TRACE_STAGE4_EVENT_SCHEMA_2026-07-13.md "Common Laws",
 * carried into the 4A0 matrix): owner-specific before/after snapshots define the outcome
 * of one completed legacy state mutation.
 *
 * CHANGED requires different typed before/after values where both exist. NO_OP requires
 * equality and means the owner method actually ran; event ABSENCE means it did not run.
 * Capture failure stays TraceCaptureFailure.Stage.STATE_EVENT, never a fake failed
 * mutation.
 *
 * COUNCIL CONSTRAINT (matrix "Incorporated review decisions"): engine-call outcome is a
 * DISTINCT closed type, EngineCallOutcome; it is never a value here, because call
 * success does not prove that engine state changed.
 */
public enum MutationOutcome {
    CHANGED,
    NO_OP
}
