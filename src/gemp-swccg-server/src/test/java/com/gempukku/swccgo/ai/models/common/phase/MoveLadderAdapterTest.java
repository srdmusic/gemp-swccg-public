package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MoveLadderAdapterTest {
    @Test
    public void randoLadderVetoRemainsAdditiveRatherThanHardVeto()
            throws Exception {
        com.gempukku.swccgo.ai.models.rando.evaluators.MoveEvaluator evaluator =
                new com.gempukku.swccgo.ai.models.rando.evaluators.MoveEvaluator();
        com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action =
                new com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction(
                        "1",
                        com.gempukku.swccgo.ai.models.rando.evaluators.ActionType.MOVE,
                        500.0f,
                        "Move using landspeed");

        armHardVeto(evaluator, "legacy additive veto");
        invokeFinalize(
                evaluator,
                action,
                com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction.class);

        assertEquals(-99500.0f, action.getScore(), 0.001f);
        assertFalse(action.isHardVetoed());
        assertTrue(action.getReasoningString().contains(
                "LADDER VETO: legacy additive veto"));
    }

    @Test
    public void chosenOneLadderVetoRemainsAdditiveRatherThanHardVeto()
            throws Exception {
        com.gempukku.swccgo.ai.models.chosenone.evaluators.MoveEvaluator evaluator =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.MoveEvaluator();
        com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction action =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction(
                        "1",
                        com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionType.MOVE,
                        500.0f,
                        "Move using landspeed");

        armHardVeto(evaluator, "legacy additive veto");
        invokeFinalize(
                evaluator,
                action,
                com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction.class);

        assertEquals(-99500.0f, action.getScore(), 0.001f);
        assertFalse(action.isHardVetoed());
        assertTrue(action.getReasoningString().contains(
                "LADDER VETO: legacy additive veto"));
    }

    private static void armHardVeto(Object evaluator, String reason)
            throws Exception {
        setField(evaluator, "ladderRank", 1);
        setField(evaluator, "ladderVetoHard", true);
        setField(evaluator, "ladderVetoHardReason", reason);
        setField(evaluator, "ladderCanWinVeto", false);
        setField(evaluator, "ladderBattleSeekingClaim", false);
        setField(evaluator, "ladderMandatoryTransit", false);
        setField(evaluator, "ladderWrongDirVeto", false);
        setField(evaluator, "ladderRankMoveRan", true);
    }

    private static void setField(Object target, String name, Object value)
            throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void invokeFinalize(
            Object evaluator, Object action, Class<?> actionClass)
            throws Exception {
        Method method = evaluator.getClass().getDeclaredMethod(
                "ladderFinalize", actionClass);
        method.setAccessible(true);
        method.invoke(evaluator, action);
    }
}
