package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.DuringPlayersTurnNumberCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Location
 * Subtype: Site
 * Title: Hoth: North Ridge (4th Marker) (V)
 */
public class Card501_021 extends AbstractSite {
    public Card501_021() {
        super(Side.DARK, Title.North_Ridge, Title.Hoth);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("AT-ATs move to and from here for free. During your first turn, AT-ATs may not deploy here.");
        setLocationLightSideGameText("T-47s are power +1 here. If two T-47s here, Force drain +1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.HOTH, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_17);
        addKeywords(Keyword.MARKER_4);
        setTestingText("Hoth: North Ridge (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MovesFreeToLocationModifier(self, Filters.AT_AT, self));
        modifiers.add(new MovesFreeFromLocationModifier(self, Filters.AT_AT, self));
        modifiers.add(new MayNotDeployToLocationModifier(self, Filters.AT_AT, new DuringPlayersTurnNumberCondition(self.getOwner(), 1), self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.T_47, Filters.here(self)), 1));
        modifiers.add(new ForceDrainModifier(self, new HereCondition(self, 2, Filters.T_47), 1, playerOnLightSideOfLocation));
        return modifiers;
    }
}