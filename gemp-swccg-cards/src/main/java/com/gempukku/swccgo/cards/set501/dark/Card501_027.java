package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
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
 * Set: Set 20
 * Type: Location
 * Subtype: Site
 * Title: Christophsis: Separatist Encampment
 */
public class Card501_027 extends AbstractSite {
    public Card501_027() {
        super(Side.DARK, "Christophsis: Separatist Encampment", Title.Christophsis);
        setLocationDarkSideGameText("Your battle droids (and [Separatist] characters) here may not be targeted by Rebel Barrier or Sorry About The Mess.");
        setLocationLightSideGameText("Your battle destiny draws are -1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.EPISODE_I, Icon.VIRTUAL_SET_20);
        setTestingText("Christophsis: Separatist Encampment");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotBeTargetedByModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.or(Filters.battle_droid, Filters.and(Icon.SEPARATIST, Filters.character))), Filters.or(Filters.Rebel_Barrier, Filters.Sorry_About_The_Mess)));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(final String playerOnLightSideOfLocation, final SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new EachBattleDestinyModifier(self, self, -1, playerOnLightSideOfLocation));
        return modifiers;
    }
}