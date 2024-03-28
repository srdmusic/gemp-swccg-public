package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;

/**
 * Set: Playtesting
 * Type: Defensive Shield
 * Title: You've Never Won A Race? (V)
 */
public class Card501_068 extends AbstractDefensiveShield {
    public Card501_068() {
        super(Side.DARK, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "You've Neve Won A Race?", ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        hideFromDeckBuilder();
    }
}
