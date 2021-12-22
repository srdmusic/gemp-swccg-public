package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.MoveUsingLocationTextAction;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Location
 * Subtype: Site
 * Title: Kef Bir: Oceanic Wreckage
 */
public class Card501_071 extends AbstractSite {
    public Card501_071() {
        super(Side.LIGHT, "Kef Bir: Oceanic Wreckage", Title.Kef_Bir);
        setLocationDarkSideGameText("Unless you occupy, Kylo and Sidious deploy -1 here.");
        setLocationLightSideGameText("Unless Rey, Han, or Ben Solo here, Force drain -1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EPISODE_VII, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_17);
        setTestingText("Kef Bir: Oceanic Wreckage");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.or(Filters.Kylo, Filters.Sidious), new UnlessCondition(new OccupiesCondition(playerOnDarkSideOfLocation, self)), -1, self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, new UnlessCondition(new HereCondition(self, Filters.or(Filters.Rey, Filters.Han, Filters.persona(Persona.BEN_SOLO)))), -1, playerOnLightSideOfLocation));
        return modifiers;
    }
}