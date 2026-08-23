package com.gempukku.swccgo.logic.evaluators;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

/**
 * An interface used to represent an evaluator used to calculate a value.
 */
public interface Evaluator {
    float evaluateExpression(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard cardAffected);

    float evaluateExpression(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard cardAffected, PhysicalCard otherCard);

    /**
     * True when evaluating against a proposed target does not require the
     * source card to be attached to that target already. Implementations
     * that inspect filters must also keep those filters independent of the
     * proposed attachment relationship.
     */
    default boolean supportsProspectiveCardEvaluation() {
        return false;
    }
}
