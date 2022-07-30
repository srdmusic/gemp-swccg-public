package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddDestinyToTotalPowerEffect;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Dark Approach (V)
 */
public class Card501_020 extends AbstractUsedInterrupt {
    public Card501_020() {
        super(Side.LIGHT, 4, "Dark Approach", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("'The Force is with you, young Skywalker. But you are not a Jedi yet.'");
        setGameText("If opponent just retrieved Force, opponent must lose 1 Force. OR Cancel Dark Strike, Stunning Leader or You Are Beaten. OR If your Skywalker defending a battle alone, add one destiny to your total power (if [CC] Luke, he is also immune to attrition).");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_20);
        setTestingText("~Dark Approach (V)");
    }


    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Dark_Strike)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Dark_Strike, Title.Dark_Strike);
            actions.add(action);
        }
        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Stunning_Leader)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Stunning_Leader, Title.Stunning_Leader);
            actions.add(action);
        }
        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.You_Are_Beaten)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.You_Are_Beaten, Title.You_Are_Beaten);
            actions.add(action);
        }


        Filter targetFilter = Filters.and(Filters.your(self), Filters.Skywalker, Filters.alone, Filters.defendingBattle);

        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, targetFilter)
                && GameConditions.canAddDestinyDrawsToPower(game, playerId)
                && GameConditions.canTarget(game, self, targetFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Add one destiny to total power");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose Skywalker", targetFilter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            String msgText = "Add one destiny to total power";
                            if (Filters.and(Icon.CLOUD_CITY, Filters.Luke).accepts(game, targetedCard)) {
                                msgText += (" and make " + GameUtils.getCardLink(targetedCard) + " immune to attrition");
                            }
                            // Allow response(s)
                            action.allowResponses(msgText,
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new AddDestinyToTotalPowerEffect(action, 1));
                                            if (Filters.and(Icon.CLOUD_CITY, Filters.Luke).accepts(game, finalTarget)) {
                                                action.appendEffect(
                                                        new AddUntilEndOfBattleModifierEffect(action,
                                                                new ImmuneToAttritionModifier(self, finalTarget),
                                                                "Makes " + GameUtils.getCardLink(finalTarget) + " immune to attrition"));
                                            }
                                        }
                                    }
                            );
                        }
                        @Override
                        protected boolean getUseShortcut() {
                            return true;
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Dark_Strike, Filters.Stunning_Leader, Filters.You_Are_Beaten))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        final String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.justRetrievedForce(game, effectResult, opponent)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Opponent loses 1 Force");
            // Allow response(s)
            action.allowResponses("Opponent loses 1 Force",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(new LoseForceEffect(action, opponent, 1));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}