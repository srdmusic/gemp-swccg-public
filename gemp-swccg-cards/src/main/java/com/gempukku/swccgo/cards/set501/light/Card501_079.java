package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;

/**
 * Set: Set 16
 * Type: Effect
 * Title: Wookiee Homestead
 */
public class Card501_079 extends AbstractNormalEffect {
    public Card501_079() {
        super(Side.LIGHT, 0, PlayCardZoneOption.ATTACHED, "Wookiee Homestead", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on Kachirho. Wookiees are deploy -1. While a Wookiee alone here, [Dark Side] icons here are canceled. If you have two Wookiees in battle, draw one battle destiny if unable to otherwise. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_16);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Wookiee Homestead");
        hideFromDeckBuilder();
    }

    @Override
    protected Filter getValidDeployTargetFilter(String playerId, SwccgGame game, PhysicalCard self, PhysicalCard sourceCard, PlayCardOption playCardOption, boolean forFree, float changeInCost, DeploymentRestrictionsOption deploymentRestrictionsOption, DeployAsCaptiveOption deployAsCaptiveOption, ReactActionOption reactActionOption, boolean isSimDeployAttached, boolean ignorePresenceOrForceIcons) {
        return Filters.title(Title.Kachirho);
    }
}