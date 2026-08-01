package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractUniqueStarshipSite;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.CancelsGameTextOnSideOfLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Invisible Hand: Observation Platform
 */
public class Card501_035 extends AbstractUniqueStarshipSite {
    public Card501_035() {
        super(Side.DARK, "Invisible Hand: Observation Platform", Persona.INVISIBLE_HAND, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("If you just took a card into hand with A Valuable Hostage, retrieve 1 Force.");
        setLocationLightSideGameText("If you occupy, opponent's Observation Platform game text here is canceled.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.INTERIOR_SITE, Icon.MOBILE, Icon.STARSHIP_SITE, Icon.SCOMP_LINK, Icon.EPISODE_I, Icon.VIRTUAL_SET_27);
        setTestingText("Invisible Hand: Observation Platform");
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new CancelsGameTextOnSideOfLocationModifier(self, Filters.and(Filters.Observation_Platform, Filters.here(self)), new OccupiesCondition(playerOnLightSideOfLocation, self), game.getOpponent(playerOnLightSideOfLocation)));
        return modifiers;
    }
}
