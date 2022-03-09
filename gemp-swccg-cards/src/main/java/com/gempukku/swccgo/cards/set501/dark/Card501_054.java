package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddDuelDestinyEffect;
import com.gempukku.swccgo.cards.effects.CancelWeaponTargetingEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.NumLightsaberCombatDestinyDrawsModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Surely You Can Do Better
 */
public class Card501_054 extends AbstractUsedOrLostInterrupt {
    public Card501_054() {
        super(Side.DARK, 4, "Surely You Can Do Better", Uniqueness.UNIQUE);
        setGameText("USED: During battle, target a character present with [Set 13] Dooku. Target is power -3. LOST: Cancel Clash Of Sabers or Projection Of A Skywalker. OR If lightsaber combat was just initiated, lose 1 force (free if involving [Set 13] Dooku) to add one destiny to your total.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_18);
        setTestingText("Surely You Can Do Better");
    }


    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();
        Filter targetFilter = Filters.and(Filters.character, Filters.presentWith(self, Filters.and(Icon.VIRTUAL_SET_13, Filters.Dooku)), Filters.canBeTargetedBy(self));
        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, targetFilter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Reduce power of a character");
            action.setActionMsg("Make a character present with [Set 13] Dooku power -3");
            // Allow response(s)
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target a character to be power -3", targetFilter) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.allowResponses(
                            new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                    // Perform result(s)
                                    action.appendEffect(
                                            new ModifyPowerUntilEndOfBattleEffect(action, finalTarget, -3));
                                }
                            }
                    );
                }
            });
            actions.add(action);
        }

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Clash_Of_Sabers)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Clash_Of_Sabers, Title.Clash_Of_Sabers);
            actions.add(action);
        }

        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.title(Title.Projection_Of_A_Skywalker))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.title(Title.Projection_Of_A_Skywalker), Title.Projection_Of_A_Skywalker);
            actions.add(action);
        }

        return actions;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Clash_Of_Sabers, Filters.title(Title.Projection_Of_A_Skywalker)))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.lightsaberCombatInitiated(game, effectResult)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Add one destiny");

            if (!GameConditions.isDuringLightsaberCombatWithParticipant(game, Filters.and(Icon.VIRTUAL_SET_13, Filters.Dooku)))
                action.appendCost(new LoseForceEffect(action, playerId, 1));

            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddUntilEndOfLightsaberCombatModifierEffect(action,
                                            new NumLightsaberCombatDestinyDrawsModifier(self, 1, playerId),
                                            "Add destiny"));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}