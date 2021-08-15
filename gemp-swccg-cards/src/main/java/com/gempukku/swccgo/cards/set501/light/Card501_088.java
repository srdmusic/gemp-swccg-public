package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: Site
 * Title: Kashyyyk: Imperial Work Settlement #121
 */
public class Card501_088 extends AbstractSite {
    public Card501_088() {
        super(Side.LIGHT, "Kashyyyk: Imperial Work Settlement #121", Title.Kashyyyk);
        setLocationDarkSideGameText("While you control, opponent's Wookiees at Kashyyyk sites are power -1.");
        setLocationLightSideGameText("While you occupy, your Wookiees at Kashyyyk sites are forfeit +1.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.VIRTUAL_SET_16, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I);
        setTestingText("Kashyyyk: Imperial Work Settlement #121");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        Filter opponentsWookieesAtKashyyykSitesFilter = Filters.and(Filters.opponents(playerOnDarkSideOfLocation), Filters.Wookiee, Filters.at(Filters.Kashyyyk_site));
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, opponentsWookieesAtKashyyykSitesFilter, new ControlsCondition(playerOnDarkSideOfLocation, self),-1));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        Filter yourWookieesAtKashyyykSitesFilter = Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.Wookiee, Filters.at(Filters.Kashyyyk_site));
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForfeitModifier(self, yourWookieesAtKashyyykSitesFilter, new OccupiesCondition(playerOnLightSideOfLocation, self), 1));
        return modifiers;
    }
}
