package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.DeathStarPowerShutDownCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.LimitForceLossFromForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Location
 * Subtype: Site
 * Title: Death Star: Central Core
 */
public class Card501_021 extends AbstractSite {
    public Card501_021() {
        super(Side.LIGHT, Title.Death_Star_Central_Core, Title.Death_Star);
        setLocationDarkSideGameText("While 2 cards stacked on A Power Loss, you lose no more than 1 Force to any Force drain.");
        setLocationLightSideGameText("If A Power Loss 'shut down', Force drain +1 here and Death Star Tractor Beam is lost.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.INTERIOR_SITE, Icon.MOBILE, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_15);
        setTestingText("Death Star: Central Core");
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ForceDrainModifier(self, new DeathStarPowerShutDownCondition(), 1, playerOnLightSideOfLocation));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new LimitForceLossFromForceDrainModifier(self, new OnTableCondition(self, Filters.and(Filters.A_Power_Loss, Filters.hasStacked(2, Filters.any))), 1, playerOnDarkSideOfLocation));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLightSideRequiredAfterTriggers(final String playerOnLightSideOfLocation, final SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.isDeathStarPowerShutDown(game)
                && GameConditions.canSpot(game, self, Filters.Death_Star_Tractor_Beam)) {
            PhysicalCard deathStarTractorBeam = Filters.findFirstActive(game, self, Filters.Death_Star_Tractor_Beam);
            if (deathStarTractorBeam != null) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setSingletonTrigger(true);
                action.setText("Make " + GameUtils.getCardLink(deathStarTractorBeam) + " lost");
                action.setActionMsg("Make " + GameUtils.getCardLink(deathStarTractorBeam) + " lost");

                // Perform result(s)
                action.appendEffect(
                        new LoseCardFromTableEffect(action, deathStarTractorBeam));
                actions.add(action);
            }
        }
        return actions;
    }
}
