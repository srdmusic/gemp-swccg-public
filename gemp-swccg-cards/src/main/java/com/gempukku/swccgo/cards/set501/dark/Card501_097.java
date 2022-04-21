package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.CancelForceDrainEffect;
import com.gempukku.swccgo.cards.effects.CancelWeaponTargetingEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Show No Mercy
 */
public class Card501_097 extends AbstractUsedInterrupt {
    public Card501_097() {
        super(Side.DARK, 4, "Show No Mercy", Uniqueness.UNIQUE);
        setGameText("If Insidious Prisoner or your [Set 17] Epic Event on table: Take The Works or a Coruscant battleground site into hand from Reserve Deck; reshuffle. OR If your Dark Jedi controls opponent’s battleground site, cancel a Force drain at the related system.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] Show No Mercy");
    }


    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.SHOW_NO_MERCY__UPLOAD_CORUSCANT_SITE;

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.or(Filters.Insidious_Prisoner, Filters.and(Filters.your(self), Icon.VIRTUAL_SET_17, Filters.Epic_Event)))
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId);
            action.setText("Take a location into hand");
            action.setActionMsg("Take The Works or a Coruscant battleground site into hand from Reserve Deck");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.title("Coruscant: The Works"), Filters.and(Filters.battleground_site, Filters.Coruscant_site)), true));
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.or(Filters.Insidious_Prisoner, Filters.and(Filters.your(self), Icon.VIRTUAL_SET_17, Filters.Objective)))
                && TriggerConditions.forceDrainInitiatedAt(game, effectResult, Filters.relatedSystemTo(self, Filters.and(Filters.opponents(self), Filters.battleground_site, Filters.controlsWith(playerId, self, Filters.and(Filters.your(self), Filters.Dark_Jedi)))))
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