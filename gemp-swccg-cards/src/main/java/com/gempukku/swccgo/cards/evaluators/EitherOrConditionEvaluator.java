package com.gempukku.swccgo.cards.evaluators;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.evaluators.BaseEvaluator;
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;

/**
 * An evaluator that returns the specified values if the specified conditions are fulfilled
 * default value.
 */
public class EitherOrConditionEvaluator extends BaseEvaluator {
    private Evaluator _condition1Fulfilled;
    private Evaluator _condition2Fulfilled;
    private Condition _condition1;
    private Condition _condition2;

    public EitherOrConditionEvaluator(Condition condition1, int value1, Condition condition2, int value2) {
        _condition1 = condition1;
        _condition1Fulfilled = new ConstantEvaluator(value1);
        _condition2 = condition2;
        _condition2Fulfilled = new ConstantEvaluator(value2);
    }

    @Override
    public float evaluateExpression(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard self) {
        if (_condition1.isFulfilled(gameState, modifiersQuerying))
            return _condition1Fulfilled.evaluateExpression(gameState, modifiersQuerying, self);
        else if (_condition2.isFulfilled(gameState, modifiersQuerying))
            return _condition2Fulfilled.evaluateExpression(gameState, modifiersQuerying, self);
        return 0;
    }
}
