package com.gempukku.swccgo.ai.models.common.trace.state;

/**
 * TRACE STAGE 4A1 (Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md
 * "Decision tracker owners"): the outer Rando tracker, the outer ChosenOne tracker, and
 * the inherited heuristic tracker remain DISTINCT scopes until a separate behavior
 * change deliberately consolidates them. An event carries the owner whose state it
 * observed; owners are never merged in the trace.
 *
 * 4A1 records the two OUTER owners only (the bot-boundary RECORD_RESPONSE hook, full
 * payload per the accepted m00372 Option A contract). HEURISTIC_SHARED landed with the
 * 4A2b shared-tracker increment: RECORD_RESPONSE and UPDATE_STATE accept it,
 * PHASE_CHANGE and BLOCK_RESPONSE require it, and TrackerClearEvent still rejects it
 * (no shared clear call exists in source).
 */
public enum TrackerOwner {
    OUTER_RANDO,
    OUTER_CHOSENONE,
    HEURISTIC_SHARED
}
