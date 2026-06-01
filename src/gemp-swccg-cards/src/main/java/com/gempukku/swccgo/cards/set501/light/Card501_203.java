package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Interrupt
 * SubType: Used
 * Title: A New Hope
 */
public class Card501_203 extends AbstractUsedInterrupt {
    public Card501_203() {
        super(Side.LIGHT, 4, Title.A_New_Hope, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("The heroes of the Rebellion know that where there is life, there is hope.");
        setGameText("[UPLOAD] Luke (or a Lars) or Leia (or Bail) with a printed deploy < 4. OR For remainder of turn, opponent may not cancel weapon destiny draws at sites or battle destiny draws. OR Where you have a lone Rebel, draw one battle destiny if unable to otherwise.");
        addIcons(Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_27);
        setTestingText("A New Hope");
        hideFromDeckBuilder();
    }
}
