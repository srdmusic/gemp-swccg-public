package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceDrainEffect;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.AbstractActionProxy;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TriggerAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.PassthruEffect;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Virtual Set 17
 * Type: Interrupt
 * Subtype: Lost
 * Title: Ominous Approach
 */
public class Card501_027 extends AbstractLostInterrupt {
    public Card501_027() {
        super(Side.DARK, 4, "Ominous Approach");
        setGameText("If your AT-AT is on Hoth or at opponent’s site: Cancel Under Attack. (Immune to Sense) OR Return an Effect that deploys on related system to owner's hand. Opponent loses 2 force if they deploy a card with the same title this turn. OR Cancel a Force drain at a related site.");
        addIcons(Icon.VIRTUAL_SET_17);
        setTestingText("Ominous Approach");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.and(Filters.your(self), Filters.AT_AT, Filters.or(Filters.on(Title.Hoth), Filters.at(Filters.and(Filters.opponents(self), Filters.site)))))
                && TriggerConditions.isPlayingCard(game, effect, Filters.Under_Attack)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setImmuneTo(Title.Sense);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            actions.add(action);
        }

        return actions;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        Filter filter = Filters.and(Filters.Effect, Filters.attachedTo(Filters.relatedSystemTo(self, Filters.and(Filters.your(self), Filters.AT_AT, Filters.or(Filters.on(Title.Hoth), Filters.at(Filters.and(Filters.opponents(self), Filters.site)))))));

        final String opponent = game.getOpponent(playerId);
        // Check condition(s)
        if (GameConditions.canTarget(game, self, filter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Return Effect to hand");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose Effect", filter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Return " + GameUtils.getCardLink(targetedCard) + " to hand",
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                            action.appendEffect(
                                                    new ReturnCardToHandFromTableEffect(action, finalTarget));

                                            final int permCardId = self.getPermanentCardId();
                                            final int gameTextSourceCardId = self.getCardId();
                                            action.appendEffect(
                                                    new AddUntilEndOfTurnActionProxyEffect(action, new AbstractActionProxy() {
                                                        @Override
                                                        public List<TriggerAction> getRequiredAfterTriggers(SwccgGame game, EffectResult effectResult) {
                                                            List<TriggerAction> actions = new LinkedList<>();
                                                            final PhysicalCard self = game.findCardByPermanentId(permCardId);

                                                            if (TriggerConditions.justDeployed(game, effectResult, opponent, Filters.sameTitle(finalTarget))) {

                                                                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                                                                action.setText("Lose 2 Force");
                                                                action.appendEffect(
                                                                        new LoseForceEffect(action, opponent, 2));
                                                               actions.add(action);
                                                            }

                                                            return actions;
                                                        }
                                                    }));
                                        }
                                    }
                            );
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.forceDrainInitiatedAt(game, effectResult, Filters.relatedSiteTo(self, Filters.and(Filters.your(self), Filters.AT_AT, Filters.or(Filters.on(Title.Hoth), Filters.at(Filters.and(Filters.opponents(self), Filters.site))))))
                && GameConditions.canCancelForceDrain(game, self)) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Cancel Force drain");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new CancelForceDrainEffect(action));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}
