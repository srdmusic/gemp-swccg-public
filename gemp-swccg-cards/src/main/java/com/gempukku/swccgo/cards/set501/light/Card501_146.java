package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.ResetDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotModifyTotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.DestinyDrawnResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Lost
 * Title: Courage Of A Skywalker (V)
 */
public class Card501_146 extends AbstractLostInterrupt {
    public Card501_146() {
        super(Side.LIGHT, 2, Title.Courage_Of_A_Skywalker, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Despite being alone, trapped and desperately outmatched, Luke continued his battle with the Dark Lord of the Sith.");
        setGameText("During battle, add one battle destiny. For remainder of turn, opponent may not modify their total battle destiny where they have a leader. OR If a Skywalker in battle, reset opponent’s second battle destiny draw to 0.");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_21);
        setTestingText("Courage Of A Skywalker (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        // Check condition(s)
        if (GameConditions.isDuringBattle(game)
                && GameConditions.canAddBattleDestinyDraws(game, self)) {

            final String opponent = game.getOpponent(playerId);

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Add one battle destiny");

            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddBattleDestinyEffect(action, 1));
                            //may not modify total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotModifyTotalBattleDestinyModifier(self, Filters.sameLocationAs(self, Filters.and(Filters.opponents(self), Filters.leader)), opponent, opponent),"Prevents "+opponent+" from modifying their total battle destiny where they have a leader"));
                        }
                    }
            );
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.isBattleDestinyJustDrawnBy(game, effectResult, game.getOpponent(playerId))
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Skywalker)) {
            DestinyDrawnResult destinyDrawnResult = (DestinyDrawnResult) effectResult;
            if (destinyDrawnResult.getNumDestinyDrawnSoFar() == 2) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self);
                action.setText("Reset destiny to 0");
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new ResetDestinyEffect(action, 0));
                            }
                        }
                );
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}