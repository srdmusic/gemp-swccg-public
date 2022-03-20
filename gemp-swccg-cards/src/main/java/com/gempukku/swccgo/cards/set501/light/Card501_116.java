package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.complete.ChooseExistingCardPileEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.PlaceCardOutOfPlayFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.StealCardIntoHandFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromUsedPileEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.LostFromTableResult;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used
 * Title: Changing The Odds & Darth Maul's Demise
 */
public class Card501_116 extends AbstractUsedInterrupt {
    public Card501_116() {
        super(Side.LIGHT, 5, "Changing The Odds & Darth Maul's Demise", Uniqueness.UNIQUE);
        addComboCardTitles("Changing The Odds", "Darth Maul's Demise");
        setGameText("If opponent just drew battle destiny, subtract one from that destiny. OR If a battle was just initiated, cancel [Set 13] Maul's game text at a different location. OR If opponent just lost a unique (•) character, may activate 1 Force (if a gangster, may also retrieve 1 Force; if a Dark Jedi may take any one card into hand from Used Pile; reshuffle). OR Once per game, search any Lost Pile; take a stolen (or [Reflections III]) lightsaber into hand.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_18);
        setTestingText("Changing The Odds & Darth Maul's Demise");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.isBattleDestinyJustDrawnBy(game, effectResult, opponent)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Subtract 1 from destiny");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ModifyDestinyEffect(action, -1));
                        }
                    }
            );
            actions.add(action);
        }

        if (TriggerConditions.battleInitiated(game, effectResult)
                && GameConditions.canTarget(game, self, Filters.and(Icon.VIRTUAL_SET_13, Filters.Maul, Filters.not(Filters.at(Filters.battleLocation))))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Cancel Maul's game text");

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target [Set 13] Maul", Filters.and(Icon.VIRTUAL_SET_13, Filters.Maul, Filters.not(Filters.at(Filters.battleLocation)))) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    // Allow response(s)
                    action.allowResponses(
                            new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                    // Perform result(s)
                                    action.appendEffect(
                                            new CancelGameTextUntilEndOfBattleEffect(action, finalTarget));
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


        // if opponent just lost a unique (•) character, may activate 1 Force (if a gangster, may also retrieve 1 Force; if a Dark Jedi may take any one card into hand from Used Pile; reshuffle).
        final GameTextActionId gameTextActionId = GameTextActionId.CHANGING_THE_ODDS_DARTH_MAULS_DEMISE__SEARCH_USED_PILE;
        if (TriggerConditions.justLost(game, effectResult, opponent, Filters.and(Filters.unique, Filters.character))) {

            final PhysicalCard justLostCharacter = ((LostFromTableResult) effectResult).getCard();

            if (justLostCharacter != null) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
                action.setText("Activate 1 Force");

                action.allowResponses(new RespondablePlayCardEffect(action) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        if (GameConditions.canActivateForce(game, playerId)) {
                            action.appendEffect(new PlayoutDecisionEffect(action, playerId, new YesNoDecision("Activate 1 Force?") {
                                @Override
                                protected void yes() {
                                    action.insertEffect(new ActivateForceEffect(action, playerId, 1));
                                }

                                @Override
                                protected void no() {
                                    action.insertEffect(new SendMessageEffect(action, playerId + " chooses to not activate Force"));
                                }
                            }));
                        } else {
                            action.insertEffect(new SendMessageEffect(action, playerId + " is unable to activate Force"));
                        }

                        if (Filters.gangster.accepts(game, justLostCharacter)) {
                            action.appendEffect(new PlayoutDecisionEffect(action, playerId, new YesNoDecision("Retrieve 1 Force?") {
                                @Override
                                protected void yes() {
                                    action.insertEffect(new RetrieveForceEffect(action, playerId, 1));
                                }

                                @Override
                                protected void no() {
                                    action.insertEffect(new SendMessageEffect(action, playerId + " chooses to not retrieve Force"));
                                }
                            }));
                        }

                        if (GameConditions.canSearchUsedPile(game, playerId, self, gameTextActionId)
                                && Filters.Dark_Jedi.accepts(game, justLostCharacter)) {
                            action.appendEffect(new PlayoutDecisionEffect(action, playerId, new YesNoDecision("Take a card into hand from Used Pile?") {
                                @Override
                                protected void yes() {
                                    action.insertEffect(new TakeCardIntoHandFromUsedPileEffect(action, playerId, true));
                                }

                                @Override
                                protected void no() {
                                    action.insertEffect(new SendMessageEffect(action, playerId + " chooses to not take a card into hand from Used Pile"));
                                }
                            }));
                        }
                    }
                });

                actions.add(action);
            }
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.CHANGING_THE_ODDS_DARTH_MAULS_DEMISE__SEARCH_LOST_PILE;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && (GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)
                || GameConditions.canSearchOpponentsLostPile(game, playerId, self, gameTextActionId))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Search for a lightsaber in a Lost Pile");

            action.appendUsage(
                    new OncePerGameEffect(action));

            action.appendTargeting(new ChooseExistingCardPileEffect(action, playerId, Zone.LOST_PILE) {
                @Override
                protected void pileChosen(SwccgGame game, String cardPileOwner, Zone cardPile) {
                    final Filter lightsaberFilter = Filters.and(Filters.lightsaber, Filters.or(Filters.stolen, Icon.REFLECTIONS_III));
                    if (playerId.equals(cardPileOwner)) {
                        // searching own Lost Pile
                        action.allowResponses(
                                new RespondablePlayCardEffect(action) {
                                    @Override
                                    protected void performActionResults(Action targetingAction) {
                                        // Perform result(s)
                                        action.appendEffect(
                                                new TakeCardIntoHandFromLostPileEffect(action, playerId, lightsaberFilter, false));
                                    }
                                }
                        );
                    } else {
                        // searching opponent's Lost Pile
                        action.allowResponses(
                                new RespondablePlayCardEffect(action) {
                                    @Override
                                    protected void performActionResults(Action targetingAction) {
                                        // Perform result(s)
                                        action.appendEffect(
                                                new StealCardIntoHandFromLostPileEffect(action, playerId, lightsaberFilter));
                                    }
                                }
                        );
                    }
                }
            });

            actions.add(action);
        }

        return actions;
    }
}