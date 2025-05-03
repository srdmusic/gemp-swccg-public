package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.DuringForceDrainAtCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.TrueCondition;
import com.gempukku.swccgo.logic.conditions.UnlessCondition;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromOffTableEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromTableEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.ReturnCardToHandFromTableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.JediTestSuspendedInsteadOfLostModifier;
import com.gempukku.swccgo.logic.modifiers.MayDeployOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifierFlag;
import com.gempukku.swccgo.logic.modifiers.PlaceJediTestOnTableWhenCompletedModifier;
import com.gempukku.swccgo.logic.modifiers.SpecialFlagModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Objective
 * Title: Mind What You Have Learned / Save You It Can
 */
public class Card501_178_BACK extends AbstractObjective {
    public Card501_178_BACK() {
        super(Side.LIGHT, 7, Title.Save_You_It_Can, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Immediately return Luke and any cards on him to owner's hand. While this side up, Luke may deploy -3 with a weapon as a 'react.' If Luke just won a battle, may place a card on Patience! out of play to retrieve 1 Force. When your [Cloud City] Rebel Force drains at a battleground site, unless a captive on table, lost Force must come from bottom of Reserve Deck if possible. Once per game, may place a completed Jedi Test out of play to take Luke into hand from Lost Pile.");
        addIcons(Icon.SPECIAL_EDITION, Icon.DAGOBAH, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Save You It Can (V)");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        final PhysicalCard lukeCard = Filters.findFirstActive(game, self, Filters.Luke);

        // Check condition(s)
        if (TriggerConditions.cardFlipped(game, effectResult, self)
                && lukeCard != null) {            

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Return Luke and cards on him to hand");
            action.setActionMsg("Return Luke and any cards on him to owner's hand");
            // Perform result(s)
            action.appendEffect(
                    new ReturnCardToHandFromTableEffect(action, lukeCard, Zone.HAND, Zone.HAND));
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();

        String playerId = self.getOwner();

        // For remainder of game
        modifiers.add(new PlaceJediTestOnTableWhenCompletedModifier(self, Filters.any, new TrueCondition()));
        modifiers.add(new JediTestSuspendedInsteadOfLostModifier(self, Filters.any, new TrueCondition()));
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Icon.DAGOBAH, Filters.Yoda), -4, Filters.Dagobah_location));

        // While this side up

        // TO DO #1: Create "MayDeployWithWeaponAsReactModifier" by copying ideas from MayDeployWithPilotOrDriverAsReactModifier
        // Temporary placeholder: Only deploys Luke, without a weapon, as a react
        modifiers.add(new MayDeployOtherCardsAsReactToLocationModifier(self, "Deploy Luke (deploy -3) as a 'react'", playerId, Filters.Luke, Filters.any));

        // Filter for a battleground site where there is a [Cloud City] Rebel
        Filter filterCloudCityRebelBattlegroundSite = Filters.and(Filters.sameSiteAs(self, Filters.and(Icon.CLOUD_CITY, Filters.Rebel)), Filters.battleground);

        // TO DO #2: Create FORCE_DRAIN_LOST_FROM_BOTTOM_OF_RESERVE_DECK
        // Temporary placeholder: Lost Force comes from top of Reserve Deck for now
        Condition duringCCRebelForceDrainAtBattlegroundSite = new DuringForceDrainAtCondition(filterCloudCityRebelBattlegroundSite);
        Condition unlessCaptiveOnTable = new UnlessCondition(new OnTableCondition(self, SpotOverride.INCLUDE_CAPTIVE, Filters.captive));
        Condition conditionsForReserveDeckForceLoss = new AndCondition(duringCCRebelForceDrainAtBattlegroundSite, unlessCaptiveOnTable);
        modifiers.add(new SpecialFlagModifier(self, conditionsForReserveDeckForceLoss, ModifierFlag.FORCE_DRAIN_LOST_FROM_RESERVE_DECK, game.getOpponent(self.getOwner())));
        
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {

        final Filter patienceWithCardStacked = Filters.and(Filters.Patience, Filters.hasStacked(Filters.any));

        if (TriggerConditions.wonBattle(game, effectResult, Filters.Luke)
                && GameConditions.canSpot(game, self, patienceWithCardStacked)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);

            action.setText("Place stacked card out of play");
            action.setActionMsg("Place a card on Patience! out of play to retrieve 1 Force");

            action.appendTargeting(
                    new ChooseStackedCardEffect(action, playerId, patienceWithCardStacked, Filters.any, false) {
                        @Override
                        protected void cardSelected(PhysicalCard selectedCard) {
                            // Pay cost(s)
                            action.appendCost(
                                    new PlaceCardOutOfPlayFromOffTableEffect(action, selectedCard));

                            // Perform result(s)
                            action.appendEffect(
                                    new RetrieveForceEffect(action, playerId, 1));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {

        GameTextActionId gameTextActionId = GameTextActionId.SAVE_YOU_IT_CAN__UPLOAD_LUKE_FROM_LOST_PILE;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canSpot(game, self, Filters.completed_Jedi_Test)
                && GameConditions.hasLostPile(game, playerId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);

            action.setText("Place completed Jedi Test out of play");
            action.setActionMsg("Place a completed Jedi Test out of play to take Luke into hand from Lost Pile");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Choose target(s)
            action.appendTargeting(
                    new ChooseCardOnTableEffect(action, playerId, "Choose a completed Jedi Test", Filters.completed_Jedi_Test) {
                        @Override
                        protected void cardSelected(PhysicalCard selectedCard) {
                            // Pay cost(s)
                            action.appendCost(
                                    new PlaceCardOutOfPlayFromTableEffect(action, selectedCard));
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromLostPileEffect(action, playerId, Filters.Luke, false));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}