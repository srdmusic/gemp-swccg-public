package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: ACTIVATE/CONTROL OPTION 2 / LIVE ROUTE INPUT (2026-07-13) ═══
// Packet: Handoffs/CODEX_ACTIVATE_CONTROL_PHASE_B_PACKET_2026-07-13.md §1.
//
// The immutable, phase-specific input the pure resolver reads. It carries exactly
// the signals routing may use and nothing else: no game state, no evaluator, no
// tracker. Two distinct player fields are kept SEPARATE on purpose:
//
//   decisionRecipient  - who must answer this decision (DecisionFacts.currentPlayer
//                        semantics: the decide() playerId). For the opponent
//                        allowance decision this is the OPPONENT, not the activator.
//   currentTurnPlayer  - whose turn it is.
//
// The committed trace schema (DecisionFacts/DecisionSnapshot/TraceSnapshots) is NOT
// expanded to carry the turn player; it is carried here, on this new input, so the
// recipient-vs-turn-player distinction survives without touching that schema.
//
// The origin field is the "typed origin or unowned origin state": a resolved
// DecisionOrigin, or null when the stamp was absent or unrecognized (both collapse
// to the resolver's LEGACY_UNOWNED bypass). Parse the wire value with
// DecisionOrigin.fromWire before constructing this input.
// ═══════════════════════════════════════════════════════════
public record ActivateControlRouteInput(
        /** The decision's phase; nullable when the decision carried no phase. */
        Phase phase,
        /** Typed origin, or null for the unowned state (absent or unrecognized stamp). */
        DecisionOrigin origin,
        /** The concrete engine wire shape of the decision. */
        AwaitingDecisionType wireDecisionType,
        /** Who must answer (decide() playerId); nonblank. */
        String decisionRecipient,
        /** Whose turn it is; nonblank. Distinct from decisionRecipient by design. */
        String currentTurnPlayer,
        /** Ordered wire results; empty for INTEGER/EMPTY shapes. Never null. */
        List<String> results,
        /** Raw MULTIPLE_CHOICE defaultIndex; null when the engine sent none. */
        Integer defaultIndex,
        /** Raw INTEGER defaultValue; null when the engine sent none. */
        Integer defaultValue) {

    public ActivateControlRouteInput {
        Objects.requireNonNull(wireDecisionType, "wireDecisionType");
        requireNonBlank(decisionRecipient, "decisionRecipient");
        requireNonBlank(currentTurnPlayer, "currentTurnPlayer");
        Objects.requireNonNull(results, "results");
        results = List.copyOf(results);
    }

    /** Whether this decision carries a typed (owned) origin rather than the unowned state. */
    public boolean hasOwnedOrigin() {
        return origin != null;
    }

    /** Capture the raw route fields once, without consulting game state or strategy services. */
    public static ActivateControlRouteInput capture(Phase phase,
                                                    AwaitingDecision decision,
                                                    String decisionRecipient,
                                                    String currentTurnPlayer) {
        Objects.requireNonNull(decision, "decision");
        Map<String, String[]> params = decision.getDecisionParameters();
        return new ActivateControlRouteInput(
                phase,
                DecisionOrigin.fromWire(single(params, DecisionOrigin.WIRE_PARAMETER)),
                decision.getDecisionType(),
                decisionRecipient,
                currentTurnPlayer,
                values(params, "results"),
                integer(params, "defaultIndex"),
                integer(params, "defaultValue"));
    }

    private static String single(Map<String, String[]> params, String key) {
        String[] values = params != null ? params.get(key) : null;
        return values != null && values.length == 1 ? values[0] : null;
    }

    private static List<String> values(Map<String, String[]> params, String key) {
        String[] values = params != null ? params.get(key) : null;
        if (values == null) {
            return Collections.emptyList();
        }
        List<String> copy = new ArrayList<>(values.length);
        Collections.addAll(copy, values);
        return copy;
    }

    private static Integer integer(Map<String, String[]> params, String key) {
        String value = single(params, key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
    }
}
