package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.ExchangeCardInHandWithCardInLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromForcePileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.LostFromTableResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Interrupt
 * Subtype: Used
 * Title: See You Around, Kid & Where's Han?
 */
public class Card501_047 extends AbstractUsedInterrupt {
    public Card501_047() {
        super(Side.LIGHT, 4, "See You Around, Kid & Where's Han?", Uniqueness.UNIQUE);
        addComboCardTitles("See You Around, Kid", "Where's Han?");
        setGameText("When you play this Interrupt, if Han (or Luke) and Kylo on table, opponent loses 1 Force.\n" +
                "Take non-[Reflections III] Han into hand from Reserve Deck; reshuffle. OR Cancel the game text of a First Order character at same site as Han, Luke, or Rey for remainder of turn. OR During your draw phase, may place a card from hand under your Used Pile to take a card into hand from Force Pile; reshuffle. OR During battle, cancel an attempt to cancel your destiny draw.");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_15);
        setTestingText("See You Around, Kid & Where's Han?");
        hideFromDeckBuilder();
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredInterruptPlayedTriggers(SwccgGame game, Effect effect, PhysicalCard self) {
        String opponent = game.getOpponent(self.getOwner());

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.or(Filters.Han, Filters.Luke))
            && GameConditions.canSpot(game, self, Filters.Kylo)) {
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, self.getCardId());
            action.setText("Make " + opponent + " lose 1 Force");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // Take non-[Reflections III] Han into hand from Reserve Deck; reshuffle.
        GameTextActionId uploadCardGametextActionId = GameTextActionId.WHERES_HAN_UPLOAD_CARD;
        // Check condition(s)
        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, uploadCardGametextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, uploadCardGametextActionId, CardSubtype.USED);
            action.setText("Take Han into hand from Reserve Deck");

            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.and(Filters.Han, Filters.not(Icon.REFLECTIONS_III)), true));
                        }
                    }
            );
            actions.add(action);
        }

        // Cancel the game text of a First Order character at same site as Han, Luke, or Rey for remainder of turn.
        Filter firstOrderCharacterFilter = Filters.and(Filters.First_Order_character, Filters.sameSiteAs(self, Filters.or(Filters.Han, Filters.Luke, Filters.Rey)));

        // Check condition(s)
        if (GameConditions.canTarget(game, self, firstOrderCharacterFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Cancel game text of a First Order character");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose First Order character", firstOrderCharacterFilter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Cancel " + GameUtils.getCardLink(targetedCard) + "'s game text",
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new CancelGameTextUntilEndOfTurnEffect(action, finalTarget));
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }

        // During your draw phase, may place a card from hand under your Used Pile to take a card into hand from Force Pile; reshuffle.
        // Check condition(s)
        if (GameConditions.isDuringYourPhase(game, playerId, Phase.DRAW)
                && GameConditions.hasInHand(game, playerId, Filters.not(self))
                && GameConditions.hasForcePile(game, playerId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Place card from hand under Used Pile");

            // Pay cost(s)
            action.appendCost(
                    new PutCardFromHandOnBottomOfUsedPileEffect(action, playerId, Filters.not(self), true));

            // Allow response(s)
            action.allowResponses("Take a card from Force Pile into hand",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromForcePileEffect(action,  playerId, true));
                        }
                    });

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(final String playerId, SwccgGame game, Effect effect, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // During battle, cancel an attempt to cancel your destiny draw.
        //TODO

        return actions;
    }
}