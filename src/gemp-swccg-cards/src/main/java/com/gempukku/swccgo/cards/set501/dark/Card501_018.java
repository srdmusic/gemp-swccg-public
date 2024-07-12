package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.LimitForceLossFromCardModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeConvertedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: System
 * Title: Bespin (V)
 */
public class Card501_018 extends AbstractSystem {
    public Card501_018() {
        super(Side.DARK, Title.Bespin, 6, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("If you occupy and Dark Deal on table, flip This Deal Is Getting Worse All The Time. Immune to Revolution.");
        setLocationLightSideGameText("While you occupy, you lose no more than 1 Force to Cloud City Occupation. May not be converted.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.CLOUD_CITY, Icon.PLANET, Icon.VIRTUAL_SET_23);
        setTestingText("Bespin (V)");
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new LimitForceLossFromCardModifier(self, Filters.Cloud_City_Occupation, new OccupiesCondition(playerOnLightSideOfLocation, self), 1, playerOnLightSideOfLocation));
        modifiers.add(new MayNotBeConvertedModifier(self));
        modifiers.add(new ImmuneToTitleModifier(self, Title.Revolution));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextDarkSideRequiredAfterTriggers(String playerOnDarkSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.occupies(game, playerOnDarkSideOfLocation, self)
                && GameConditions.canSpot(game, self, Filters.Dark_Deal)
                && GameConditions.canSpot(game, self, Filters.This_Deal_Is_Getting_Worse_All_The_Time)){

            PhysicalCard thisDeal = Filters.findFirstActive(game, self, Filters.This_Deal_Is_Getting_Worse_All_The_Time);
            
            if (GameConditions.canBeFlipped(game, thisDeal)) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                // Build action using common utility
                action.appendEffect(new FlipCardEffect(action, thisDeal));
                return Collections.singletonList(action);
            }
        }
        return null;
    }

}
