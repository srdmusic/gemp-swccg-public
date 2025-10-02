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
 * Title: Coruscant (V)
 */
public class Card501_065 extends AbstractSystem {
    public Card501_065() {
        super(Side.DARK, Title.Coruscant, 0, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Invisible Hand is hyperspeed +3 moving to or from here. Your [Episode I] pilots deploy -1 here.");
        setLocationLightSideGameText("If Insidious Prisoner on table, unless a Jedi here, your total battle destiny -1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PLANET, Icon.EPISODE_I, Icon.VIRTUAL_SET_26);
        setVirtualSuffix(true);
        setTestingText("Coruscant (V)");
    }
    
}
