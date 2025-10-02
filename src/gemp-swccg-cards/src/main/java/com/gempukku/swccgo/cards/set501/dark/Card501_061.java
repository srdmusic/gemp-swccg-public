package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Information Exchange (V)
 */
public class Card501_061 extends AbstractNormalEffect {
    public Card501_061() {
        super(Side.DARK, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Information_Exchange, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'Chisa nyooda ishaley. Kun Jabba neguda len malta.' 'Ikkit ui! Yobbit, yobbit. Nelan tui ke bada.'");
        setGameText("Deploy on table. Black Sun agents are defense value +1 and forfeit +1. Once per turn, if you just deployed an information broker, may exchange the top card of Force Pile with any one card in hand. Guri and [Reflections II] Emperor deploy -2 and move for free. [Immune to Alter.]");
        addIcons(Icon.JABBAS_PALACE, Icon.REFLECTIONS_II, Icon.VIRTUAL_SET_26);
        setVirtualSuffix(true);
        setTestingText("Information Exchange (V)");
    }
    
}
