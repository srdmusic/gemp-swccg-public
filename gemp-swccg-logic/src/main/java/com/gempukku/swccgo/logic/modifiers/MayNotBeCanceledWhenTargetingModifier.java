package com.gempukku.swccgo.logic.modifiers;

import com.gempukku.swccgo.common.Filterable;
import com.gempukku.swccgo.game.PhysicalCard;

/*
 * A modifier that prevents cards from being canceled when targeting cards meeting a certain criteria.
 */
public class MayNotBeCanceledWhenTargetingModifier extends AbstractModifier {
    
    /*
     * Creates a modifier that prevents cards accepted by affectFilter from being canceled when targeting cards accepted by targetFilter.
     * @param source the source of the modifier
     * @param affectFilter the filter describing the card(s) that may not be canceled
     * @param targetFilter the filter describing the card(s) that are the targets of the affected cards
     */
    public MayNotBeCanceledWhenTargetingModifier(PhysicalCard source, Filterable affectFilter, Filterable targetFilter) {
        super(source, "May not be canceled", affectFilter, );
    }
}
