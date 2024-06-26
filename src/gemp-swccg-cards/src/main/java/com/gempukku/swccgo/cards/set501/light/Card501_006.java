package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ForfeitModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 23
 * Type: Location
 * Subtype: Site
 * Title: Nevarro: Nevarro City
 */
public class Card501_006 extends AbstractSite {
    public Card501_006() {
        super(Side.LIGHT, "Nevarro: Nevarro City", Title.Nevarro, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("Din, Grogu, Cara Dune, Greef and IG-11 are power and forfeit +1 here.");
        setLocationDarkSideGameText("Moff Gideon deploys -1 here.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.MUDHORN, Icon.VIRTUAL_SET_24);
        addKeyword(Keyword.NEVARRO_CITY_SITE);
        setTestingText("Nevarro: Nevarro City");
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.or(Filters.Din, Filters.Grogu, Filters.Cara_Dune, Filters.Greef, Filters.IG11), Filters.here(self)), 1));
        modifiers.add(new ForfeitModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.or(Filters.Din, Filters.Grogu, Filters.Cara_Dune, Filters.Greef, Filters.IG11), Filters.here(self)), 1));
        return modifiers;
    }
        
    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.Gideon), -1, self));
        return modifiers;
    }
}
