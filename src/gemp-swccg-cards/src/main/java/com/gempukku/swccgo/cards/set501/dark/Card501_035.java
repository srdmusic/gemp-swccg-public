package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUniqueStarshipSite;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Invisible Hand: Observation Platform
 */
public class Card501_035 extends AbstractUniqueStarshipSite {
    public Card501_035() {
        super(Side.DARK, "Invisible Hand: Observation Platform", Persona.INVISIBLE_HAND, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("If you just took a card into hand with A Valuable Hostage, retrieve 1 Force.");
        setLocationLightSideGameText("If you occupy, cancel Observation Platform text.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.INTERIOR_SITE, Icon.MOBILE, Icon.STARSHIP_SITE, Icon.SCOMP_LINK, Icon.EPISODE_I, Icon.VIRTUAL_SET_27);
        setTestingText("Invisible Hand: Observation Platform");
        hideFromDeckBuilder();
    }
}
