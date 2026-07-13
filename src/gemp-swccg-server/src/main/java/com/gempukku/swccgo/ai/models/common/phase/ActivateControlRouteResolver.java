package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;

import java.util.Objects;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: ACTIVATE/CONTROL OPTION 2 / PURE SHADOW RESOLVER (2026-07-13) ═══
// Packet: Handoffs/CODEX_ACTIVATE_CONTROL_PHASE_PACKET_2026-07-13.md §2.
//
// Deterministic, side-effect-free mapping from (phase, origin, wire shape) to one
// closed ActivateControlRoute. It reads NO game state, calls NO evaluator, mutates
// NO tracker, finalizes NO response, emits NO trace, and alters NO fallback. Same
// input always yields the same route.
//
// Route matrix (packet §2):
//   ACTIVATE + PHASE_ACTION              -> ACTIVATE_TOP_LEVEL
//   ACTIVATE + ACTIVATE_AMOUNT           -> ACTIVATE_AMOUNT
//   ACTIVATE + ACTIVATE_ALLOWANCE        -> ACTIVATE_ALLOWANCE
//   ACTIVATE + ACTIVATE_ZERO_CONFIRM     -> ACTIVATE_ZERO_CONFIRM
//   ACTIVATE + ACTIVATE_INTERRUPTION_ACK -> ACTIVATE_ACK
//   CONTROL  + PHASE_ACTION              -> CONTROL_TOP_LEVEL
//   any      + absent/unknown/wrong phase/wrong shape -> LEGACY_UNOWNED
//
// LEGACY_UNOWNED is the intentional bypass: absent or unrecognized origin (input
// .origin() == null), a wire shape that does not match the origin's required type,
// or an origin appearing in a phase it does not own. NO production call site exists
// in this phase; the resolver is shadow-only.
// ═══════════════════════════════════════════════════════════
public final class ActivateControlRouteResolver {

    private ActivateControlRouteResolver() {
        // static access only
    }

    /**
     * Resolve one route from one immutable input. Pure and total: every input maps
     * to exactly one route, with LEGACY_UNOWNED covering everything this phase does
     * not own.
     */
    public static ActivateControlRoute resolve(ActivateControlRouteInput input) {
        Objects.requireNonNull(input, "input");

        DecisionOrigin origin = input.origin();
        // Absent or unrecognized stamp: the unowned state.
        if (origin == null) {
            return ActivateControlRoute.LEGACY_UNOWNED;
        }
        // Wrong wire shape: the stamped decision does not carry the origin's required
        // wire type. Compared by name across the common/logic module boundary (see
        // DecisionOrigin header); a resolver fixture proves every name is real.
        if (!origin.requiredWireTypeName().equals(input.wireDecisionType().name())) {
            return ActivateControlRoute.LEGACY_UNOWNED;
        }

        Phase phase = input.phase();
        switch (origin) {
            case PHASE_ACTION:
                if (phase == Phase.ACTIVATE) {
                    return ActivateControlRoute.ACTIVATE_TOP_LEVEL;
                }
                if (phase == Phase.CONTROL) {
                    return ActivateControlRoute.CONTROL_TOP_LEVEL;
                }
                return ActivateControlRoute.LEGACY_UNOWNED; // wrong phase
            case ACTIVATE_AMOUNT:
                return phase == Phase.ACTIVATE
                        ? ActivateControlRoute.ACTIVATE_AMOUNT
                        : ActivateControlRoute.LEGACY_UNOWNED;
            case ACTIVATE_ALLOWANCE:
                return phase == Phase.ACTIVATE
                        ? ActivateControlRoute.ACTIVATE_ALLOWANCE
                        : ActivateControlRoute.LEGACY_UNOWNED;
            case ACTIVATE_ZERO_CONFIRM:
                return phase == Phase.ACTIVATE
                        ? ActivateControlRoute.ACTIVATE_ZERO_CONFIRM
                        : ActivateControlRoute.LEGACY_UNOWNED;
            case ACTIVATE_INTERRUPTION_ACK:
                return phase == Phase.ACTIVATE
                        ? ActivateControlRoute.ACTIVATE_ACK
                        : ActivateControlRoute.LEGACY_UNOWNED;
            default:
                // Closed enum: unreachable. Bypass rather than throw.
                return ActivateControlRoute.LEGACY_UNOWNED;
        }
    }
}
