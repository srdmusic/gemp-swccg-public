package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.ModifyTotalBattleDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotBeFiredModifier;
import com.gempukku.swccgo.logic.modifiers.ExtraForceCostToPlayInterruptModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ArtworkCardRevealedResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: History, Philosophy, And Art
 */
public class Card501_041 extends AbstractUsedOrLostInterrupt {
    public Card501_041() {
        super(Side.DARK, 5, "History, Philosophy, And Art", Uniqueness.UNIQUE);
        setGameText("USED: During battle, add 1 to your total battle destiny for each card stacked on Thrawn's Art Collection. LOST: Once per game, if you just revealed an Interrupt as 'artwork,' for remainder of battle, opponent may not fire weapons and must first use 1 Force to play an Interrupt.");
        addIcons(Icon.VIRTUAL_SET_19);
        setTestingText("History, Philosophy, And Art");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.and(Filters.Thrawns_Art_Collection, Filters.hasStacked(Filters.any)))
                && GameConditions.isDuringBattle(game)) {

            final int num = Filters.countStacked(game, Filters.stackedOn(self, Filters.Thrawns_Art_Collection));
            if (num>0) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
                action.setText("Add "+num+" to total battle destiny");
                // Choose target(s)
                action.allowResponses("Add "+num+" to total battle destiny", new RespondablePlayCardEffect(action) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        action.appendEffect(new ModifyTotalBattleDestinyEffect(action, playerId, num));
                    }
                });
                actions.add(action);
            }
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        final String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.HISTORY_PHILOSOPHY_AND_ART__STOP_INTERRUPTS_AND_WEAPONS;

        if (effectResult.getType() == EffectResult.Type.ARTWORK_CARD_REVEALED
                && GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.isDuringBattle(game)) {

            PhysicalCard artwork = ((ArtworkCardRevealedResult) effectResult).getCard();

            if (artwork != null
                    && Filters.Interrupt.accepts(game, artwork)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.LOST);
                action.setText("Prevent opponent from firing weapons and they must use 1 Force to play Interrupts");

                action.appendUsage(
                        new OncePerGameEffect(action));

                action.allowResponses("Prevent opponent from firing weapons and they must pay 1 force to play interrupts.", new RespondablePlayCardEffect(action) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        action.appendEffect(
                                new AddUntilEndOfBattleModifierEffect(action,
                                        new ExtraForceCostToPlayInterruptModifier (self, Filters.and(Filters.opponents(self), Filters.Interrupt), 1),
                                        "Opponent must use 1 Force to play Interrupts"));
                        action.appendEffect(
                                new AddUntilEndOfBattleModifierEffect(action,
                                        new MayNotBeFiredModifier(self, Filters.and(Filters.opponents(playerId), Filters.any)), "Prevents opponent's weapons from being fired")
                        );
                    }
                });

                return Collections.singletonList(action);
            }
        }

        return null;
    }
}