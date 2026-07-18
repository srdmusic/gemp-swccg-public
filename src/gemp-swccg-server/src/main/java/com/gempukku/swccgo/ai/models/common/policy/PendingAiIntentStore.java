package com.gempukku.swccgo.ai.models.common.policy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** One-slot lifecycle owner for AI-only parent-to-child intent. */
public final class PendingAiIntentStore {
    private PendingAiIntent pending;

    public void remember(PendingAiIntent intent) {
        pending = Objects.requireNonNull(intent, "intent");
    }

    public Optional<PendingAiIntent> current() {
        return Optional.ofNullable(pending);
    }

    public boolean clear(ClearReason reason) {
        Objects.requireNonNull(reason, "reason");
        boolean hadPending = pending != null;
        pending = null;
        return hadPending;
    }

    public Resolution resolve(PendingAiIntent.ChildDecision childDecision) {
        Objects.requireNonNull(childDecision, "childDecision");
        if (pending == null) {
            return Resolution.empty();
        }

        PendingAiIntent.ExpiryKey expected = pending.expiryKey();
        PendingAiIntent.ExpiryKey actual = childDecision.expiryKey();
        if (!expected.gameId().equals(actual.gameId())) {
            return fallback(ClearReason.GAME_RESET);
        }
        if (expected.turn() != actual.turn()) {
            return fallback(ClearReason.TURN_CHANGED);
        }
        if (expected.phase() != actual.phase()) {
            return fallback(ClearReason.PHASE_CHANGED);
        }
        if (pending.expectedChildShape() != childDecision.shape()) {
            return fallback(ClearReason.CHILD_SHAPE_MISMATCH);
        }

        List<PendingAiIntent.ChildCandidate> matches = childDecision.candidates().stream()
                .filter(pending::matches)
                .toList();
        if (matches.isEmpty()) {
            return fallback(ClearReason.MISSING_CANDIDATE);
        }
        if (matches.size() > 1) {
            return fallback(ClearReason.AMBIGUOUS_MATCH);
        }

        PendingAiIntent consumed = pending;
        PendingAiIntent.ChildCandidate match = matches.get(0);
        pending = null;
        return Resolution.matched(consumed, match);
    }

    private Resolution fallback(ClearReason reason) {
        pending = null;
        return Resolution.fallback(reason);
    }

    public enum ClearReason {
        COMPLETED,
        PASS_OR_NO,
        FAILED_SEARCH,
        GAME_RESET,
        TURN_CHANGED,
        PHASE_CHANGED,
        CHILD_SHAPE_MISMATCH,
        MISSING_CANDIDATE,
        AMBIGUOUS_MATCH
    }

    public enum ResolutionStatus {
        EMPTY,
        MATCHED,
        FALLBACK
    }

    public record Resolution(ResolutionStatus status, PendingAiIntent consumedIntent,
                             PendingAiIntent.ChildCandidate matchedCandidate,
                             ClearReason clearReason) {
        public Resolution {
            Objects.requireNonNull(status, "status");
            if (status == ResolutionStatus.EMPTY
                    && (consumedIntent != null || matchedCandidate != null || clearReason != null)) {
                throw new IllegalArgumentException("EMPTY resolution cannot carry match or clear data");
            }
            if (status == ResolutionStatus.MATCHED
                    && (consumedIntent == null || matchedCandidate == null || clearReason != null)) {
                throw new IllegalArgumentException("MATCHED resolution requires intent and candidate only");
            }
            if (status == ResolutionStatus.FALLBACK
                    && (consumedIntent != null || matchedCandidate != null || clearReason == null)) {
                throw new IllegalArgumentException("FALLBACK resolution requires a clear reason only");
            }
        }

        static Resolution empty() {
            return new Resolution(ResolutionStatus.EMPTY, null, null, null);
        }

        static Resolution matched(PendingAiIntent intent,
                                  PendingAiIntent.ChildCandidate candidate) {
            return new Resolution(ResolutionStatus.MATCHED, intent, candidate, null);
        }

        static Resolution fallback(ClearReason reason) {
            return new Resolution(ResolutionStatus.FALLBACK, null, null, reason);
        }
    }
}
