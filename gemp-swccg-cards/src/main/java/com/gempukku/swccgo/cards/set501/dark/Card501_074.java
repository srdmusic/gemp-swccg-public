package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.cards.conditions.OccupiesWithCondition;
import com.gempukku.swccgo.cards.effects.ConvertLocationByRaisingToTopEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.modifiers.AttemptToBlowAwayShieldGateTotalModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Location
 * Subtype: Site
 * Title: Scarif: Citadel Tower
 */
public class Card501_074 extends AbstractSite {
    public Card501_074() {
        super(Side.DARK, Title.Scarif_Citadel_Tower, Title.Scarif);
        setLocationDarkSideGameText("If a player just Force drained here, they may raise a converted Scarif location to the top.");
        setLocationLightSideGameText("If you occupy, add 1 to attempts to 'blow away' Shield Gate (2 if you occupy with a spy).");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PLANET, Icon.INTERIOR_SITE, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_16);
        setTestingText("Scarif: Citadel Tower");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextDarkSideOptionalAfterTriggers(String playerOnDarkSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        Filter scarifSite = Filters.and(Filters.Scarif_site, Filters.or(Filters.canBeConvertedByRaisingLocationToTop(playerOnDarkSideOfLocation), Filters.canBeConvertedByRaisingLocationToTop(game.getOpponent(playerOnDarkSideOfLocation))));

        // Check condition(s)
        if (TriggerConditions.forceDrainInitiatedAt(game, effectResult, self)
                && GameConditions.canTarget(game, self, scarifSite)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerOnDarkSideOfLocation, gameTextSourceCardId);
            action.setText("Raise a converted Scarif site");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerOnDarkSideOfLocation, "Target site to convert", scarifSite) {
                        @Override
                        protected void cardTargeted(int targetGroupId, final PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Convert " + GameUtils.getCardLink(targetedCard),
                                    new UnrespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new ConvertLocationByRaisingToTopEffect(action, targetedCard, false));
                                        }
                                    }
                            );
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        Condition occupiesCondition = new OccupiesCondition(playerOnLightSideOfLocation, self);
        Condition occupiesWithSpyCondition = new OccupiesWithCondition(playerOnLightSideOfLocation, self, Filters.spy);

        modifiers.add(new AttemptToBlowAwayShieldGateTotalModifier(self, occupiesCondition, new ConditionEvaluator(1, 2, occupiesWithSpyCondition)));
        return modifiers;
    }
}
