package com.gempukku.swccgo.logic.modifiers;

import com.gempukku.swccgo.common.Filterable;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.conditions.CanAddToPowerWhenPilotingCondition;
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.evaluators.Evaluator;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

/**
 * A modifier that adds power to cards the specified card is piloting.
 */
public class AddsPowerToPilotedBySelfModifier extends PowerModifier {
    private final Evaluator _prospectiveEvaluator;
    private final Filter _prospectiveTargetFilter;

    /**
     * Creates a modifier that adds power to anything the source card is piloting.
     * @param source the source of the modifier
     * @param modifierAmount the amount of the modifier
     */
    public AddsPowerToPilotedBySelfModifier(PhysicalCard source, int modifierAmount) {
        this(source, new ConstantEvaluator(modifierAmount));
    }

    /**
     * Creates a modifier that adds power to anything the source card is piloting.
     * @param source the source of the modifier
     * @param evaluator the evaluator that calculates the amount of the modifier
     */
    public AddsPowerToPilotedBySelfModifier(PhysicalCard source, Evaluator evaluator) {
        super(source, Filters.hasPiloting(source), new CanAddToPowerWhenPilotingCondition(source), evaluator);
        _prospectiveEvaluator = evaluator;
        _prospectiveTargetFilter = Filters.any;
    }

    /**
     * Creates a modifier that adds power to cards accepted by the filter that the source card is piloting.
     * @param source the source of the modifier
     * @param modifierAmount the amount of the modifier
     * @param filter the filter for cards piloted by source card whole power is modified
     */
    public AddsPowerToPilotedBySelfModifier(PhysicalCard source, int modifierAmount, Filterable filter) {
        this(source, new ConstantEvaluator(modifierAmount), filter);
    }

    /**
     * Creates a modifier that adds power to cards accepted by the filter that the source card is piloting.
     * @param source the source of the modifier
     * @param evaluator the evaluator that calculates the amount of the modifier
     * @param filter the filter for cards piloted by source card whole power is modified
     */
    public AddsPowerToPilotedBySelfModifier(PhysicalCard source, Evaluator evaluator, Filterable filter) {
        super(source, Filters.and(filter, Filters.hasPiloting(source)), new CanAddToPowerWhenPilotingCondition(source), evaluator);
        _prospectiveEvaluator = evaluator;
        _prospectiveTargetFilter = Filters.and(filter);
    }

    /**
     * Returns this card-text modifier's intrinsic power addition for the
     * proposed piloted card. Null means the evaluator depends on board state
     * created by attachment and cannot be projected without mutating state.
     * Runtime suppression and power-increase limits are intentionally outside
     * this intrinsic source fact.
     */
    public Float getProspectiveIntrinsicPowerModifier(
            GameState gameState, ModifiersQuerying modifiersQuerying,
            PhysicalCard proposedPilotedCard) {
        if (!_prospectiveTargetFilter.accepts(
                gameState, modifiersQuerying, proposedPilotedCard)) {
            return 0.0f;
        }
        if (!_prospectiveEvaluator.supportsProspectiveCardEvaluation()) {
            return null;
        }
        float amount = _prospectiveEvaluator.evaluateExpression(
                gameState, modifiersQuerying, proposedPilotedCard);
        return Float.isFinite(amount) ? amount : null;
    }
}
