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
 * Title: Orrimaarko (V)
 */

public class Card501_089 extends AbstractRebel {
    public Card501_089() {
        super(Side.LIGHT, 1, 4, 4, 4, 5, "Orrimaarko (V)", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setTestingText("Orrimaarko (V)");
        excludeFromDeckBuilder();
    }
}
