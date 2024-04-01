package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.CardSubtype;
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
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.CancelGameTextUntilEndOfTurnEffect;
import com.gempukku.swccgo.logic.effects.LookAtLostPileEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardsOutOfPlayFromOffTableEffect;
import com.gempukku.swccgo.logic.effects.PutStackedCardInLostPileEffect;
import com.gempukku.swccgo.logic.effects.RecirculateEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.StackCardFromLostPileEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: The Empire's Back (V)
 */
public class Card501_064 extends AbstractUsedOrLostInterrupt {
    public Card501_064() {
        super(Side.DARK, 3, "The Empire's Back", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("No star system will dare oppose the Emperor now.");
        setGameText("USED: Deploy Empire's New Order or Overseeing It Personally from Reserve Deck; reshuffle. LOST: Once per game, choose: Stack an Interrupt from opponent’s Lost Pile on [V] A Useless Gesture. OR If Xizor (or two Imperial leaders) in battle, re-circulate.");
        addIcons(Icon.VIRTUAL_SET_23);
        setVirtualSuffix(true);
        setTestingText("The Empire's Back (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        GameTextActionId gameTextActionId = GameTextActionId.THE_EMPIRES_BACK_V__DOWNLOAD_EFFECTS;

        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Deploy a card from Reserve Deck");
            action.setActionMsg("Deploy Empire's New Order or Overseeing It Personally from Reserve Deck");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Empires_New_Order, Filters.Overseeing_It_Personally), true));
                        }
                    }
            );
            actions.add(action);
        }

        GameTextActionId gameTextActionId2 = GameTextActionId.THE_EMPIRES_BACK_V__STACK_CARD_OR_RECIRCULATE;
        final String opponent = game.getOpponent(playerId);

        // Check Condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId2)) {
            if (GameConditions.canSpot(game, self, Filters.and(Filters.A_Useless_Gesture, Filters.icon(Icon.VIRTUAL_DEFENSIVE_SHIELD)))) {
                final PhysicalCard auselessgesturev = Filters.findFirstActive(game, self, Filters.and(Filters.A_Useless_Gesture, Filters.icon(Icon.VIRTUAL_DEFENSIVE_SHIELD)));
                if (! GameConditions.hasStackedCards(game, auselessgesturev)) {
                    final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId2, CardSubtype.LOST);
                    action.setText("Stack a card on A Useless Gesture from opponent's Lost Pile.");
                    // Update usage limit(s)
                    action.appendUsage(
                            new OncePerGameEffect(action));
                    // Allow response(s)
                    action.allowResponses(
                            new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    // Perform result(s)
                                    action.appendEffect(
                                            new LookAtLostPileEffect(action, playerId, opponent) {
                                                @Override
                                                protected void cardsInCardPile(List<PhysicalCard> cardsInCardPile) {
                                                    Collection<PhysicalCard> dockingBays = Filters.filter(cardsInCardPile, game, Filters.docking_bay);
                                                    if (!dockingBays.isEmpty()) {
                                                        action.appendEffect(
                                                                new StackCardFromLostPileEffect(action, dockingBays));
                                                    }
                                                }
                                            });
                                }
                            }
                    );
                    actions.add(action);
                }
            }
        
            if (GameConditions.isDuringBattle(game)
                    && (GameConditions.canTarget(game, self, Filters.and(Filters.Xizor, Filters.participatingInBattle)) 
                        || GameConditions.canTarget(game, self, 2, Filters.and(Filters.Imperial_leader, Filters.participatingInBattle)))
                    && GameConditions.hasUsedPile(game, playerId)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId2, CardSubtype.LOST);
                action.setText("Re-circulate");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerGameEffect(action));
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new RecirculateEffect(action, playerId));                                            
                            }
                        }
                );
                actions.add(action);
            }
        }
        return actions;
    }
}
