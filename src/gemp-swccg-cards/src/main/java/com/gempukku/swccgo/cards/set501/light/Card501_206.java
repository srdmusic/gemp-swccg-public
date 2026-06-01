package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Jedi Master
 * Title: Yoda, Hopeful Jedi
 */
public class Card501_206 extends AbstractJediMaster {
    public Card501_206() {
        super(Side.LIGHT, 3, 4, 3, 7, 7, "Yoda, Hopeful Jedi", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'You must feel the Force around you. Here, between you, me, the tree, the rock, everywhere! Yes, even between the land and the ship.'");
        setGameText("Power +1 for each Padawan on table. Battle destiny draws may not be canceled where you have a Jedi. Your total battle destiny is +1 for each of opponent’s completed battle destiny draw here. Immune to attrition.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_27);
        setTestingText("Yoda, Hopeful Jedi");
        hideFromDeckBuilder();
    }
}
