package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;

/**
 * Set: Playtesting
 * Type: Defensive Shield
 * Title: A Close Race (V)
 */
public class Card501_180 extends AbstractDefensiveShield {
    public Card501_180() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "A Close Race", ExpansionSet.PLAYTESTING, Rarity.V);
        hideFromDeckBuilder();
    }
}
