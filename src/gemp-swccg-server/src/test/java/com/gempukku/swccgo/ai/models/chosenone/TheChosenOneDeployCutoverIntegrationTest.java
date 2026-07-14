package com.gempukku.swccgo.ai.models.chosenone;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.DecisionRejectionKind;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.phase.AbstractDeployCutoverIntegrationTest;
import com.gempukku.swccgo.ai.models.common.phase.DeployTransaction;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceTestSupport;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** ChosenOne adapter for the shared DEPLOY direct and mediator lifecycle contract. */
public class TheChosenOneDeployCutoverIntegrationTest
        extends AbstractDeployCutoverIntegrationTest {

    @Override
    protected DirectCapture runDirect(AwaitingDecision decision,
                                      GameState gameState) {
        TheChosenOneAi ai = new TheChosenOneAi();
        String wire = ai.decide(PLAYER, decision, gameState);
        return new DirectCapture(
                wire, ai.getDeployPhasePlanner().getCurrentTransaction());
    }

    @Override
    protected MediatorCapture runMediatorAccepted(
            AwaitingDecision decision, GameState gameState,
            RejectionHistory history) {
        TheChosenOneAi ai = new TheChosenOneAi();
        AiDecisionResult result = ai.decideForEngine(
                PLAYER, decision, gameState, history);
        DeployTransaction before =
                ai.getDeployPhasePlanner().getCurrentTransaction();
        ai.onDecisionAccepted(PLAYER, decision, gameState, result);
        return new MediatorCapture(result, before,
                ai.getDeployPhasePlanner().getCurrentTransaction());
    }

    @Override
    protected MediatorCapture runMediatorRejected(
            AwaitingDecision decision, GameState gameState,
            RejectionHistory history) {
        TheChosenOneAi ai = new TheChosenOneAi();
        AiDecisionResult result = ai.decideForEngine(
                PLAYER, decision, gameState, history);
        DeployTransaction before =
                ai.getDeployPhasePlanner().getCurrentTransaction();
        ai.onDecisionRejected(PLAYER, decision, gameState, result,
                DecisionRejectionKind.ENGINE_REJECTED, "fixture rejection");
        return new MediatorCapture(result, before,
                ai.getDeployPhasePlanner().getCurrentTransaction());
    }

    @Override
    protected V170Capture runV170(
            V170Path path,
            AwaitingDecision parentDecision,
            AwaitingDecision v170Decision,
            GameState gameState,
            SwccgGame game,
            RejectionHistory history) {
        TheChosenOneAi ai = new TheChosenOneAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        ai.setGame(game);

        if (path == V170Path.DIRECT) {
            ai.decide(PLAYER, parentDecision, gameState);
        } else {
            AiDecisionResult parent = ai.decideForEngine(
                    PLAYER, parentDecision, gameState, RejectionHistory.empty());
            ai.onDecisionAccepted(PLAYER, parentDecision, gameState, parent);
        }
        assertFalse("parent disposition must close its trace", TraceSession.isActive());

        DeployTransaction before =
                ai.getDeployPhasePlanner().getCurrentTransaction();
        AiDecisionResult result = null;
        String wire;
        if (path == V170Path.DIRECT) {
            wire = ai.decide(PLAYER, v170Decision, gameState);
        } else {
            result = ai.decideForEngine(
                    PLAYER, v170Decision, gameState, history);
            assertTrue("mediator V170 keeps trace open until disposition",
                    TraceSession.isActive());
            wire = result.wireResponse();
            if (path == V170Path.MEDIATOR_ACCEPTED) {
                ai.onDecisionAccepted(PLAYER, v170Decision, gameState, result);
            } else {
                ai.onDecisionRejected(PLAYER, v170Decision, gameState, result,
                        DecisionRejectionKind.ENGINE_REJECTED,
                        "fixture V170 rejection");
            }
        }
        assertFalse("V170 disposition must close its trace", TraceSession.isActive());
        return new V170Capture(
                wire, result, sink.getTraces().get(sink.getTraces().size() - 1),
                before, ai.getDeployPhasePlanner().getCurrentTransaction());
    }

    @Override
    protected LegacyV170Capture runLegacyV170(
            boolean mediator,
            AwaitingDecision parentDecision,
            AwaitingDecision v170Decision,
            GameState gameState,
            SwccgGame game) {
        TheChosenOneAi ai = new TheChosenOneAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        ai.setGame(game);

        if (mediator) {
            AiDecisionResult parent = ai.decideForEngine(
                    PLAYER, parentDecision, gameState, RejectionHistory.empty());
            ai.onDecisionAccepted(PLAYER, parentDecision, gameState, parent);
        } else {
            ai.decide(PLAYER, parentDecision, gameState);
        }
        DeployTransaction before =
                ai.getDeployPhasePlanner().getCurrentTransaction();

        AiDecisionResult result = null;
        String wire;
        if (mediator) {
            result = ai.decideForEngine(
                    PLAYER, v170Decision, gameState, RejectionHistory.empty());
            wire = result.wireResponse();
            ai.onDecisionAccepted(PLAYER, v170Decision, gameState, result);
        } else {
            wire = ai.decide(PLAYER, v170Decision, gameState);
        }
        assertFalse("legacy V170 disposition must close its trace", TraceSession.isActive());
        return new LegacyV170Capture(
                wire, result, sink.getTraces().get(sink.getTraces().size() - 1),
                before, ai.getDeployPhasePlanner().getCurrentTransaction());
    }


    @Override
    protected BlockedReplayCapture runBlockedReplay(
            AwaitingDecision first,
            AwaitingDecision second,
            GameState gameState) {
        TheChosenOneAi ai = new TheChosenOneAi();
        TraceTestSupport.CaptureSink sink = new TraceTestSupport.CaptureSink();
        ai.setDecisionTraceSinkForTesting(sink);
        String firstWire = ai.decide(PLAYER, first, gameState);
        DeployTransaction firstCursor = ai.getDeployPhasePlanner().getCurrentTransaction();
        String secondWire = ai.decide(PLAYER, second, gameState);
        DeployTransaction secondCursor = ai.getDeployPhasePlanner().getCurrentTransaction();
        return new BlockedReplayCapture(
                List.of(firstWire, secondWire),
                java.util.Arrays.asList(firstCursor, secondCursor),
                sink.getTraces());
    }
}
