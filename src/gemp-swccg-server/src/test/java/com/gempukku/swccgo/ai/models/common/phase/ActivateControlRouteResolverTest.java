package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Pure resolver + input contract for the ACTIVATE/CONTROL Option 2 shadow phase.
 * Packet: Handoffs/CODEX_ACTIVATE_CONTROL_PHASE_PACKET_2026-07-13.md §2, §3.
 * There is deliberately no production consumer; this is the only caller.
 */
public class ActivateControlRouteResolverTest {

    private static final String ACTIVATOR = "darkPlayer";
    private static final String OPPONENT = "lightPlayer";

    // ── The six owned matrix rows ──

    @Test
    public void activatePhaseActionRoutesTopLevel() {
        assertEquals(ActivateControlRoute.ACTIVATE_TOP_LEVEL,
                resolve(Phase.ACTIVATE, DecisionOrigin.PHASE_ACTION, AwaitingDecisionType.CARD_ACTION_CHOICE));
    }

    @Test
    public void controlPhaseActionRoutesControlTopLevel() {
        assertEquals(ActivateControlRoute.CONTROL_TOP_LEVEL,
                resolve(Phase.CONTROL, DecisionOrigin.PHASE_ACTION, AwaitingDecisionType.CARD_ACTION_CHOICE));
    }

    @Test
    public void activateAmountRoutesAmount() {
        assertEquals(ActivateControlRoute.ACTIVATE_AMOUNT,
                resolve(Phase.ACTIVATE, DecisionOrigin.ACTIVATE_AMOUNT, AwaitingDecisionType.INTEGER));
    }

    @Test
    public void activateAllowanceRoutesAllowance() {
        assertEquals(ActivateControlRoute.ACTIVATE_ALLOWANCE,
                resolve(Phase.ACTIVATE, DecisionOrigin.ACTIVATE_ALLOWANCE, AwaitingDecisionType.INTEGER));
    }

    @Test
    public void activateZeroConfirmRoutesZeroConfirm() {
        assertEquals(ActivateControlRoute.ACTIVATE_ZERO_CONFIRM,
                resolve(Phase.ACTIVATE, DecisionOrigin.ACTIVATE_ZERO_CONFIRM, AwaitingDecisionType.MULTIPLE_CHOICE));
    }

    @Test
    public void activateInterruptionAckRoutesAck() {
        assertEquals(ActivateControlRoute.ACTIVATE_ACK,
                resolve(Phase.ACTIVATE, DecisionOrigin.ACTIVATE_INTERRUPTION_ACK, AwaitingDecisionType.MULTIPLE_CHOICE));
    }

    // ── LEGACY_UNOWNED bypass: absent, unknown, wrong phase, wrong shape ──

    @Test
    public void absentOriginBypassesToLegacyUnowned() {
        // Absent stamp: fromWire(null) is the unowned state (null origin).
        assertNull(DecisionOrigin.fromWire(null));
        ActivateControlRouteInput input = input(Phase.ACTIVATE, DecisionOrigin.fromWire(null),
                AwaitingDecisionType.CARD_ACTION_CHOICE, ACTIVATOR, ACTIVATOR, List.of("a"), null, null);
        assertFalse(input.hasOwnedOrigin());
        assertEquals(ActivateControlRoute.LEGACY_UNOWNED, ActivateControlRouteResolver.resolve(input));
    }

    @Test
    public void unknownOriginBypassesToLegacyUnowned() {
        // Unrecognized stamp value: fromWire collapses it to the unowned state.
        assertNull(DecisionOrigin.fromWire("NOT_A_REAL_ORIGIN"));
        ActivateControlRouteInput input = input(Phase.ACTIVATE, DecisionOrigin.fromWire("NOT_A_REAL_ORIGIN"),
                AwaitingDecisionType.CARD_ACTION_CHOICE, ACTIVATOR, ACTIVATOR, List.of("a"), null, null);
        assertEquals(ActivateControlRoute.LEGACY_UNOWNED, ActivateControlRouteResolver.resolve(input));
    }

    @Test
    public void wrongPhaseBypassesToLegacyUnowned() {
        // A valid origin appearing in a phase it does not own.
        assertEquals(ActivateControlRoute.LEGACY_UNOWNED,
                resolve(Phase.DEPLOY, DecisionOrigin.PHASE_ACTION, AwaitingDecisionType.CARD_ACTION_CHOICE));
        assertEquals(ActivateControlRoute.LEGACY_UNOWNED,
                resolve(Phase.CONTROL, DecisionOrigin.ACTIVATE_AMOUNT, AwaitingDecisionType.INTEGER));
    }

    @Test
    public void wrongShapeBypassesToLegacyUnowned() {
        // Right origin + phase, but the wire type does not match the origin's required type.
        assertEquals(ActivateControlRoute.LEGACY_UNOWNED,
                resolve(Phase.ACTIVATE, DecisionOrigin.ACTIVATE_AMOUNT, AwaitingDecisionType.MULTIPLE_CHOICE));
        assertEquals(ActivateControlRoute.LEGACY_UNOWNED,
                resolve(Phase.ACTIVATE, DecisionOrigin.PHASE_ACTION, AwaitingDecisionType.INTEGER));
    }

    // ── Recipient vs turn player + preserved ordering/defaults ──

    @Test
    public void recipientDiffersFromTurnPlayerAndRoutingIsUnaffected() {
        // The opponent allowance decision: recipient is the OPPONENT, turn player is
        // the activator. The distinction is preserved on the input and does not alter
        // the route.
        ActivateControlRouteInput input = input(Phase.ACTIVATE, DecisionOrigin.ACTIVATE_ALLOWANCE,
                AwaitingDecisionType.INTEGER, OPPONENT, ACTIVATOR, List.of(), 4, 5);
        assertEquals(OPPONENT, input.decisionRecipient());
        assertEquals(ACTIVATOR, input.currentTurnPlayer());
        assertEquals(ActivateControlRoute.ACTIVATE_ALLOWANCE, ActivateControlRouteResolver.resolve(input));
    }

    @Test
    public void inputPreservesOrderedResultsAndDefaults() {
        ActivateControlRouteInput input = input(Phase.ACTIVATE, DecisionOrigin.ACTIVATE_ZERO_CONFIRM,
                AwaitingDecisionType.MULTIPLE_CHOICE, ACTIVATOR, ACTIVATOR, List.of("Yes", "No"), 1, null);
        assertEquals(List.of("Yes", "No"), input.results());
        assertEquals(Integer.valueOf(1), input.defaultIndex());
        assertNull(input.defaultValue());
        assertTrue(input.hasOwnedOrigin());
    }

    @Test
    public void resultsListIsDefensivelyImmutable() {
        ActivateControlRouteInput input = input(Phase.ACTIVATE, DecisionOrigin.ACTIVATE_ZERO_CONFIRM,
                AwaitingDecisionType.MULTIPLE_CHOICE, ACTIVATOR, ACTIVATOR, List.of("Yes", "No"), 0, null);
        try {
            input.results().add("Maybe");
            org.junit.Assert.fail("results must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected: List.copyOf produces an unmodifiable list
        }
    }

    // ── Determinism ──

    @Test
    public void resolverIsDeterministicAcrossRepeatedCalls() {
        ActivateControlRouteInput input = input(Phase.ACTIVATE, DecisionOrigin.ACTIVATE_AMOUNT,
                AwaitingDecisionType.INTEGER, ACTIVATOR, ACTIVATOR, List.of(), null, 5);
        ActivateControlRoute first = ActivateControlRouteResolver.resolve(input);
        for (int i = 0; i < 100; i++) {
            assertSame(first, ActivateControlRouteResolver.resolve(input));
        }
        assertEquals(ActivateControlRoute.ACTIVATE_AMOUNT, first);
    }

    // ── Drift guard: every origin's required wire-type name is a real AwaitingDecisionType ──

    @Test
    public void everyOriginRequiredWireTypeNameIsARealAwaitingDecisionType() {
        for (DecisionOrigin origin : DecisionOrigin.values()) {
            // Throws IllegalArgumentException if the name ever drifts from the enum.
            AwaitingDecisionType type = AwaitingDecisionType.valueOf(origin.requiredWireTypeName());
            org.junit.Assert.assertNotNull(type);
        }
    }

    // ── helpers ──

    private static ActivateControlRoute resolve(Phase phase, DecisionOrigin origin,
                                                AwaitingDecisionType wireType) {
        return ActivateControlRouteResolver.resolve(
                input(phase, origin, wireType, ACTIVATOR, ACTIVATOR, List.of(), null, null));
    }

    private static ActivateControlRouteInput input(Phase phase, DecisionOrigin origin,
                                                   AwaitingDecisionType wireType, String recipient,
                                                   String turnPlayer, List<String> results,
                                                   Integer defaultIndex, Integer defaultValue) {
        return new ActivateControlRouteInput(phase, origin, wireType, recipient, turnPlayer,
                results, defaultIndex, defaultValue);
    }
}
