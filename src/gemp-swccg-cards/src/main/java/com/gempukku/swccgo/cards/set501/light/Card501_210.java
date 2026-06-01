package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: Watch Out For That Crossfire, Boys
 */
public class Card501_210 extends AbstractUsedInterrupt {
    public Card501_210() {
        super(Side.LIGHT, 6, "Watch Out For That Crossfire, Boys", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Add 2 to the power and maneuver of your T-47 for remainder of this turn. (Interrupt may even affect the result immediately after a destiny draw targeting T-47’s maneuver.) OR Cancel High Speed Tactics. OR Move your T-47 to the 1st Marker during any move phase.");
        addIcons(Icon.VIRTUAL_SET_27);
        setTestingText("Watch Out For That Crossfire, Boys");
        hideFromDeckBuilder();
    }
}
