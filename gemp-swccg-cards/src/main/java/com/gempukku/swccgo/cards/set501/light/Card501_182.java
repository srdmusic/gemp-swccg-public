package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Capital
 * Title: Quantum Storm
 */
public class Card501_182 extends AbstractCapitalStarship {
    public Card501_182() {
        super(Side.LIGHT, 3, 3, 2, 4, null, 4, 5, "Quantum Storm", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        hideFromDeckBuilder();
    }
}
