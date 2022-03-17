package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddBattleDestinyEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.CancelCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Lost
 * Title: Skywalkers (V)
 */
public class Card501_068 extends AbstractLostInterrupt {
    public Card501_068() {
        super(Side.LIGHT, 5, Title.Skywalkers, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Luke and Leia escaped to an unused portion of the Death Star, evading security checkpoints. At a retracted bridge, they swung across on a grappling line through enemy fire.");
        setGameText("If your [Skywalker] Objective on table, add X to your just drawn weapon or battle destiny, where X = the number of Skywalkers on table. OR If targeting your Skywalker, cancel Dark Strike, Imperial Barrier, or You Are Beaten.");
        addIcons(Icon.SKYWALKER, Icon.VIRTUAL_SET_18);
        setTestingText("Skywalkers (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.and(Filters.your(self), Icon.SKYWALKER, Filters.Objective))
            && (TriggerConditions.isWeaponDestinyJustDrawnBy(game, effectResult, playerId)
                || TriggerConditions.isBattleDestinyJustDrawnBy(game, effectResult, playerId))) {

            final int skywalkerCount = Filters.countActive(game, self, Filters.Skywalker);
            if(skywalkerCount>0) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
                action.setText("Add "+skywalkerCount+" to destiny");
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new ModifyDestinyEffect(action, skywalkerCount));
                            }
                        }
                );
                actions.add(action);
            }
        }
        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        Filter cardToCancelFilter = Filters.and(Filters.or(Filters.Dark_Strike, Filters.Imperial_Barrier, Filters.You_Are_Beaten), Filters.cardTargeting(self, Filters.and(Filters.your(self), Filters.Skywalker)));

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, cardToCancelFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            action.setText("Cancel Dark Strike, Imperial Barrier, or You Are Beaten");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose card to cancel", TargetingReason.TO_BE_CANCELED, cardToCancelFilter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId1, PhysicalCard cardToCancelTargeted) {
                            action.addAnimationGroup(cardToCancelTargeted);
                            // Allow response(s)
                            action.allowResponses("Cancel " + GameUtils.getCardLink(cardToCancelTargeted),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the final targeted card(s)
                                            PhysicalCard finalCardToCancel = targetingAction.getPrimaryTargetCard(targetGroupId1);
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new CancelCardOnTableEffect(action, finalCardToCancel));
                                        }
                                    });
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self) {
        Filter filter = Filters.or(Filters.Dark_Strike, Filters.Imperial_Barrier, Filters.You_Are_Beaten);

        // Check condition(s)
        if (TriggerConditions.isPlayingCardTargeting(game, effect, filter, Filters.and(Filters.your(self), Filters.Skywalker))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }
}