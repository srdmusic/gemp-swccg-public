package com.gempukku.swccgo.ai.models.common.trace;

import java.util.List;
import java.util.Objects;

/**
 * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
 * "Finalization record"): CombinedEvaluator's selected action is the PRE-SAFETY winner,
 * not the final response — the two are recorded separately, plus everything between them.
 *
 * TRACE-V2 GATE P0-3 (Handoffs/CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md "COMPLETE is
 * not route-complete"): "not reached in this run" is no longer silently null. Routes that
 * legitimately skip a finalization fact mark it EXPLICITLY not-applicable (the
 * *NotApplicableReason fields); the collector's route-completeness matrix in finish()
 * refuses COMPLETE when a route-required fact is neither recorded nor marked. A recorded
 * value and a not-applicable marker are mutually exclusive by construction.
 * preSafetyWinnerRecorded distinguishes "evaluator lane ran and produced null" from
 * "never reached" the same way finalResponseRecorded distinguishes a recorded
 * empty-string pass from a never-recorded response.
 */
public record TraceFinalization(
        // 1. pre-safety candidate/winner and score bits (CombinedEvaluator output)
        String preSafetyWinnerActionId,
        Integer preSafetyWinnerScoreBits,
        boolean preSafetyWinnerVetoed,
        String preSafetyWinnerVetoReason,
        boolean preSafetyWinnerRecorded,
        String preSafetyWinnerNotApplicableReason,
        // 2. semantic pass/cancel eligibility and the exact facts used (V148 semantics)
        Boolean passEligible,
        String passEligibilityFacts,
        String passEligibilityNotApplicableReason,
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
        boolean skippedCommonFinalizer,
        // ── FINALIZER RUNTIME (2026-07-13,
        //    Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md §7): the
        //    closed engine-disposition lifecycle evidence, recorded in the disposition
        //    callback AFTER the engine reports acceptance/rejection/failure. Absent
        //    (disposition == null) for a DIRECT decide() call, which keeps the legacy
        //    finalResponse meaning. ──
        // 8. the exact wire SUBMITTED to the engine; recorded from the outer result inside
        //    the disposition callback so a Curator override records the actual submitted wire.
        //    The recorded flag lets Pass "" differ from "no submitted wire".
        String proposedWireResponse,
        boolean proposedWireRecorded,
        // 9. the one closed engine disposition; nonblank detail required for every
        //    non-accepted disposition.
        Disposition disposition,
        String dispositionDetail,
        // 10. accepted-mutation mode + whether that mutation actually completed (ENGINE_ACCEPTED
        //     only). finalResponse is populated ONLY on ENGINE_ACCEPTED (or a direct call).
        MutationMode acceptedMutationMode,
        boolean acceptedMutationCompleted) {

    /** The one closed engine disposition of a mediator-facing attempt. */
    public enum Disposition {
        ENGINE_ACCEPTED,
        ENGINE_REJECTED,
        TYPED_REJECTION,
        ATTEMPT_FAILED
    }

    /** Post-acceptance outer-mutation ownership recorded on an ENGINE_ACCEPTED disposition. */
    public enum MutationMode {
        NONE,
        OUTER_COMMON
    }

    public TraceFinalization {
        Objects.requireNonNull(corrections, "corrections");
        corrections = List.copyOf(corrections);
        if (!finalResponseRecorded && finalResponse != null) {
            throw new IllegalArgumentException("finalResponse present but finalResponseRecorded=false");
        }
        if (!proposedWireRecorded && proposedWireResponse != null) {
            throw new IllegalArgumentException(
                "proposedWireResponse present but proposedWireRecorded=false");
        }
        if (disposition == null) {
            // Direct decide() / legacy call: no engine disposition lifecycle.
            if (dispositionDetail != null || acceptedMutationMode != null || acceptedMutationCompleted) {
                throw new IllegalArgumentException(
                    "disposition lifecycle fields present without a recorded disposition");
            }
        } else {
            requireNonBlankWhenPresent(dispositionDetail, "dispositionDetail");
            if (disposition == Disposition.ENGINE_ACCEPTED) {
                Objects.requireNonNull(acceptedMutationMode,
                    "ENGINE_ACCEPTED requires an accepted-mutation mode");
            } else {
                if (acceptedMutationMode != null || acceptedMutationCompleted) {
                    throw new IllegalArgumentException(
                        "only ENGINE_ACCEPTED carries an accepted-mutation mode/outcome");
                }
            }
        }
        // P0-3: winner value fields require the recorded flag (a null actionId can still
        // be RECORDED — the evaluator lane ran and produced no winner).
        if (!preSafetyWinnerRecorded
                && (preSafetyWinnerActionId != null || preSafetyWinnerScoreBits != null
                    || preSafetyWinnerVetoed || preSafetyWinnerVetoReason != null)) {
            throw new IllegalArgumentException(
                "pre-safety winner fields present but preSafetyWinnerRecorded=false");
        }
        if (preSafetyWinnerRecorded && preSafetyWinnerNotApplicableReason != null) {
            throw new IllegalArgumentException(
                "pre-safety winner both recorded and marked not-applicable");
        }
        if (passEligible != null && passEligibilityNotApplicableReason != null) {
            throw new IllegalArgumentException(
                "pass eligibility both recorded and marked not-applicable");
        }
        if (passEligible == null && passEligibilityFacts != null) {
            throw new IllegalArgumentException(
                "pass eligibility facts present without a recorded eligibility value");
        }
        requireNonBlankWhenPresent(preSafetyWinnerNotApplicableReason, "preSafetyWinnerNotApplicableReason");
        requireNonBlankWhenPresent(passEligibilityNotApplicableReason, "passEligibilityNotApplicableReason");
    }

    private static void requireNonBlankWhenPresent(String s, String name) {
        if (s != null && s.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank when present");
        }
    }
}
