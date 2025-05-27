package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Lost
 * Title: Honoring What They Fight For
 */
public class Card501_190 extends AbstractLostInterrupt {
    public Card501_190() {
        super(Side.LIGHT, 5, "Honoring What They Fight For", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Luke's experience on Dagobah gave him great skill in using the Force. Vader had to keep his focus on Luke at all times, or face the consequences.");
        setGameText("At the start of your turn, if a [Cloud City] Rebel controls a battleground, turn a card stacked on Patience! face up. OR If a [Cloud City] Rebel in battle, add one battle destiny. OR Place a [Cloud City] Rebel (except Luke) out of play from hand to cancel all battle damage against you.");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_25);
        setTestingText("Honoring What They Fight For");
    }
}
