package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Jedi Into Exile
 */
public class Card501_166 extends AbstractUsedOrLostInterrupt {
    public Card501_166() {
        super(Side.LIGHT, 5, "Jedi Into Exile", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("[UPLOAD] [A New Hope] Jedi Council Chamber or Lars' Moisture Farm. OR Subtract 1 from a just-drawn choke, Force Lightning or [Permanent Weapon] blaster weapon destiny. LOST: If you are about to draw battle destiny at a Padawan or Leia's site, instead use that character's ability number.");
        addIcons(Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_25);
        setTestingText("Jedi Into Exile");
        hideFromDeckBuilder();
    }
}
