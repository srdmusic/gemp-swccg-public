package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Mapuzo: Safehouse
 */

public class Card501_204 extends AbstractSite {
    public Card501_204() {
        super(Side.LIGHT, Title.Safehouse, Title.Mapuzo, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("Jedi Survivors move to here for free. Once per game, may [download] Tala Durith here.");
        setLocationDarkSideGameText("Unless Third Sister here, Force drain -1 here.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_26);
        setTestingText("Mapuzo: Safehouse");
        hideFromDeckBuilder();
    }
    
}
