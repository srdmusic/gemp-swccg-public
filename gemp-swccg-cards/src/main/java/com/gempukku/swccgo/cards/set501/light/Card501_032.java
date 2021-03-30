package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Set 15
 * Type: Location
 * Subtype: Site
 * Title: Kashyyyk: Kachirho
 */
public class Card501_032 extends AbstractSite {
    public Card501_032() {
        super(Side.LIGHT, "Kashyyyk: Kachirho", Title.Kashyyyk);
        setLocationDarkSideGameText("Total ability of 6 or more required for you to draw battle destiny here.");
        setLocationLightSideGameText("While you occupy with a Wookiee, may deploy a Kashyyyk location from your reserve deck; reshuffle.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.VIRTUAL_SET_15, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I);
        setTestingText("Kashyyyk: Kachirho");
        hideFromDeckBuilder();
    }
}
