package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.conditions.PresentAtCondition;
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
import com.gempukku.swccgo.logic.modifiers.IconModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByWeaponsModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotTargetToBeCapturedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Death Star: Docking Control Room 327
 */

public class Card501_120 extends AbstractSite {
    public Card501_120() {
        super(Side.LIGHT, Title.Docking_Control_Room_327, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("While Luke here, he may not be captured.  C-3P0 may not be targeted by weapons here.");
        setLocationDarkSideGameText("Unless your blaster present, Rebel Barrier may not be cancelled here.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.INTERIOR_SITE, Icon.MOBILE, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_23);
        setTestingText(Title.Docking_Control_Room_327);
        setVirtualSuffix(true);
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        Condition lukeHere = new HereCondition(self, Filters.Luke);
        Condition c3p0Here = new HereCondition(self, Filters.C3PO);
        modifiers.add(new MayNotTargetToBeCapturedModifier(self, Filters.Luke, lukeHere));
        modifiers.add(new MayNotBeTargetedByWeaponsModifier(self, Filters.C3PO, c3p0Here));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotBeCanceledModifier(self, Filters.and(Filters.Rebel_Barrier, Filters.cardBeingPlayedTargeting(self, Filters.here(self))), new UnlessCondition(new PresentAtCondition(Filters.and(Filters.your(self), Filters.blaster), Filters.here(self)))));
        return modifiers;
    }
}
