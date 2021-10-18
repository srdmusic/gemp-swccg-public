package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Courage Of A Skywalker (V)
 */
public class Card501_018 extends AbstractUsedOrLostInterrupt {
    public Card501_018() {
        super(Side.LIGHT, 2, "Courage Of A Skywalker", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Despite being alone, trapped and desperately outmatched, Luke continued his battle with the Dark Lord of the Sith.");
        setGameText("USED: Peek at the top 2 cards (3 if Luke on table) of your Reserve Deck and take one into hand; reshuffle. LOST: Once per game, at the start of any battle phase, initiate a battle between Luke and opponent's character present. Loser takes no battle damage.");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_17);
        setTestingText("Courage Of A Skywalker (V)");
        hideFromDeckBuilder();
    }
}