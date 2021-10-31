package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddDuelDestinyEffect;
import com.gempukku.swccgo.cards.effects.CancelWeaponTargetingEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfLightsaberCombatModifierEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.LoseOneForceEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.NumLightsaberCombatDestinyDrawsModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Surely You Can Do Better
 */
public class Card501_054 extends AbstractUsedOrLostInterrupt {
    public Card501_054() {
        super(Side.DARK, 4, "Surely You Can Do Better", Uniqueness.UNIQUE);
        setLore("");
        setGameText("USED: Unless your non-[Episode I] objective on table, take The Works into hand from Reserve deck; reshuffle. OR Cancel an opponent's attempt to target [Set 13] Dooku with a weapon; opponent loses 1 Force. LOST: If lightsaber combat was just initiated, add one destiny to your total.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        setTestingText("Surely You Can Do Better");
    }


    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        GameTextActionId gameTextActionId = GameTextActionId.SURELY_YOU_CAN_DO_BETTER__UPLOAD_THE_WORKS;

        // Check condition(s)
        if (!GameConditions.canSpot(game, self, Filters.and(Filters.your(self), Filters.not(Icon.EPISODE_I), Filters.Objective))
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Take The Works into hand");
            action.setActionMsg("Take The Works into hand from Reserve Deck");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.title("Coruscant: The Works"), true));
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextOptionalBeforeActions(final String playerId, final SwccgGame game, final Effect effect, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        // Check condition(s)
        if (TriggerConditions.isTargetedByWeapon(game, effect, Filters.and(Icon.VIRTUAL_SET_13, Filters.Dooku), Filters.opponents(self))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Cancel weapon targeting");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new CancelWeaponTargetingEffect(action));
                            action.appendEffect(
                                    new LoseForceEffect(action, game.getOpponent(playerId), 1));
                        }
                    }
            );
            actions.add(action);
        }

        return actions;

    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.lightsaberCombatInitiated(game, effectResult)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Add one destiny");
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