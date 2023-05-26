package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 21
 * Type: Location
 * Subtype: Site
 * Title: Cloud City: Chasm Walkway (V)
 */
public class Card501_130 extends AbstractSite {
    public Card501_130() {
        super(Side.DARK, Title.Chasm_Walkway, Title.Bespin, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("If you have more than one character here, you must target one to be lost (cannot be prevented).");
        setLocationLightSideGameText("If you have more than two characters here, you must target one to be lost (cannot be prevented).");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.CLOUD_CITY, Icon.SPECIAL_EDITION, Icon.INTERIOR_SITE, Icon.MOBILE, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_21);
        addKeywords(Keyword.CLOUD_CITY_LOCATION);
        setTestingText("Cloud City: Chasm Walkway (DARK) (V)");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextDarkSideRequiredAfterTriggers(String playerOnDarkSideOfLocation, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canSpot(game, self, 2, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.character, Filters.here(self)))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Choose a character to be lost");
            action.setPerformingPlayer(playerOnDarkSideOfLocation);
            action.appendTargeting(new TargetCardOnTableEffect(action, playerOnDarkSideOfLocation, "Choose a character to be lost (cannot be prevented)", TargetingReason.TO_BE_LOST, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.character, Filters.here(self))) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.addAnimationGroup(self);
                    action.allowResponses(new RespondableEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard toBeLost = action.getPrimaryTargetCard(targetGroupId);
                            action.appendEffect(
                                    new LoseCardFromTableEffect(action, toBeLost));
                        }
                    });
                }
            });

            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLightSideRequiredAfterTriggers(String playerOnLightSideOfLocation, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canSpot(game, self, 3, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.character, Filters.here(self)))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Choose a character to be lost");
            action.setPerformingPlayer(playerOnLightSideOfLocation);
            action.appendTargeting(new TargetCardOnTableEffect(action, playerOnLightSideOfLocation, "Choose a character to be lost (cannot be prevented)", TargetingReason.TO_BE_LOST, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.character, Filters.here(self))) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.addAnimationGroup(self);
                    action.allowResponses(new RespondableEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard toBeLost = action.getPrimaryTargetCard(targetGroupId);
                            action.appendEffect(
                                    new LoseCardFromTableEffect(action, toBeLost));
                        }
                    });
                }
            });

            return Collections.singletonList(action);
        }
        return null;
    }
}