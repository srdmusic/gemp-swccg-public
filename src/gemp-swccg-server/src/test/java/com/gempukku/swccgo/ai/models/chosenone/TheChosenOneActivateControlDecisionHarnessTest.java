package com.gempukku.swccgo.ai.models.chosenone;

import com.gempukku.swccgo.ai.models.common.phase.AbstractActivateControlDecisionHarnessTest;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * TheChosenOneAi adapter for the ACTIVATE + CONTROL decide-equivalent harness
 * (packet Handoffs/CODEX_ACTIVATE_CONTROL_DECIDE_HARNESS_PACKET_2026-07-13.md).
 *
 * Overrides ONLY {@link #runDecision}: the abstract contract owns the six fixtures and
 * every structured assertion. This adapter exists solely because TheChosenOneAi's
 * {@code setDecisionTraceSinkForTesting} is package-visible — no reflection, no widening.
 * It differs from the Rando adapter ONLY by the bot package/class substitutions; the
 * packet requires Rando and ChosenOne to agree on candidate/score/veto/route/response.
 */
public class TheChosenOneActivateControlDecisionHarnessTest extends AbstractActivateControlDecisionHarnessTest {

    @Override
    protected CapturedDecision runDecision(AwaitingDecision decision, GameState gameState) {
        // run 1: production-default no-op sink — no session opens, byte-identical behavior
        TheChosenOneAi untraced = new TheChosenOneAi();
        String untracedResponse = untraced.decide(DECIDING_PLAYER, decision, gameState);
        assertFalse("no-op run must not leak a trace session", TraceSession.isActive());

        // run 2: identical fresh bot + identical decision with a StrictFixtureSink (a
        // COMPLETE fixture must never accept an INCOMPLETE trace as evidence)
        TheChosenOneAi traced = new TheChosenOneAi();
        TraceTestSupport.StrictFixtureSink sink = new TraceTestSupport.StrictFixtureSink();
        traced.setDecisionTraceSinkForTesting(sink);
        String tracedResponse = traced.decide(DECIDING_PLAYER, decision, gameState);
        assertFalse("captured run must not leak a trace session", TraceSession.isActive());

        // traced and untraced wire responses are identical
        assertEquals("traced and untraced wire responses must be identical",
            untracedResponse, tracedResponse);

        DecisionTrace trace = sink.single();
        return new CapturedDecision(tracedResponse, trace);
    }
}
