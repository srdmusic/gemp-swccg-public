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
 * Title: My Sister Has It
 */
public class Card501_177 extends AbstractUsedInterrupt {
    public Card501_177() {
        super(Side.LIGHT, 5, "My Sister Has It", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("During a battle involving Leia, the number of battle destiny draws may not be limited. OR Subtract 1 from a just drawn Force Lightning or 'choke' destiny (unless targeting an Undercover spy). OR If a [Skywalker] Effect on table, [upload] Chief Chirpa's Hut or Guest Quarters.");
        addIcons(Icon.SKYWALKER, Icon.VIRTUAL_SET_25);
        setTestingText("My Sister Has It");
    }
}
