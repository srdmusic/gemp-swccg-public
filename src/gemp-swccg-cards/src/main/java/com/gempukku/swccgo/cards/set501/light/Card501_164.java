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
 * Title: Coruscant: Jedi Council Chamber (V)
 */
public class Card501_164 extends AbstractSite {
    public Card501_164() {
        super(Side.LIGHT, Title.Jedi_Council_Chamber, Title.Coruscant, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("[Episode I] Vader may deploy here regardless of presence of Force icons.");
        setLocationLightSideGameText("Deploys only if you have deployed a battleground or a Jedi 'communing.'");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.A_NEW_HOPE, Icon.INTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Coruscant: Jedi Council Chamber (V)");
        hideFromDeckBuilder();
    }
}