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
 * Title: Endor: Rebel Landing Site (Forset) (V)
 */
public class Card501_160 extends AbstractSite{
    public Card501_160() {
        super(Side.LIGHT, Title.Rebel_Landing_Site, Title.Endor, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Unless you occupy, you must first use 1 Force to deploy a non-Sout character here.");
        setLocationLightSideGameText("Your scounts of ability <5 are deploy -1 (and are defense value +2) here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.PLANET, Icon.EXTERIOR_SITE, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Endor: Rebel Landing Site (Forest) (V)");
        hideFromDeckBuilder();
    }
}
