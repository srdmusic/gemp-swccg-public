package com.gempukku.swccgo.ai.models.chosenone;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.phase.AbstractPullCutoverIntegrationTest;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** ChosenOne adapter for the shared PULL cutover integration contract. */
public class TheChosenOnePullCutoverIntegrationTest extends AbstractPullCutoverIntegrationTest {

    @Override
    protected CapturedAccepted runAccepted(AwaitingDecision decision,
                                            GameState gameState,
                                            RejectionHistory history) {
        TheChosenOneAi ai = new TheChosenOneAi();
        TraceTestSupport.StrictFixtureSink sink = new TraceTestSupport.StrictFixtureSink();
        ai.setDecisionTraceSinkForTesting(sink);

        AiDecisionResult result = ai.decideForEngine(
                DECIDING_PLAYER, decision, gameState, history);
        assertTrue("mediator path keeps trace open until disposition", TraceSession.isActive());
        assertEquals("no trace emits before acceptance", 0, sink.getTraces().size());

        ai.onDecisionAccepted(DECIDING_PLAYER, decision, gameState, result);

        assertFalse("accepted callback closes trace", TraceSession.isActive());
        return new CapturedAccepted(result, sink.single());
    }
}
