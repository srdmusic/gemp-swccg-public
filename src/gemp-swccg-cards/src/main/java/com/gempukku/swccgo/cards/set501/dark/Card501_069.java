package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

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
import com.gempukku.swccgo.logic.modifiers.FiresForFreeModifier;
import com.gempukku.swccgo.logic.modifiers.MayDeployOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Cloud City: Dining Room (V)
 */

public class Card501_069 extends AbstractSite {
    public Card501_069() {
        super(Side.DARK, Title.Dining_Room, Title.Bespin, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Your characters and weapons may deploy here as a 'react.'");
        setLocationLightSideGameText("Your blasters here fire for free.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.CLOUD_CITY, Icon.INTERIOR_SITE, Icon.MOBILE, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_26);
        addKeywords(Keyword.CLOUD_CITY_LOCATION);
        setVirtualSuffix(true);
        setTestingText("Cloud City: Dining Room (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayDeployOtherCardsAsReactToLocationModifier(self, "Deploy card as a 'react'", playerOnDarkSideOfLocation, Filters.or(Filters.character, Filters.weapon), self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new FiresForFreeModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.blaster, Filters.here(self))));
        return modifiers;
    }
    
}
