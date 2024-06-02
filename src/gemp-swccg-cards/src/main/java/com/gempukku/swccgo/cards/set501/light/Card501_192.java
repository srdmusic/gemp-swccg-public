package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.DrawsNoMoreThanBattleDestinyEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.LoseForceAndStackFaceDownEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 24
 * Type: Interrupt
 * Subtype: Used
 * Title: Neck And Neck (V)
 */
public class Card501_192 extends AbstractUsedInterrupt {
    public Card501_192() {
        super(Side.LIGHT, 4, Title.Neck_And_Neck, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("When Anakin had finally caught up with Sebulba, he knew he needed to make some kind of a move to break away from the Dug to win the race.");
        setGameText("If [Set 21] Anakin in battle, he is power +1 for each 'credit' card (if any) and opponent may not draw more than one battle destiny. OR If [Set 21] Anakin just won a battle (or Force drained at a battleground), opponent loses 1 Force and stacks lost card under Credits Will Do Fine.");
        setVirtualSuffix(true);
        addIcons(Icon.TATOOINE, Icon.EPISODE_I, Icon.VIRTUAL_SET_24);
        setTestingText("Neck And Neck (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        final String opponent = game.getOpponent(playerId);
        if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.icon(Icon.VIRTUAL_SET_21), Filters.Anakin))) {
            int creditCount = Filters.countStacked(game, Filters.creditCard);

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Make Anakin power + " + creditCount);

            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Target Anakin", Filters.and(Filters.icon(Icon.VIRTUAL_SET_21), Filters.Anakin)) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, final PhysicalCard character) {
                            action.addAnimationGroup(character);
                            // Allow response(s)

                            action.allowResponses(
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            PhysicalCard finalQuiGon = action.getPrimaryTargetCard(targetGroupId);
                                            int finalCreditCount = Filters.countStacked(game, Filters.creditCard);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new AddUntilEndOfBattleModifierEffect(action, new PowerModifier(self, finalQuiGon, finalCreditCount), "Makes " + GameUtils.getCardLink(finalQuiGon) + " power +" + finalCreditCount));
                                            action.appendEffect(
                                                    new DrawsNoMoreThanBattleDestinyEffect(action, opponent, 1));
                                        }
                                    }
                            );
                        }

                        @Override
                        protected boolean getUseShortcut() {
                            return true;
                        }
                    });

            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        Filter set21Anakin = Filters.and(Filters.icon(Icon.VIRTUAL_SET_21), Filters.Anakin);
        if (GameConditions.canSpot(game, self, Filters.Credits_Will_Do_Fine)
                && (TriggerConditions.wonBattle(game, effectResult, set21Anakin)
                    || TriggerConditions.forceDrainCompleted(game, effectResult, Filters.sameLocationAs(self, set21Anakin)))) {
            final String opponent = game.getOpponent(playerId);
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            PhysicalCard creditsWillDoFine = Filters.findFirstActive(game, self, Filters.Credits_Will_Do_Fine);
            action.setText("Make opponent lose 1 Force");
            action.setActionMsg("Make opponent lose 1 Force and stack lost card face down on " + GameUtils.getCardLink(creditsWillDoFine));
            // Perform result(s)
            action.appendEffect(
                    new LoseForceAndStackFaceDownEffect(action, opponent, 1, creditsWillDoFine) {
                        @Override
                        public boolean isShownIfLostFromHand() {
                            return true;
                        }
                    });
            return Collections.singletonList(action);
        }
        return null;
    }
}