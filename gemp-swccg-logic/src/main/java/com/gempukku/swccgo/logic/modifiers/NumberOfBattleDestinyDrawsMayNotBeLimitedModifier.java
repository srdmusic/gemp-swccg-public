package com.gempukku.swccgo.logic.modifiers;

import com.gempukku.swccgo.common.Filterable;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.evaluators.Evaluator;

/**
 * A modifier for "The number of battle destiny draws may not be limited" at specified battle locations.
 */
public class NumberOfBattleDestinyDrawsMayNotBeLimitedModifier extends AbstractModifier {

    /**
     * Creates a "The number of battle destiny draws may not be limited" modifier.
     * @param source the source of the modifier
     * @param locationFilter the filter for battle locations
     * @param playerId the player that may not have the number of battle destinies they draw be limited
     */
    public NumberOfBattleDestinyDrawsMayNotBeLimitedModifier(PhysicalCard source, Filterable locationFilter, String playerId) {
        super(source, "Number of battle destiny draws may not be limited", Filters.and(locationFilter, Filters.battleLocation), ModifierType.BATTLE_DESTINY_DRAWS_MAY_NOT_BE_LIMITED, true);
        _playerId = playerId;
    }

    @Override
    public String getText(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard self) {
        return "Number of battle destiny draws drawn by " + _playerId + " may not be limited";
    }
}
