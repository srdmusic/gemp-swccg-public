package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActivateActionPolicyTest {
    @Test
    public void topLevelKeepsTheThreeCardBattleBuffer() {
        assertEvaluation(ActivateActionPolicy.topLevel("A", 3, true),
                ActivateActionPolicy.Mode.TOP_LEVEL_KEEP_BUFFER,
                "V61c-activate-choice", TraceOutputKind.VETO, -6000.0f,
                "V61c DESTINY BUFFER: reserve <= 3 — pass activation, keep 3 for destiny");
    }

    @Test
    public void topLevelActivatesWhenNoBattleIsPlausible() {
        assertEvaluation(ActivateActionPolicy.topLevel("A", 3, false),
                ActivateActionPolicy.Mode.TOP_LEVEL_ACTIVATE_WITHOUT_BATTLE,
                "V168-activate-choice", TraceOutputKind.ORDERING, 5000.0f,
                "V168 ALWAYS ACTIVATE: never pass Force activation while Force can be activated");
    }

    @Test
    public void zeroConfirmationHonorsTheBattleBuffer() {
        assertEvaluation(ActivateActionPolicy.zeroConfirmation("Y", "yes", 3, true),
                ActivateActionPolicy.Mode.CONFIRM_KEEP_BUFFER,
                "V61c-confirm-pass", TraceOutputKind.VETO, 9999.0f,
                "V61c DESTINY BUFFER: reserve <= 3 — confirm pass, keep 3 for destiny");
        assertEvaluation(ActivateActionPolicy.zeroConfirmation("N", "no", 3, true),
                ActivateActionPolicy.Mode.REJECT_BUFFER_REACTIVATION,
                "V61c-reject-reactivation", TraceOutputKind.VETO, -9999.0f,
                "V61c DESTINY BUFFER: reserve <= 3 — do not go back and activate");
    }

    @Test
    public void zeroConfirmationReturnsToActivationWithoutABattle() {
        assertEvaluation(ActivateActionPolicy.zeroConfirmation("N", "no", 3, false),
                ActivateActionPolicy.Mode.CONFIRM_REACTIVATION_WITHOUT_BATTLE,
                "V38.3-confirm-reactivation", TraceOutputKind.VETO, 9999.0f,
                "V38.3 MUST ACTIVATE: Go back and activate Force!");
        assertEvaluation(ActivateActionPolicy.zeroConfirmation("Y", "yes", 3, false),
                ActivateActionPolicy.Mode.REJECT_SKIP,
                "V38.3-reject-skip", TraceOutputKind.VETO, -9999.0f,
                "V38.3 NEVER SKIP ACTIVATION: Do not pass without activating!");
    }

    @Test
    public void exactActivateActionKeepsItsIndependentBaseContribution() {
        assertEvaluation(ActivateActionPolicy.alwaysActivate("A"),
                ActivateActionPolicy.Mode.ALWAYS_ACTIVATE,
                "V38.3-activate-base", TraceOutputKind.ORDERING, 500.0f,
                "V38.3 ALWAYS ACTIVATE: Force is currency — activate it!");
    }

    private static void assertEvaluation(ActivateActionPolicy.Evaluation evaluation,
                                         ActivateActionPolicy.Mode mode,
                                         String ruleId,
                                         TraceOutputKind outputKind,
                                         float delta,
                                         String reason) {
        assertEquals(mode, evaluation.mode());
        assertEquals("ACTIVATE_ACTION_POLICY", evaluation.result().producerId());
        assertEquals(1, evaluation.result().operations().size());
        PolicyOperation operation = evaluation.result().operations().get(0);
        assertEquals(ruleId, operation.ruleArmId().id());
        assertEquals(outputKind, operation.outputKind());
        assertEquals(Float.floatToRawIntBits(delta),
                Float.floatToRawIntBits(operation.delta()));
        assertEquals(reason, operation.reason());
    }
}
