package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: Site
 * Title: Kashyyyk: Imperial Work Settlement #121
 */
public class Card501_088 extends AbstractSite {
    public Card501_088() {
        super(Side.LIGHT, "Kashyyyk: Imperial Work Settlement #121", Title.Kashyyyk);
        setLocationDarkSideGameText("If you control, Wookiees at Kashyyyk sites are power -1.");
        setLocationLightSideGameText("While you occupy, Wookiees at Kashyyyk sites are forfeit +1.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.VIRTUAL_SET_16, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I);
        setTestingText("Kashyyyk: Imperial Work Settlement #121");
        hideFromDeckBuilder();
    }
}
