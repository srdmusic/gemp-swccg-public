package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.MoveCardUsingLandspeedEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: A Jedi
 */
public class Card501_052 extends AbstractUsedInterrupt {
    public Card501_052() {
        super(Side.LIGHT, 5, "A Jedi", Uniqueness.RESTRICTED_2, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Once per game, if Luke is on table when you play this Interrupt, may return it to hand. Take Grogu or R2-D2 into hand from Reserve Deck; reshuffle. OR If a battle just initiated at same site as Grogu, move Luke there from an adjacent site. OR If Grogu and Luke in battle, add one battle destiny.");
        addIcons(Icon.VIRTUAL_SET_24);
        setTestingText("A Jedi");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.A_JEDI__UPLOAD_CHARACTER;

        // Check condition(s)
        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Take Grogu or R2-D2 into hand from Reserve Deck");

            // add option to return it to hand when it resolves once per game
            addChoiceToReturnToHandOncePerGame(game, self, action);

            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.Grogu, Filters.R2D2), true));
                        }
                    }
            );
            actions.add(action);
        }


        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.Grogu, Filters.canBeTargetedBy(self)))
                && GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.Luke, Filters.canBeTargetedBy(self)))
                && GameConditions.canAddBattleDestinyDraws(game, self)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Add one battle destiny");

            // add option to return it to hand when it resolves once per game
            addChoiceToReturnToHandOncePerGame(game, self, action);

            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddBattleDestinyEffect(action, 1));
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        Filter filter = Filters.and(Filters.Luke, Filters.movableAsRegularMoveUsingLandspeed(playerId, false, false, true, 0, null, Filters.battleLocation));

        // Check condition(s)
        if (TriggerConditions.battleInitiatedAt(game, effectResult, Filters.adjacentSiteTo(self, filter))
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Grogu)) {

            final PhysicalCard battleLocation = game.getGameState().getBattleLocation();
            if (GameConditions.canTarget(game, self, filter)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self);
                action.setText("Move Luke to battle");

                // add option to return it to hand when it resolves once per game
                addChoiceToReturnToHandOncePerGame(game, self, action);

                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose Luke", filter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                                action.addAnimationGroup(targetedCard);
                                action.addAnimationGroup(battleLocation);
                                // Allow response(s)
                                action.allowResponses("Move " + GameUtils.getCardLink(targetedCard) + " to " + GameUtils.getCardLink(battleLocation),
                                        new RespondablePlayCardEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Get the targeted card(s) from the action using the targetGroupId.
                                                // This needs to be done in case the target(s) were changed during the responses.
                                                final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                                // Perform result(s)
                                                action.appendEffect(
                                                        new MoveCardUsingLandspeedEffect(action, playerId, finalTarget, true, Filters.battleLocation));
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


    private void addChoiceToReturnToHandOncePerGame(SwccgGame game, PhysicalCard self, PlayInterruptAction action) {
        GameTextActionId gameTextActionId = GameTextActionId.A_JEDI__TAKE_INTO_HAND;
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canTarget(game, self, Filters.Luke)) {
            PlayInterruptAction blankAction = new PlayInterruptAction(game, self, gameTextActionId);
            action.setNeedsDecisionReturnToHandWhenResolving(true, new OncePerGameEffect(blankAction));
        }
    }
}