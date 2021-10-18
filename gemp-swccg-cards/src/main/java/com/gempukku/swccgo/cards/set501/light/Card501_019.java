package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Glancing Blow (V)
 */
public class Card501_019 extends AbstractUsedOrLostInterrupt {
    public Card501_019() {
        super(Side.LIGHT, 3, "Glancing Blow", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("It had been decades since Vader had felt the sting of an enemy's blade.");
        setGameText("USED: If your non-[PW] character just 'hit' opponent's character of equal or greater ability with a lightsaber, opponent's character may not fire weapons this turn (if 'hit' by Luke, character is also power -3). LOST: Cancel the game text of a 'hit' character.");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_17);
        setTestingText("Glancing Blow (V)");
        hideFromDeckBuilder();
    }
}