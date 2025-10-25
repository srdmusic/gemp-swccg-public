package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.HereCondition;
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
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Mapuzo: Safehouse
 */

public class Card501_204 extends AbstractSite {
    public Card501_204() {
        super(Side.LIGHT, Title.Safehouse, Title.Mapuzo, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("The first Jedi you deploy here each turn is immune to Imperial Barrier.");
        setLocationDarkSideGameText("Unless Third Sister here, Force drain -1 here.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_26);
        setTestingText("Mapuzo: Safehouse");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();

        Condition UnlessThirdSisterHere = new UnlessCondition(new HereCondition(self, Filters.Third_Sister));

        modifiers.add(new ForceDrainModifier(self, UnlessThirdSisterHere, -1, playerOnDarkSideOfLocation));
        return modifiers;
    }
}
