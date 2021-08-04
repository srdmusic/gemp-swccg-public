package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.CommuningCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.MayDeployOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotForceDrainAtLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: Site
 * Title: Coruscant: Jedi Temple Meditation Room
 */
public class Card501_102 extends AbstractSite {
    public Card501_102() {
        super(Side.LIGHT, "Coruscant: Jedi Temple Meditation Room", Title.Coruscant);
        setLocationDarkSideGameText("While [Set 16] Qui-Gon 'communing,' no Force drains here.");
        setLocationLightSideGameText("If [Set 16] Qui-Gon 'communing,' Anakin and Obi-Wan may deploy -2 anywhere as a 'react'.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I, Icon.VIRTUAL_SET_16);
        setTestingText("Coruscant: Jedi Temple Meditation Room");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotForceDrainAtLocationModifier(self, new CommuningCondition(Filters.and(Icon.VIRTUAL_SET_16, Filters.QuiGon))));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayDeployOtherCardsAsReactToLocationModifier(self, "Deploy Anakin or Obi-Wan -2 as a react",
                new CommuningCondition(Filters.and(Icon.VIRTUAL_SET_16, Filters.QuiGon)), playerOnLightSideOfLocation,
                Filters.or(Filters.Anakin, Filters.ObiWan), Filters.location, -2));
        return modifiers;
    }
}