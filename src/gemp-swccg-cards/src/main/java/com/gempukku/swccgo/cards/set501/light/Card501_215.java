package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Jedi Master
 * Title: Kelnacca
 */

public class Card501_215 extends AbstractJediMaster {
    public Card501_215() {
        super(Side.LIGHT, 2, 5, 5, 7, 7, "Kelnacca", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Wookiee.");
        setGameText("At the start of each turn, if Wookiee Homestead on table, may place a card from hand on bottom of Force Pile to draw top card from Force Pile. Once per game, may [download] a unique (•) Forest. Immune to Wookiee Strangle and You Are Beaten.");
        addIcons(Icon.EPISODE_I, Icon.WARRIOR, Icon.VIRTUAL_SET_26);
        setSpecies(Species.WOOKIEE);
        setTestingText("Kelnacca");
        hideFromDeckBuilder();
    }
    
}
