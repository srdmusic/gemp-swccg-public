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
 * Title: Mapuzo: Underground Corridor
 */

public class Card501_205 extends AbstractSite {
    public Card501_205() {
        super(Side.LIGHT, Title.Underground_Corridor, Title.Mapuzo, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("During your move phase, Jedi Survivors may move from here to a Jabiim site.");
        setLocationDarkSideGameText("While Gather Allies and Train on table, Force drain +1 here.");
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcon(Icon.DARK_FORCE, 0);
        addIcons(Icon.UNDERGROUND, Icon.INTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_26);
        setTestingText("Mapuzo: Underground Corridor");
    }
    
}
