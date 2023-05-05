package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.PhaseCondition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.EachWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeConvertedModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Cantina (V)
 */
public class Card501_010 extends AbstractSite {
    public Card501_010() {
        super(Side.DARK, Title.Cantina, Title.Tatooine, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("May not be converted. Lightsaber weapon destiny draws are -1 here.");
        setLocationLightSideGameText("Unless Obi-Wan here (or at Mos Eisley), you may not move here during your control phase.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_21);
        setTestingText("Tatooine: Cantina (DARK) (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(final String playerOnDarkideOfLocation, SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotBeConvertedModifier(self));
        modifiers.add(new EachWeaponDestinyModifier(self, Filters.and(Filters.lightsaber, Filters.here(self)), -1));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(final String playerOnLightSideOfLocation, SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotMoveToLocationModifier(self, Filters.your(playerOnLightSideOfLocation),
                new AndCondition(new PhaseCondition(Phase.CONTROL, playerOnLightSideOfLocation), new UnlessCondition(new AtCondition(self, Filters.or(Filters.here(self), Filters.Mos_Eisley), Filters.ObiWan))), Filters.here(self)));
        return modifiers;
    }
}