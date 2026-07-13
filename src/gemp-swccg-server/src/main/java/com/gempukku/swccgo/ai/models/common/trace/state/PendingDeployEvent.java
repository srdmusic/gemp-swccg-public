package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Objects;

/**
 * TRACE STAGE 4A1 (Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md
 * "Outer bot owner", PENDING_DEPLOY_SET/PENDING_DEPLOY_CLEAR rows): one completed direct
 * write of the outer bot's retained lastPendingDeployType field, carrying the exact
 * legacy value before/after.
 *
 * SET is emitted only at an actual deploy-branch direct write (no branch write, no
 * event); a same-value rewrite is a real NO_OP SET. CLEAR is the next-turn direct clear
 * (CLEAR requires an absent after value). This event replaces the retired wrapper-level
 * strategic-intent event: the wrapper's strategyController calls are recorded at the
 * StrategyController owner (its own later increment), never here.
 */
public record PendingDeployEvent(
    Operation operation,
    String typeBefore,
    String typeAfter,
    MutationOutcome outcome) implements TraceStateEvent {

    /** The two reachable owner operations on lastPendingDeployType. */
    public enum Operation {
        SET,
        CLEAR
    }

    public PendingDeployEvent {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(outcome, "outcome");
        if (operation == Operation.SET && typeAfter == null) {
            throw new IllegalArgumentException("SET requires an after value");
        }
        if (operation == Operation.CLEAR && typeAfter != null) {
            throw new IllegalArgumentException("CLEAR requires an absent after value, was '" + typeAfter + "'");
        }
        boolean changed = !Objects.equals(typeBefore, typeAfter);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with before/after (changed=" + changed + ")");
        }
    }

    /** Factory used by the recording choke point: derives the outcome from before/after. */
    public static PendingDeployEvent of(Operation operation, String typeBefore, String typeAfter) {
        boolean changed = !Objects.equals(typeBefore, typeAfter);
        return new PendingDeployEvent(operation, typeBefore, typeAfter,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
