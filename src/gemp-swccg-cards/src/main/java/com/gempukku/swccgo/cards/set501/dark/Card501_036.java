package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;

/*
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Scarif: Ocean
 */

public class Card501_036 extends AbstractSite {
    public Card501_036() {
        super(Side.DARK, "Scarif: Ocean", Title.Scarif, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Once per game, may download An Inkling Of Its Destructive Potential here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PLANET, Icon.EXTERIOR_SITE, Icon.VIRTUAL_SET_24);
        setTestingText("Scarif: Ocean");
        hideFromDeckBuilder();
    }
}
