package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.conditions.ControlsWithCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * •Eriadu (V)
 * [Jedi Pack - PM]
 * DARK - LOCATION - SYSTEM
 * DARK (2): If Imperials control three battlegrounds, Force drain +1 here.
 * LIGHT (1): If Tarkin here, your starships deploy +1 here.
 * [Planet] [Parsec 1] [Set 16]
 */

/**
 * Set: Set 16
 * Type: Location
 * Subtype: System
 * Title: Eriadu (V)
 */
public class Card501_059 extends AbstractSystem {
    public Card501_059() {
        super(Side.DARK, Title.Eriadu, 1);
        setLocationDarkSideGameText("If Imperials control three battlegrounds, Force drain +1 here.");
        setLocationLightSideGameText("If Tarkin here, your starships deploy +1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PREMIUM, Icon.PLANET, Icon.VIRTUAL_SET_16);
        setTestingText("[Set 17] Eriadu (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, new ControlsWithCondition(self, playerOnDarkSideOfLocation, 3, Filters.battleground, Filters.Imperial), 1, playerOnDarkSideOfLocation));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.starship, Filters.owner(playerOnLightSideOfLocation)), new HereCondition(self, Filters.Tarkin), 1, Filters.here(self)));
        return modifiers;
    }
}