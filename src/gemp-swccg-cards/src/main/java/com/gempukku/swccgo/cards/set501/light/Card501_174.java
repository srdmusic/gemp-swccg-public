package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Playtesting
 * Type: Starship
 * Subtype: Capital
 * Title: Liberator
 */

public class Card501_174 extends AbstractCapitalStarship {
    public Card501_174() {
        super(Side.LIGHT, 2, 4, 5, 4, null, 3, 7, "Liberator", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Phoenix Squadron.");
        setGameText("May add 3 pilots and 4 passengers. Permanent pilot provides ability of 2. Phoenix Squadron pilots deploy -1 aboard. While at opponent’s battleground system, Force drains may not be modified or canceled here.");
        addIcons(Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_24);
        addIcon(Icon.PILOT, 1);
        addKeywords(Keyword.PHOENIX_SQUADRON);
        addModelType(ModelType.CORELLIAN_CORVETTE);
        setPilotCapacity(3);
        setPassengerCapacity(4);
        setTestingText("Liberator");
        hideFromDeckBuilder();
    }
}
