package com.gempukku.swccgo.ai.models.chosenone;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.phase.AbstractBattleCutoverIntegrationTest;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** ChosenOne adapter for the shared BATTLE accepted-response contract. */
public class TheChosenOneBattleCutoverIntegrationTest
        extends AbstractBattleCutoverIntegrationTest {

    @Override
    protected Capture runAccepted(AwaitingDecision decision,
                                  GameState gameState) {
        TheChosenOneAi ai = new TheChosenOneAi();
        TraceTestSupport.StrictFixtureSink sink =
                new TraceTestSupport.StrictFixtureSink();
        ai.setDecisionTraceSinkForTesting(sink);

        AiDecisionResult result = ai.decideForEngine(
                PLAYER, decision, gameState, RejectionHistory.empty());
        assertTrue(TraceSession.isActive());
        ai.onDecisionAccepted(PLAYER, decision, gameState, result);
        assertFalse(TraceSession.isActive());
        return new Capture(result, sink.single());
    }
}
