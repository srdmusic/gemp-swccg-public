package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
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
public class Card501_108 extends AbstractSite {
    public Card501_108() {
        super(Side.LIGHT, Title.Lothal_Jedi_Temple, Title.Lothal);
        setLocationDarkSideGameText("Vader may not deploy here and your characters deploy and move to here for +3 Force.");
        setLocationLightSideGameText("Ezra and Kanan are deploy -1 and power +1 here. If you occupy, opponent's game text canceled.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 3);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal: Jedi Temple");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployToLocationModifier(self, Filters.Vader, self));
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.character), 3, self));
        modifiers.add(new MoveCostToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.character), 3, self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.or(Filters.Ezra, Filters.Kanan), -1, self));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.here(self), Filters.or(Filters.Ezra, Filters.Kanan)), 1));
        modifiers.add(new CancelsGameTextOnSideOfLocationModifier(self, self, new OccupiesCondition(playerOnLightSideOfLocation, self), game.getOpponent(playerOnLightSideOfLocation)));
        return modifiers;
    }
}
