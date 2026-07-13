package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", RESET): the one reset() call immediately after setSide
 * in each bot's new-game branch, observed AFTER the legacy call ran. reset() resets every
 * retained field except mySide and lastDecisionReason, so a fresh controller after side
 * assignment may be NO_OP.
 *
 * No call argument. The operation-specific invariant is the exact reset projection:
 * after must equal before with the preserved side and decision reason and every other
 * field at its reset constant.
 */
public record StrategyResetEvent(
    StrategyControllerOwner owner,
    StrategyControllerSnapshot before,
    StrategyControllerSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public StrategyResetEvent {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (!after.equals(StrategySnapshotProjections.resetProjection(before))) {
            throw new IllegalArgumentException(
                "RESET after must be the exact reset projection: preserved side and decision"
                    + " reason, every other field at its reset constant");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static StrategyResetEvent of(StrategyControllerOwner owner,
                                        StrategyControllerSnapshot before,
                                        StrategyControllerSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new StrategyResetEvent(owner, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
