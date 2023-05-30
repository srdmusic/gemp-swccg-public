package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveTotalAbilityReducedModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotResetTotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
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
        setGameText("During battle, add one battle destiny. For remainder of turn, opponent may not reset your total battle destiny or reduce your total ability. OR Once per game, if Jabba, Bib, or your guard in battle, cancel a non-[Immune to Sense] Interrupt.");
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

                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotHaveTotalAbilityReducedModifier(self, Filters.location, playerId),"Prevents your total ability from being reduced"));
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self) {
        GameTextActionId gameTextActionId = GameTextActionId.HUTT_SMOOCH_V__CANCEL_INTERRUPT;

        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, Filters.or(Filters.Bib, Filters.Jabba, Filters.and(Filters.your(self), Filters.guard)))
                && TriggerConditions.isPlayingCard(game, effect, Filters.and(Filters.Interrupt, Filters.not(Filters.immuneToCardTitle(Title.Sense))))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            action.appendUsage(
                    new OncePerGameEffect(action));
            return Collections.singletonList(action);
        }
        return null;
    }
}