package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.conditions.ControlsWithCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalForceGenerationModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: System
 * Title: Naboo (V) (Dark)
 */
public class Card501_018 extends AbstractSystem {
    public Card501_018() {
        super(Side.DARK, Title.Naboo, 5);
        setLocationDarkSideGameText("If you control with a [Trade Federation] starship, your Force generation is +1 here.");
        setLocationLightSideGameText("If opponent controls, your characters and vehicles deploy +1 to Naboo sites.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.PLANET, Icon.VIRTUAL_SET_16);
        setTestingText("Naboo (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new TotalForceGenerationModifier(self, new ControlsWithCondition(playerOnDarkSideOfLocation, self, Filters.and(Filters.icon(Icon.TRADE_FEDERATION), Filters.starship)), 1, playerOnDarkSideOfLocation));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.or(Filters.character, Filters.vehicle)),
                new ControlsCondition(game.getOpponent(playerOnLightSideOfLocation), self), 1, Filters.Naboo_site, true));
        return modifiers;
    }
}