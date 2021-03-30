package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;

/**
 * Set: Set 15
 * Type: Interrupt
 * Subtype: Used
 * Title: This Is Where The Fun Begins
 */
public class Card501_035 extends AbstractUsedInterrupt {
    public Card501_035() {
        super(Side.DARK, 4, "This Is Where The Fun Begins");
        setLore("");
        setGameText("If a battle was just initiated, all of your [Republic] starfighters are power +1 and immune to attrition for the remainder of the turn. OR. If Anakin and Obi-Wan in battle together may cancel one opponent’s destiny (except battle destiny.)");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_15);
        setTestingText("This Is Where The Fun Begins");
        hideFromDeckBuilder();
    }
}