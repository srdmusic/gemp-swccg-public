package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.ForceGenerationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotExistAtLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 8
 * Type: Location
 * Subtype: Site
 * Title: Endor: Ewok Village (V)
 */
public class Card501_112 extends AbstractSite {
    public Card501_112() {
        super(Side.LIGHT, Title.Ewok_Village, Title.Endor);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("Force drain -1 here. No starships or vehicles here.");
        setLocationLightSideGameText("While Prophecy Of The Force with a Skywalker here, it may not relocate and opponent's Force generation is -1 here.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.ENDOR, Icon.INTERIOR_SITE, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_8);
        setTestingText("Endor: Ewok Village (V) (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ForceDrainModifier(self, -1, playerOnDarkSideOfLocation));
        modifiers.add(new MayNotExistAtLocationModifier(self, Filters.or(Filters.starship, Filters.vehicle), self));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        Condition prophecyWithSkywalkerHereCondition = new AndCondition(new HereCondition(self, Filters.Prophecy_Of_The_Force), new HereCondition(self, Filters.Skywalker));
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.Prophecy_Of_The_Force, prophecyWithSkywalkerHereCondition, ModifyGameTextType.PROPHECY_OF_THE_FORCE__MAY_NOT_BE_RELOCATED));
        modifiers.add(new ForceGenerationModifier(self, prophecyWithSkywalkerHereCondition, -1, game.getOpponent(playerOnLightSideOfLocation)));
        return modifiers;
    }
}