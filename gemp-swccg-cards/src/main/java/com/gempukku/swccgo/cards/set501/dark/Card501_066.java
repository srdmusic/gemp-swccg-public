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
 * Title: Death Star Sentry & A Useless Gesture
 */
public class Card501_066 extends AbstractDefensiveShield {
    public Card501_066() {
        super(Side.DARK, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Death Star Sentry & A Useless Gesture", ExpansionSet.PLAYTESTING, Rarity.V);
        addComboCardTitles(Title.Death_Star_Sentry, Title.A_Useless_Gesture);
        hideFromDeckBuilder();
    }
}
