package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceDrainEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Coruscant
 * Type: Interrupt
 * Subtype: Used
 * Title: Masterful Move & Endor Occupation
 */
public class Card501_094 extends AbstractUsedInterrupt {
    public Card501_094() {
        super(Side.DARK, 5, "Masterful Move & Endor Occupation", Uniqueness.UNIQUE);
        addComboCardTitles(Title.Masterful_Move, Title.Endor_Occupation);
        setVirtualSuffix(true);
        setGameText("Cancel Tatooine Celebration, Cloud City Celebration, or Coruscant Celebration. (Immune to Sense.) OR Use 1 Force to take one hologram, dejarik, or Imperial Holotable into hand from Reserve Deck; reshuffle. OR Cancel Mantellian Savrip. OR Cancel opponent's Force drain at a holosite.");
        setGameText("Cancel Tatooine Celebration, Cloud City Celebration, or Coruscant Celebration. [Immune to Sense.] OR For remainder of turn, cancel BB-8 and/or Rose's game text. OR Cancel Mantellian Savrip or Projection Of A Skywalker. OR Use 1 Force to take a dejarik into hand from Reserve Deck; reshuffle. OR If you occupy more battlegrounds than opponent, suspend Menace Fades for remainder of turn.");
        addIcons(Icon.CORUSCANT, Icon.VIRTUAL_SET_18);
        setTestingText("Masterful Move & Endor Occupation (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Tatooine_Celebration)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Tatooine_Celebration, Title.Tatooine_Celebration);
            action.setImmuneTo(Title.Sense);
            actions.add(action);
        }
        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Cloud_City_Celebration)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Cloud_City_Celebration, Title.Cloud_City_Celebration);
            action.setImmuneTo(Title.Sense);
            actions.add(action);
        }
        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Coruscant_Celebration)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Coruscant_Celebration, Title.Coruscant_Celebration);
            action.setImmuneTo(Title.Sense);
            actions.add(action);
        }

        if (GameConditions.canTarget(game, self, Filters.or(Filters.BB8, Filters.Rose))) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self);
                action.setText("Cancel game text of BB-8 and/or Rose");

                action.appendTargeting(new TargetCardsOnTableEffect(action, playerId, "Target BB-8 and/or Rose", 1, 2, Filters.or(Filters.BB8, Filters.Rose)) {
                    @Override
                    protected void cardsTargeted(final int targetGroupId, Collection<PhysicalCard> targetedCards) {
                        // Allow response(s)
                        action.allowResponses("Cancel game text of "+ GameUtils.getAppendedNames(targetedCards) +" for remainder of turn",
                                new RespondablePlayCardEffect(action) {
                                    @Override
                                    protected void performActionResults(Action targetingAction) {
                                        Collection<PhysicalCard> finalTargets = action.getPrimaryTargetCards(targetGroupId);
                                        // Perform result(s)
                                        for (PhysicalCard card:finalTargets) {
                                            action.appendEffect(
                                                    new CancelGameTextUntilEndOfTurnEffect(action, card));
                                        }
                                    }
                                }
                        );
                    }
                });
                actions.add(action);
        }

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Mantellian_Savrip)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Mantellian_Savrip, Title.Mantellian_Savrip);
            actions.add(action);
        }
        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.title(Title.Projection_Of_A_Skywalker))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.title(Title.Projection_Of_A_Skywalker), Title.Projection_Of_A_Skywalker);
            actions.add(action);
        }

        GameTextActionId gameTextActionId = GameTextActionId.MASTERFUL_MOVE_ENDOR_OCCUPATION__UPLOAD_DEJARIK;

        // Check condition(s)
        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.canUseForceToPlayInterrupt(game, playerId, self, 1)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Take a dejarik into hand from Reserve Deck");
            // Pay cost(s)
            action.appendCost(
                    new UseForceEffect(action, playerId, 1));
            // Allow response(s)
            action.allowResponses("Take a dejarik into hand from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.dejarik, true));
                        }
                    }
            );
            actions.add(action);
        }

        if (GameConditions.canTarget(game, self, TargetingReason.TO_BE_SUSPENDED, Filters.title("Menace Fades"))) {
            int selfBattlegrounds = Filters.countTopLocationsOnTable(game, Filters.and(Filters.battleground, Filters.occupies(playerId)));
            int oppBattlegrounds = Filters.countTopLocationsOnTable(game, Filters.and(Filters.battleground, Filters.occupies(game.getOpponent(playerId))));
            if (selfBattlegrounds > oppBattlegrounds) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self);
                action.setText("Suspend Menace Fades");

                action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target Menace Fades to suspend", TargetingReason.TO_BE_SUSPENDED, Filters.title("Menace Fades")) {
                    @Override
                    protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                        // Allow response(s)
                        action.allowResponses("Suspend "+ GameUtils.getCardLink(targetedCard) +" for remainder of turn",
                                new RespondablePlayCardEffect(action) {
                                    @Override
                                    protected void performActionResults(Action targetingAction) {
                                        PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                        // Perform result(s)
                                        action.appendEffect(
                                                new SuspendCardUntilEndOfTurnEffect(action, finalTarget));
                                    }
                                }
                        );
                    }

                    @Override
                    protected boolean getUseShortcut() {
                        return true;
                    }
                });
                actions.add(action);
            }
        }
        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Tatooine_Celebration, Filters.Cloud_City_Celebration, Filters.Coruscant_Celebration))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            action.setImmuneTo(Title.Sense);
            actions.add(action);
        }
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.Mantellian_Savrip)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            actions.add(action);
        }
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.title(Title.Projection_Of_A_Skywalker))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            actions.add(action);
        }
        return actions;
    }
}