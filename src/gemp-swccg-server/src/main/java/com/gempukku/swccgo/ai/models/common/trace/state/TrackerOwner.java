package com.gempukku.swccgo.ai.models.common.trace.state;

/**
 * TRACE STAGE 4A1 (Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md
 * "Decision tracker owners"): the outer Rando tracker, the outer ChosenOne tracker, and
 * the inherited heuristic tracker remain DISTINCT scopes until a separate behavior
 * change deliberately consolidates them. An event carries the owner whose state it
 * observed; owners are never merged in the trace.
 *
 * 4A1 records the two OUTER owners only (the bot-boundary RECORD_RESPONSE hook, full
 * payload per the accepted m00372 Option A contract). HEURISTIC_SHARED is declared here
 * because the owner set is closed by the matrix, but its events land with the
 * tracker-owner increment (4A2); TrackerRecordResponseEvent rejects it.
 */
public enum TrackerOwner {
    OUTER_RANDO,
    OUTER_CHOSENONE,
    HEURISTIC_SHARED
}
