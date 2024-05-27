package com.gempukku.swccgo.logic.modifiers;

import com.gempukku.swccgo.common.Filterable;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.logic.conditions.Condition;

/**
 * A modifier for not being able to have deploy cost decreased.
 */
public class MayNotHaveDeployCostDecreasedModifier extends AbstractModifier {

    /**
     * Creates a modifier for not being able to have deploy cost decreased.
     * @param source the source of the modifier and card affected by modifier
     */
    public MayNotHaveDeployCostDecreasedModifier(PhysicalCard source) {
        this(source, source, null, null);
    }

    /**
     * Creates a modifier for not being able to have deploy cost decreased.
     * @param source the source of the modifier
     * @param affectFilter the filter for cards that may not have deploy cost decreased
     * @param playerId the player that may not increase deploy cost
     */
    public MayNotHaveDeployCostDecreasedModifier(PhysicalCard source, Filterable affectFilter) {
        this(source, affectFilter, null, null);
    }

    /**
     * Creates a modifier for not being able to have deploy cost decreased.
     * @param source the source of the modifier
     * @param affectFilter the filter for cards that may not have deploy cost decreased
     * @param condition the condition that must be fulfilled for the modifier to be in effect
     * @param playerId the player that may not increase deploy cost
     */
    private MayNotHaveDeployCostDecreasedModifier(PhysicalCard source, Filterable affectFilter, Condition condition, String playerId) {
        super(source, "May not have deploy cost decreased", affectFilter, condition, ModifierType.MAY_NOT_HAVE_DEPLOY_COST_DECREASED, true);
        _playerId = playerId;
    }
    
}
