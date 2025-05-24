package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeFlippedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Cloud City: Beldon's Corridor
 */
public class Card501_187 extends AbstractSite {
    public Card501_187() {
        super(Side.LIGHT, Title.Beldons_Corridor, Title.Bespin, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Your troopers are defense value +1 here. Unless Beldon's Eye on table, Force drain -1 here.");
        setLocationLightSideGameText("While your [Cloud City] Rebel here, Their Fire Has Gone Out Of The Universe flips and may not flip back.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.CLOUD_CITY, Icon.INTERIOR_SITE, Icon.MOBILE, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.CLOUD_CITY_LOCATION);
        setTestingText("Cloud City: Beldon's Corridor");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        Filter filterYourTroopersHere = Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.trooper, Filters.here(self));
        modifiers.add(new DefenseValueModifier(self, filterYourTroopersHere, 1));
        modifiers.add(new ForceDrainModifier(self, self, new UnlessCondition(new OnTableCondition(self, Filters.Beldons_Eye)), -1, playerOnDarkSideOfLocation));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotBeFlippedModifier(self, new HereCondition(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Icon.CLOUD_CITY, Filters.Rebel)), Filters.Hunt_Down_And_Destroy_The_Jedi));
        return modifiers;
    }
}