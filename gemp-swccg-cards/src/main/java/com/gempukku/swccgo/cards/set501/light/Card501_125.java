package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Rebel
 * Title: Corran Horn, Jedi
 */

public class Card501_125 extends AbstractRebel {
    public Card501_125() {
        super(Side.LIGHT, 1, 5, 4, 6, 6, "Corran Horn, Jedi", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        excludeFromDeckBuilder();
    }
}
