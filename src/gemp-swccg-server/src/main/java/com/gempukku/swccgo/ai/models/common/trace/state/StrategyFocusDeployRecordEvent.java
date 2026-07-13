package com.gempukku.swccgo.ai.models.common.trace.state;

import java.util.Locale;
import java.util.Objects;

/**
 * TRACE STAGE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
 * "Source-Complete Owner Table", FOCUS_DEPLOY_RECORD): the one onSuccessfulDeploy(String)
 * call in each bot's turn-changed branch, reached only when the outer lastPendingDeployType
 * is non-null, observed AFTER the legacy call ran. It mutates only when the focus is ground
 * or space and the card type matches. Current production never calls setFocus(...), so
 * normal production calls are presently NO_OP; fixtures may seed focus through the existing
 * public mutator.
 *
 * Payload: the exact non-null String cardType argument. The operation-specific invariant:
 * a balanced or nonmatching focus requires identical snapshots; a matching ground/space
 * card increments deployments once and raises confidence by exactly 0.2f, capped at 1.0f,
 * only from the second matching deployment onward; every other field is frozen (the focus
 * itself included).
 */
public record StrategyFocusDeployRecordEvent(
    StrategyControllerOwner owner,
    String cardType,
    StrategyControllerSnapshot before,
    StrategyControllerSnapshot after,
    MutationOutcome outcome) implements TraceStateEvent {

    public StrategyFocusDeployRecordEvent {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(cardType, "cardType");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(outcome, "outcome");
        boolean changed = !before.equals(after);
        if (changed != (outcome == MutationOutcome.CHANGED)) {
            throw new IllegalArgumentException("outcome " + outcome
                + " inconsistent with snapshot equality (changed=" + changed + ")");
        }
        // The deployed card type versus the before-focus, NOT a mere deployment-count delta,
        // decides whether this record may mutate. Mirrors StrategyController.cardMatchesFocus
        // (rando/chosenone .../strategy/StrategyController.java:458-472), evaluated on the focus
        // in effect at deploy time (before.focus(), the same currentFocus onSuccessfulDeploy uses).
        // Snapshot inequality alone never proves a matching deploy: an increment recorded for a
        // balanced or nonmatching card is an impossible transition and is rejected here.
        if (!cardMatchesFocus(before.focus(), cardType)) {
            if (!after.equals(before)) {
                throw new IllegalArgumentException(
                    "FOCUS_DEPLOY_RECORD on a balanced-focus or nonmatching-card deploy must leave"
                        + " the snapshot identical (focus=" + before.focus()
                        + ", cardType=" + cardType + ")");
            }
        } else {
            int depBefore = before.focusDeployments();
            int depAfter = after.focusDeployments();
            if (depAfter != depBefore + 1) {
                throw new IllegalArgumentException(
                    "FOCUS_DEPLOY_RECORD on a matching card must raise the deployment count by"
                        + " exactly one, got " + depBefore + " -> " + depAfter);
            }
            int expectedBits;
            if (depAfter >= 2) {
                float raised = Math.min(1.0f,
                    Float.intBitsToFloat(before.focusConfidenceBits()) + 0.2f);
                expectedBits = Float.floatToRawIntBits(raised);
            } else {
                expectedBits = before.focusConfidenceBits();
            }
            if (after.focusConfidenceBits() != expectedBits) {
                throw new IllegalArgumentException(
                    "FOCUS_DEPLOY_RECORD raises confidence by exactly 0.2f (capped at 1.0f) only"
                        + " from the second matching deployment onward");
            }
            if (!StrategySnapshotProjections.focusFrozen(before)
                    .equals(StrategySnapshotProjections.focusFrozen(after))) {
                throw new IllegalArgumentException(
                    "FOCUS_DEPLOY_RECORD may change only the deployment count and confidence;"
                        + " every other field including the focus is frozen");
            }
        }
    }

    /**
     * Mirrors {@code StrategyController.cardMatchesFocus}
     * (rando/chosenone .../strategy/StrategyController.java:458-472): a GROUND focus matches a
     * card type whose lowercased form contains {@code character}, {@code vehicle}, or
     * {@code site}; a SPACE focus matches {@code starship} or {@code system}; a balanced or any
     * other focus matches nothing. The snapshot serializes focus as the lowercase
     * {@code StrategyFocus.getValue()} string, so it is compared directly; the card type is
     * lowercased via {@link Locale#ROOT} exactly as the controller does.
     */
    private static boolean cardMatchesFocus(String focus, String cardType) {
        if (focus == null || cardType == null) {
            return false;
        }
        String typeLower = cardType.toLowerCase(Locale.ROOT);
        if ("ground".equals(focus)) {
            return typeLower.contains("character") || typeLower.contains("vehicle")
                || typeLower.contains("site");
        } else if ("space".equals(focus)) {
            return typeLower.contains("starship") || typeLower.contains("system");
        }
        return false;
    }

    /** Factory used by the recording choke point: derives the outcome from the snapshots. */
    public static StrategyFocusDeployRecordEvent of(StrategyControllerOwner owner, String cardType,
                                                    StrategyControllerSnapshot before,
                                                    StrategyControllerSnapshot after) {
        boolean changed = !Objects.requireNonNull(before, "before")
            .equals(Objects.requireNonNull(after, "after"));
        return new StrategyFocusDeployRecordEvent(owner, cardType, before, after,
            changed ? MutationOutcome.CHANGED : MutationOutcome.NO_OP);
    }
}
