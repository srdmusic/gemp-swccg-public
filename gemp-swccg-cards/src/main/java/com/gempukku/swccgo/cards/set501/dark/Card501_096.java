package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.TrueCondition;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 13
 * Type: Location
 * Subtype: Site
 * Title: Malachor: Sith Temple Gateway
 */
public class Card501_096 extends AbstractSite {
    public Card501_096() {
        super(Side.DARK, "Malachor: Sith Temple Gateway", Title.Malachor);
        setLocationDarkSideGameText("[Set 13] Maul ignores [Set 13] objective deployment restrictions here.");
        setLocationLightSideGameText("Opponent may not target Ezra with weapons here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.UNDERGROUND, Icon.INTERIOR_SITE, Icon.PLANET, Icon.CLOUD_CITY, Icon.VIRTUAL_SET_16);
        setTestingText("[Set 17] Malachor: Sith Temple Gateway");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        //TODO make it only ignore objective deployment restrictions **here** (currently ignores them everywhere)
        modifiers.add(new IgnoresDeploymentRestrictionsFromCardModifier(self, Filters.and(Icon.VIRTUAL_SET_13, Filters.Maul), new TrueCondition(), playerOnDarkSideOfLocation, Filters.and(Icon.VIRTUAL_SET_13, Filters.Objective)));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotBeTargetedByModifier(self, Filters.Ezra, Filters.and(Filters.weapon, Filters.here(self))));
        return modifiers;
    }
}