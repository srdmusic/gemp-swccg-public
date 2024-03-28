package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;

/**
 * Set: Playtesting
 * Type: Defensive Shield
 * Title: Your Ship? (V)
 */
public class Card501_190 extends AbstractDefensiveShield {
    public Card501_190() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Your Ship?", ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        hideFromDeckBuilder();
    }
}
