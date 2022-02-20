package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.WeaponFiringState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;
import com.gempukku.swccgo.logic.timing.results.EnhanceForceDrainResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Lightsaber Deficiency (V)
 */
public class Card501_007 extends AbstractUsedOrLostInterrupt {
    public Card501_007() {
        super(Side.DARK, 5, "Lightsaber Deficiency");
        setVirtualSuffix(true);
        setLore("'Ah...Uh...'");
        setGameText("USED: If your character of ability < 5 was just targeted by opponent's lightsaber, subtract 1 from each weapon destiny draw. LOST: Lose 1 Force to cancel Clash Of Sabers (unless canceling Presence Of The Force) or Sorry About The Mess (when 'swinging' a lightsaber).");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_18);
        setTestingText("Lightsaber Deficiency (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();
        // Check condition(s)
        if (GameConditions.canTargetToCancel(game, self, Filters.Clash_Of_Sabers)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Clash_Of_Sabers, Title.Clash_Of_Sabers);
            actions.add(action);
        }
        if (GameConditions.canTargetToCancel(game, self, Filters.Sorry_About_The_Mess)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Sorry_About_The_Mess, Title.Sorry_About_The_Mess);
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        if (TriggerConditions.isTargetedByWeapon(game, effect, Filters.and(Filters.your(self), Filters.character, Filters.abilityLessThan(5)), Filters.lightsaber)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Subtract 1 from each weapon destiny draw");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ModifyEachWeaponDestinyBeforeDrawingDestinyEffect(action, -1));
                        }
                    }
            );
            actions.add(action);
        }


        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.Clash_Of_Sabers)
                && !TriggerConditions.isPlayingCardTargeting(game, effect, Filters.Clash_Of_Sabers, TargetingReason.TO_BE_CANCELED, Filters.Presence_Of_The_Force)
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            action.appendCost(new LoseForceEffect(action, playerId, 1, true));
            actions.add(action);
        }
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.Sorry_About_The_Mess)
                && (TriggerConditions.isPlayingCardTargeting(game, effect, Filters.Sorry_About_The_Mess, Filters.lightsaber)
                || GameConditions.isDuringWeaponFiringAtTarget(game, Filters.lightsaber, Filters.any))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            action.appendCost(new LoseForceEffect(action, playerId, 1, true));
            actions.add(action);
        }


        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.forceDrainEnhancedByWeapon(game, effectResult, Filters.and(Filters.lightsaber, Filters.attachedTo(Filters.and(Filters.character, Filters.abilityLessThan(4)))))) {
            PhysicalCard lightsaber = ((EnhanceForceDrainResult) effectResult).getWeapon();
            PhysicalCard character = ((EnhanceForceDrainResult) effectResult).getWeapon().getAttachedTo();
            if (GameConditions.canTarget(game, self, TargetingReason.TO_BE_LOST, character)) {

                PlayInterruptAction action = getTargetCharacterUsingLightsaber(playerId, game, self, character, lightsaber);
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    private PlayInterruptAction getTargetCharacterUsingLightsaber(final String playerId, SwccgGame game, final PhysicalCard self, final PhysicalCard character, final PhysicalCard lightsaber) {
        final PlayInterruptAction action = new PlayInterruptAction(game, self);
        action.setText("Target character using lightsaber");
        // Choose target(s)
        action.appendTargeting(
                new TargetCardOnTableEffect(action, playerId, "Choose character", TargetingReason.TO_BE_LOST, character) {
                    @Override
                    protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                        // Allow response(s)
                        action.allowResponses("Target " + GameUtils.getCardLink(targetedCard) + " using a lightsaber",
                                new RespondablePlayCardEffect(action) {
                                    @Override
                                    protected void performActionResults(Action targetingAction) {
                                        // Get the targeted card(s) from the action using the targetGroupId.
                                        // This needs to be done in case the target(s) were changed during the responses.
                                        final PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                        // Perform result(s)
                                        action.appendEffect(
                                                new DrawDestinyEffect(action, playerId) {
                                                    @Override
                                                    protected Collection<PhysicalCard> getGameTextAbilityManeuverOrDefenseValueTargeted() {
                                                        return Collections.singletonList(finalTarget);
                                                    }
                                                    @Override
                                                    protected void destinyDraws(SwccgGame game, List<PhysicalCard> destinyCardDraws, List<Float> destinyDrawValues, Float totalDestiny) {
                                                        GameState gameState = game.getGameState();
                                                        if (totalDestiny == null) {
                                                            gameState.sendMessage("Result: Failed due to failed destiny draw");
                                                            return;
                                                        }

                                                        float ability = game.getModifiersQuerying().getAbility(game.getGameState(), finalTarget);
                                                        gameState.sendMessage("Destiny: " + GuiUtils.formatAsString(totalDestiny));
                                                        gameState.sendMessage("Ability: " + GuiUtils.formatAsString(ability));

                                                        if (totalDestiny > ability) {
                                                            gameState.sendMessage("Result: Target lost");
                                                            action.appendEffect(
                                                                    new LoseCardFromTableEffect(action, finalTarget));
                                                        }
                                                        else if (totalDestiny == ability
                                                                && Filters.lightsaber.accepts(game, lightsaber)) {
                                                            gameState.sendMessage("Result: Lightsaber lost");
                                                            action.appendEffect(
                                                                    new LoseCardFromTableEffect(action, lightsaber));
                                                        }
                                                        else {
                                                            gameState.sendMessage("Result: Failed");
                                                        }
                                                    }
                                                });
                                    }
                                }
                        );
                    }
                }
        );
        return action;
    }
}