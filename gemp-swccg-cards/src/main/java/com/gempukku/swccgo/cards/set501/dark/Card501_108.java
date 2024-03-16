package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.IconModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Crait: Salt Plateau
 */
public class Card501_108 extends AbstractSite {
    public Card501_108() {
        super(Side.DARK, Title.Crait_Salt_Plateau, Title.Crait, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Your combat vehicles are defense value +1 here.");
        setLocationLightSideGameText("Gains [Light icon] while The Resistance Is Doomed on table.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_VII, Icon.VIRTUAL_SET_23);
        setTestingText(Title.Crait_Salt_Plateau);
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DefenseValueModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.combat_vehicle, Filters.here(self)), 1));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new IconModifier(self, self, new OnTableCondition(self, Filters.The_Resistance_Is_Doomed), Icon.LIGHT_FORCE));
        return modifiers;
    }
}