package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.conditions.CantSpotCondition;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Location
 * Subtype: System
 * Title: Malachor
 */
public class Card501_114 extends AbstractSystem {
    public Card501_114() {
        super(Side.LIGHT, Title.Malachor, 6);
        setLocationDarkSideGameText("If you control, Ezra is power -2 and does not apply ability towards drawing battle destiny.");
        setLocationLightSideGameText("If you control, Vader is power -3.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 2);
        addIcons(Icon.PLANET, Icon.VIRTUAL_SET_18);
        setTestingText("Malachor");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new PowerModifier(self, Filters.Ezra, new ControlsCondition(playerOnDarkSideOfLocation, self), -2));
        modifiers.add(new MayNotApplyAbilityForBattleDestinyModifier(self, Filters.Ezra, new ControlsCondition(playerOnDarkSideOfLocation, self)));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new PowerModifier(self, Filters.Vader, new ControlsCondition(playerOnLightSideOfLocation, self), -3));
        return modifiers;
    }
}