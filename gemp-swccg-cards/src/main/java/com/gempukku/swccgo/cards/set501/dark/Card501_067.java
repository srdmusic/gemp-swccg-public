package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;

/**
 * Set: Playtesting
 * Type: Defensive Shield
 * Title: We'll Let Fate-a Decide, Huh? (V)
 */
public class Card501_067 extends AbstractDefensiveShield {
    public Card501_067() {
        super(Side.DARK, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "We'll Let Fate-a Decide, Huh?", ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        hideFromDeckBuilder();
    }
}
