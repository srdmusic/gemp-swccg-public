package com.gempukku.swccgo.cards.set501.dark;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used
 * Title: Blast Points (V)
 */
public class Card501_053 extends AbstractUsedInterrupt {
    public Card501_053() {
        super(Side.DARK, 5, "Blast Points", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Only Imperial stormtroopers are so precise.");
        setGameText("[Upload] Ghhhk or Hyperwave Scan. OR If you just won a battle, cancel Tatooine Celebration. OR If opponent just looked at cards in their Force Pile or Used Pile, peek at the top 2 cards of your Reserve Deck; take one into hand.");
        addIcons(Icon.SPECIAL_EDITION, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Blast Points (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        Filter pullFilter = Filters.or(Filters.Ghhhk, Filters.title("Hyperwave Scan"));
        GameTextActionId gameTextActionId = GameTextActionId.BLAST_POINTS_V__UPLOAD_CARD;

        // Check condition(s)
        if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);

            action.setText("Take card into hand from Reserve Deck");

            action.allowResponses("Take Ghhhk or Hyperwave Scan into hand from Reserve Deck",
                new RespondablePlayCardEffect(action) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        // Perform result(s)
                        action.appendEffect(
                                new TakeCardIntoHandFromReserveDeckEffect(action, playerId, pullFilter, true));
                    }
                }
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.wonBattle(game, effectResult, playerId)
                && GameConditions.canTargetToCancel(game, self, Filters.Tatooine_Celebration)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Tatooine_Celebration, Title.Tatooine_Celebration);
            return Collections.singletonList(action);
        }
        return null;
    }
}