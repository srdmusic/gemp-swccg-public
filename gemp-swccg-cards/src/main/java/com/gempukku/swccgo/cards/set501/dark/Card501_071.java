package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Playtesting
 * Type: Defensive Shield
 * Title: You Cannot Hide Forever (V)
 */
public class Card501_071 extends AbstractDefensiveShield {
    public Card501_071() {
        super(Side.DARK, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.You_Cannot_Hide_Forever, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        hideFromDeckBuilder();
    }
}
