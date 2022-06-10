package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.complete.ChooseExistingCardPileEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.StealCardIntoHandFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 19
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: My Sister Has It
 */
public class Card501_061 extends AbstractUsedOrLostInterrupt {
    public Card501_061() {
        super(Side.LIGHT, 4, "My Sister Has It", Uniqueness.UNIQUE);
        setGameText("USED: If a [Skywalker] Epic Event on table, take a lightsaber or [Endor] Leia into hand from Reserve Deck; reshuffle. OR If [Endor] Leia on table, add or subtract 1 from a just drawn weapon or battle destiny. LOST: Search a Lost Pile; take your lightsaber or a 'stolen' lightsaber into hand.");
        addIcons(Icon.VIRTUAL_SET_19);
        setTestingText("My Sister Has It");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.MY_SISTER_HAS_IT__UPLOAD_LIGHTSABER_OR_LEIA;

        // Check condition(s)
        if (GameConditions.canTarget(game, self, Filters.and(Icon.SKYWALKER, Filters.Epic_Event))
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Take a lightsaber or Leia into hand");

            // Allow response(s)
            action.allowResponses("Take a lightsaber or [Endor] Leia into hand from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.lightsaber, Filters.and(Icon.ENDOR, Filters.Leia)), true));
                        }
                    }
            );
            actions.add(action);
        }


        gameTextActionId = GameTextActionId.MY_SISTER_HAS_IT__SEARCH_LOST_PILE;

        // Check condition(s)
        if (GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)
            || GameConditions.canSearchOpponentsLostPile(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.LOST);
            action.setText("Search a lost pile");

            // Allow response(s)
            action.appendTargeting(new ChooseExistingCardPileEffect(action, playerId, Zone.LOST_PILE) {
                @Override
                protected void pileChosen(final SwccgGame game, final String cardPileOwner, final Zone cardPile) {
                    action.allowResponses("Search a lost pile and take your lightsaber or a stolen lightsaber into hand",
                            new RespondablePlayCardEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    Filter lightsaberFilter = Filters.and(Filters.lightsaber, Filters.or(Filters.your(self), Filters.stolen));
                                    // Perform result(s)
                                    if (cardPileOwner.equals(playerId)) {
                                        action.appendEffect(
                                                new TakeCardIntoHandFromLostPileEffect(action, playerId, lightsaberFilter, false));
                                    } else if (cardPileOwner.equals(game.getOpponent(playerId))) {
                                        action.appendEffect(
                                                new StealCardIntoHandFromLostPileEffect(action, playerId, lightsaberFilter));
                                    }
                                }
                            }
                    );
                }
            });

            actions.add(action);
        }

        return actions;
    }


    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.canTarget(game, self, Filters.and(Icon.ENDOR, Filters.Leia))
                && (TriggerConditions.isWeaponDestinyJustDrawn(game, effectResult)
                || TriggerConditions.isBattleDestinyJustDrawn(game, effectResult))) {

            final PlayInterruptAction action1 = new PlayInterruptAction(game, self, CardSubtype.USED);
            action1.setText("Add 1 to destiny");
            // Allow response(s)
            action1.allowResponses(
                    new RespondablePlayCardEffect(action1) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action1.appendEffect(
                                    new ModifyDestinyEffect(action1, 1));
                        }
                    }
            );
            actions.add(action1);

            final PlayInterruptAction action2 = new PlayInterruptAction(game, self, CardSubtype.USED);
            action2.setText("Subtract 1 from destiny");
            // Allow response(s)
            action2.allowResponses(
                    new RespondablePlayCardEffect(action2) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action2.appendEffect(
                                    new ModifyDestinyEffect(action2, -1));
                        }
                    }
            );
            actions.add(action2);

        }
        return actions;
    }
}
