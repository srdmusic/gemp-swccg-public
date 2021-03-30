package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Set 15
 * Type: Location
 * Subtype: Site
 * Title: Kashyyyk: Forest Depths
 */
public class Card501_031 extends AbstractSite {
    public Card501_031() {
        super(Side.LIGHT, "Forest Depths", Title.Kashyyyk);
        setLocationDarkSideGameText("No starships or vehicles here.");
        setLocationLightSideGameText("While you control, Wookiees are destiny +1.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.VIRTUAL_SET_15, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I);
        setTestingText("Kashyyyk: Forest Depths");
        hideFromDeckBuilder();
    }
}
