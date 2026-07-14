package com.gempukku.swccgo.ai.models.rando;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.phase.AbstractActivateControlDecisionHarnessTest;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * RandoCalAi adapter for the ACTIVATE + CONTROL decide-equivalent harness
 * (packet Handoffs/CODEX_ACTIVATE_CONTROL_DECIDE_HARNESS_PACKET_2026-07-13.md).
 *
 * Overrides only the direct and mediator-facing execution seams; the abstract contract
 * owns every fixture and structured assertion. This adapter exists solely because RandoCalAi's
 * {@code setDecisionTraceSinkForTesting} is package-visible — no reflection, no widening.
 * It differs from the TheChosenOne adapter ONLY by the bot package/class substitutions; the
 * packet requires Rando and ChosenOne to agree on candidate/score/veto/route/response.
 */
public class RandoActivateControlDecisionHarnessTest extends AbstractActivateControlDecisionHarnessTest {

    @Override
    protected CapturedDecision runDecision(AwaitingDecision decision, GameState gameState) {
        // run 1: production-default no-op sink — no session opens, byte-identical behavior
        RandoCalAi untraced = new RandoCalAi();
        String untracedResponse = untraced.decide(DECIDING_PLAYER, decision, gameState);
        assertFalse("no-op run must not leak a trace session", TraceSession.isActive());

        // run 2: identical fresh bot + identical decision with a StrictFixtureSink (a
        // COMPLETE fixture must never accept an INCOMPLETE trace as evidence)
        RandoCalAi traced = new RandoCalAi();
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

    @Override
    protected AiDecisionResult runEngineDecision(
            AwaitingDecision decision, GameState gameState, RejectionHistory history) {
        AiDecisionResult result = new RandoCalAi().decideForEngine(
            DECIDING_PLAYER, decision, gameState, history);
        assertFalse("engine-facing run must not leak a trace session", TraceSession.isActive());
        return result;
    }
}
