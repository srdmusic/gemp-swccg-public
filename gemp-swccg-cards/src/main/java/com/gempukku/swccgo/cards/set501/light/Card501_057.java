package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;

/**
 * Set: Set 20
 * Type: Starship
 * Subtype: Starfighter
 * Title: Din Djarin's Modified N-1
 */
public class Card501_057 extends AbstractStarfighter {
    public Card501_057() {
        super(Side.LIGHT, 3, 3, 3, null, 5, 3, 5, "Din Djarin's Modified N-1", Uniqueness.UNIQUE);
        setLore("");
        setGameText("May add 1 pilot and Grogu as a passenger. Din Djarin deploys -1 aboard. While Din piloting, immune to attrition < 5 and once per game may use 3 Force to cancel a battle just initiated here.");
        addIcons(Icon.NAV_COMPUTER, Icon.INDEPENDENT, Icon.VIRTUAL_SET_20);
        setTestingText("Din Djarin's Modified N-1");
        setMatchingPilotFilter(Filters.Din);
        setTestingText("Din Djarin's Modified N-1");
        hideFromDeckBuilder();
    }
}
