package com.gempukku.swccgo.ai.models.curator;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.DecisionRejectionKind;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.rando.RandoCalAi;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * FINALIZER RUNTIME (2026-07-13, packet §6/§9 "Curator"): Curator forwards the mediator-facing
 * lifecycle to the wrapped RandoCalAi WITHOUT invoking the local model. It uses the
 * package-private injected-Rando seam and the pure applyOverride helper. No network.
 */
public class CuratorAiLifecycleTest {

    @After
    public void clearAnyLeakedSession() {
        if (TraceSession.isActive()) {
            TraceSession.abandon();
        }
    }

    /** A non-consequential decision (no options) so shouldConsult() returns false: passthrough,
     *  never contacting the model. */
    private static AwaitingDecision trivialDecision() {
        return new AwaitingDecision() {
            @Override public int getAwaitingDecisionId() { return 1; }
            @Override public String getText() { return "Choose"; }
            @Override public AwaitingDecisionType getDecisionType() { return AwaitingDecisionType.MULTIPLE_CHOICE; }
            @Override public Map<String, String[]> getDecisionParameters() { return new HashMap<>(); }
            @Override public void decisionMade(String result) { }
        };
    }

    /** A RandoCalAi spy that scripts decideForEngine and records forwarded dispositions. */
    private static class SpyRando extends RandoCalAi {
        RejectionHistory lastHistory;
        int decideForEngineCalls;
        int acceptedForwarded;
        int rejectedForwarded;
        int attemptFailedForwarded;
        DecisionRejectionKind lastRejectedKind;
        AiDecisionResult lastAcceptedResult;
        AiDecisionResult scripted;

        @Override
        public AiDecisionResult decideForEngine(String playerId, AwaitingDecision decision,
                                                GameState gameState, RejectionHistory history) {
            decideForEngineCalls++;
            lastHistory = history;
            return scripted;
        }

        @Override
        public void onDecisionAccepted(String playerId, AwaitingDecision decision,
                                       GameState gameState, AiDecisionResult result) {
            acceptedForwarded++;
            lastAcceptedResult = result;
        }

        @Override
        public void onDecisionRejected(String playerId, AwaitingDecision decision,
                                       GameState gameState, AiDecisionResult result,
                                       DecisionRejectionKind kind, String detail) {
            rejectedForwarded++;
            lastRejectedKind = kind;
        }

        @Override
        public void onDecisionAttemptFailed(String playerId, AwaitingDecision decision,
                                            GameState gameState, String detail) {
            attemptFailedForwarded++;
        }
    }

    @Test
    public void passthroughForwardsAcceptedCallbackOnce() {
        SpyRando spy = new SpyRando();
        spy.scripted = AiDecisionResult.wire("2", AiDecisionResult.MutationMode.OUTER_COMMON, "1");
        CuratorAi curator = new CuratorAi(spy);
        AwaitingDecision decision = trivialDecision();

        AiDecisionResult result = curator.decideForEngine("dark", decision, null, RejectionHistory.empty());
        assertEquals("passthrough returns the wrapped wire", "2", result.wireResponse());
        assertEquals("wrapped Rando consulted once", 1, spy.decideForEngineCalls);

        curator.onDecisionAccepted("dark", decision, null, result);
        assertEquals("accepted forwarded exactly once", 1, spy.acceptedForwarded);
        assertSame("forwarded the same result", result, spy.lastAcceptedResult);
    }

    @Test
    public void forwardsRejectionAndAttemptFailedExactlyOnce() {
        SpyRando spy = new SpyRando();
        spy.scripted = AiDecisionResult.wire("2", AiDecisionResult.MutationMode.NONE, "1");
        CuratorAi curator = new CuratorAi(spy);
        AwaitingDecision decision = trivialDecision();
        AiDecisionResult result = curator.decideForEngine("dark", decision, null, RejectionHistory.empty());

        curator.onDecisionRejected("dark", decision, null, result,
                DecisionRejectionKind.ENGINE_REJECTED, "rejected");
        curator.onDecisionAttemptFailed("dark", decision, null, "aborted");

        assertEquals("rejection forwarded once", 1, spy.rejectedForwarded);
        assertEquals("kind forwarded", DecisionRejectionKind.ENGINE_REJECTED, spy.lastRejectedKind);
        assertEquals("attempt-failed forwarded once", 1, spy.attemptFailedForwarded);
    }

    @Test
    public void forwardsIdenticalRejectionHistoryToWrappedRando() {
        SpyRando spy = new SpyRando();
        spy.scripted = AiDecisionResult.wire("2", AiDecisionResult.MutationMode.NONE, "1");
        CuratorAi curator = new CuratorAi(spy);
        RejectionHistory history = RejectionHistory.empty()
                .append("0", FinalizedResponse.RejectReason.ENGINE_DECISION_INVALID, "invalid");

        curator.decideForEngine("dark", trivialDecision(), null, history);

        assertSame("Curator forwards the IDENTICAL immutable history instance", history, spy.lastHistory);
    }

    @Test
    public void typedRejectionFromWrappedRandoBypassesConsultAndReturnsUnchanged() {
        SpyRando spy = new SpyRando();
        AiDecisionResult typed = AiDecisionResult.typedRejection(
                FinalizedResponse.RejectReason.NO_LEGAL_FALLBACK, "nothing", "1");
        spy.scripted = typed;
        CuratorAi curator = new CuratorAi(spy);

        AiDecisionResult result = curator.decideForEngine("dark", trivialDecision(), null,
                RejectionHistory.empty());

        assertSame("typed rejection returned unchanged", typed, result);
    }

    @Test
    public void pureApplyOverrideReplacesWirePreservingMetadata() {
        AiDecisionResult wrapped = AiDecisionResult.wire("5", AiDecisionResult.MutationMode.OUTER_COMMON, "1");
        AiDecisionResult overridden = CuratorAi.applyOverride(wrapped, "7");

        assertEquals("7", overridden.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON, overridden.mutationMode());
        assertEquals("1", overridden.decisionId());
    }

    @Test
    public void noneOverrideForwardsTheOverrideWireAsTheAcceptedResult() {
        // Curator overrides a wrapped mode-NONE result to "9" and forwards the accepted
        // disposition; the wrapped Rando receives the OVERRIDE as the accepted result (its
        // callback records result.wireResponse() as the final/accepted wire — proven in
        // RandoCalAiLifecycleTest). Metadata (mode NONE) is preserved. No network.
        SpyRando spy = new SpyRando();
        spy.scripted = AiDecisionResult.wire("", AiDecisionResult.MutationMode.NONE, "3");
        CuratorAi curator = new CuratorAi(spy);
        AwaitingDecision decision = trivialDecision();

        AiDecisionResult randoResult = curator.decideForEngine("dark", decision, null,
                RejectionHistory.empty());
        assertEquals(AiDecisionResult.MutationMode.NONE, randoResult.mutationMode());

        AiDecisionResult overridden = CuratorAi.applyOverride(randoResult, "9");
        curator.onDecisionAccepted("dark", decision, null, overridden);

        assertEquals("accepted forwarded once", 1, spy.acceptedForwarded);
        assertEquals("wrapped Rando receives the OVERRIDE wire as the accepted result", "9",
                spy.lastAcceptedResult.wireResponse());
        assertEquals("lifecycle mode preserved as NONE", AiDecisionResult.MutationMode.NONE,
                spy.lastAcceptedResult.mutationMode());
    }
}
