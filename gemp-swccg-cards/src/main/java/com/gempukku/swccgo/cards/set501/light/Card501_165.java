package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Droid
 * Title: BB-8, Keeper of the Map
 */
public class Card501_165 extends AbstractDroid {
    public Card501_165() {
        super(Side.LIGHT, Math.PI, 2, 1, 4, "BB-8, Keeper of the Map", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        hideFromDeckBuilder();
    }
}
