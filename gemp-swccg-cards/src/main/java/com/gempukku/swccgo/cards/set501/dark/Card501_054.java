package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToPutOnBottomEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Surely You Can Do Better
 */
public class Card501_054 extends AbstractUsedOrLostInterrupt {
    public Card501_054() {
        super(Side.DARK, 4, "Surely You Can Do Better", Uniqueness.UNIQUE);
        setLore("Those who can use the Force are able to manipulate the objects around them to their advantage.");
        setGameText("USED: If opponent occupies your location, peek at top two cards of opponent's Reserve Deck; you may place one of those cards on bottom of that Reserve Deck. " +
                "LOST: Once per game, during battle at Generator Core, cancel a non-[Immune to Sense.] Interrupt.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_16);
        setTestingText("Surely You Can Do Better");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        GameTextActionId gameTextActionId = GameTextActionId.SURELY_YOU_CAN_DO_BETTER__CANCEL_INTERRUPT;

        Filter filter = Filters.and(Filters.not(Filters.immune_to_Sense), Filters.Interrupt);

        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, filter)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.isDuringBattleAt(game, Filters.Theed_Palace_Generator_Core)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.canSpotLocation(game, Filters.and(Filters.your(self), Filters.location, Filters.occupies(opponent)))
                && GameConditions.hasReserveDeck(game, opponent)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Peek at top two cards of Reserve Deck");
            // Allow response(s)
            action.allowResponses("Peek at top two cards of opponent's Reserve Deck and place one on bottom",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new PeekAtTopCardsOfReserveDeckAndChooseCardsToPutOnBottomEffect(action, opponent, 2, 1, 1));
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }
}
