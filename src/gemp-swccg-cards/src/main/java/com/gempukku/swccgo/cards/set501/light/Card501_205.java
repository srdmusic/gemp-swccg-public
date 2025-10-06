package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

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
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Mapuzo: Underground Corridor
 */

public class Card501_205 extends AbstractSite {
    public Card501_205() {
        super(Side.LIGHT, Title.Underground_Corridor, Title.Mapuzo, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("During your move phase, Jedi Survivors may move from here to a Jabiim site.");
        setLocationDarkSideGameText("While Gather Allies and Train on table, Force drain +1 here.");
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcon(Icon.DARK_FORCE, 0);
        addIcons(Icon.UNDERGROUND, Icon.INTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_26);
        setTestingText("Mapuzo: Underground Corridor");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();

        Condition GatherAlliesAndTrainOnTable = new OnTableCondition(self, Filters.Gather_Allies_And_Train);

        modifiers.add(new ForceDrainModifier(self, GatherAlliesAndTrainOnTable, 1, playerOnDarkSideOfLocation));
        return modifiers;
    }    
}
