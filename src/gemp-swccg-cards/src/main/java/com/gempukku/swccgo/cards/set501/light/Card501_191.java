package com.gempukku.swccgo.cards.set501.light;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtAndReorderTopCardsOfReserveDeckEffect;
import com.gempukku.swccgo.cards.effects.complete.ChooseExistingCardPileEffect;
import com.gempukku.swccgo.cards.effects.usage.NumTimesPerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnActionProxyEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.CancelCardsOnTableEffect;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.PutCardFromHandOnUsedPileEffect;
import com.gempukku.swccgo.logic.effects.ReduceAttritionEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Epic Event
 * Title: How Liberty Dies
 */
public class Card501_191 extends AbstractEpicEventDeployable {
    public Card501_191() {
        super(Side.LIGHT, PlayCardZoneOption.ATTACHED, Title.How_Liberty_Dies, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Deploy on Galactic Senate. Your Political Effects are canceled. Twice per turn, may target your agenda here: Justice: During battle, subtract 1 from a just drawn weapon destiny. Order: During any move phase, peek at the top 2 cards of any Reserve Deck and replace in any order. Peace: Subtract 1 from attrition against you. Taxation: Place an [Episode I] character from hand on Used Pile; the next [Episode I] character you deploy this turn is cumulatively deploy -1.");
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.VIRTUAL_SET_25);
        setTestingText("How Liberty Dies");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Galactic_Senate;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(SwccgGame game, Effect effect, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.and(Filters.your(self), Filters.Political_Effect))
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
        List<RequiredGameTextTriggerAction> actions = new LinkedList<RequiredGameTextTriggerAction>();

        Filter yourPoliticalEffectsFilter = Filters.and(Filters.your(self), Filters.Political_Effect);

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canTargetToCancel(game, self, yourPoliticalEffectsFilter)) {

            Collection<PhysicalCard> cardsToCancel = Filters.filterActive(game, self, TargetingReason.TO_BE_CANCELED, yourPoliticalEffectsFilter);

            if (!cardsToCancel.isEmpty()) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Cancel your Political Effects");

                action.appendEffect(
                        new CancelCardsOnTableEffect(action, cardsToCancel));
                actions.add(action);
            }
        }
        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new ArrayList<>();

        GameTextActionId gameTextActionId = GameTextActionId.HOW_LIBERTY_DIES__TARGET_AGENDA;
        Filter justiceAgendaFilter = Filters.and(Filters.your(playerId), Filters.justice_agenda, Filters.here(self));

        // Check condition(s)
        if (TriggerConditions.isWeaponDestinyJustDrawn(game, effectResult)
                && GameConditions.isDuringBattle(game)
                && GameConditions.isNumTimesPerTurn(game, self, playerId, 2, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canTarget(game, self, justiceAgendaFilter)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Subtract 1 from destiny");
            // Update usage limit(s)
            action.appendUsage(
                    new NumTimesPerTurnEffect(action, 2));

            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Target justice agenda", justiceAgendaFilter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, final PhysicalCard cardTargeted) {
                            action.addAnimationGroup(cardTargeted);

                            // Allow response(s)
                            action.allowResponses("Target justice agenda on " + GameUtils.getCardLink(cardTargeted) + " to subtract 1 from destiny",
                                    new UnrespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new ModifyDestinyEffect(action, -1));
                                        }
                                    }
                            );

                        }
                    });
            actions.add(action);
        }

        Filter peaceAgendaFilter = Filters.and(Filters.your(playerId), Filters.peace_agenda, Filters.here(self));
        // Check condition(s)
        if (TriggerConditions.isInitialAttritionJustCalculated(game, effectResult)
                && GameConditions.isDuringBattle(game)
                && GameConditions.isNumTimesPerTurn(game, self, playerId, 2, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canTarget(game, self, peaceAgendaFilter)) {

            final BattleState battleState = game.getGameState().getBattleState();
            if (battleState.hasAttritionTotal(game.getOpponent(playerId))) {

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action.setText("Reduce attrition by 1");
                // Update usage limit(s)
                action.appendUsage(
                        new NumTimesPerTurnEffect(action, 2));
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Target peace agenda", peaceAgendaFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard cardTargeted) {
                                action.addAnimationGroup(cardTargeted);

                                // Allow response(s)
                                action.allowResponses("Target peace agenda on " + GameUtils.getCardLink(cardTargeted) + " to subtract 1 from attrition",
                                        new UnrespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {                                            
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new ReduceAttritionEffect(action, playerId, 1));
                                            }
                                        }
                                );

                            }
                        });
                actions.add(action);
            }
        }
        return actions;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, final int gameTextSourceCardId) {
        final List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.HOW_LIBERTY_DIES__TARGET_AGENDA;
        Filter orderAgendaFilter = Filters.and(Filters.your(playerId), Filters.order_agenda, Filters.here(self));
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.isNumTimesPerTurn(game, self, playerId, 2, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canTarget(game, self, orderAgendaFilter)
                && GameConditions.isDuringYourPhase(game, playerId, Phase.MOVE)
                && (GameConditions.hasReserveDeck(game, playerId) || GameConditions.hasReserveDeck(game, opponent))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Peek at top 2 cards of a Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new NumTimesPerTurnEffect(action, 2));

            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Target order agenda", orderAgendaFilter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, final PhysicalCard cardTargeted) {
                            action.addAnimationGroup(cardTargeted);

                            // Choose more target(s)
                            action.appendTargeting(
                                    new ChooseExistingCardPileEffect(action, playerId, Zone.RESERVE_DECK) {
                                        @Override
                                        protected void pileChosen(SwccgGame game, final String cardPileOwner, Zone cardPile) {
                                            action.setActionMsg("Target order agenda on " + GameUtils.getCardLink(cardTargeted) + " to peek at top 2 cards of " + cardPileOwner + "'s " + cardPile.getHumanReadable() + " and replace in any order");

                                            // Allow response(s)
                                            action.allowResponses("Target order agenda on " + GameUtils.getCardLink(cardTargeted) + " to peek at top 2 cards of " + cardPileOwner + "'s " + cardPile.getHumanReadable() + " and replace in any order",
                                                    new UnrespondableEffect(action) {
                                                        @Override
                                                        protected void performActionResults(Action targetingAction) {                                            
                                                            // Perform result(s)
                                                            action.appendEffect(
                                                                    new PeekAtAndReorderTopCardsOfReserveDeckEffect(action, cardPileOwner, 2));
                                                        }
                                                    }
                                            );
                                        }
                                    }
                            );
                        }
                    });
            actions.add(action);
        }

        Filter taxationAgendaFilter = Filters.and(Filters.your(playerId), Filters.taxation_agenda, Filters.here(self));
        Filter yourEp1CharacterFilter = Filters.and(Filters.your(playerId), Icon.EPISODE_I, Filters.character);

        // Check condition(s)
        if (GameConditions.isNumTimesPerTurn(game, self, playerId, 2, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canTarget(game, self, taxationAgendaFilter)
                && GameConditions.hasInHand(game, playerId, yourEp1CharacterFilter)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Place card from hand on Used Pile");
            action.setActionMsg("Make the next [Episode I] character they deploy this turn deploy -1");

            // This is a special separate usage tracker for keeping the two one-time deployment -1 modifiers separate.
            // Below they'll be set up such that each deployment -1 is allowed to be used once per turn.
            GameTextActionId taxationUsageActionId;
            if (game.getModifiersQuerying().getUntilEndOfTurnLimitCounter(self, playerId, gameTextSourceCardId, gameTextActionId).getUsedLimit() == 0) {
                taxationUsageActionId = GameTextActionId.OTHER_CARD_ACTION_1;
            }
            else {
                taxationUsageActionId = GameTextActionId.OTHER_CARD_ACTION_2;
            }

            // Update usage limit(s)
            action.appendUsage(
                    new NumTimesPerTurnEffect(action, 2));
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Target taxation agenda", taxationAgendaFilter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, final PhysicalCard cardTargeted) {
                            action.addAnimationGroup(cardTargeted);
                            // Pay cost(s)
                            action.appendCost(
                                    new PutCardFromHandOnUsedPileEffect(action, playerId, Filters.and(Icon.EPISODE_I, Filters.character), false));
                            // Allow response(s)
                            action.allowResponses("Target taxation agenda on " + GameUtils.getCardLink(cardTargeted) + " to make their next [Episode I] character deploy -1",
                                    new UnrespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            final int permanentCardId = self.getPermanentCardId();
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new AddUntilEndOfTurnActionProxyEffect(action,
                                                            new AbstractActionProxy() {
                                                                @Override
                                                                public List<TriggerAction> getRequiredAfterTriggers(SwccgGame game, EffectResult effectResult) {
                                                                    List<TriggerAction> actions = new LinkedList<>();

                                                                    // don't actually need to return an action
                                                                    // only need to track that an Episode I character was deployed to increment the limit counter so the modifier is turned off
                                                                    if (TriggerConditions.justDeployed(game, effectResult, playerId, yourEp1CharacterFilter)) {
                                                                        PhysicalCard card = game.findCardByPermanentId(permanentCardId);
                                                                        for (String title : card.getTitles()) {
                                                                            game.getModifiersQuerying().getUntilEndOfTurnForCardTitleLimitCounter(title, taxationUsageActionId).incrementToLimit(1, 1);
                                                                        }
                                                                    }
                                                                    return actions;
                                                                }
                                                            }
                                                    ));

                                            // this can't use EndOfTurnLimitCounterNotReachedCondition just in case it leaves the table because that resets the cardId that it uses to find the limit
                                            Condition turnLimitCounterNotReachedCondition = new Condition() {
                                                @Override
                                                public boolean isFulfilled(GameState gameState, ModifiersQuerying modifiersQuerying) {
                                                    PhysicalCard card = gameState.findCardByPermanentId(permanentCardId);
                                                    for (String title : card.getTitles()) {
                                                        if (modifiersQuerying.getUntilEndOfTurnForCardTitleLimitCounter(title, taxationUsageActionId).getUsedLimit() >= 1)
                                                            return false;
                                                    }
                                                    return true;
                                                }
                                            };
                                            action.appendEffect(
                                                    new AddUntilEndOfTurnModifierEffect(action, new DeployCostModifier(self, Filters.and(yourEp1CharacterFilter, Filters.not(Filters.onTable)),
                                                            turnLimitCounterNotReachedCondition,-1), "Reduces the cost of the next [Episode I] character you deploy this turn by 1"));
                                        }
                                    }
                            );

                        }
                    });
            actions.add(action);
        }
        return actions;
    }

}
