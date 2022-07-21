package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.cards.effects.CancelTargetingEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.TargetingActionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Where's Han and Solo Han
 */
public class Card501_125 extends AbstractUsedOrLostInterrupt {
    public Card501_125() {
        super(Side.LIGHT, 4, "Where's Han and Solo Han", Uniqueness.UNIQUE);
        addComboCardTitles(Title.Wheres_Han, Title.Solo_Han);
        setGameText("USED: If Cantina on table, take non-[M] Han, Mara or No Questions Asked into hand from " +
                "Reserve Deck; reshuffle. LOST: Cancel an attempt to target a smuggler with Hidden Weapons, " +
                "You Are Beaten or Dr. Evazan. OR Cancel Overwhelmed or Lateral Damage if at same system as two " +
                "smugglers piloting. OR If your smuggler defending a battle alone, add one battle destiny.");
        addIcons(Icon.VIRTUAL_SET_20);
        setTestingText("~Where's Han and Solo Han");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();
        // USED: If Cantina on table, take non-[M] Han, Mara, or No Questions Asked into hand from Reserve

        GameTextActionId uploadCardGametextActionId = GameTextActionId.WHERES_HAN_SOLO_HAN_UPLOAD_CARD;
        final Filter nonMainHanMaraNQA = Filters.or(
                Filters.and(Filters.Han, Filters.except(Icon.MAINTENANCE)),
                Filters.Mara_Jade,
                Filters.title(Title.No_Questions_Asked)
        );

        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, uploadCardGametextActionId)
                && GameConditions.canSpot(game, self, Filters.Cantina)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, uploadCardGametextActionId, CardSubtype.USED);
            action.setText("Upload Card");

            //Allow Responses
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, nonMainHanMaraNQA, true));
                        }
                    }
            );
            actions.add(action);
        }

        // add a battle destiny if Smuggler defending alone
        Filter targetFilter = Filters.and(Filters.your(self), Filters.smuggler, Filters.defendingBattle, Filters.alone);
        if (GameConditions.isDuringBattleWithParticipant(game, targetFilter)
                && GameConditions.canAddBattleDestinyDraws(game, self)
                && GameConditions.canTarget(game, self, targetFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Add one battle destiny");

            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose smuggler", targetFilter) {
                        @Override
                        protected boolean getUseShortcut() {
                            return true;
                        }

                        @Override
                        protected void cardTargeted(final int targetGroupId1, final PhysicalCard targetFilter) {
                            action.addAnimationGroup(targetFilter);
                            // Allow response(s)
                            action.allowResponses("Add one battle destiny by targeting " + GameUtils.getCardLink(targetFilter),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new AddBattleDestinyEffect(action, 1));
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }


        Filter smugglerPiloting = Filters.and(Filters.your(playerId), Filters.smuggler, Filters.piloting(Filters.starship));
        Filter filter = Filters.and(Filters.Lateral_Damage, Filters.at(Filters.sameSystemAs(self, Filters.and(smugglerPiloting, Filters.with(self, smugglerPiloting)))));

        if (GameConditions.canSpot(game, self, filter) &&
                GameConditions.canTargetToCancel(game, self, Filters.Lateral_Damage)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Lateral_Damage, Title.Lateral_Damage);
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();
        Filter smugglerPiloting = Filters.and(Filters.your(playerId), Filters.smuggler, Filters.piloting(Filters.starship));
        Filter filter = Filters.and(Filters.starship, Filters.at(Filters.sameSystemAs(self, Filters.and(smugglerPiloting, Filters.with(self, smugglerPiloting)))));

        // Check condition(s)
        if (TriggerConditions.isPlayingCardTargeting(game, effect, Filters.Overwhelmed, filter)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {
            PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            actions.add(action);
        }

        // Check condition(s)
        if (TriggerConditions.isPlayingCardTargeting(game, effect, Filters.or(Filters.Hidden_Weapons, Filters.You_Are_Beaten), Filters.smuggler)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {
            PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            actions.add(action);
        }

        Collection<TargetingReason> targetingReasons = Collections.singletonList(TargetingReason.TO_BE_LOST);
        if (TriggerConditions.isTargetedForReasonBy(game, effect, game.getOpponent(playerId), Filters.smuggler, Filters.Dr_Evazan, targetingReasons)) {
            final RespondableEffect respondableEffect = (RespondableEffect) effect;
            final List<PhysicalCard> cardsTargeted = TargetingActionUtils.getCardsTargetedForReason(game, respondableEffect.getTargetingAction(), targetingReasons, Filters.smuggler);
            if (!cardsTargeted.isEmpty()) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
                action.setText("Cancel targeting");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose smuggler", Filters.in(cardsTargeted)) {
                            @Override
                            protected void cardTargeted(final int targetGroupId1, final PhysicalCard smugglerTargeted) {
                                action.addAnimationGroup(smugglerTargeted);
                                // Allow response(s)
                                action.allowResponses("Cancel targeting of " + GameUtils.getCardLink(smugglerTargeted),
                                        new RespondablePlayCardEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new CancelTargetingEffect(action, respondableEffect));
                                            }
                                        }
                                );
                            }
                        }
                );
                return Collections.singletonList(action);
            }
        }

        return actions;
    }
}