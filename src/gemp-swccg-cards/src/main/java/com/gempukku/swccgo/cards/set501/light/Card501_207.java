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
 * Title: Jabiim: Hangar Bay
 */

public class Card501_207 extends AbstractSite {
    public Card501_207() {
        super(Side.LIGHT, Title.Jabiim_Hangar_Bay, Title.Jabiim, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("While a Jedi Survivor present, you lose no Force to Visage Of The Emperor.");
        setLocationDarkSideGameText("Once per game, may [download] (or deploy from Lost Pile) Grand Inquisitor here.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.INTERIOR_SITE, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_26);
        setTestingText("Jabiim: Hangar Bay");
        hideFromDeckBuilder();
    }
    
}
