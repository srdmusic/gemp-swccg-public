package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.RevealUsedPileEffect;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.ShuffleUsedPileEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.ForceRetrievalModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Used
 * Title: Free Ride & Endor Celebration (V)
 */
public class Card501_107 extends AbstractUsedInterrupt {
    public Card501_107() {
        super(Side.LIGHT, 5, "Free Ride & Endor Celebration", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        addComboCardTitles(Title.Free_Ride, Title.Endor_Celebration);
        setGameText("Cancel Cloud City Occupation, Force Lightning, Rebel Base Occupation, or Tatooine Occupation. [Immune to Sense.] OR Opponent’s Force retrieval from A Million Voices Crying Out is -2 for remainder of turn. OR During your turn, target opponent's spy, non-[Immune to Alter] Effect, or unpiloted combat vehicle at a site you control; target is lost. (Immune to Oh, Switch Off.) OR Reveal opponent's Used Pile. If opponent has more than one card of printed destiny > 6, they lose 1 Force (2 Force if more than three); reshuffle.");
        addIcons(Icon.CORUSCANT, Icon.VIRTUAL_SET_16);
        setTestingText("Free Ride & Endor Celebration (V) (ERRATA)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        final String opponent = game.getOpponent(self.getOwner());

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Tatooine_Occupation)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Tatooine_Occupation, Title.Tatooine_Occupation);
            action.setImmuneTo(Title.Sense);
            actions.add(action);
        }
        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Cloud_City_Occupation)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Cloud_City_Occupation, Title.Cloud_City_Occupation);
            action.setImmuneTo(Title.Sense);
            actions.add(action);
        }
        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Rebel_Base_Occupation)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Rebel_Base_Occupation, Title.Rebel_Base_Occupation);
            action.setImmuneTo(Title.Sense);
            actions.add(action);
        }
        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Force_Lightning)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Force_Lightning, Title.Force_Lightning);
            action.setImmuneTo(Title.Sense);
            actions.add(action);
        }

        if (GameConditions.canTarget(game, self, Filters.title(Title.A_Million_Voices_Crying_Out))) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Make A Million Voices Crying Out retrieve -2");

            // Allow response(s)
            action.allowResponses("Make opponent's Force retrieval from A Million Voices Crying Out -2 for remainder of turn",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            action.appendEffect(
                                    new AddUntilEndOfTurnModifierEffect(action,
                                            new ForceRetrievalModifier(self, new CardMatchesEvaluator(0, -2, Filters.title(Title.A_Million_Voices_Crying_Out)),  opponent),
                                            "Opponent's Force retrieval from A Million Voices Crying Out is -2")
                            );
                        }
                    }
            );
            actions.add(action);
        }

        // Check condition(s)
        TargetingReason targetingReason = TargetingReason.TO_BE_LOST;
        Filter filter = Filters.and(Filters.at(Filters.and(Filters.controls(playerId), Filters.site)), Filters.opponents(playerId),
                Filters.or(Filters.spy, Filters.and(Filters.unpiloted, Filters.combat_vehicle), Filters.and(Filters.Effect, Filters.not(Filters.immune_to_Alter))),
                Filters.canBeTargetedBy(self, targetingReason));

        if (GameConditions.isDuringYourTurn(game, playerId)
                && GameConditions.canSpot(game, self, SpotOverride.INCLUDE_UNDERCOVER, filter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Make spy, Effect, or combat vehicle lost");
            action.setImmuneTo(Title.Oh_Switch_Off);

            // Allow response(s)
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target card to be lost", SpotOverride.INCLUDE_UNDERCOVER, targetingReason, filter) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.allowResponses(null,
                            new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    // Perform result(s)
                                    PhysicalCard finalCard = action.getPrimaryTargetCard(targetGroupId);
                                    action.appendEffect(new LoseCardFromTableEffect(action, finalCard));
                                }
                            }
                    );
                }
            });
            actions.add(action);
        }

        if (GameConditions.hasUsedPile(game, opponent)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Reveal opponent's Used Pile");

            // Allow response(s)
            action.allowResponses("Reveal opponent's Used Pile, if opponent has more than one card of printed destiny > 6 they lose 1 Force (2 Force if more than 3); reshuffle.",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            action.appendEffect(
                                    new RevealUsedPileEffect(action, opponent) {
                                        @Override
                                        protected void cardsRevealed(List<PhysicalCard> revealedCards) {
                                            int highDestinyCardCount = Filters.filter(revealedCards, game, Filters.printedDestinyGreaterThan(6)).size();

                                            int forceLoss = 0;
                                            if (highDestinyCardCount > 3)
                                                forceLoss = 2;
                                            else if (highDestinyCardCount > 1)
                                                forceLoss = 1;

                                            if (forceLoss > 0) {
                                                action.appendEffect(
                                                        new LoseForceEffect(action, opponent, forceLoss));
                                            }

                                            action.appendEffect(
                                                    new ShuffleUsedPileEffect(action, self, opponent));
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Force_Lightning, Filters.Tatooine_Occupation, Filters.Cloud_City_Occupation, Filters.Rebel_Base_Occupation))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            action.setImmuneTo(Title.Sense);
            actions.add(action);
        }


        return actions;
    }
}