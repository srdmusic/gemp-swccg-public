package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceDrainEffect;
import com.gempukku.swccgo.cards.effects.CancelWeaponTargetingEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: That Armor's Too Strong For Blasters
 */
public class Card501_024 extends AbstractUsedOrLostInterrupt {
    public Card501_024() {
        super(Side.DARK, 4, "That Armor's Too Strong For Blasters", Uniqueness.UNIQUE);
        setGameText("USED: If a Hoth site has been 'blown away,' subtract 1 from a just drawn destiny. " +
                "LOST: Cancel an attempt to target your AT-AT with a blaster or a rifle. OR If your piloted AT-AT is on Hoth (or opponent’s site), cancel a Force drain at a related site.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_19);
        setTestingText("That Armor's Too Strong For Blasters");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.canSpotLocation(game, Filters.and(Filters.Hoth_site, Filters.blown_away))
                && TriggerConditions.isDestinyJustDrawn(game, effectResult)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Subtract 1 from destiny");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ModifyDestinyEffect(action, -1));
                        }
                    }
            );
            actions.add(action);

        }

        if (TriggerConditions.forceDrainInitiatedAt(game, effectResult, Filters.relatedSiteTo(self, Filters.and(Filters.your(self), Filters.AT_AT, Filters.or(Filters.on(Title.Hoth), Filters.at(Filters.and(Filters.opponents(self), Filters.site))))))
                && GameConditions.canCancelForceDrain(game, self)) {

            final PlayInterruptAction action2 = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action2.setText("Cancel Force drain");
            // Allow response(s)
            action2.allowResponses(
                    new RespondablePlayCardEffect(action2) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action2.appendEffect(
                                    new CancelForceDrainEffect(action2));
                        }
                    }
            );
            actions.add(action2);
        }
        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(final String playerId, SwccgGame game, final Effect effect, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.isTargetedByWeapon(game, effect, Filters.and(Filters.your(self), Filters.AT_AT), Filters.or(Filters.blaster, Filters.rifle))) {
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Cancel weapon targeting");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new CancelWeaponTargetingEffect(action));
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }
}