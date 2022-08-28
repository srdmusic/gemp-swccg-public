package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.conditions.OccupiesWithCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Location
 * Subtype: System
 * Title: Christophsis
 */
public class Card501_073 extends AbstractSystem {
    public Card501_073() {
        super(Side.LIGHT, Title.Christophsis, 7);
        setLocationDarkSideGameText("If you occupy with a [Separatist] starship, opponent must use +1 Force to move or deploy a starship to here.");
        setLocationLightSideGameText("If you occupy with a [Clone Army] starship, opponent must use +1 Force to move or deploy a starship to here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.CLONE_ARMY, Icon.VIRTUAL_SET_20, Icon.PLANET, Icon.EPISODE_I);
        setTestingText("Christophsis");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        Condition condition = new OccupiesWithCondition(playerOnDarkSideOfLocation, self, Filters.and(Icon.SEPARATIST, Filters.starship));
        Filter opponentsStarships = Filters.and(Filters.opponents(playerOnDarkSideOfLocation), Filters.starship);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, opponentsStarships, condition, 1, self));
        modifiers.add(new MoveCostToLocationModifier(self, opponentsStarships, condition,1, self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        Condition condition = new OccupiesWithCondition(playerOnLightSideOfLocation, self, Filters.and(Icon.CLONE_ARMY, Filters.starship));
        Filter opponentsStarships = Filters.and(Filters.opponents(playerOnLightSideOfLocation), Filters.starship);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, opponentsStarships, condition, 1, self));
        modifiers.add(new MoveCostToLocationModifier(self, opponentsStarships, condition,1, self));
        return modifiers;
    }
}