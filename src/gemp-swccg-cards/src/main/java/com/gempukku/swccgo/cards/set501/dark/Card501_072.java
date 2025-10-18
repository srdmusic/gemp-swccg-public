package com.gempukku.swccgo.cards.set501.dark;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetDeployCostToLocationModifier;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: System
 * Title: Bespin (V)
 * Errata E1 of Card223_008.java
 */

public class Card501_072 extends AbstractSystem {
    public Card501_072() {
        super(Side.DARK, Title.Bespin, 6, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Executor may not move from here unless Vader aboard. If your [Cloud City] objective on table, Executor is deploy = 7 here.");
        setLocationLightSideGameText("You lose no more than 2 Force to Cloud City Occupation. Intensify the Forward Batteries is canceled.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.SPECIAL_EDITION, Icon.PLANET, Icon.VIRTUAL_SET_23);
        setVirtualSuffix(true);
        setTestingText("Bespin (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        Condition vaderAboardExecutor = new OnTableCondition(self, Filters.and(Filters.Vader, Filters.aboard(Filters.Executor)));
        Condition unlessVaderAboardExecutor = new UnlessCondition(vaderAboardExecutor);
        Condition yourCloudCityObjOnTable = new OnTableCondition(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Icon.CLOUD_CITY, Filters.Objective));

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotMoveFromLocationModifier(self, Filters.Executor, unlessVaderAboardExecutor, self));
        modifiers.add(new ResetDeployCostToLocationModifier(self, Filters.Executor, yourCloudCityObjOnTable, 7, Filters.here(self)));
        return modifiers;
    }
    
}
