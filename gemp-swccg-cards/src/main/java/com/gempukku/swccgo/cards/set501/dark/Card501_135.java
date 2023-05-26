package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.CancelCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotResetTotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Lost
 * Title: Hutt Smooch (V)
 */
public class Card501_135 extends AbstractLostInterrupt {
    public Card501_135() {
        super(Side.DARK, 2, Title.Hutt_Smooch, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("'We have powerful friends. You're gonna regret this.' 'Rota go ma namatota.'");
        setGameText("During battle, add one battle destiny. For remainder of turn, opponent may not reset your total battle destiny or reduce your total ability. (If a gangster or guard in battle, this is a Used Interrupt.) OR Cancel A Gift if deployed on a droid.");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_21);
        setTestingText("Hutt Smooch (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.isDuringBattle(game)
                && GameConditions.canAddBattleDestinyDraws(game, self)) {

            final String opponent = game.getOpponent(playerId);

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Add one battle destiny");

            if (GameConditions.isDuringBattleWithParticipant(game, Filters.or(Filters.gangster, Filters.guard)))
                action.setPlayedAsSubtype(CardSubtype.USED);

            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddBattleDestinyEffect(action, 1));

                            //may not reset total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotResetTotalBattleDestinyModifier(self, playerId, opponent),"Prevents "+opponent+" from resetting your total battle destiny"));
                        }
                    }
            );
            actions.add(action);
        }


        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, SpotOverride.INCLUDE_UNDERCOVER, Filters.and(Filters.A_Gift, Filters.attachedTo(Filters.droid)))) {
            final PhysicalCard agift = Filters.findFirstActive(game, self, SpotOverride.INCLUDE_UNDERCOVER, TargetingReason.TO_BE_CANCELED, Filters.and(Filters.A_Gift, Filters.attachedTo(Filters.droid)));

                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
                action.setText("Cancel A Gift");
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new CancelCardOnTableEffect(action, agift));
                            }
                        }
                );
                actions.add(action);
        }
        return actions;
    }
}