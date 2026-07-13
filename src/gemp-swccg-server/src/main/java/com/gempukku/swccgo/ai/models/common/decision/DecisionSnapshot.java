package com.gempukku.swccgo.ai.models.common.decision;

import java.util.List;
import java.util.Objects;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FACTS-MODEL / DECISION SNAPSHOT (2026-07-13) ═══
// Batch-2 typed-facts foundation, increment 1 (no production consumer yet).
// Contract: Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md §"Minimal shared model".
//
// One immutable, ordered snapshot per decision:
//  - one DecisionFacts,
//  - one ActionFacts entry per ORIGINAL candidate ordinal (enforced: entry i
//    must carry ordinal i — candidate order is never sorted or rebuilt),
//  - immutable observational service facts (e.g. force obligations),
//  - a snapshot version used by fixture traces.
//
// The snapshot records what the engine offered and what can be observed; it
// never decides whether an action is good. No mutators, no untyped extension
// map, no mutable game objects in the public contract. Being a shared-common
// type (like strategy/FormationSafety), Rando and ChosenOne consume the SAME
// class in later increments, so the model cannot mirror-drift.
// ═══════════════════════════════════════════════════════════
public record DecisionSnapshot(
        DecisionFacts decisionFacts,
        List<ActionFacts> actionFacts,
        ServiceFacts serviceFacts,
        int snapshotVersion) {

    /** Version stamped on snapshots built by this increment of the model.
     *  Version 1 was the e4e0aa213 scaffold shape (string-typed, never
     *  serialized to any fixture); version 2 is the post-gate typed shape
     *  (Handoffs/CODEX_B2_INCREMENT1_GATE_E4E0AA213_2026-07-13.md deltas). */
    public static final int CURRENT_VERSION = 2;

    public DecisionSnapshot {
        Objects.requireNonNull(decisionFacts, "decisionFacts");
        Objects.requireNonNull(actionFacts, "actionFacts");
        Objects.requireNonNull(serviceFacts, "serviceFacts");
        if (snapshotVersion < 1) {
            // Gate item 6: positive snapshot versions only. Zero/negative would
            // let a fixture trace silently claim an unversioned shape.
            throw new IllegalArgumentException("snapshotVersion must be >= 1, was " + snapshotVersion);
        }
        actionFacts = List.copyOf(actionFacts);
        for (int i = 0; i < actionFacts.size(); i++) {
            if (actionFacts.get(i).ordinal() != i) {
                throw new IllegalArgumentException("actionFacts must be ordered by original ordinal: entry at index "
                        + i + " carries ordinal " + actionFacts.get(i).ordinal()
                        + " — candidate order is never sorted or rebuilt");
            }
        }
    }

    // ═══ Immutable observational service facts ═══
    // Truly observational shared-service outputs only (contract's example:
    // force obligations). Grows one TYPED field per migrated service — never
    // a key-value map. No policy: an obligation count is an observation;
    // whether it blocks an action belongs to a domain assessment.
    public record ServiceFacts(
            /* Σ maintenance/obligation Force the player must keep available
               (observational mirror of ForceReserveService.Facts.maintenanceObligation). */
            FactValue<Integer> forceObligations) {

        public ServiceFacts {
            Objects.requireNonNull(forceObligations, "forceObligations");
            // Gate item 6: negative counts rejected.
            if (forceObligations.isKnown() && forceObligations.value() < 0) {
                throw new IllegalArgumentException("forceObligations must be >= 0 when known, was "
                        + forceObligations.value());
            }
        }
    }
}
