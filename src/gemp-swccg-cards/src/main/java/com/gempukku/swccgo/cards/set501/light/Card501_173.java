package com.gempukku.swccgo.cards.set501.light;

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
 * Title: Tatooine: Dune Sea (V)
 */

public class Card501_173 extends AbstractSite {
    public Card501_173() {
        super(Side.LIGHT, Title.Dune_Sea, Title.Tatooine, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("While Stolen Data Tapes here, Obi-Wan deploys -3 here.");
        setLocationDarkSideGameText("Total ability of 6 required for you to draw battle destiny here.");
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.PLANET, Icon.EXTERIOR_SITE, Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_24);
        setVirtualSuffix(true);
        setTestingText("Tatooine: Dune Sea (V)");
        hideFromDeckBuilder();
    }
}
