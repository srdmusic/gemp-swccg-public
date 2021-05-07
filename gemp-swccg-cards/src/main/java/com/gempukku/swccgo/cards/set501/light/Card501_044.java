package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;


/**
 * Set: Set 15
 * Type: Interrupt
 * Subtype: Used
 * Title: Slight Weapons Malfunction (V)
 */
public class Card501_044 extends AbstractUsedInterrupt {
    public Card501_044() {
        super(Side.LIGHT, 4, "Slight Weapons Malfunction", Uniqueness.UNIQUE);
        setLore("Han was gifted in the art of understatement.");
        setGameText("Take a Rebel stormtrooper into hand from Reserve Deck; reshuffle. OR Cancel a weapon destiny targeting a stormtrooper (or Chewie). OR If a stormtrooper just fired a non-lightsaber weapon during battle, add one battle destiny.");
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_15);
        setVirtualSuffix(true);
        setTestingText("Slight Weapons Malfunction (V)");
        hideFromDeckBuilder();
    }
}