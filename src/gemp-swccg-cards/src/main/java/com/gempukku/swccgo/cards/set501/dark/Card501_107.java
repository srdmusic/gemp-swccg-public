package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.ArmorModifier;
import com.gempukku.swccgo.logic.modifiers.HyperspeedWhenMovingFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ShuttlesFreeFromLocationModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: System
 * Title: Crait
 */
public class Card501_107 extends AbstractSystem {
    public Card501_107() {
        super(Side.DARK, Title.Crait, 8, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Your shuttling from here is free.");
        setLocationLightSideGameText("Your capital starships are armor and hyperspeed +1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PLANET, Icon.EPISODE_VII, Icon.VIRTUAL_SET_24);
        setTestingText(Title.Crait);
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ShuttlesFreeFromLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.or(Filters.character, Filters.vehicle)), self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ArmorModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.capital_starship), 1));
        modifiers.add(new HyperspeedWhenMovingFromLocationModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.capital_starship), 1, self));
        return modifiers;
    }
}