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
 * Title: Kashyyyk: Chewie's Hut
 */

public class Card501_213 extends AbstractSite {
    public Card501_213() {
        super(Side.LIGHT, "Kashyyyk: Chewie's Hut", Title.Kashyyyk, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("Deploys only as a starting location. Once per game, may [upload] a Wookiee. While Wookiee Homestead on table, no Force Drains here.");
        setLocationDarkSideGameText("");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 0);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I, Icon.VIRTUAL_SET_26);
        setTestingText("Kashyyyk: Chewie's Hut");
    }
    
}
