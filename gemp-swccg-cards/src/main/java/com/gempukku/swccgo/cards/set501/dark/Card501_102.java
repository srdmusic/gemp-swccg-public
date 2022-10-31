package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.conditions.DuringForceDrainAtCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifiersMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainsMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Capital City
 */
public class Card501_102 extends AbstractSite {
    public Card501_102() {
        super(Side.DARK, Title.Lothal_Capital_City, Title.Lothal, Uniqueness.UNIQUE, ExpansionSet.SET_19, Rarity.V);
        setLocationDarkSideGameText("If you control, your Force drains (and Force drain bonuses) at Lothal sites may not be canceled.");
        setLocationLightSideGameText("Unless a Rebel here, force drain -1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.EXTERIOR_SITE, Icon.INTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal: Capital City (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        String opponent = game.getOpponent(playerOnDarkSideOfLocation);
        Filter siteFilter = Filters.sameOrRelatedSite(self);
        Condition darkSideControls = new ControlsCondition(playerOnDarkSideOfLocation, self);
        Condition duringForceDrain = new DuringForceDrainAtCondition(siteFilter);
        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, siteFilter, new ControlsCondition(playerOnDarkSideOfLocation, self), opponent, playerOnDarkSideOfLocation));
        modifiers.add(new ForceDrainModifiersMayNotBeCanceledModifier(self, new AndCondition(darkSideControls, duringForceDrain), Filters.your(playerOnDarkSideOfLocation)));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, new UnlessCondition(new HereCondition(self, Filters.Rebel)), -1, playerOnLightSideOfLocation));
        return modifiers;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        return "GameState.isForceDrain: " + game.getGameState().isDuringForceDrain();
    }
}