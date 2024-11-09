package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Starfighter
 * Title: Starspeeder 3000
 */
public class Card501_195 extends AbstractStarfighter {
    public Card501_195() {
        super(Side.LIGHT, 4, 2, 2, null, 3, 6, 4, "Starspeeder 3000", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May add 6 passengers. Permanent pilot provides no ability. Once per game, during your move phase, may use 2 Force to relocate (even if landed). Has ship-docking capability. May deploy to exterior sites.");
        addIcons(Icon.EPISODE_VII, Icon.PILOT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_24);
        addModelType(ModelType.TRANSPORT);
        setPassengerCapacity(6);
        setTestingText("Starspeeder 3000");
        hideFromDeckBuilder();
    }
}
