package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractJediMaster;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Jedi Master
 * Title: Qui-Gon Jinn (V)
 */
public class Card501_204 extends AbstractJediMaster {
    public Card501_204() {
        super(Side.LIGHT, 1, 6, 6, 7, 9, Title.QuiGon_Jinn, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("Power +1 for each Credit (limit +4). While at a site with [Tat] Anakin, adds one [LS] and one [DS] icon here. Anakin ignores location deployment restrictions. May [DOWNLOAD] [TAT] Anakin here (deploy -2). Immune to Dark Strike, Disarmed, and attrition.");
        setVirtualSuffix(true);
        addIcons(Icon.WARRIOR);
        setTestingText("Qui-Gon Jinn (V)");
        hideFromDeckBuilder();
    }
}
