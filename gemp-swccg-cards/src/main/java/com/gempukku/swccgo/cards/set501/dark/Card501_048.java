package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.AloneAtCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.EachWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Location
 * Subtype: Site
 * Title: Cloud City: Chasm Walkway (V)
 */
public class Card501_048 extends AbstractSite {
    public Card501_048 () {
        super(Side.DARK, "Cloud City: Chasm Walkway", Title.Bespin);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("While Vader alone here, Force drain +1 and [Special Edition] Vader's weapon destiny draws are +1 here.");
        setLocationLightSideGameText("If Vader alone here (and Luke not here), [Special Edition] Vader is defense value +1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.VIRTUAL_SET_20, Icon.CLOUD_CITY, Icon.INTERIOR_SITE, Icon.MOBILE, Icon.SCOMP_LINK);
        addKeywords(Keyword.CLOUD_CITY_LOCATION);
        setTestingText("~Cloud City: Chasm Walkway (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        Filter vaderAlone = Filters.and(Filters.Vader, Filters.alone);
        Filter seVaderAlone = Filters.and(Icon.SPECIAL_EDITION, vaderAlone);
        Condition vaderAloneHereCondition = new HereCondition(self, vaderAlone);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, vaderAloneHereCondition, 1, playerOnDarkSideOfLocation));
        modifiers.add(new EachWeaponDestinyModifier(self, Filters.any, vaderAloneHereCondition, Filters.and(seVaderAlone, Filters.here(self)), 1));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DefenseValueModifier(self, Filters.and(Icon.SPECIAL_EDITION, Filters.Vader, Filters.here(self)), new AndCondition(new AloneAtCondition(self, Filters.Vader, self), new NotCondition(new HereCondition(self, Filters.Luke))), 1));
        return modifiers;
    }
}