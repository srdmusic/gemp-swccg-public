package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.PutCardsFromHandOnReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.ShuffleReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardsFromHandEffect;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Defensive Shield
 * Title: Thrown Back (V)
 */
public class Card501_083 extends AbstractDefensiveShield {
    public Card501_083() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Thrown Back");
        setVirtualSuffix(true);
        setGameText("Plays on table. During any move phase, may use 2 Force to target opponent’s hand of > 12 cards. Opponent selects 2 cards, you randomly select 10 cards; shuffle all other hand cards into opponent’s Reserve Deck. Grimtaash and Monnok are canceled.");
        addIcons(Icon.VIRTUAL_DEFENSIVE_SHIELD);
        setTestingText("Thrown Back (V)");
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.isDuringEitherPlayersPhase(game, Phase.MOVE)
                && GameConditions.numCardsInHand(game, opponent) > 12
                && GameConditions.canUseForce(game, playerId, 2)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Target opponent's hand");

            action.appendCost(
                    new UseForceEffect(action, playerId, 2));
            // Perform result(s)
            action.appendEffect(
                    new ChooseCardsFromHandEffect(action, opponent, 2, 2) {
                        @Override
                        protected void cardsSelected(SwccgGame game, Collection<PhysicalCard> selectedCards) {
                            List<PhysicalCard> toRemove = new LinkedList<>();
                            toRemove.addAll(game.getGameState().getHand(opponent));
                            toRemove.removeAll(selectedCards);
                            Collection<PhysicalCard> random = GameUtils.getRandomCards(toRemove, 10);
                            toRemove.removeAll(random);

                            action.appendEffect(
                                    new PutCardsFromHandOnReserveDeckEffect(action, opponent, toRemove.size(), toRemove.size(), Filters.in(toRemove), true));
                            action.appendEffect(
                                    new ShuffleReserveDeckEffect(action, opponent));
                        }
                    });
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(final SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Grimtaash, Filters.Monnok))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<RequiredGameTextTriggerAction>();

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)) {
            if (GameConditions.canTargetToCancel(game, self, Filters.Grimtaash)) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                // Build action using common utility
                CancelCardActionBuilder.buildCancelCardAction(action, Filters.Grimtaash, Title.Grimtaash);
                actions.add(action);
            }
            if (GameConditions.canTargetToCancel(game, self, Filters.Monnok)) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                // Build action using common utility
                CancelCardActionBuilder.buildCancelCardAction(action, Filters.Monnok, Title.Monnok);
                actions.add(action);
            }
        }

        return actions;
    }
}