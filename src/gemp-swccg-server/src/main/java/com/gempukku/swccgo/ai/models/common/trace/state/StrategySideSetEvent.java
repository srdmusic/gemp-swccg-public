package com.gempukku.swccgo.ai.models.common.trace.state;

import com.gempukku.swccgo.common.Side;

import java.util.Objects;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", SIDE_SET): the one setSide(Side) call in each bot's
 * new-game branch, observed AFTER the legacy call ran. setSide unconditionally assigns
 * mySide, so a same-value assignment executes and is a real NO_OP.
 *
 * The side argument is intentionally NULLABLE because the legacy call accepts null; it is
 * the exact call argument, never inferred from the after snapshot. The operation-specific
 * invariant is stronger than snapshot inequality: after must equal before with ONLY the
 * nullable side replaced by the exact argument.
 */
public record StrategySideSetEvent(
    StrategyControllerOwner owner,
    Side side,
    StrategyControllerSnapshot before,
    StrategyControllerSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public StrategySideSetEvent {
        Objects.requireNonNull(owner, "owner");
        // side is intentionally nullable: setSide accepts a null argument
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        if (!after.equals(StrategySnapshotProjections.withSide(before, side))) {
            throw new IllegalArgumentException(
                "SIDE_SET after must equal before with only the side replaced by the exact"
                    + " argument " + side + "; other fields may not differ");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static StrategySideSetEvent of(StrategyControllerOwner owner, Side side,
                                          StrategyControllerSnapshot before,
                                          StrategyControllerSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new StrategySideSetEvent(owner, side, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
