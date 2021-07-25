package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;


/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Used
 * Title: Wookiee Guide
 */
public class Card501_085 extends AbstractUsedInterrupt {
    public Card501_085() {
        super(Side.LIGHT, 4, "Wookiee Guide", Uniqueness.UNIQUE);
        setLore("Chewie felt right at home in the forests of Endor, which closely resemble the environment on his homeworld of Kashyyyk.");
        setGameText("Move a non-unique Wookiee as a 'react' to a battle just initiated at an Endor, forest or Kashyyyk site. OR Deploy a Wookiee to a Kashyyyk site from Reserve Deck; reshuffle. OR Activate one Force for each Kashyyyk location you occupy.");
        addIcons(Icon.ENDOR, Icon.EPISODE_I, Icon.VIRTUAL_SET_16);
        setVirtualSuffix(true);
        setTestingText("Wookiee Guide (V)");
        hideFromDeckBuilder();
    }
}