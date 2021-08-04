package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddDestinyToTotalPowerEffect;
import com.gempukku.swccgo.cards.effects.CancelBattleEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.RetrieveCardEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Return Of A Jedi (V)
 */
public class Card501_032 extends AbstractUsedOrLostInterrupt {
    public Card501_032() {
        super(Side.LIGHT, 3, Title.Return_Of_A_Jedi);
        setLore("'Where did you dig up that old fossil?' 'I don't think he exists anymore.' 'Surely he must be dead by now.' 'I can't believe he's gone.' 'Oh, he's not dead, not yet.' Obi's back!");
        setGameText("USED: Use 1 Force to take a card with 'Obi-Wan' in title into hand from Reserve Deck; reshuffle (or, once per game, retrieve into hand). " +
                "LOST: If opponent just initiated battle where Obi-Wan alone on Tatooine, choose: Add one destiny to power. OR Opponent must use 3 Force or cancel the battle.");
        addIcon(Icon.VIRTUAL_SET_16);
        setVirtualSuffix(true);
        setTestingText("Return Of A Jedi (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.battleInitiatedAt(game, effectResult, Filters.on(Title.Tatooine))
                && GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.ObiWan, Filters.alone))) {

            final PlayInterruptAction action1 = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action1.setText("Add one destiny to power");
            // Allow response(s)
            action1.allowResponses("Add one destiny to power",
                    new RespondablePlayCardEffect(action1) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action1.appendEffect(
                                    new AddDestinyToTotalPowerEffect(action1, 1)
                            );
                        }
                    }
            );
            actions.add(action1);

            final int amountOfForce = 3;
            final PlayInterruptAction action2 = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action2.setText("Prevent opponent from battling");
            if (GameConditions.canUseForce(game, opponent, amountOfForce)) {
                action2.appendEffect(
                        new PlayoutDecisionEffect(action2, opponent,
                                new YesNoDecision("Pay " + amountOfForce + " force to prevent the battle from being cancelled?") {
                                    @Override
                                    protected void yes() {
                                        game.getGameState().sendMessage(opponent + " chooses to use " + amountOfForce + " Force");
                                        action2.appendEffect(
                                                new UseForceEffect(action2, opponent, amountOfForce));
                                    }

                                    @Override
                                    protected void no() {
                                        game.getGameState().sendMessage(playerId + " cancels the battle");
                                        action2.appendEffect(
                                                new CancelBattleEffect(action2));
                                    }
                                }
                        )
                );
            } else {
                action2.appendEffect(
                        new CancelBattleEffect(action2)
                );
            }
            actions.add(action2);
        }
        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        if (GameConditions.canUseForceToPlayInterrupt(game, playerId, self, 1)) {
            GameTextActionId gameTextActionId = GameTextActionId.RETURN_OF_A_JEDI__RETRIEVE_OBIWAN_INTO_HAND;

            // Check condition(s)
            if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                    && GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
                action.setText("Retrieve Obi-Wan");
                // Pay cost(s)
                action.appendCost(
                        new UseForceEffect(action, playerId, 1));
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new RetrieveCardEffect(action, playerId, Filters.ObiWan));
                            }
                        }
                );
                actions.add(action);
            }

            gameTextActionId = GameTextActionId.RETURN_OF_A_JEDI__UPLOAD_CARD_WITH_OBIWAN_IN_TITLE;

            // Check condition(s)
            if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
                action.setText("Take a card with 'Obi-Wan' in title into hand from Reserve Deck");
                // Pay cost(s)
                action.appendCost(
                        new UseForceEffect(action, playerId, 1));
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.titleContains("Obi-Wan"), true));
                            }
                        }
                );
                actions.add(action);
            }
        }

        return actions;
    }
}