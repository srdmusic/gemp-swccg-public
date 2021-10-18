package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.*;

/**
 * Set: Set 17
 * Type: Effect
 * Title: An Effective Demonstration
 */
public class Card501_004 extends AbstractNormalEffect {
    public Card501_004() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "An Effective Demonstration");
        setLore("");
        setGameText("Deploy on table. [A New Hope] Epic Event destinies are +5 when targeting Alderaan. If Alderaan ‘blown away’, Death Star gains one [Light Side], and opponent's total battle destiny -1. Once per game, take Superlaser into hand from Reserve deck; reshuffle. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("An Effective Demonstration");
        hideFromDeckBuilder();
    }
}

