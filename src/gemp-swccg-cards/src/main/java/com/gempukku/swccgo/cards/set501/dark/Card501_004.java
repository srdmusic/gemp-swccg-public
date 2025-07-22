package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.cards.effects.ConvertLocationByRaisingToTopEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.evaluators.ConstantEvaluator;
import com.gempukku.swccgo.logic.modifiers.ExtraForceCostToDeployCardToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Desert Heart (V)
 */
public class Card501_004 extends AbstractSite {
    public Card501_004() {
        super(Side.DARK, Title.Desert_Heart, Title.Tatooine, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("If you occupy this site or control Audience Chamber, may raise your converted [Jabba's Palace] site to the top.");
        setLocationLightSideGameText("Unless you occupy, you must first use 1 Force to deploy a non-alien character here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.PLANET, Icon.EXTERIOR_SITE, Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Tatooine: Desert Heart (V)");
    }


    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextDarkSideOptionalAfterTriggers(String playerOnDarkSideOfLocation, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        //Your [Jabba's Palace] icon sites (the site underneath)
        Filter yourJPSiteFilter = Filters.and(Filters.your(playerOnDarkSideOfLocation), Icon.JABBAS_PALACE, Filters.site);
        //Sites on top of your [Jabba's Palace] icon sites (and which are allowed to be converted by raising yours)
        Filter convertableTargetsFilter = Filters.and(Filters.canBeConvertedByRaisingYourLocationToTop(playerOnDarkSideOfLocation), Filters.convertedLocationOnTopOfLocation(yourJPSiteFilter));


        // Check condition(s)
        if (TriggerConditions.justDeployedTo(game, effectResult, playerOnDarkSideOfLocation, Filters.alien, Filters.here(self))
                && GameConditions.canTarget(game, self, convertableTargetsFilter)
                && GameConditions.isOncePerTurn(game, self, playerOnDarkSideOfLocation, gameTextSourceCardId, gameTextActionId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Raise your converted Jabba's Palace site");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerOnDarkSideOfLocation, "Target site to convert", convertableTargetsFilter) {
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
                                                    new ConvertLocationByRaisingToTopEffect(action, targetedCard, true));
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
    protected List<TopLevelGameTextAction> getGameTextDarkSideTopLevelActions(final String playerOnDarkSideOfLocation, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        //Your [Jabba's Palace] icon sites (the site underneath)
        Filter yourJPSiteFilter = Filters.and(Filters.your(playerOnDarkSideOfLocation), Icon.JABBAS_PALACE, Filters.site);
        //Sites on top of your [Jabba's Palace] icon sites (and which are allowed to be converted by raising yours)
        Filter convertableTargetsFilter = Filters.and(Filters.canBeConvertedByRaisingYourLocationToTop(playerOnDarkSideOfLocation), Filters.convertedLocationOnTopOfLocation(yourJPSiteFilter));

        // Check condition(s)
        if ((GameConditions.occupies(game, playerOnDarkSideOfLocation, self)
                || GameConditions.controls(game, playerOnDarkSideOfLocation, Filters.Audience_Chamber))
                && GameConditions.canTarget(game, self, convertableTargetsFilter)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnDarkSideOfLocation, gameTextSourceCardId);
            action.setText("Raise your converted Jabba's Palace site");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerOnDarkSideOfLocation, "Target site to convert", convertableTargetsFilter) {
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
                                                    new ConvertLocationByRaisingToTopEffect(action, targetedCard, true));
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
        Condition unlessYouOccupy = new UnlessCondition(new OccupiesCondition(playerOnLightSideOfLocation, self));
        Filter yourNonAlienCharacters = Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.character, Filters.not(Filters.alien));

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ExtraForceCostToDeployCardToLocationModifier(self, yourNonAlienCharacters, unlessYouOccupy, new ConstantEvaluator(1), self));     
        return modifiers;
    }
}
