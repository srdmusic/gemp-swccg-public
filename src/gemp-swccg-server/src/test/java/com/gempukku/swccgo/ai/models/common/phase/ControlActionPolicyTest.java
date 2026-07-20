package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ControlActionPolicyTest {
    @Test
    public void noEscapeRetrievalKeepsItsExactContribution() {
        assertOperation(ControlActionPolicy.noEscapeRetrieval("A"),
                "V29.14-noescape-retrieval", TraceOutputKind.BANDED, 200.0f,
                "V29.14 NO ESCAPE: Free card from Lost Pile — always take it!");
    }

    @Test
    public void forceDrainModifierKeepsItsExactContribution() {
        assertOperation(ControlActionPolicy.forceDrainModifier("A"),
                "V24.2-drain", TraceOutputKind.BANDED, 80.0f,
                "V24.2 FORCE DRAIN BONUS: +1 to force drain — always use!");
    }

    @Test
    public void selfCancelRemainsAnAdditiveHistoricalVeto() {
        String reason = "V52 NEVER SELF-CANCEL DRAIN: Canceling own force drain is suicide!";
        assertOperation(ControlActionPolicy.selfCancelDrain("A", reason),
                "V52-self-cancel", TraceOutputKind.VETO, -9999.0f, reason);
    }

    @Test
    public void revealBoundaryIsSixThenSeven() {
        assertOperation(ControlActionPolicy.revealOpponentHand("A", 6),
                "CONTROL-reveal-opponent-hand", TraceOutputKind.ORDERING,
                -50.0f, "Opponent has few cards - save reveal");
        assertOperation(ControlActionPolicy.revealOpponentHand("A", 7),
                "CONTROL-reveal-opponent-hand", TraceOutputKind.ORDERING,
                50.0f, "Opponent has many cards - reveal worth it");
    }

    @Test
    public void retrieveBoundaryIsFifteenThenSixteen() {
        assertOperation(ControlActionPolicy.retrieve("A", 15),
                "CONTROL-retrieve", TraceOutputKind.ORDERING,
                -30.0f, "Low lost pile - save retrieve");
        assertOperation(ControlActionPolicy.retrieve("A", 16),
                "CONTROL-retrieve", TraceOutputKind.ORDERING,
                30.0f, "High lost pile - retrieve worth it");
    }

    @Test
    public void fixedUtilityArmsKeepExactAdditiveScores() {
        assertOperation(ControlActionPolicy.makeOpponentLose("A"),
                "CONTROL-make-opponent-lose", TraceOutputKind.ORDERING,
                30.0f, "Making opponent lose force");
        assertOperation(ControlActionPolicy.peekAtTop("A"),
                "CONTROL-peek-at-top", TraceOutputKind.ORDERING,
                30.0f, "Peek for card advantage");
        assertOperation(ControlActionPolicy.steal("A"),
                "CONTROL-steal", TraceOutputKind.ORDERING,
                30.0f, "Stealing is good");
        assertOperation(ControlActionPolicy.dangerousCard("A"),
                "CONTROL-dangerous-card", TraceOutputKind.ORDERING,
                -50.0f, "Known dangerous card");
    }

    private static void assertOperation(PolicyResult result, String ruleId,
                                        TraceOutputKind outputKind, float delta,
                                        String reason) {
        assertEquals("CONTROL_ACTION_POLICY", result.producerId());
        assertEquals(1, result.operations().size());
        PolicyOperation operation = result.operations().get(0);
        assertEquals("A", operation.actionId());
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(TraceDomainId.DRAIN_CONTROL, operation.domainId());
        assertEquals(outputKind, operation.outputKind());
        assertEquals(PolicyOperationKind.ADD, operation.kind());
        assertEquals(Float.floatToRawIntBits(delta),
                Float.floatToRawIntBits(operation.delta()));
        assertEquals(reason, operation.reason());
    }
}
