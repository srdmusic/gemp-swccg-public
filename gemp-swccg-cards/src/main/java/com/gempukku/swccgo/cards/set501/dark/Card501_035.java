package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.conditions.ControlsWithCondition;
import com.gempukku.swccgo.cards.conditions.DuringBattleWithParticipantCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.MayDeployOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Imperial Complex
 */
public class Card501_035 extends AbstractSite {
    public Card501_035() {
        super(Side.DARK, "Lothal: Imperial Complex", Title.Lothal);
        setLocationDarkSideGameText("If you control with a leader, once per battle involving an Imperial, may deploy a card as a 'react.'");
        setLocationLightSideGameText("Unless you control Capital City, your non-Rebel characters may not deploy or move to here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 0);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal: Imperial Complex");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayDeployOtherCardsAsReactToLocationModifier(self, "Deploy a card as a 'react'", new AndCondition(new ControlsWithCondition(playerOnDarkSideOfLocation, self, Filters.leader), new DuringBattleWithParticipantCondition(Filters.Imperial)), playerOnDarkSideOfLocation, Filters.any, Filters.battleLocation));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployToLocationModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.character, Filters.not(Filters.Rebel)), new UnlessCondition(new ControlsCondition(playerOnLightSideOfLocation, Filters.Lothal_Capital_City)), self));
        modifiers.add(new MayNotMoveToLocationModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.character, Filters.not(Filters.Rebel)), new UnlessCondition(new ControlsCondition(playerOnLightSideOfLocation, Filters.Lothal_Capital_City)), self));
        return modifiers;
    }
}
