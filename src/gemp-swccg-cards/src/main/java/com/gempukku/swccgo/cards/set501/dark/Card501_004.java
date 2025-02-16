package com.gempukku.swccgo.cards.set501.dark;

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
 * Title: Tatooine: Desert Heart (V)
 */
public class Card501_004 extends AbstractSite {
    public Card501_004() {
        super(Side.DARK, Title.Desert_Heart, Title.Tatooine, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("If you occupy this site (or control Audience Chamber), may raise your converted [JP] site to the top.");
        setLocationLightSideGameText("Unless you occupy, you must first use 1 Force to deploy a non-alien character here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PLANET, Icon.EXTERIOR_SITE, Icon.PREMIUM, Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Tatooine: Desert Heart (V)");
        hideFromDeckBuilder();
    }
}
