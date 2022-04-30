package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelCardResultEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.CancelDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetAllCardsAtSameLocationEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 19
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Do You Stand By Your Work?
 */
public class Card501_039 extends AbstractUsedOrLostInterrupt {
    public Card501_039() {
        super(Side.DARK, 4, "Do You Stand By Your Work?", Uniqueness.UNIQUE);
        setGameText("USED: If opponent is about to cancel and redraw a destiny, it is canceled instead. LOST: Once per game, if your objective just canceled a battle, none of the opponent's cards participating in that battle may move for remainder of turn.");
        addIcons(Icon.VIRTUAL_SET_19);
        setTestingText("Do You Stand By Your Work?");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, final Effect effect, PhysicalCard self) {
        String opponent = game.getOpponent(playerId);
        if (TriggerConditions.isPlayingCardForReason(game, effect, Filters.any, PlayCardActionReason.ATTEMPTING_TO_CANCEL_AND_REDRAW_A_DESTINY)
                && TriggerConditions.isPlayingCard(game, effect, opponent, Filters.any)
                && GameConditions.canCancelDestiny(game, playerId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Cancel destiny");
            action.allowResponses(new RespondablePlayCardEffect(action) {
                @Override
                protected void performActionResults(Action targetingAction) {
                    // cancel the redrawing
                    action.appendEffect(
                            new CancelCardResultEffect(action, effect));
                    // and then cancel the destiny`
                    action.appendEffect(
                            new CancelDestinyEffect(action));
                }
            });

            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        GameTextActionId gameTextActionId = GameTextActionId.DO_YOU_STAND_BY_YOUR_WORK__PREVENT_MOVEMENT;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && TriggerConditions.battleCanceledBy(game, effectResult, playerId, Filters.and(Filters.your(self), Filters.Objective))) {

            final PhysicalCard battleLocation = Filters.findFirstFromTopLocationsOnTable(game, Filters.battleLocation);

            if (battleLocation != null) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.LOST);
                action.setText("Prevent cards from moving");

                action.appendUsage(
                        new OncePerGameEffect(action));
                action.appendTargeting(new TargetAllCardsAtSameLocationEffect(action, playerId, "Prevent opponent's cards from moving", Filters.and(Filters.opponents(self), Filters.participatingInBattle)) {
                    @Override
                    protected void cardsTargeted(int targetGroupId, final Collection<PhysicalCard> targetedCards) {
                        action.allowResponses("Prevent "+GameUtils.getAppendedNames(targetedCards)+" from moving", new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                action.appendEffect(
                                        new AddUntilEndOfTurnModifierEffect(action, new MayNotMoveModifier(self, Filters.in(targetedCards)), "Prevents "+GameUtils.getAppendedNames(targetedCards)+" from moving"));
                            }
                        });
                    }

                    @Override
                    protected boolean getUseShortcut() {
                        return true;
                    }
                });

                return Collections.singletonList(action);

            }
        }

        return null;
    }
}