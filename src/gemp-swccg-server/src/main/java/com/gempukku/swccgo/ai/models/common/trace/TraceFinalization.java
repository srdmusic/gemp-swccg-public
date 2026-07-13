package com.gempukku.swccgo.ai.models.common.trace;

import java.util.List;
import java.util.Objects;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Finalization record"): CombinedEvaluator's selected action is the PRE-SAFETY winner,
 * not the final response — the two are recorded separately, plus everything between them.
 *
 * Nullable fields mean "this boundary was not reached in this run" (e.g. a session opened
 * through the pure CombinedEvaluator test seam never reaches the bot's final-response
 * boundary; finalResponseRecorded distinguishes a recorded empty-string pass from a
 * never-recorded response). A bot-boundary session that closes without a recorded final
 * response is marked INCOMPLETE by the collector.
 */
public record TraceFinalization(
        // 1. pre-safety candidate/winner and score bits (CombinedEvaluator output)
        String preSafetyWinnerActionId,
        Integer preSafetyWinnerScoreBits,
        boolean preSafetyWinnerVetoed,
        String preSafetyWinnerVetoReason,
        // 2. semantic pass/cancel eligibility and the exact facts used (V148 semantics)
        Boolean passEligible,
        String passEligibilityFacts,
        // 3. multi-select formatting result (comma-joined card ids), when applied
        String multiSelectResponse,
        // 4. raw-noPass emergency action, if any
        String emergencyResponse,
        String emergencyReason,
        // 5. every DecisionSafety correction, ordered, with typed reason and before/after
        List<TraceCorrection> corrections,
        // 6. final response returned by the bot after safety
        String finalResponse,
        boolean finalResponseRecorded,
        // 7. whether the route skipped the common finalizer under legacy behavior
        //    (true for the five direct interceptors, which return before outer
        //    emergency validation and decision recording)
        boolean skippedCommonFinalizer) {

    public TraceFinalization {
        Objects.requireNonNull(corrections, "corrections");
        corrections = List.copyOf(corrections);
        if (!finalResponseRecorded && finalResponse != null) {
            throw new IllegalArgumentException("finalResponse present but finalResponseRecorded=false");
        }
    }
}
