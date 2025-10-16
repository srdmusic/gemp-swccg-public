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
  * Title: I'm Sorry (V)
 */

public class Card501_067 extends AbstractNormalEffect {
    public Card501_067() {
        super(Side.DARK, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Im_Sorry, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'I'm sorry, too.'");
        setGameText("If your [Cloud City] objective on table, deploy on table. You may not play Imperial Barrier. Once per turn, may [download] an interior Cloud City site (or Lando to Dining Room). Your unique (•) characters of ability < 4 are forfeit +2 (limit +2). [Immune to Alter.]");
        addIcons(Icon.TATOOINE, Icon.CLOUD_CITY, Icon.VIRTUAL_SET_26);
        addImmuneToCardTitle(Title.Alter);
        setVirtualSuffix(true);
        setTestingText("I'm Sorry (V)");
        hideFromDeckBuilder();
    }
    
}
