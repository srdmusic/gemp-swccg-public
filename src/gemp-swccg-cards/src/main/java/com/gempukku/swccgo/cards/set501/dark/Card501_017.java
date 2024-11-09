package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Imperial
 * Title: Baron Soontir Fel (V)
 */
public class Card501_017 extends AbstractImperial {
    public Card501_017() {
        super(Side.DARK, 1, 4, 2, 3, 5, Title.Fel, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Corellian Baron. Leader of famed 181st Imperial Fighter Wing. Taught at the Imperial Academy on Prefsbelt IV. Instructed Biggs Darklighter.");
        setGameText("Deploys -1 to Endor. Adds 3 to the power of anything he pilots. When piloting a starship, adds one battle destiny. Anything he pilots is immune to attrition <5.");
        addIcons(Icon.DEATH_STAR_II, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_24);
        addKeywords(Keyword.LEADER, Keyword.SABER_SQUADRON);
        setSpecies(Species.CORELLIAN);
        setMatchingStarshipFilter(Filters.Saber_1);
        setVirtualSuffix(true);
        setTestingText("Baron Soontir Fel (V)");
        hideFromDeckBuilder();
    }
}
