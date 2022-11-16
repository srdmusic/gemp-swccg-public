package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.conditions.OccupiesWithCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.IconModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Location
 * Subtype: System
 * Title: Lothal
 */
public class Card501_117 extends AbstractSystem {
    public Card501_117() {
        super(Side.DARK, Title.Lothal, 6);
        setLocationDarkSideGameText("While you occupy with an admiral, gains one [Dark Side] icon and one [Light Side] icon.");
        setLocationLightSideGameText("While you control, gains one [Light Side] icon.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcons(Icon.PLANET, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal (DARK) (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        Condition condition = new OccupiesWithCondition(playerOnDarkSideOfLocation, self, Filters.admiral);

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new IconModifier(self, condition, Icon.DARK_FORCE, 1));
        modifiers.add(new IconModifier(self, condition, Icon.LIGHT_FORCE, 1));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new IconModifier(self, new ControlsCondition(playerOnLightSideOfLocation, self), Icon.LIGHT_FORCE, 1));
        return modifiers;
    }
}