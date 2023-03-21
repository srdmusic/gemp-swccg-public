package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.HereCondition;
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
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.MayNotReactModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 22
 * Type: Location
 * Subtype: Site
 * Title: Hoth: Snow Trench (2nd Marker) (V)
 */
public class Card501_098 extends AbstractSite {
    public Card501_098() {
        super(Side.LIGHT, "Hoth: Snow Trench (2nd Marker)", Title.Hoth, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("Unless your combat vehicle here, your characters are power -1 here.");
        setLocationLightSideGameText("Vehicles may not react from or through this site.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.HOTH, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_22);
        addKeywords(Keyword.MARKER_2);
        setTestingText("Hoth: Snow Trench (2nd Marker) (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.character, Filters.here(self)), new UnlessCondition(new HereCondition(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.combat_vehicle))), -1));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotReactModifier(self, Filters.and(Filters.vehicle, Filters.here(self))));
        //TODO "or through"
        return modifiers;
    }
}