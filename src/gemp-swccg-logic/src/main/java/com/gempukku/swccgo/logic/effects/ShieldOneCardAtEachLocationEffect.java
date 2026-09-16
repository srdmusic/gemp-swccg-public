package com.gempukku.swccgo.logic.effects;

import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.SubAction;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardsOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.DestinyModifier;
import com.gempukku.swccgo.logic.timing.AbstractSubActionEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.CardsShieldedResult;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An effect that causes the player to choose one card at each location to be 'shielded'
 * (add 3 to destiny number of) until end of turn.
 */
public class ShieldOneCardAtEachLocationEffect extends AbstractSubActionEffect {
    private String _performingPlayerId;
    private Collection<PhysicalCard> _cards = new ArrayList<PhysicalCard>();
    private ShieldOneCardAtEachLocationEffect _that;

    /**
     * Creates an effect that 'shields' (adds 3 to destiny) one card at each location until end of turn
     * @param action the action performing this effect
     * @param cards the cards a player can choose to 'shield'
     */
    public ShieldOneCardAtEachLocationEffect(Action action, Collection<PhysicalCard> cards) {
        super(action);
        _performingPlayerId = action.getPerformingPlayer();
        _cards.addAll(cards);
        _that = this;
    }

    @Override
    public boolean isPlayableInFull(SwccgGame game) {
        return true;
    }

    @Override
    protected SubAction getSubAction(SwccgGame game) {
        final SubAction subAction = new SubAction(_action, _performingPlayerId != null ? _performingPlayerId : game.getGameState().getCurrentPlayerId());

        subAction.appendEffect(
                new PassthruEffect(subAction) {
                    @Override
                    protected void doPlayEffect(SwccgGame game) {
                        // Filter for cards that are still on the table and at a location
                        _cards = Filters.filter(_cards, game, Filters.and(Filters.onTable, Filters.at(Filters.location)));

                        if (!_cards.isEmpty()) {
                            subAction.appendEffect(
                                    new ChooseNextCardToShield(subAction, game, _cards));
                        }
                    }
                }
        );
        return subAction;
    }

    @Override
    protected boolean wasActionCarriedOut() {
        return true;
    }

    /**
     * A private effect for choosing the next card to 'shield' (add 3 to destiny)
     */
    private class ChooseNextCardToShield extends ChooseCardsOnTableEffect {
        private SubAction _subAction;
        private SwccgGame _game;
        private Collection<PhysicalCard> _remainingCards;

        /**
         * Creates an effect for choosing the next card to 'shield' (add 3 to destiny)
         * @param subAction the action
         * @param remainingCards the remaining cards eligible to be 'shielded'
         */
        public ChooseNextCardToShield(SubAction subAction, SwccgGame game, Collection<PhysicalCard> remainingCards) {
            super(subAction, subAction.getPerformingPlayer(), "Choose card to 'shield'", 1, 1, remainingCards);
            _subAction = subAction;
            _game = game;
            _remainingCards = remainingCards;
        }

        @Override
        protected void cardsSelected(Collection<PhysicalCard> selectedCards) {
            for (PhysicalCard selectedCard : selectedCards) {

                // SubAction to carry out 'shielding' a card
                PhysicalCard source = _action.getActionSource();
                SubAction shieldCardsSubAction = new SubAction(_subAction);
                shieldCardsSubAction.appendEffect(
                            new AddUntilEndOfTurnModifierEffect(_subAction,
                                    new DestinyModifier(source, Filters.in(selectedCards), 3),
                                    "'Shields' (adds 3 to destiny number of) " + GameUtils.getAppendedNames(selectedCards)));
                // Stack sub-action
                _subAction.stackSubAction(shieldCardsSubAction);

                _remainingCards.remove(selectedCard);
                _remainingCards.removeIf(otherCard -> Filters.atSameLocation(selectedCard).accepts(_game, otherCard));

                if (!_remainingCards.isEmpty()) {

                    _subAction.appendEffect(
                            new PassthruEffect(_subAction) {
                                @Override
                                protected void doPlayEffect(SwccgGame game) {
                                    // Filter for cards that are still on the table and at a location
                                    _remainingCards = Filters.filter(_remainingCards, game, Filters.and(Filters.onTable, Filters.at(Filters.location)));

                                    if (!_remainingCards.isEmpty()) {
                                        _subAction.appendEffect(
                                                new ShieldOneCardAtEachLocationEffect.ChooseNextCardToShield(_subAction, _game, _remainingCards));
                                    }
                                }
                            }
                    );
                }
                //emit result (no cards left to shield)
                else {
                    _subAction.appendEffect(
                        new PassthruEffect(_subAction) {
                            @Override
                            protected void doPlayEffect(SwccgGame game) {
                                game.getActionsEnvironment().emitEffectResult(new CardsShieldedResult(_action.getPerformingPlayer()));
                            }
                        }
                    );
                }
            }
        }
    }
}
