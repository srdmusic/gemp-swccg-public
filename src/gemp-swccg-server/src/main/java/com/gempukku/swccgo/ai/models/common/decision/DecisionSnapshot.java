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

    /** Version stamped on snapshots built by this increment of the model. */
    public static final int CURRENT_VERSION = 1;

    public DecisionSnapshot {
        Objects.requireNonNull(decisionFacts, "decisionFacts");
        Objects.requireNonNull(actionFacts, "actionFacts");
        Objects.requireNonNull(serviceFacts, "serviceFacts");
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
        }
    }
}
