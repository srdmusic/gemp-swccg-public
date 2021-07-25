package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: Site
 * Title: Kashyyyk: Sacred Falls (Forest)
 */
public class Card501_087 extends AbstractSite {
    public Card501_087() {
        super(Side.LIGHT, "Kashyyyk: Sacred Falls (Forest)", Title.Kashyyyk);
        setLocationDarkSideGameText("While opponent occupies with a Wookiee, your Force generation here is canceled.");
        setLocationLightSideGameText("Your Wookiees move from here for free when using landspeed.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.VIRTUAL_SET_16, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I);
        addKeyword(Keyword.FOREST);
        setTestingText("Kashyyyk: Sacred Falls (Forest)");
        hideFromDeckBuilder();
    }
}
