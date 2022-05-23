package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.HereCondition;
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
 * Title: Lothal: Tarkintown
 */
public class Card501_110 extends AbstractSite {
    public Card501_110() {
        super(Side.LIGHT, "Lothal: Tarkintown", Title.Lothal);
        setLocationDarkSideGameText("Unless Tarkin here, Force drain -1 here.");
        setLocationLightSideGameText("If you occupy, Force generation +1 here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal: Tarkintown");
    }


    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, new UnlessCondition(new HereCondition(self, Filters.Tarkin)),-1, playerOnDarkSideOfLocation));
        return modifiers;
    }


    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceGenerationModifier(self, new OccupiesCondition(playerOnLightSideOfLocation, self), 1, playerOnLightSideOfLocation));
        return modifiers;
    }
}
