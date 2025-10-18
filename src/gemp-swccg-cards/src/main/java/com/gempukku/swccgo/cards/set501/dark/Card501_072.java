package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: System
 * Title: Bespin (V)
 * Errata E1 of Card223_008.java
 */

public class Card501_072 extends AbstractSystem {
    public Card501_072() {
        super(Side.DARK, Title.Bespin, 6, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Executor may not move from here unless Vader aboard. If your [Cloud City] objective on table, Executor is deploy = 7 here.");
        setLocationLightSideGameText("You lose no more than 2 Force to Cloud City Occupation. Intensify the Forward Batteries is canceled.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.SPECIAL_EDITION, Icon.PLANET, Icon.VIRTUAL_SET_23);
        setVirtualSuffix(true);
        setTestingText("Bespin (V)");
    }
    
}
