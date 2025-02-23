package com.gempukku.swccgo.cards.set501.light;

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
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Endor: Rebel Landing Site (Forset) (V)
 */
public class Card501_160 extends AbstractSite{
    public Card501_160() {
        super(Side.LIGHT, Title.Rebel_Landing_Site, Title.Endor, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Unless you occupy, you must first use 1 Force to deploy a non-scout character here.");
        setLocationLightSideGameText("Your scouts of ability < 5 are deploy -1 and defense value +2 here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.ENDOR, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.FOREST);
        setVirtualSuffix(true);
        setTestingText("Endor: Rebel Landing Site (Forest) (V)");
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        Filter yourScoutsWithLowAbility = Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.scout, Filters.abilityLessThan(5));
        Filter yourScoutsWithLowAbilityHere = Filters.and(yourScoutsWithLowAbility, Filters.here(self));

        modifiers.add(new DeployCostToLocationModifier(self, yourScoutsWithLowAbility, -1, self));
        modifiers.add(new DefenseValueModifier(self, yourScoutsWithLowAbilityHere, 2));
        return modifiers;
    }
}
