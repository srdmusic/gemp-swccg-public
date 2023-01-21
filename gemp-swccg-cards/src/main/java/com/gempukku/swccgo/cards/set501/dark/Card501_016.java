package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.HereCondition;
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
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 21
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Great Pit Of Carkoon (V)
 */
public class Card501_016 extends AbstractSite {
    public Card501_016() {
        super(Side.DARK, Title.Great_Pit_Of_Carkoon, Title.Tatooine, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("Jabba's Sail Barge deploys -1 (and is defense value +2) here.");
        setLocationLightSideGameText("Unless Chewie, Leia of ability < 5, or Luke of ability < 6 here, Force drain -1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.JABBAS_PALACE, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_21);
        addKeywords(Keyword.PIT);
        setTestingText("Tatooine: Great Pit Of Carkoon (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.Jabbas_Sail_Barge, -1, Filters.here(self)));
        modifiers.add(new DefenseValueModifier(self, Filters.and(Filters.Jabbas_Sail_Barge, Filters.here(self)), 2));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        Filter characterFilter = Filters.or(Filters.Chewie, Filters.and(Filters.Leia, Filters.abilityLessThan(5)), Filters.and(Filters.Luke, Filters.abilityLessThan(6)));
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, new UnlessCondition(new HereCondition(self, characterFilter)), -1, playerOnLightSideOfLocation));
        return modifiers;
    }
}