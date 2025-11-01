package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractStartingInterrupt;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Set 26
 * Type: Interrupt
 * Subtype: Starting
 * Title: Something About This Boy
 */

public class Card501_216 extends AbstractStartingInterrupt {
    public Card501_216() {
        super(Side.LIGHT, 3, "Something About This Boy", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'What'd he mean by that?' 'I'll tell you later.'");
        setGameText("If your starting location was City Outskirts, deploy Slave Quarters (with Prophecy Of The Force there), Jedi Business and Your Thoughts Dwell On Your Mother. Light Side goes first. When you draw your starting hand, draw only 6 cards. Place Interrupt in Lost Pile.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_26);
        setTestingText("Something About This Boy");
    }
    
}
