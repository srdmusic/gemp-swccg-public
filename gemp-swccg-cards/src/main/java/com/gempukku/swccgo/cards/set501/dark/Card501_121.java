package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceRetrievalEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.PlaceCardOutOfPlayFromLostPileEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToRetrieveForceResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used
 * Title: Ommni Box & It's Worse (V)
 */
public class Card501_121 extends AbstractUsedInterrupt {
    public Card501_121() {
        super(Side.DARK, 5, "Ommni Box & It's Worse");
        addComboCardTitles(Title.Ommni_Box, Title.Its_Worse);
        setVirtualSuffix(true);
        setGameText("For remainder of turn, opponent may not retrieve Force from Resistance characters’ game text. OR Cancel It Could Be Worse. OR Place Projection Of A Skywalker in owner’s Used Pile. OR If you occupy more battlegrounds than opponent, suspend Menace Fades for remainder of turn. OR Search opponent's Lost Pile; place one device you find there out of play. (Immune to It's A Hit!)");
        addIcons(Icon.REFLECTIONS_II, Icon.VIRTUAL_SET_18);
        setTestingText("Ommni Box & It's Worse (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        final PlayInterruptAction action1 = new PlayInterruptAction(game, self);
        action1.setText("Prevent retrieval by Resistance characters");
        action1.setImmuneTo(Title.Its_A_Hit);
        // Allow response(s)
        action1.allowResponses("Prevent retrieval from game text of Resistance characters for remainder of turn",
                new RespondablePlayCardEffect(action1) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        // Perform result(s)
                        final int permCardId = self.getPermanentCardId();
                        final int gameTextSourceCardId = self.getCardId();
                        action1.appendEffect(
                                new AddUntilEndOfTurnActionProxyEffect(action1, new AbstractActionProxy() {

                                    @Override
                                    public List<TriggerAction> getRequiredAfterTriggers(SwccgGame game, EffectResult effectResult) {
                                        final PhysicalCard self = game.findCardByPermanentId(permCardId);
                                        if (TriggerConditions.isAboutToRetrieveForce(game, effectResult, game.getOpponent(self.getOwner()))) {
                                            PhysicalCard retrievingCard = ((AboutToRetrieveForceResult)effectResult).getSourceCard();

                                            if (retrievingCard != null && Filters.Resistance_character.accepts(game, retrievingCard)) {

                                                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                                                action.setText("Cancel retrieval");
                                                action.appendEffect(
                                                        new CancelForceRetrievalEffect(action)
                                                );
                                                return Collections.singletonList((TriggerAction) action);
                                            }
                                        }
                                        return null;
                                    }
                                }));
                    }
                }
        );
        actions.add(action1);


        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.It_Could_Be_Worse)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.It_Could_Be_Worse, Title.It_Could_Be_Worse);
            action.setImmuneTo(Title.Its_A_Hit);
            actions.add(action);
        }


        if (GameConditions.canTarget(game, self, Filters.title(Title.Projection_Of_A_Skywalker))) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Place Projection Of A Skywalker in Used Pile");
            action.setImmuneTo(Title.Its_A_Hit);
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target Projection Of A Skywalker to place in Used Pile", Filters.title(Title.Projection_Of_A_Skywalker)) {

                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.allowResponses("Place " + GameUtils.getCardLink(targetedCard) + " in Used Pile", new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard projection = action.getPrimaryTargetCard(targetGroupId);
                            action.appendEffect(new PlaceCardInUsedPileFromTableEffect(action, projection));
                        }
                    });
                }
            });
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

        GameTextActionId gameTextActionId = GameTextActionId.OMMNI_BOX_ITS_WORSE_V__SEARCH_LOST_PILE;
        final String opponent = game.getOpponent(playerId);

        if (GameConditions.canSearchOpponentsLostPile(game, playerId, self, gameTextActionId)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Search opponent's Lost Pile");
            action.setImmuneTo(Title.Its_A_Hit);
            action.allowResponses("Place a device out of play from opponent's Lost Pile", new RespondablePlayCardEffect(action) {
                @Override
                protected void performActionResults(Action targetingAction) {
                    action.appendEffect(
                            new PlaceCardOutOfPlayFromLostPileEffect(action, playerId, opponent, Filters.device, false));
                }
            });
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.It_Could_Be_Worse)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            action.setImmuneTo(Title.Its_A_Hit);
            actions.add(action);
        }

        return actions;
    }
}