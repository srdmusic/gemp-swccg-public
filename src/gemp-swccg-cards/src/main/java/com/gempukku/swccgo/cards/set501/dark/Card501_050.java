package com.gempukku.swccgo.cards.set501.dark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractDarkJediMaster;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AloneCondition;
import com.gempukku.swccgo.cards.conditions.OnCondition;
import com.gempukku.swccgo.cards.effects.PayRelocateBetweenLocationsCostEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
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
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Dark Jedi Master
 * Title: Master Sidious
 */
public class Card501_050 extends AbstractDarkJediMaster {
    public Card501_050() {
        super(Side.DARK, 1, 6, 5, 7, 8, "Master Sidious", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leader. Trade Federation.");
        setGameText("While alone on Coruscant, your apprentice is immune to attrition. Disarmed is canceled here. During your move phase, if at a site you control, may use 1 Force to relocate between a Coruscant site and your apprentice's site. Immune to attrition.");
        addIcons(Icon.EPISODE_I, Icon.WARRIOR, Icon.VIRTUAL_SET_25);
        addKeywords(Keyword.LEADER);
        addPersona(Persona.SIDIOUS);
        setTestingText("Master Sidious");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        Condition aloneOnCoruscantCondition = new AndCondition(new AloneCondition(self), new OnCondition(self, Title.Coruscant));

        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ImmuneToAttritionModifier(self, Filters.Sith_Apprentice, aloneOnCoruscantCondition));
        modifiers.add(new ImmuneToAttritionModifier(self));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(final SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCardTargeting(game, effect, Filters.title(Title.Disarmed), Filters.here(self))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        Filter disarmedHere = Filters.and(Filters.title(Title.Disarmed), Filters.here(self));

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canTargetToCancel(game, self, disarmedHere)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, disarmedHere, Title.Disarmed);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.MOVE)
                && GameConditions.isAtLocation(game, self, Filters.and(Filters.site, Filters.controls(playerId)))
                && GameConditions.canTarget(game, self, Filters.and(Filters.Coruscant_site))
                && GameConditions.canTarget(game, self, Filters.and(Filters.Sith_Apprentice))) {
            
            Filter siteToRelocateTo;
            
            if (GameConditions.isWith(game, self, Filters.Sith_Apprentice)) {
                siteToRelocateTo = Filters.Coruscant_site;
            } else if (GameConditions.isAtLocation(game, self, Filters.Coruscant_site)) {
                siteToRelocateTo = Filters.and(Filters.sameLocationAs(self, Filters.Sith_Apprentice), Filters.site);
            } else {
                siteToRelocateTo = Filters.none;
            }
            siteToRelocateTo = Filters.and(siteToRelocateTo, Filters.locationCanBeRelocatedTo(self, false, 1));

            if (GameConditions.canSpotLocation(game, siteToRelocateTo)) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Relocate " + GameUtils.getFullName(self) + " to a site");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Choose target(s)
                action.appendTargeting(
                        new ChooseCardOnTableEffect(action, self.getOwner(), "Choose site to relocate " + GameUtils.getFullName(self) + " to", siteToRelocateTo) {
                            @Override
                            protected void cardSelected(final PhysicalCard selectedCard) {
                                action.addAnimationGroup(selectedCard);
                                // Pay cost(s)
                                action.appendCost(
                                        new PayRelocateBetweenLocationsCostEffect(action, playerId, self, selectedCard, 1));
                                // Allow response(s)
                                action.allowResponses("Relocate " + GameUtils.getFullName(self) + " to " + GameUtils.getCardLink(selectedCard),
                                        new UnrespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new RelocateBetweenLocationsEffect(action, self, selectedCard, false));
                                            }
                                        }
                                );
                            }
                        }
                );            
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}
