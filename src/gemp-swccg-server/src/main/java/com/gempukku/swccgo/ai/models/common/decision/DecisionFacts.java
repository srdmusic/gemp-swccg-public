package com.gempukku.swccgo.ai.models.common.decision;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;

import java.util.Objects;
import java.util.Set;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FACTS-MODEL / DECISION FACTS (2026-07-13) ═══
// Batch-2 typed-facts foundation, increment 1 (no production consumer yet).
// Contract: Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md §"Minimal shared model".
//
// What the engine offered for ONE decision plus what can be OBSERVED from game
// state. Records what is — never whether an action is good: no scores, ranks,
// vetoes, policy verdicts, and no Map<String,Object> escape hatch (the very
// thing this model replaces in DecisionContext.extra).
//
// Field groups per the contract's own bullets:
//  1. decision id/type/text, phase/window, turn, current player, side, obligation flags
//  2. noPass, minimum, maximum, blocked responses
//  3. force / life-force / hand / pile / objective identity / objective flip
//     observations — FactValue-typed, so "when known" is explicit and UNKNOWN
//     keeps producer + provenance + reason
//  4. the selected route and the evidence used to select it
//
// Java 21 record: components are final, accessors generated, no mutators
// possible; compact constructor validates and defensively copies the sets, so
// even direct canonical construction cannot smuggle in a mutable collection.
// ═══════════════════════════════════════════════════════════
public record DecisionFacts(
        // ── group 1: decision identity + context ──
        String decisionId,
        String decisionType,
        String decisionText,
        Phase phase,                     // nullable: absent when the raw decision carries no phase
        String window,                   // nullable: absent when the raw decision carries no window
        int turn,
        String currentPlayer,
        Side side,
        // Raw obligation-flag names surfaced by the decision (e.g. required-trigger markers).
        // A typed enum lands when the increment-2 shadow builder enumerates the real flag set;
        // until then this is a closed set of names, NOT a key-value extension map.
        Set<String> obligationFlags,
        // ── group 2: response constraints ──
        boolean noPass,
        int minimum,
        int maximum,
        Set<String> blockedResponses,
        // ── group 3: observations when known (UNKNOWN preserves producer/provenance/reason) ──
        FactValue<Integer> forceAvailable,
        FactValue<Integer> lifeForce,
        FactValue<Integer> handSize,
        FactValue<Integer> reserveDeckSize,
        FactValue<String> objectiveIdentity,
        FactValue<Boolean> objectiveFlipped,
        // ── group 4: route selection (from decision type/phase/window/obligations/candidate
        //     shape ONLY — scores and assessments cannot influence route selection) ──
        String selectedRoute,
        String routeSelectionEvidence) {

    public DecisionFacts {
        Objects.requireNonNull(decisionId, "decisionId");
        Objects.requireNonNull(decisionType, "decisionType");
        Objects.requireNonNull(decisionText, "decisionText");
        Objects.requireNonNull(currentPlayer, "currentPlayer");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(obligationFlags, "obligationFlags");
        Objects.requireNonNull(blockedResponses, "blockedResponses");
        Objects.requireNonNull(forceAvailable, "forceAvailable");
        Objects.requireNonNull(lifeForce, "lifeForce");
        Objects.requireNonNull(handSize, "handSize");
        Objects.requireNonNull(reserveDeckSize, "reserveDeckSize");
        Objects.requireNonNull(objectiveIdentity, "objectiveIdentity");
        Objects.requireNonNull(objectiveFlipped, "objectiveFlipped");
        Objects.requireNonNull(selectedRoute, "selectedRoute");
        Objects.requireNonNull(routeSelectionEvidence, "routeSelectionEvidence");
        obligationFlags = Set.copyOf(obligationFlags);
        blockedResponses = Set.copyOf(blockedResponses);
    }

    /** Builder for readable construction; the compact constructor still validates everything. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String decisionId;
        private String decisionType;
        private String decisionText;
        private Phase phase;
        private String window;
        private int turn;
        private String currentPlayer;
        private Side side;
        private Set<String> obligationFlags = Set.of();
        private boolean noPass;
        private int minimum;
        private int maximum;
        private Set<String> blockedResponses = Set.of();
        private FactValue<Integer> forceAvailable;
        private FactValue<Integer> lifeForce;
        private FactValue<Integer> handSize;
        private FactValue<Integer> reserveDeckSize;
        private FactValue<String> objectiveIdentity;
        private FactValue<Boolean> objectiveFlipped;
        private String selectedRoute;
        private String routeSelectionEvidence;

        private Builder() {}

        public Builder decisionId(String v) { this.decisionId = v; return this; }
        public Builder decisionType(String v) { this.decisionType = v; return this; }
        public Builder decisionText(String v) { this.decisionText = v; return this; }
        public Builder phase(Phase v) { this.phase = v; return this; }
        public Builder window(String v) { this.window = v; return this; }
        public Builder turn(int v) { this.turn = v; return this; }
        public Builder currentPlayer(String v) { this.currentPlayer = v; return this; }
        public Builder side(Side v) { this.side = v; return this; }
        public Builder obligationFlags(Set<String> v) { this.obligationFlags = v; return this; }
        public Builder noPass(boolean v) { this.noPass = v; return this; }
        public Builder minimum(int v) { this.minimum = v; return this; }
        public Builder maximum(int v) { this.maximum = v; return this; }
        public Builder blockedResponses(Set<String> v) { this.blockedResponses = v; return this; }
        public Builder forceAvailable(FactValue<Integer> v) { this.forceAvailable = v; return this; }
        public Builder lifeForce(FactValue<Integer> v) { this.lifeForce = v; return this; }
        public Builder handSize(FactValue<Integer> v) { this.handSize = v; return this; }
        public Builder reserveDeckSize(FactValue<Integer> v) { this.reserveDeckSize = v; return this; }
        public Builder objectiveIdentity(FactValue<String> v) { this.objectiveIdentity = v; return this; }
        public Builder objectiveFlipped(FactValue<Boolean> v) { this.objectiveFlipped = v; return this; }
        public Builder selectedRoute(String v) { this.selectedRoute = v; return this; }
        public Builder routeSelectionEvidence(String v) { this.routeSelectionEvidence = v; return this; }

        public DecisionFacts build() {
            return new DecisionFacts(decisionId, decisionType, decisionText, phase, window, turn,
                    currentPlayer, side, obligationFlags, noPass, minimum, maximum, blockedResponses,
                    forceAvailable, lifeForce, handSize, reserveDeckSize, objectiveIdentity,
                    objectiveFlipped, selectedRoute, routeSelectionEvidence);
        }
    }
}
