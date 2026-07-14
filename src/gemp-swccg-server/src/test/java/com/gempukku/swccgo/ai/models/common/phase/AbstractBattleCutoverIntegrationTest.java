package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceFinalization;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceStatus;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerRecordResponseEvent;
import com.gempukku.swccgo.common.BattleDecisionWire;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import org.junit.After;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Both-bot boundary proof for the typed optional-forfeit repair. */
public abstract class AbstractBattleCutoverIntegrationTest {
    protected static final String PLAYER = "battle-tester";

    public record Capture(AiDecisionResult result, DecisionTrace trace) {
    }

    protected abstract Capture runAccepted(AwaitingDecision decision,
                                             GameState gameState);

    @After
    public void clearAnyLeakedTrace() {
        if (TraceSession.isActive()) {
            TraceSession.abandon();
        }
    }

    @Test
    public void optionalImmuneForfeitFinalizesEmptyOnceWithoutEvaluatorLane() {
        Capture capture = runAccepted(optionalForfeitDecision(), battleState());

        AiDecisionResult result = capture.result();
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals("", result.wireResponse());
        assertTrue(result.fromTypedFinalizer());
        assertNotNull(result.trackerMutation());

        DecisionTrace trace = capture.trace();
        assertEquals(TraceStatus.COMPLETE, trace.getStatus());
        assertTrue(trace.getCaptureFailures().isEmpty());
        assertEquals(TraceRoute.BATTLE_TACTIC, trace.getRoute().selected());
        assertTrue(trace.getMergeOrder().isEmpty());
        assertTrue(trace.getOperations().isEmpty());
        TraceFinalization finalization = trace.getFinalization();
        assertEquals("", finalization.finalResponse());
        assertEquals(TraceFinalization.Disposition.ENGINE_ACCEPTED,
                finalization.disposition());
        assertTrue(finalization.acceptedMutationCompleted());
        assertFalse(finalization.skippedCommonFinalizer());
        assertEquals(1L, trace.getStateEvents().stream()
                .filter(TrackerRecordResponseEvent.class::isInstance).count());
        assertFalse(TraceSession.isActive());
    }

    private static GameState battleState() {
        return AbstractActivateControlDecisionHarnessTest.stubWithTurnPlayer(
                Phase.BATTLE, PLAYER);
    }

    private static AwaitingDecision optionalForfeitDecision() {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put(DecisionOrigin.WIRE_PARAMETER,
                new String[]{DecisionOrigin.BATTLE_FORFEIT.name()});
        params.put("cardId", new String[]{"501", "502"});
        params.put("min", new String[]{"0"});
        params.put("max", new String[]{"1"});
        params.put(BattleDecisionWire.OPTIONAL_IMMUNE_FORFEIT,
                new String[]{"true"});
        return new AwaitingDecision() {
            @Override
            public int getAwaitingDecisionId() {
                return 901;
            }

            @Override
            public String getText() {
                return "Choose a card from battle to forfeit (if desired)";
            }

            @Override
            public AwaitingDecisionType getDecisionType() {
                return AwaitingDecisionType.CARD_SELECTION;
            }

            @Override
            public Map<String, String[]> getDecisionParameters() {
                return params;
            }

            @Override
            public void decisionMade(String result) throws DecisionResultInvalidException {
            }
        };
    }
}
