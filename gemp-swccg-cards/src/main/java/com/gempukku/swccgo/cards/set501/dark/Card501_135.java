package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.DestinyType;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleActionProxyEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.ModifyTotalPowerUntilEndOfBattleEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.modifiers.AttritionModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotResetTotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
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
        setGameText("During battle, add one battle destiny. For remainder of turn, opponent may not reset your total battle destiny. If your gangster or guard in battle, choose: your total power is +2 OR If opponent draws a destiny to power or attrition this battle, attrition against you is -2.");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_21);
        setTestingText("Hutt Smooch (V)");
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

                            //may not reset total battle destiny
                            action.appendEffect(new AddUntilEndOfTurnModifierEffect(action,
                                    new MayNotResetTotalBattleDestinyModifier(self, playerId, opponent),"Prevents "+opponent+" from resetting your total battle destiny"));

                            if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.your(self), Filters.or(Filters.gangster, Filters.guard)))) {
                                action.appendEffect(new PlayoutDecisionEffect(action, playerId, new MultipleChoiceAwaitingDecision("Choose", new String[]{"Add 2 to total power", "Subtract 2 from attrition if opponent draws a destiny to power or attrition this battle"}) {
                                    @Override
                                    protected void validDecisionMade(int index, String result) {
                                        if (index==0) {
                                            action.appendEffect(new ModifyTotalPowerUntilEndOfBattleEffect(action, 2, playerId, "Adds 2 to total power"));
                                        } else {
                                            final int permCardId = self.getPermanentCardId();
                                            action.appendEffect(new AddUntilEndOfBattleActionProxyEffect(action, new AbstractActionProxy() {
                                                @Override
                                                public List<TriggerAction> getRequiredAfterTriggers(SwccgGame game, EffectResult effectResult) {
                                                    final PhysicalCard self = game.findCardByPermanentId(permCardId);

                                                    GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

                                                    if (TriggerConditions.isDestinyJustDrawnBy(game, effectResult, opponent)
                                                            && (TriggerConditions.isDestinyDrawType(game, effectResult, DestinyType.DESTINY_TO_TOTAL_POWER)
                                                            || TriggerConditions.isDestinyDrawType(game, effectResult, DestinyType.DESTINY_TO_ATTRITION))
                                                            && GameConditions.isOncePerBattle(game, self, self.getCardId(), gameTextActionId)) {

                                                        RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, self.getCardId(), gameTextActionId);
                                                        action.setText("Subtract 2 from attrition");

                                                        action.addAnimationGroup(self);

                                                        action.appendUsage(
                                                                new OncePerBattleEffect(action));
                                                        action.appendEffect(new AddUntilEndOfBattleModifierEffect(action, new AttritionModifier(self, -2, playerId),
                                                                "Subtracts 2 from attrition against " + playerId));

                                                        return Collections.singletonList((TriggerAction) action);
                                                    }

                                                    return null;
                                                }
                                            }));
                                        }
                                    }
                                }));
                                action.appendEffect(new AddUntilEndOfBattleModifierEffect(action,
                                        new AttritionModifier(self, -2, playerId), "Subtracts 2 from attrition against you"));
                            }

                        }
                    }
            );
            return Collections.singletonList(action);
        }

        return null;
    }
}