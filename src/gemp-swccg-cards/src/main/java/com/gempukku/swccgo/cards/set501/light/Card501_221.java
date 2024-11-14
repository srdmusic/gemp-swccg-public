package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Starfighter
 * Title: Bo-Katan's Gauntlet Starfighter
 */
public class Card501_221 extends AbstractStarfighter {
    public Card501_221() {
        super(Side.LIGHT, 3, 4, 4, null, 6, 5, 6, "Bo-Katan's Gauntlet Starfighter", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May add 2 pilots and 4 passengers. Permanent pilot provides ability of 2. While Bo-Katan piloting, power +3 and cancels Imperial Barrier. Once per game, may ▼ a Mandalorian aboard. Immune to attrition < 5.");
        addIcons(Icon.SCOMP_LINK, Icon.INDEPENDENT, Icon.PILOT, Icon.NAV_COMPUTER, Icon.VIRTUAL_SET_24);
        addModelType(ModelType.TRANSPORT);
        setPilotCapacity(2);
        setPassengerCapacity(4);
        setMatchingPilotFilter(Filters.Bo_Katan);
        setTestingText("Bo-Katan's Gauntlet Starfighter");
        hideFromDeckBuilder();
    }
}