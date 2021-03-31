package com.gempukku.swccgo.logic.modifiers;

import com.gempukku.swccgo.common.Filterable;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.timing.GuiUtils;

public class ResetForceDrainModifier extends AbstractModifier {
    private Evaluator _evaluator;

    public ResetForceDrainModifier(PhysicalCard source, Filterable affectFilter, float modifier) {
        this(source, affectFilter, null, modifier);
    }

    public ResetForceDrainModifier(PhysicalCard source, Filterable affectFilter, Condition condition, float modifier) {
        this(source, affectFilter, condition, new ConstantEvaluator(modifier));
    }

    public ResetForceDrainModifier(PhysicalCard source, Filterable affectFilter, Condition condition, Evaluator evaluator) {
        super(source, null, affectFilter, condition, ModifierType.UNMODIFIABLE_FORCE_DRAIN_AMOUNT, false);
        _evaluator = evaluator;
    }

    @Override
    public String getText(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard self) {
        final float value = _evaluator.evaluateExpression(gameState, modifiersQuerying, self);
        return "Force Drain = " + GuiUtils.formatAsString(value);
    }

    @Override
    public float getUnmodifiableAbility(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard physicalCard) {
        return _evaluator.evaluateExpression(gameState, modifiersQuerying, physicalCard);
    }
}
