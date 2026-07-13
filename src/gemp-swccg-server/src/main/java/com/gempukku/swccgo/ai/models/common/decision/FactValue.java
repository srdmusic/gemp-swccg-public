package com.gempukku.swccgo.ai.models.common.decision;

import java.util.Objects;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FACTS-MODEL / TYPED FACT VALUE (2026-07-13) ═══
// Batch-2 typed-facts foundation, increment 1 (no production consumer yet).
// Contract: Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md
// ("Minimal shared model" + "Unknown handling").
//
// One observed value with explicit KNOWN/UNKNOWN state. UNKNOWN is DATA, not
// a fail-open default: it always carries WHO tried to produce the value
// (producerId), WHERE it would have come from (provenance), and WHY it is
// absent (unknownReason). Known Boolean false and known numeric zero are
// first-class values, fully distinct from UNKNOWN — this deliberately fixes
// the ForceReserveService.Facts empty-sentinel pattern that mapped missing
// input to false/zero.
//
// value() THROWS on an UNKNOWN fact instead of returning null/default, so a
// consumer that forgets to check isKnown() fails loudly rather than silently
// deciding from a fabricated value. No project-wide fail-open: each rule arm
// declares its own unknown policy downstream (DEFER / CONSERVATIVE_BLOCK /
// CONSERVATIVE_ALLOW / TEST_ERROR) — none of that policy lives here.
// Optional<T> is permitted internally by the contract but is NOT the public
// surface; this class needs no Optional at all.
// ═══════════════════════════════════════════════════════════
public final class FactValue<T> {

    /** Observation state. There is no third state and no null-means-unknown. */
    public enum State { KNOWN, UNKNOWN }

    private final State state;
    private final T value;              // non-null iff state == KNOWN
    private final String producerId;    // never null
    private final String provenance;    // never null: source evidence / stable provenance key
    private final String unknownReason; // non-null iff state == UNKNOWN

    private FactValue(State state, T value, String producerId, String provenance, String unknownReason) {
        this.state = state;
        this.value = value;
        this.producerId = Objects.requireNonNull(producerId, "producerId");
        this.provenance = Objects.requireNonNull(provenance, "provenance");
        this.unknownReason = unknownReason;
    }

    /** A known observation. Known false and known zero are valid values here. */
    public static <T> FactValue<T> known(T value, String producerId, String provenance) {
        Objects.requireNonNull(value, "known value must be non-null; use unknown(...) with a reason instead");
        return new FactValue<>(State.KNOWN, value, producerId, provenance, null);
    }

    /** An unknown observation. The reason is mandatory: unknown is data, not absence. */
    public static <T> FactValue<T> unknown(String producerId, String provenance, String unknownReason) {
        Objects.requireNonNull(unknownReason, "unknownReason");
        return new FactValue<>(State.UNKNOWN, null, producerId, provenance, unknownReason);
    }

    public State state() {
        return state;
    }

    public boolean isKnown() {
        return state == State.KNOWN;
    }

    public boolean isUnknown() {
        return state == State.UNKNOWN;
    }

    /**
     * The known value. Throws on UNKNOWN so no consumer can silently fail-open;
     * check isKnown() first, then apply the arm's declared unknown policy.
     */
    public T value() {
        if (state != State.KNOWN) {
            throw new IllegalStateException("Fact is UNKNOWN (producer=" + producerId
                    + ", provenance=" + provenance + ", reason=" + unknownReason
                    + ") — caller must check isKnown() and apply its declared unknown policy");
        }
        return value;
    }

    public String producerId() {
        return producerId;
    }

    public String provenance() {
        return provenance;
    }

    /** Why the value is absent. Only meaningful on UNKNOWN; throws on KNOWN. */
    public String unknownReason() {
        if (state != State.UNKNOWN) {
            throw new IllegalStateException("Fact is KNOWN (producer=" + producerId
                    + ", provenance=" + provenance + ") — it has no unknownReason");
        }
        return unknownReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FactValue)) return false;
        FactValue<?> f = (FactValue<?>) o;
        return state == f.state
                && Objects.equals(value, f.value)
                && producerId.equals(f.producerId)
                && provenance.equals(f.provenance)
                && Objects.equals(unknownReason, f.unknownReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, value, producerId, provenance, unknownReason);
    }

    @Override
    public String toString() {
        return state == State.KNOWN
                ? "FactValue[KNOWN " + value + " producer=" + producerId + " provenance=" + provenance + "]"
                : "FactValue[UNKNOWN reason=" + unknownReason + " producer=" + producerId + " provenance=" + provenance + "]";
    }
}
