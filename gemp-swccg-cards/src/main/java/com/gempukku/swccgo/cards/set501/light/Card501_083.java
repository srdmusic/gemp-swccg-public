package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDefensiveShield;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Effect
 * Title: You Want To Make That Move?
 */
public class Card501_083 extends AbstractDefensiveShield {
    public Card501_083() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "You Want To Make That Move?", ExpansionSet.SET_20, Rarity.V);
        setGameText("Plays on table. At the end of every turn, players lose 1 force if they have more than 12 cards in hand. At the end of the opponent’s control phase, a player loses 1 Force for each under cover spy they control. Grimtaash and Monnok are canceled.");
        addIcons(Icon.VIRTUAL_DEFENSIVE_SHIELD);
        setTestingText("You Want To Make That Move?");
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


        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        // At the end of every turn, players lose 1 force if they have more than 12 cards in hand.
        if (TriggerConditions.isEndOfEachTurn(game, effectResult)) {
            if (GameConditions.numCardsInHand(game, playerId) > 12) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Lose 1 Force");
                action.appendEffect(
                        new LoseForceEffect(action, playerId, 1));
                actions.add(action);
            }

            gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;
            if (GameConditions.numCardsInHand(game, opponent) > 12) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Opponent loses 1 Force");
                action.appendEffect(
                        new LoseForceEffect(action, opponent, 1));
                actions.add(action);
            }
        }

        if (TriggerConditions.isEndOfOpponentsPhase(game, effectResult, Phase.CONTROL, playerId)) {
            int playersUCspies = Filters.countActive(game, self, SpotOverride.INCLUDE_UNDERCOVER, Filters.and(Filters.your(playerId), Filters.undercover_spy));
            int opponentUCspies = Filters.countActive(game, self, SpotOverride.INCLUDE_UNDERCOVER, Filters.and(Filters.your(opponent), Filters.undercover_spy));

            gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_3;
            if (playersUCspies > 0) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Lose "+playersUCspies+" Force");
                action.appendEffect(
                        new LoseForceEffect(action, playerId, playersUCspies));
                actions.add(action);
            }

            gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_4;
            if (opponentUCspies > 0) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Opponent loses "+opponentUCspies+" Force");
                action.appendEffect(
                        new LoseForceEffect(action, opponent, opponentUCspies));
                actions.add(action);
            }
        }
        return actions;
    }
}