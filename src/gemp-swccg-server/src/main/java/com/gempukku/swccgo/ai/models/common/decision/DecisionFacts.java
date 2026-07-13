package com.gempukku.swccgo.ai.models.common.decision;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: FACTS-MODEL / DECISION FACTS (2026-07-13) ═══
// Batch-2 typed-facts foundation, increment 1 (no production consumer yet).
// Contract: Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md §"Minimal shared model".
// Gate deltas applied: Handoffs/CODEX_B2_INCREMENT1_GATE_E4E0AA213_2026-07-13.md items 1-4, 6.
//
// What the engine offered for ONE decision plus what can be OBSERVED from game
// state. Records what is — never whether an action is good: no scores, ranks,
// vetoes, policy verdicts, and no Map<String,Object> escape hatch (the very
// thing this model replaces in DecisionContext.extra).
//
// Field groups per the contract's own bullets:
//  1. decision id/type/text, phase/window, turn, current player, side,
//     obligation flags. decisionType is the engine's real AwaitingDecisionType
//     enum (gate item 1), never a free string. Obligation flags are a typed
//     enum set, UNKNOWN-capable so "params absent" is distinct from "no
//     obligations" (gate items 1 + 3).
//  2. noPass, minimum, maximum, blocked responses. noPass/minimum/maximum are
//     FactValue-wrapped: the raw decision may omit them, and a bare
//     boolean/int would fabricate a default the engine never sent (gate item 3).
//  3. force / life-force / hand / pile / objective identity / objective flip
//     observations, named for the EXACT engine measurement (gate item 4) and
//     FactValue-typed so UNKNOWN keeps producer + provenance + reason.
//  4. the selected route (typed enum) and a STRUCTURED RouteSelectionEvidence
//     record proving route selection used only decision type, phase/window,
//     obligations, and candidate shape (gate items 1 + 2). Human-readable
//     trace text is DERIVED from the record via describe(), never stored.
//
// Java 21 record: components are final, accessors generated, no mutators
// possible; compact constructor validates (nonblank ids, non-negative counts,
// well-formed response ranges, evidence consistency) and defensively copies
// collections, so even direct canonical construction cannot smuggle in a
// mutable collection or malformed metadata.
// ═══════════════════════════════════════════════════════════
public record DecisionFacts(
        // ── group 1: decision identity + context ──
        String decisionId,               // nonblank (raw form of the engine's int decision id)
        AwaitingDecisionType decisionType,
        String decisionText,
        Phase phase,                     // nullable: absent when the raw decision carries no phase
        String window,                   // nullable: absent when the raw decision carries no window; nonblank when present
        int turn,                        // >= 0 (0 permitted for pre-game setup decisions)
        String currentPlayer,            // nonblank
        Side side,
        // Typed obligation flags, UNKNOWN-capable: an absent decision-parameter block is
        // UNKNOWN (with reason), which is NOT the same fact as a known-empty obligation set.
        FactValue<Set<ObligationFlag>> obligationFlags,
        // ── group 2: response constraints (FactValue-wrapped: never fabricate defaults) ──
        FactValue<Boolean> noPass,
        FactValue<Integer> minimum,      // known value must be >= 0
        FactValue<Integer> maximum,      // known value must be >= 0 and >= known minimum
        Set<String> blockedResponses,    // AI-internal loop-prevention ids; elements nonblank
        // ── group 3: observations when known (UNKNOWN preserves producer/provenance/reason) ──
        /** Card count of the player's Force Pile (engine zone Zone.FORCE_PILE). */
        FactValue<Integer> forcePileSize,
        /** Exact mirror of GameState.getPlayerLifeForce(playerId): reserve deck
         *  + force pile + used pile + unresolved destiny draws + sabacc hand
         *  card counts; 0 when the engine marked life force depleted. Hand is
         *  NOT included. */
        FactValue<Integer> lifeForceCardCount,
        /** Card count of the player's hand (engine zone Zone.HAND). */
        FactValue<Integer> handSize,
        /** Exact mirror of GameState.getReserveDeckSize(playerId). */
        FactValue<Integer> reserveDeckSize,
        FactValue<String> objectiveIdentity,   // known value = objective blueprint id, nonblank
        FactValue<Boolean> objectiveFlipped,
        // ── group 4: route selection (typed; evidence proves the inputs used) ──
        DecisionRoute selectedRoute,
        RouteSelectionEvidence routeSelectionEvidence) {

    public DecisionFacts {
        requireNonBlank(decisionId, "decisionId");
        Objects.requireNonNull(decisionType, "decisionType");
        Objects.requireNonNull(decisionText, "decisionText");
        if (window != null && window.isBlank()) {
            throw new IllegalArgumentException("window must be nonblank when present");
        }
        if (turn < 0) {
            throw new IllegalArgumentException("turn must be >= 0, was " + turn);
        }
        requireNonBlank(currentPlayer, "currentPlayer");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(obligationFlags, "obligationFlags");
        Objects.requireNonNull(noPass, "noPass");
        Objects.requireNonNull(minimum, "minimum");
        Objects.requireNonNull(maximum, "maximum");
        Objects.requireNonNull(blockedResponses, "blockedResponses");
        Objects.requireNonNull(forcePileSize, "forcePileSize");
        Objects.requireNonNull(lifeForceCardCount, "lifeForceCardCount");
        Objects.requireNonNull(handSize, "handSize");
        Objects.requireNonNull(reserveDeckSize, "reserveDeckSize");
        Objects.requireNonNull(objectiveIdentity, "objectiveIdentity");
        Objects.requireNonNull(objectiveFlipped, "objectiveFlipped");
        Objects.requireNonNull(selectedRoute, "selectedRoute");
        Objects.requireNonNull(routeSelectionEvidence, "routeSelectionEvidence");

        // Response-range validation (gate item 3: "malformed ranges" must be rejected).
        requireNonNegativeWhenKnown(minimum, "minimum");
        requireNonNegativeWhenKnown(maximum, "maximum");
        if (minimum.isKnown() && maximum.isKnown() && maximum.value() < minimum.value()) {
            throw new IllegalArgumentException("malformed response range: maximum " + maximum.value()
                    + " < minimum " + minimum.value());
        }

        // Count/size range validation (gate item 6: negative counts rejected).
        requireNonNegativeWhenKnown(forcePileSize, "forcePileSize");
        requireNonNegativeWhenKnown(lifeForceCardCount, "lifeForceCardCount");
        requireNonNegativeWhenKnown(handSize, "handSize");
        requireNonNegativeWhenKnown(reserveDeckSize, "reserveDeckSize");
        if (objectiveIdentity.isKnown() && objectiveIdentity.value().isBlank()) {
            throw new IllegalArgumentException("known objectiveIdentity must be a nonblank blueprint id");
        }

        // Defensive copies: a KNOWN obligation set must be unmodifiable.
        if (obligationFlags.isKnown()) {
            obligationFlags = FactValue.known(Set.copyOf(obligationFlags.value()),
                    obligationFlags.producerId(), obligationFlags.provenance());
        }
        for (String response : blockedResponses) {
            if (response == null || response.isBlank()) {
                throw new IllegalArgumentException("blockedResponses must contain nonblank response ids");
            }
        }
        blockedResponses = Set.copyOf(blockedResponses);

        // Route-evidence consistency (gate item 2): the evidence record must be
        // provably derived from THIS decision's type/phase/window/obligations;
        // a mismatched evidence record is a construction error, not data.
        if (routeSelectionEvidence.decisionType() != decisionType) {
            throw new IllegalArgumentException("routeSelectionEvidence.decisionType "
                    + routeSelectionEvidence.decisionType() + " does not match decisionType " + decisionType);
        }
        if (!Objects.equals(routeSelectionEvidence.phase(), phase)) {
            throw new IllegalArgumentException("routeSelectionEvidence.phase "
                    + routeSelectionEvidence.phase() + " does not match phase " + phase);
        }
        if (!Objects.equals(routeSelectionEvidence.window(), window)) {
            throw new IllegalArgumentException("routeSelectionEvidence.window "
                    + routeSelectionEvidence.window() + " does not match window " + window);
        }
        if (!routeSelectionEvidence.obligations().equals(obligationFlags)) {
            throw new IllegalArgumentException(
                    "routeSelectionEvidence.obligations does not match obligationFlags");
        }
    }

    private static String requireNonBlank(String s, String name) {
        Objects.requireNonNull(s, name);
        if (s.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank, was \"" + s + "\"");
        }
        return s;
    }

    private static void requireNonNegativeWhenKnown(FactValue<Integer> fact, String name) {
        if (fact.isKnown() && fact.value() < 0) {
            throw new IllegalArgumentException(name + " must be >= 0 when known, was " + fact.value());
        }
    }

    // ═══ Typed obligation flags (gate item 1) ═══
    // Constants derive STRICTLY from the obligation signals the runtime actually
    // exposes on a raw decision (RandoCalAi.buildEvaluatorContext /
    // TheChosenOneAi parse exactly noPass, min, max from decision parameters):
    //  - NO_PASS             <- decision parameter noPass=true: engine will not accept a pass
    //  - MANDATORY_SELECTION <- decision parameter min > 0: at least `minimum` responses required
    // A new constant is added ONLY when the engine exposes a new obligation
    // signal; this enum is a closed set, never a string escape hatch. The
    // increment-2 shadow builder derives the set from the noPass/minimum facts;
    // parity fixtures catch derivation drift.
    public enum ObligationFlag {
        NO_PASS,
        MANDATORY_SELECTION
    }

    // ═══ Typed decision route (gate item 1: "add a route enum before any route consumer exists") ═══
    // MINIMAL structural set for increment 1: exactly one route per engine
    // decision shape (AwaitingDecisionType), because decision shape is the only
    // routing signal that provably exists before the router batch lands. The
    // gate doc requires the enum to EXIST but does not enumerate the taxonomy:
    // the router batch owns refinement (phase-specific deploy routes, the
    // parent/child mediated deploy-destination route from the contract) and
    // refines by ADDING constants under its own gate, never by reusing these
    // constants with changed semantics.
    public enum DecisionRoute {
        EMPTY,
        INTEGER,
        MULTIPLE_CHOICE,
        ARBITRARY_CARDS,
        CARD_ACTION_CHOICE,
        ACTION_CHOICE,
        CARD_SELECTION
    }

    // ═══ Candidate shape (gate item 2) ═══
    // The raw decision's candidate-array shape: how many entries each candidate
    // array carried. CARD_ACTION_CHOICE decisions carry parallel action AND card
    // arrays, so the two counts are independent, not a sum.
    public record CandidateShape(int actionCandidateCount, int cardCandidateCount) {
        public CandidateShape {
            if (actionCandidateCount < 0) {
                throw new IllegalArgumentException("actionCandidateCount must be >= 0, was " + actionCandidateCount);
            }
            if (cardCandidateCount < 0) {
                throw new IllegalArgumentException("cardCandidateCount must be >= 0, was " + cardCandidateCount);
            }
        }
    }

    // ═══ Structured route-selection evidence (gate item 2) ═══
    // A typed immutable record of the ONLY inputs route selection may use:
    // decision type, phase/window, obligations, and candidate shape (contract
    // §"Construction and dependencies" step 2: scores and assessments cannot
    // influence route selection). DecisionFacts' compact constructor enforces
    // that this record matches the facts' own fields, so the evidence is
    // machine-checkable, not narrative. Human-readable trace text is DERIVED
    // via describe(); no free-form evidence string exists anywhere.
    public record RouteSelectionEvidence(
            AwaitingDecisionType decisionType,
            Phase phase,                     // nullable, mirrors DecisionFacts.phase
            String window,                   // nullable, nonblank when present, mirrors DecisionFacts.window
            FactValue<Set<ObligationFlag>> obligations,
            CandidateShape candidateShape) {

        public RouteSelectionEvidence {
            Objects.requireNonNull(decisionType, "decisionType");
            if (window != null && window.isBlank()) {
                throw new IllegalArgumentException("window must be nonblank when present");
            }
            Objects.requireNonNull(obligations, "obligations");
            Objects.requireNonNull(candidateShape, "candidateShape");
            if (obligations.isKnown()) {
                obligations = FactValue.known(Set.copyOf(obligations.value()),
                        obligations.producerId(), obligations.provenance());
            }
        }

        /** Human-readable trace text, DERIVED from the typed fields (deterministic:
         *  obligation flags render in enum declaration order). */
        public String describe() {
            String obligationText = obligations.isKnown()
                    ? new TreeSet<>(obligations.value()).toString()
                    : "UNKNOWN(" + obligations.unknownReason() + ")";
            return "decisionType=" + decisionType
                    + " phase=" + (phase != null ? phase : "n/a")
                    + " window=" + (window != null ? window : "n/a")
                    + " obligations=" + obligationText
                    + " candidates[actions=" + candidateShape.actionCandidateCount()
                    + " cards=" + candidateShape.cardCandidateCount() + "]";
        }
    }

    /** Builder for readable construction; the compact constructor still validates everything.
     *  Deliberately NO defaults for any engine-fact field: an unset fact fails construction
     *  instead of fabricating a value (gate item 3). blockedResponses alone defaults to empty
     *  because it is AI-internal loop-prevention state that is truly empty at decision start,
     *  not an engine observation. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String decisionId;
        private AwaitingDecisionType decisionType;
        private String decisionText;
        private Phase phase;
        private String window;
        private int turn;
        private String currentPlayer;
        private Side side;
        private FactValue<Set<ObligationFlag>> obligationFlags;
        private FactValue<Boolean> noPass;
        private FactValue<Integer> minimum;
        private FactValue<Integer> maximum;
        private Set<String> blockedResponses = Set.of();
        private FactValue<Integer> forcePileSize;
        private FactValue<Integer> lifeForceCardCount;
        private FactValue<Integer> handSize;
        private FactValue<Integer> reserveDeckSize;
        private FactValue<String> objectiveIdentity;
        private FactValue<Boolean> objectiveFlipped;
        private DecisionRoute selectedRoute;
        private RouteSelectionEvidence routeSelectionEvidence;

        private Builder() {}

        public Builder decisionId(String v) { this.decisionId = v; return this; }
        public Builder decisionType(AwaitingDecisionType v) { this.decisionType = v; return this; }
        public Builder decisionText(String v) { this.decisionText = v; return this; }
        public Builder phase(Phase v) { this.phase = v; return this; }
        public Builder window(String v) { this.window = v; return this; }
        public Builder turn(int v) { this.turn = v; return this; }
        public Builder currentPlayer(String v) { this.currentPlayer = v; return this; }
        public Builder side(Side v) { this.side = v; return this; }
        public Builder obligationFlags(FactValue<Set<ObligationFlag>> v) { this.obligationFlags = v; return this; }
        public Builder noPass(FactValue<Boolean> v) { this.noPass = v; return this; }
        public Builder minimum(FactValue<Integer> v) { this.minimum = v; return this; }
        public Builder maximum(FactValue<Integer> v) { this.maximum = v; return this; }
        public Builder blockedResponses(Set<String> v) { this.blockedResponses = v; return this; }
        public Builder forcePileSize(FactValue<Integer> v) { this.forcePileSize = v; return this; }
        public Builder lifeForceCardCount(FactValue<Integer> v) { this.lifeForceCardCount = v; return this; }
        public Builder handSize(FactValue<Integer> v) { this.handSize = v; return this; }
        public Builder reserveDeckSize(FactValue<Integer> v) { this.reserveDeckSize = v; return this; }
        public Builder objectiveIdentity(FactValue<String> v) { this.objectiveIdentity = v; return this; }
        public Builder objectiveFlipped(FactValue<Boolean> v) { this.objectiveFlipped = v; return this; }
        public Builder selectedRoute(DecisionRoute v) { this.selectedRoute = v; return this; }
        public Builder routeSelectionEvidence(RouteSelectionEvidence v) { this.routeSelectionEvidence = v; return this; }

        public DecisionFacts build() {
            return new DecisionFacts(decisionId, decisionType, decisionText, phase, window, turn,
                    currentPlayer, side, obligationFlags, noPass, minimum, maximum, blockedResponses,
                    forcePileSize, lifeForceCardCount, handSize, reserveDeckSize, objectiveIdentity,
                    objectiveFlipped, selectedRoute, routeSelectionEvidence);
        }
    }
}
