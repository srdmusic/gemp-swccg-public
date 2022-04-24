package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.ModifyEachWeaponDestinyBeforeDrawingDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Lightsaber Deficiency (V)
 */
public class Card501_007 extends AbstractUsedOrLostInterrupt {
    public Card501_007() {
        super(Side.DARK, 5, "Lightsaber Deficiency");
        setVirtualSuffix(true);
        setLore("'Ah...Uh...'");
        setGameText("USED: If your character of ability < 5 was just targeted by opponent's lightsaber, subtract 1 from each weapon destiny draw. LOST: Lose 1 Force to cancel Clash Of Sabers (unless canceling Presence Of The Force) or Blaster Proficiency (if targeting a lightsaber or while swinging a lightsaber).");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_19);
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
        if (GameConditions.canTargetToCancel(game, self, Filters.Blaster_Proficiency)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardAction(action, Filters.Blaster_Proficiency, Title.Blaster_Proficiency);
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(String playerId, SwccgGame game, Effect effect, PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

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
        if (TriggerConditions.isPlayingCard(game, effect, Filters.Blaster_Proficiency)
                && (TriggerConditions.isPlayingCardTargeting(game, effect, Filters.Blaster_Proficiency, Filters.or(Filters.lightsaber, Filters.hasPermanentWeapon(Filters.lightsaber)))
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
}