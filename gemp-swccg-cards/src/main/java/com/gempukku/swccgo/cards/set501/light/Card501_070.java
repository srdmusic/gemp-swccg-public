package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayMoveOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.MovesFreeFromLocationToLocationModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Location
 * Subtype: System
 * Title: Geonosis
 */
public class Card501_070 extends AbstractSystem {
    public Card501_070() {
        super(Side.LIGHT, Title.Geonosis, 6, ExpansionSet.SET_20, Rarity.V);
        setLocationDarkSideGameText("Your [Separatist] starships deploy -1 here and move for free to or from the nearest related asteroid sector.");
        setLocationLightSideGameText("Your [Clone Army] starships deploy -1 here. Your starships at the nearest related asteroid sector may move here as a 'react.'");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.CLONE_ARMY, Icon.EPISODE_I, Icon.PLANET, Icon.VIRTUAL_SET_20);
        setTestingText("Geonosis");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Icon.SEPARATIST, Filters.starship), -1, self));
        modifiers.add(new MovesFreeFromLocationToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Icon.SEPARATIST, Filters.starship), Filters.nearestRelatedAsteroidSector(self), Filters.any));
        modifiers.add(new MovesFreeFromLocationToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Icon.SEPARATIST, Filters.starship), Filters.any, Filters.nearestRelatedAsteroidSector(self)));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Icon.CLONE_ARMY, Filters.starship), -1, self));
        modifiers.add(new MayMoveOtherCardsAsReactToLocationModifier(self, "Move starship as a react", playerOnLightSideOfLocation, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.starship, Filters.at(Filters.nearestRelatedAsteroidSector(self))), Filters.any));
        return modifiers;
    }
}