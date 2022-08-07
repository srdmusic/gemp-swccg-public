package com.gempukku.swccgo.cards.set219.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Jedi Temple
 */
public class Card219_041 extends AbstractSite {
    public Card219_041() {
        super(Side.LIGHT, Title.Lothal_Jedi_Temple, Title.Lothal);
        setLocationDarkSideGameText("");
        setLocationLightSideGameText("Ezra and Kanan deploy -1 here. " +
                                     "Unless you occupy, Vader may not deploy here and opponent's characters, vehicles, " +
                                     "and starships deploy and move to here for +3 Force.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 3);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_19);
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        Condition unlessYouOccupyCondition = new UnlessCondition(new OccupiesCondition(playerOnLightSideOfLocation, self));

        modifiers.add(new DeployCostToLocationModifier(self, Filters.or(Filters.Ezra, Filters.Kanan),  -1, self));

        modifiers.add(new MayNotDeployToLocationModifier(self, Filters.Vader, unlessYouOccupyCondition, self));
        Filter filter = Filters.and(Filters.opponents(playerOnLightSideOfLocation), Filters.or(Filters.character, Filters.vehicle, Filters.starship));
        modifiers.add(new DeployCostToLocationModifier(self, filter, unlessYouOccupyCondition, 3, self));
        modifiers.add(new MoveCostToLocationModifier(self, Filters.and(Filters.opponents(playerOnLightSideOfLocation), filter), unlessYouOccupyCondition, 3, self));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.here(self), Filters.or(Filters.Ezra, Filters.Kanan)), 1));
        return modifiers;
    }
}
