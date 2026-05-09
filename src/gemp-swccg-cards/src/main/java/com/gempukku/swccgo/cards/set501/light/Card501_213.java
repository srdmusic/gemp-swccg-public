package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtestig
 * Type: Interrupt
 * Subtype: Used
 * Title: The Force Will Be With You, Always
 */
public class Card501_213 extends AbstractUsedInterrupt {
    public Card501_213() {
        super(Side.LIGHT, 4, "The Force Will Be With You, Always", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("You can't win Darth. If you strike me down I shall become more powerful than you can possibly imagine.' Obi-Wan's sacrifice gave the Rebels time to escape.");
        setGameText("If a Jedi ‘communing’, choose: [UPLOAD] Coruscant: Jedi Temple. OR Take your just drawn destiny into hand to cancel and redraw that destiny. OR Draw bottom card of your Force pile.");
        addIcons(Icon.VIRTUAL_SET_27);
        setTestingText("The Force Will Be With You, Always");
        hideFromDeckBuilder();
    }
}
